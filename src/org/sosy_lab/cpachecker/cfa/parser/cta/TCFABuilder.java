// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Optional;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression.Operator;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAExitNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.BooleanCondition;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.BooleanCondition.NumericVariableExpression;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.BooleanCondition.ParametricVariableExpression;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.BooleanCondition.VariableExpression;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.ModuleInstantiation;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.ModuleSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.StateSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.SystemSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.TransitionSpecification;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.VariableSpecification.VariableType;
import org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification.VariableSpecification.VariableVisibility;

class TCFABuilder {
  private SystemSpecification systemSpec;
  private ModuleSpecification moduleSpec;
  private ModuleInstantiation instantiation;
  private Map<String, Number> instantiatedConstantInitializations;
  private Set<String> uninstantiatedLocalVariableNames;
  private String moduleInstanceName;

  private Map<String, CFANode> nodesByName;
  private Map<String, TaVariable> variablesByName;
  private TaDeclaration moduleDeclaration;

  private SortedSetMultimap<String, CFANode> nodesByAutomaton;
  private NavigableMap<String, FunctionEntryNode> entryNodesByAutomaton;

  public void instantiateSpecification(SystemSpecification pSpecification) {
    instantiateSpecification(pSpecification, new HashMap<>(), new HashMap<>());
  }

  private void instantiateSpecification(
      SystemSpecification pSpecification,
      Map<String, Number> pConstantInitializations,
      Map<String, TaVariable> pVariables) {
    init(pSpecification, pConstantInitializations, pVariables);

    var localVariables =
        moduleSpec.variables.stream()
            .filter(variable -> variable.visibility == VariableVisibility.LOCAL)
            .map(variable -> variable.name)
            .collect(Collectors.toSet());
    var instantiatedLocalVariables =
        Sets.intersection(localVariables, instantiation.variableMappings.keySet());
    checkArgument(
        instantiatedLocalVariables.isEmpty(),
        "Cannot instantiate local variable(s) %s for instantiated module %s",
        String.join(", ", instantiatedLocalVariables),
        moduleInstanceName);

    var inputVariables =
        moduleSpec.variables.stream()
            .filter(variable -> variable.visibility == VariableVisibility.INPUT)
            .map(variable -> variable.name)
            .collect(Collectors.toSet());
    var uninstantiatedInputVariables =
        Sets.difference(inputVariables, instantiation.variableMappings.keySet());
    checkArgument(
        uninstantiatedInputVariables.isEmpty(),
        "Uninstantiated input variable(s) %s for instantiated module %s",
        String.join(", ", uninstantiatedInputVariables),
        moduleInstanceName);

    createFunctionDeclaration();
    createAutomaton();
    createSubInstantiations();
    addToResult();
  }

  public SortedSetMultimap<String, CFANode> getNodesByAutomatonResult() {
    return nodesByAutomaton;
  }

  public NavigableMap<String, FunctionEntryNode> getEntryNodesByAutomatoResult() {
    return entryNodesByAutomaton;
  }

  private void init(
      SystemSpecification pSpecification,
      Map<String, Number> pConstantInitializations,
      Map<String, TaVariable> pVariables) {
    nodesByName = new HashMap<>();
    variablesByName = pVariables;
    nodesByAutomaton = TreeMultimap.create();
    entryNodesByAutomaton = new TreeMap<>();

    systemSpec = pSpecification;
    instantiation = pSpecification.instantiation;
    moduleInstanceName = instantiation.instanceName;
    moduleSpec =
        pSpecification.modules.stream()
            .filter(spec -> spec.moduleName.equals(instantiation.specificationName))
            .findFirst()
            .orElseThrow();
    instantiatedConstantInitializations = new HashMap<>(pConstantInitializations);

    uninstantiatedLocalVariableNames =
        moduleSpec.variables.stream()
            .filter(variable -> variable.visibility == VariableVisibility.LOCAL)
            .map(variable -> variable.name)
            .collect(Collectors.toSet());
  }

  private void createFunctionDeclaration() {
    var variablesByType =
        moduleSpec.variables.stream().collect(Collectors.groupingBy(variable -> variable.type));

    var clockVariables =
        variablesByType.getOrDefault(VariableType.CLOCK, ImmutableList.of()).stream()
            .map(variable -> getOrCreateVariable(variable.name))
            .collect(Collectors.toSet());
    var actionVariables =
        variablesByType.getOrDefault(VariableType.SYNC, ImmutableList.of()).stream()
            .map(variable -> getOrCreateVariable(variable.name))
            .collect(Collectors.toSet());
    moduleDeclaration =
        new TaDeclaration(FileLocation.DUMMY, moduleInstanceName, clockVariables, actionVariables);

    variablesByType
        .getOrDefault(VariableType.CONST, new ArrayList<>())
        .forEach(
            variable -> {
              if (variable.initialization.isPresent()) {
                setConstantInitialization(variable.name, variable.initialization.get());
              }
            });
  }

  private void createAutomaton() {
    if (!moduleSpec.automaton.isPresent()) {
      return;
    }

    var automatonSpec = moduleSpec.automaton.get();

    automatonSpec.stateSpecifications.forEach(this::createNode);
    automatonSpec.transitions.forEach(this::createTransition);
    addDummyNodes();
  }

  private void createNode(StateSpecification specification) {
    var stateName = specification.name;
    Optional<TaVariableCondition> invariant =
        specification.invariant.transform(this::createVariableCondition);

    var result =
        new TCFANode(
            stateName,
            invariant,
            moduleDeclaration,
            specification.isInitialState,
            specification.isTargetState);
    nodesByName.put(stateName, result);
  }

  private void createTransition(TransitionSpecification specification) {
    verify(
        nodesByName.containsKey(specification.source),
        "Source state %s is unknown in instantiated module %s",
        specification.source,
        moduleInstanceName);
    verify(
        nodesByName.containsKey(specification.target),
        "Target state %s is unknown in transition from %s in instantiated module %s",
        specification.target,
        specification.source,
        moduleInstanceName);

    var resetClocks =
        specification.resetClocks.stream()
            .map(this::getOrCreateVariable)
            .collect(Collectors.toSet());
    verify(
        moduleDeclaration.getClocks().containsAll(resetClocks),
        "Undeclared clocks are reset in transition %s->%s in instantiated module %s",
        specification.source,
        specification.target,
        moduleInstanceName);

    var action = specification.syncMark.transform(this::getOrCreateVariable);
    if (action.isPresent()) {
      verify(
          moduleDeclaration.getActions().contains(action.get()),
          "Undeclared action %s on transition %s->%s in instantiated module %s",
          specification.syncMark.get(),
          specification.source,
          specification.target,
          moduleInstanceName);
    }

    Optional<TaVariableCondition> guard =
        specification.guard.transform(this::createVariableCondition);
    TCFANode source = (TCFANode) nodesByName.get(specification.source);
    TCFANode target = (TCFANode) nodesByName.get(specification.target);

    var edge =
        new TCFAEdge(
            FileLocation.DUMMY, source, target, guard, resetClocks, action, moduleDeclaration);
    source.addLeavingEdge(edge);
    target.addEnteringEdge(edge);
  }

  private TaVariableCondition createVariableCondition(BooleanCondition specification) {
    List<TaVariableExpression> expressions =
        specification.expressions.stream()
            .map(this::createVariableExpression)
            .collect(Collectors.toList());
    return new TaVariableCondition(expressions);
  }

  private TaVariableExpression createVariableExpression(VariableExpression varExpr) {
    var instantiatedVariable = getOrCreateVariable(varExpr.variableName);
    verify(
        moduleDeclaration.getClocks().contains(instantiatedVariable),
        "Undeclared variable %s (uninstantiated name: %s) in instantiated module %s",
        instantiatedVariable.getName(),
        varExpr.variableName,
        moduleInstanceName);

    var operator = convertOperatorType(varExpr.operator);

    Number constant;
    if (varExpr instanceof NumericVariableExpression) {
      constant = ((NumericVariableExpression) varExpr).constant;
    } else {
      var constantVariableName = ((ParametricVariableExpression) varExpr).constant;
      constant = getConstantInitialization(constantVariableName);
    }
    return new TaVariableExpression(instantiatedVariable, operator, constant);
  }

  private static TaVariableExpression.Operator convertOperatorType(BooleanCondition.Operator op) {
    switch (op) {
      case LESS:
        return Operator.LESS;
      case LESS_EQUAL:
        return Operator.LESS_EQUAL;
      case GREATER:
        return Operator.GREATER;
      case GREATER_EQUAL:
        return Operator.GREATER_EQUAL;
      case EQUAL:
        return Operator.EQUAL;
      default:
        throw new VerifyException("Unknown binary operator " + op);
    }
  }

  private void addDummyNodes() {
    // unique entry and exit nodes are required for CFAs:
    var exitNode = new TCFAExitNode(moduleDeclaration);
    var entryNode = new TCFAEntryNode(FileLocation.DUMMY, exitNode, moduleDeclaration);
    exitNode.setEntryNode(entryNode);

    var edge = new BlankEdge("", FileLocation.DUMMY, entryNode, exitNode, "dummy edge");
    entryNode.addLeavingEdge(edge);
    exitNode.addEnteringEdge(edge);

    // add edges from entry node to all initial states
    for (var node : nodesByName.values()) {
      var tcfaNode = (TCFANode) node;
      if (tcfaNode.isInitialState()) {
        edge = new BlankEdge("", FileLocation.DUMMY, entryNode, node, "initial dummy edge");
        entryNode.addLeavingEdge(edge);
        node.addEnteringEdge(edge);
      }
    }

    nodesByName.put("__#entry", entryNode);
    nodesByName.put("__#exit", exitNode);
  }

  /**
   * Add the processed nodes to the maps that will be accessible to the client of this class.
   * Modules without an automaton can be ignored.
   */
  private void addToResult() {
    if (!moduleSpec.automaton.isPresent()) {
      return;
    }
    // once the automaton of this module is created, populate nodesByAutomaton and
    // entryNodesByAutomaton
    var automatonName = moduleInstanceName;
    nodesByAutomaton.putAll(automatonName, nodesByName.values());

    var entryNodes =
        nodesByName.values().stream()
            .filter(node -> node instanceof TCFAEntryNode)
            .collect(Collectors.toSet());
    verify(
        entryNodes.size() == 1,
        "Expected automaton to have 1 entry node but found %s in instantiated module %s",
        entryNodes.size(),
        automatonName);
    entryNodesByAutomaton.put(moduleInstanceName, (FunctionEntryNode) entryNodes.iterator().next());
  }

  private void createSubInstantiations() {
    for (var subInstantiation : moduleSpec.instantiations) {
      // transfer relevant variable mappings
      Map<String, TaVariable> variableInstanceMapping = new HashMap<>();
      for (var mapping : subInstantiation.variableMappings.entrySet()) {
        variableInstanceMapping.put(mapping.getKey(), getOrCreateVariable(mapping.getValue()));
      }

      // qualify instance name
      var qualifiedInstanceName = subInstantiation.instanceName;
      if (!moduleSpec.isRoot) {
        qualifiedInstanceName = moduleInstanceName + "-" + qualifiedInstanceName;
      }
      var updatedSubInstantiation =
          ModuleInstantiation.Builder.copy(subInstantiation)
              .instanceName(qualifiedInstanceName)
              .build();

      var constantInits = new HashMap<>(instantiatedConstantInitializations);
      var tcfaBuilder = new TCFABuilder();

      var updatedSystemSpecification =
          new SystemSpecification.Builder()
              .modules(systemSpec.modules)
              .instantiation(updatedSubInstantiation)
              .build();

      tcfaBuilder.instantiateSpecification(
          updatedSystemSpecification, constantInits, variableInstanceMapping);

      var entryNodeResults = tcfaBuilder.getEntryNodesByAutomatoResult();
      var nodeResults = tcfaBuilder.getNodesByAutomatonResult();

      verify(
          !entryNodeResults.containsKey(moduleInstanceName)
              && !nodeResults.containsKey(moduleInstanceName),
          "Cannot instantiate more than one module with name %s",
          moduleInstanceName);

      entryNodesByAutomaton.putAll(entryNodeResults);
      nodesByAutomaton.putAll(nodeResults);
    }
  }

  /**
   * Returns a variable of its instantiated variable name. Returned elements are unique for each
   * name.
   */
  private TaVariable getOrCreateVariable(String name) {
    if (!variablesByName.containsKey(name)) {
      var isLocal = !moduleSpec.isRoot && uninstantiatedLocalVariableNames.contains(name);
      var variable = new TaVariable(name, moduleInstanceName, isLocal);
      variablesByName.put(name, variable);
    }

    return variablesByName.get(name);
  }

  private String getInstantiatedVariableName(String uninstantiatedName) {
    return getOrCreateVariable(uninstantiatedName).getName();
  }

  /**
   * Takes the uninstantiated name (i.e. the name as it appears in the module specification) of a
   * constant and stores the provided initialization value.
   */
  private void setConstantInitialization(String uninstantiatedName, Number value) {
    instantiatedConstantInitializations.put(getInstantiatedVariableName(uninstantiatedName), value);
  }

  /**
   * Takes the uninstantiated name (i.e. the name as it appears in the module specification) of a
   * constant and returns the stored initialization value. If no value is stored, an exception is
   * thrown.
   */
  private Number getConstantInitialization(String uninstantiatedName) {
    var instantiatedName = getInstantiatedVariableName(uninstantiatedName);
    verify(
        instantiatedConstantInitializations.containsKey(instantiatedName),
        "Uninitialized constant variable %s (uninstantiated name: %s) in instantiated module %s",
        instantiatedName,
        uninstantiatedName,
        moduleInstanceName);

    return instantiatedConstantInitializations.get(instantiatedName);
  }
}
