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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
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
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

class TCFABuilder {
  private SystemSpecification systemSpec;
  private ModuleSpecification moduleSpec;
  private ModuleInstantiation instantiation;
  private Map<String, Number> constantInitializations;
  private Set<String> uninstantiatedLocalVariableNames;

  private Map<String, CFANode> nodesByName;
  private Map<String, CIdExpression> currentIdExpressionsByVariableName;
  private TaDeclaration moduleDeclaration;

  private SortedSetMultimap<String, CFANode> nodesByAutomaton;
  private NavigableMap<String, FunctionEntryNode> entryNodesByAutomaton;

  public void instantiateSpecification(
      SystemSpecification pSpecification, Map<String, Number> pConstantInitializations) {
    init(pSpecification, pConstantInitializations);

    var localVariables =
        moduleSpec.variables.stream()
            .filter(variable -> variable.visibility == VariableVisibility.LOCAL)
            .map(variable -> variable.name)
            .collect(Collectors.toSet());
    var instantiatedLocalVariables =
        Sets.intersection(localVariables, instantiation.variableMappings.keySet());
    checkArgument(
        instantiatedLocalVariables.isEmpty(),
        "Cannot instantiate local variables "
            + String.join(", ", instantiatedLocalVariables)
            + " for instantiated module "
            + instantiation.instanceName);

    var inputVariables =
        moduleSpec.variables.stream()
            .filter(variable -> variable.visibility == VariableVisibility.INPUT)
            .map(variable -> variable.name)
            .collect(Collectors.toSet());
    var uninstantiatedInputVariables =
        Sets.difference(inputVariables, instantiation.variableMappings.keySet());
    checkArgument(
        uninstantiatedInputVariables.isEmpty(),
        "Uninstantiated input variables "
            + String.join(", ", uninstantiatedInputVariables)
            + " for instantiated module "
            + instantiation.instanceName);

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
      SystemSpecification pSpecification, Map<String, Number> pConstantInitializations) {
    nodesByName = new HashMap<>();
    currentIdExpressionsByVariableName = new HashMap<>();
    nodesByAutomaton = TreeMultimap.create();
    entryNodesByAutomaton = new TreeMap<>();

    systemSpec = pSpecification;
    instantiation = pSpecification.instantiation;
    moduleSpec =
        pSpecification.modules.stream()
            .filter(spec -> spec.moduleName.equals(instantiation.specificationName))
            .findFirst()
            .orElseThrow();
    constantInitializations = new HashMap<>(pConstantInitializations);

    uninstantiatedLocalVariableNames =
        moduleSpec.variables.stream()
            .filter(variable -> variable.visibility == VariableVisibility.LOCAL)
            .map(variable -> variable.name)
            .collect(Collectors.toSet());
  }

  private void createFunctionDeclaration() {
    var moduleName = instantiation.instanceName;
    var variablesByType =
        moduleSpec.variables.stream().collect(Collectors.groupingBy(variable -> variable.type));

    var clockVariableNames =
        variablesByType.getOrDefault(VariableType.CLOCK, ImmutableList.of()).stream()
            .map(variable -> getInstantiatedName(variable.name))
            .collect(Collectors.toSet());
    var actionVariableNames =
        variablesByType.getOrDefault(VariableType.SYNC, ImmutableList.of()).stream()
            .map(variable -> getInstantiatedName(variable.name))
            .collect(Collectors.toSet());
    moduleDeclaration =
        new TaDeclaration(FileLocation.DUMMY, moduleName, clockVariableNames, actionVariableNames);

    variablesByType
        .get(VariableType.CONST)
        .forEach(
            variable -> {
              if (variable.initialization.isPresent()) {
                constantInitializations.put(
                    getInstantiatedName(variable.name), variable.initialization.get());
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
        new TCFANode(stateName, invariant, moduleDeclaration, specification.isInitialState);
    nodesByName.put(stateName, result);
  }

  private void createTransition(TransitionSpecification specification) {
    verify(
        nodesByName.containsKey(specification.source),
        "Source state "
            + specification.source
            + " is unknown in instantiated module "
            + moduleDeclaration.getName());
    verify(
        nodesByName.containsKey(specification.target),
        "Target state "
            + specification.target
            + " is unknown in transition from "
            + specification.source
            + " in instantiated module "
            + moduleDeclaration.getName());

    var instantiatedResetClocks =
        specification.resetClocks.stream()
            .map(this::getInstantiatedName)
            .collect(Collectors.toSet());
    verify(
        moduleDeclaration.getClocks().containsAll(instantiatedResetClocks),
        "Undeclared clocks are reset in transition "
            + specification.source
            + "->"
            + specification.target
            + " in instantiated module "
            + moduleDeclaration.getName());

    var instantiatedAction = specification.syncMark.transform(this::getInstantiatedName);
    if (instantiatedAction.isPresent()) {
      verify(
          moduleDeclaration.getActions().contains(instantiatedAction.get()),
          "Undeclared action on transition "
              + specification.source
              + "->"
              + specification.target
              + " in instantiated module "
              + moduleDeclaration.getName());
    }

    Optional<TaVariableCondition> guard =
        specification.guard.transform(this::createVariableCondition);
    Optional<String> action = specification.syncMark;
    TCFANode source = (TCFANode) nodesByName.get(specification.source);
    TCFANode target = (TCFANode) nodesByName.get(specification.target);

    var edge =
        new TCFAEdge(FileLocation.DUMMY, source, target, guard, instantiatedResetClocks, action);
    source.addLeavingEdge(edge);
    target.addEnteringEdge(edge);
  }

  private TaVariableCondition createVariableCondition(BooleanCondition specification) {
    List<CExpression> expressions =
        specification.expressions.stream()
            .map(this::createVariableExpression)
            .collect(Collectors.toList());
    return new TaVariableCondition(FileLocation.DUMMY, expressions);
  }

  private CExpression createVariableExpression(VariableExpression varExpr) {
    var instantiatedVariable = getInstantiatedName(varExpr.variableName);
    verify(
        moduleDeclaration.getClocks().contains(instantiatedVariable),
        "Undeclared variable "
            + instantiatedVariable
            + " (uninstantiated name: "
            + varExpr.variableName
            + ") in instantiated module "
            + moduleDeclaration.getName());

    var variableIdExpression = createIdExpression(instantiatedVariable);
    var operator = convertOperatorType(varExpr.operator);

    Number constant;
    if (varExpr instanceof NumericVariableExpression) {
      constant = ((NumericVariableExpression) varExpr).constant;
    } else {
      var constantVariableName = ((ParametricVariableExpression) varExpr).constant;
      var instantiatedConstantVariableName = getInstantiatedName(constantVariableName);
      verify(
          constantInitializations.containsKey(instantiatedConstantVariableName),
          "Uninitialized constant variable "
              + instantiatedConstantVariableName
              + " (uninstantiated name: "
              + constantVariableName
              + ") in instantiated module "
              + moduleDeclaration.getName());

      constant = constantInitializations.get(instantiatedConstantVariableName);
    }

    var constantExpression = createFloatLiteralExpression(constant);

    return createBinaryExpression(variableIdExpression, constantExpression, operator);
  }

  private CIdExpression createIdExpression(String name) {
    if (!currentIdExpressionsByVariableName.containsKey(name)) {
      var declaration =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              createFloatType(),
              name,
              name,
              name,
              null);
      var idExpression =
          new CIdExpression(FileLocation.DUMMY, createFloatType(), name, declaration);
      currentIdExpressionsByVariableName.put(name, idExpression);
    }

    return currentIdExpressionsByVariableName.get(name);
  }

  private static CFloatLiteralExpression createFloatLiteralExpression(Number pValue) {
    var value = new BigDecimal(pValue.toString());
    return new CFloatLiteralExpression(FileLocation.DUMMY, createFloatType(), value);
  }

  private static CBinaryExpression createBinaryExpression(
      AExpression op1, AExpression op2, BinaryOperator operator) {
    return new CBinaryExpression(
        FileLocation.DUMMY,
        CNumericTypes.BOOL,
        CNumericTypes.BOOL,
        (CExpression) op1,
        (CExpression) op2,
        operator);
  }

  private static CType createFloatType() {
    return CNumericTypes.LONG_DOUBLE;
  }

  private static BinaryOperator convertOperatorType(BooleanCondition.Operator op) {
    switch (op) {
      case LESS:
        return BinaryOperator.LESS_THAN;
      case LESS_EQUAL:
        return BinaryOperator.LESS_EQUAL;
      case GREATER:
        return BinaryOperator.GREATER_THAN;
      case GREATER_EQUAL:
        return BinaryOperator.GREATER_EQUAL;
      case EQUAL:
        return BinaryOperator.EQUALS;
      default:
        throw new VerifyException("Unknown binary operator " + op);
    }
  }

  private String getInstantiatedName(String variableName) {
    // local variables are prefixed:
    if (moduleSpec.isRoot && uninstantiatedLocalVariableNames.contains(variableName)) {
      return instantiation.instanceName + "::" + variableName;
    }
    // others can be renamed via instantiation:
    return instantiation.variableMappings.getOrDefault(variableName, variableName);
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
    var automatonName = moduleDeclaration.getName();
    nodesByAutomaton.putAll(automatonName, nodesByName.values());

    var entryNodes =
        nodesByName.values().stream()
            .filter(node -> node instanceof TCFAEntryNode)
            .collect(Collectors.toSet());
    verify(
        entryNodes.size() == 1,
        "Expected automaton to have 1 entry node but found "
            + entryNodes.size()
            + " in instantiated module "
            + automatonName);
    entryNodesByAutomaton.put(
        moduleDeclaration.getName(), (FunctionEntryNode) entryNodes.iterator().next());
  }

  private void createSubInstantiations() {
    for (var subInstantiation : moduleSpec.instantiations) {
      var instantiatedInstantiation = instantiateInstantiation(subInstantiation);
      var updatedSystemSpecification =
          new SystemSpecification.Builder()
              .modules(systemSpec.modules)
              .instantiation(instantiatedInstantiation)
              .build();
      var constantInits = new HashMap<>(constantInitializations);
      var tcfaBuilder = new TCFABuilder();

      tcfaBuilder.instantiateSpecification(updatedSystemSpecification, constantInits);

      var entryNodeResults = tcfaBuilder.getEntryNodesByAutomatoResult();
      var nodeResults = tcfaBuilder.getNodesByAutomatonResult();

      verify(
          !entryNodeResults.containsKey(moduleDeclaration.getName())
              && !nodeResults.containsKey(moduleDeclaration.getName()),
          "Cannot instantiate more than one module with name " + moduleDeclaration.getName());

      entryNodesByAutomaton.putAll(entryNodeResults);
      nodesByAutomaton.putAll(nodeResults);
    }
  }

  /**
   * Overwrites instantiated variable names of the given module with instantiated values of current
   * module. E.g. the instatiation maps u to v, but in the current module, v is mapped to w. Then
   * the mapping u to v is replaced by u to w.
   */
  private ModuleInstantiation instantiateInstantiation(ModuleInstantiation subInstantiation) {
    // apply overwriting instantiations
    var instantiationBuilder =
        new ModuleInstantiation.Builder()
            .specificationName(subInstantiation.specificationName)
            .instanceName(subInstantiation.instanceName);

    for (var variableMapping : subInstantiation.variableMappings.entrySet()) {
      var originalVariable = variableMapping.getKey();
      var instantiatedName = getInstantiatedName(variableMapping.getValue());

      instantiationBuilder.variableMapping(originalVariable, instantiatedName);
    }

    return instantiationBuilder.build();
  }
}
