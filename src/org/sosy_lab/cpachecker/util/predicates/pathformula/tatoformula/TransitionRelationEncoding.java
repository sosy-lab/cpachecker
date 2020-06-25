// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.timedautomata.TAUnrollingState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TatoFormulaConverter.VariableType;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

public class TransitionRelationEncoding implements TAFormulaEncoding {
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bFmgr;
  private final int delayActionId;
  private Map<TaDeclaration, Collection<TCFAEdge>> edgesByAutomaton;
  private Map<TaDeclaration, Collection<TCFANode>> nodesByAutomaton;
  private Map<TaDeclaration, Collection<TCFANode>> errorNodesByAutomaton;
  private Map<CFANode, Integer> nodeIds;
  private Map<TaVariable, Integer> actionIds;
  private Map<TaDeclaration, Integer> localActionIds;

  TransitionRelationEncoding(FormulaManagerView pFmgr, CFA pCfa) {
    fmgr = pFmgr;
    bFmgr = fmgr.getBooleanFormulaManager();
    nodeIds = new HashMap<>();
    actionIds = new HashMap<>();
    localActionIds = new HashMap<>();

    var allEdges =
        from(pCfa.getAllNodes())
            .transformAndConcat(node -> CFAUtils.leavingEdges(node))
            .filter(instanceOf(TCFAEdge.class))
            .transform(edge -> (TCFAEdge) edge);
    edgesByAutomaton =
        allEdges.index(edge -> (TaDeclaration) edge.getPredecessor().getFunction()).asMap();
    nodesByAutomaton =
        from(pCfa.getAllNodes())
            .filter(instanceOf(TCFANode.class))
            .transform(node -> (TCFANode) node)
            .index(node -> (TaDeclaration) node.getFunction())
            .asMap();
    errorNodesByAutomaton = new HashMap<>();
    for (var automatonNodes : nodesByAutomaton.entrySet()) {
      var errorStates = from(automatonNodes.getValue()).filter(node -> node.isErrorLocation());
      errorNodesByAutomaton.put(automatonNodes.getKey(), errorStates.toSet());
    }

    var allNodes = from(pCfa.getAllNodes()).filter(instanceOf(TCFANode.class));
    int idx = 0;
    for (var node : allNodes) {
      nodeIds.put(node, ++idx);
    }

    var allActions =
        allEdges
            .filter(edge -> edge.getAction().isPresent())
            .transform(edge -> edge.getAction().get());
    idx = 0;
    for (var action : allActions) {
      actionIds.put(action, ++idx);
    }

    for (var automatonEntry : pCfa.getAllFunctions().values()) {
      if (automatonEntry.getFunction() instanceof TaDeclaration) {
        localActionIds.put((TaDeclaration) automatonEntry.getFunction(), ++idx);
      }
    }

    delayActionId = ++idx;
  }

  @Override
  public BooleanFormula getInitialFormula(CFANode pInitialNode, int pStepCount) {
    var result =
        from(edgesByAutomaton.keySet())
            .transform(automaton -> makeInitialConditionForAutomaton(automaton, pStepCount))
            .toSet();
    return bFmgr.and(result);
  }

  private BooleanFormula makeInitialConditionForAutomaton(TaDeclaration automaton, int pStepCount) {
    Set<BooleanFormula> result = new HashSet<>();
    var allVariablesZero =
        from(automaton.getClocks())
            .transform(variable -> makeVariableReset(variable, pStepCount, true))
            .toSet();
    result.addAll(allVariablesZero);

    var initialStates =
        from(nodesByAutomaton.get(automaton))
            .filter(node -> node.isInitialState())
            .transform(node -> makeLocationEquals(automaton, pStepCount, node))
            .toSet();
    result.add(bFmgr.or(initialStates));

    return bFmgr.and(result);
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pStepCount, CFAEdge pEdge) {
    throw new UnsupportedOperationException(
        "Location wise formula constrution not supported by "
            + TransitionRelationEncoding.class.getSimpleName());
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pStepCount) {
    // ignore predecessor formula, then every abstract state will represent one step.
    // the whole unrolling formula can be constructed by conjunction of abstract states.
    // (see getFormulaFromReachedSet())
    var result =
        from(edgesByAutomaton.keySet())
            .transform(automaton -> makeStepEncoding(automaton, pStepCount));

    var delayVariable = makeVariable(VariableType.FLOAT, getDelayVariableName(), pStepCount);
    var zero = makeNumber(VariableType.FLOAT, 0);
    var delayGreaterZero = fmgr.makeGreaterOrEqual(delayVariable, zero, true);

    result = result.append(delayGreaterZero);

    return ImmutableSet.of(bFmgr.and(result.toSet()));
  }

  @Override
  public BooleanFormula getFormulaFromReachedSet(Iterable<AbstractState> pReachedSet) {
    Set<BooleanFormula> result = new HashSet<>();
    // collect all formulas and conjunct them
    pReachedSet.forEach(
        aState -> {
          var taState = AbstractStates.extractStateByType(aState, TAUnrollingState.class);
          result.add(taState.getFormula());
        });

    var maxUnrolling =
        from(pReachedSet)
            .transform(
                aState ->
                    AbstractStates.extractStateByType(aState, TAUnrollingState.class)
                        .getStepCount())
            .stream()
            .collect(Collectors.maxBy(Integer::compareTo));

    for (var errorNodes : errorNodesByAutomaton.entrySet()) {
      var targetLocations =
          from(errorNodes.getValue())
              .transform(
                  node -> makeLocationEquals(errorNodes.getKey(), maxUnrolling.orElse(0), node))
              .toSet();
      if (!targetLocations.isEmpty()) {
        result.add(bFmgr.or(targetLocations));
      }
    }

    return bFmgr.and(result);
  }

  private BooleanFormula makeStepEncoding(TaDeclaration automaton, int currentStep) {
    var result =
        from(edgesByAutomaton.get(automaton))
            .transform(edge -> makeEdgeEncoding(edge, automaton, currentStep));
    result = result.append(makeIdleTransition(automaton, currentStep));
    result = result.append(makeDelayTransition(automaton, currentStep));

    return bFmgr.or(result.toSet());
  }

  private BooleanFormula makeDelayTransition(TaDeclaration automaton, int indexAfter) {
    var result =
        from(nodesByAutomaton.get(automaton))
            .filter(node -> node.getInvariant().isPresent())
            .transform(
                node -> {
                  var location = makeLocationEquals(automaton, indexAfter - 1, node);
                  var invariant = makeVariableCondition(node.getInvariant().get(), indexAfter);
                  return bFmgr.implication(location, invariant);
                });

    result = result.append(makeLocationDoesNotChange(automaton, indexAfter));

    var delayVariable = makeVariable(VariableType.FLOAT, getDelayVariableName(), indexAfter);
    var variablesUpdated =
        from(automaton.getClocks())
            .transform(
                variable -> {
                  var successorVariable =
                      makeVariable(VariableType.FLOAT, variable.getName(), indexAfter);
                  var previousVariable =
                      makeVariable(VariableType.FLOAT, variable.getName(), indexAfter - 1);
                  var delay = fmgr.makePlus(previousVariable, delayVariable);
                  return fmgr.makeEqual(successorVariable, delay);
                });
    result = result.append(variablesUpdated);

    var actionVariable = makeVariable(VariableType.INTEGER, getActionVariableName(), indexAfter);
    var actionValue = makeNumber(VariableType.INTEGER, delayActionId);
    result = result.append(fmgr.makeEqual(actionVariable, actionValue));

    return bFmgr.and(result.toSet());
  }

  private BooleanFormula makeIdleTransition(TaDeclaration automaton, int indexAfter) {
    FluentIterable<BooleanFormula> result = FluentIterable.of();

    result = result.append(makeLocationDoesNotChange(automaton, indexAfter));

    var variablesDontChange =
        from(automaton.getClocks())
            .transform(variable -> makeVariableReset(variable, indexAfter, false));
    result = result.append(variablesDontChange);

    var actionVariable = makeVariable(VariableType.INTEGER, getActionVariableName(), indexAfter);
    result =
        result.append(
            from(automaton.getActions())
                .transform(
                    action -> {
                      var actionValue = makeNumber(VariableType.INTEGER, actionIds.get(action));
                      return fmgr.makeNot(fmgr.makeEqual(actionVariable, actionValue));
                    }));

    var delayAction = makeNumber(VariableType.INTEGER, delayActionId);
    var noDelay = fmgr.makeNot(fmgr.makeEqual(actionVariable, delayAction));
    result = result.append(noDelay);

    return bFmgr.and(result.toSet());
  }

  private BooleanFormula makeLocationDoesNotChange(TaDeclaration automaton, int indexAfter) {
    var locationVarName = getAutomatonLocationVariableName(automaton.getName());
    var locationBeforeVariable =
        makeVariable(VariableType.INTEGER, locationVarName, indexAfter - 1);
    var locationAfterVariable = makeVariable(VariableType.INTEGER, locationVarName, indexAfter);
    return fmgr.makeEqual(locationAfterVariable, locationBeforeVariable);
  }

  private BooleanFormula makeEdgeEncoding(TCFAEdge edge, TaDeclaration automaton, int indexAfter) {
    Set<BooleanFormula> result = new HashSet<>();
    result.add(makeLocationEquals(automaton, indexAfter - 1, edge.getPredecessor()));
    result.add(makeLocationEquals(automaton, indexAfter, edge.getSuccessor()));

    var actionVariable = makeVariable(VariableType.INTEGER, getActionVariableName(), indexAfter);
    var actionId = edge.getAction().transform(actionIds::get).or(localActionIds.get(automaton));
    var actionValue = makeNumber(VariableType.INTEGER, actionId);
    result.add(fmgr.makeEqual(actionVariable, actionValue));

    if (edge.getGuard().isPresent()) {
      var guardFormula = makeVariableCondition(edge.getGuard().get(), indexAfter - 1);
      result.add(guardFormula);
    }

    var resetFormulas =
        from(automaton.getClocks())
            .transform(
                variable ->
                    makeVariableReset(
                        variable, indexAfter, edge.getVariablesToReset().contains(variable)))
            .toSet();
    result.addAll(resetFormulas);

    if (edge.getSuccessor() instanceof TCFANode) {
      var tcfaSuccessor = (TCFANode) edge.getSuccessor();
      if (tcfaSuccessor.getInvariant().isPresent()) {
        var invariantFormula =
            makeVariableCondition(tcfaSuccessor.getInvariant().get(), indexAfter);
        result.add(invariantFormula);
      }
    }

    return bFmgr.and(result);
  }

  private BooleanFormula makeLocationEquals(TaDeclaration automaton, int index, CFANode node) {
    var locationVarName = getAutomatonLocationVariableName(automaton.getName());
    var locationVariable = makeVariable(VariableType.INTEGER, locationVarName, index);
    var locationValue = makeNumber(VariableType.INTEGER, nodeIds.get(node));
    return fmgr.makeEqual(locationVariable, locationValue);
  }

  private Formula makeVariable(VariableType type, String name, int index) {
    return fmgr.makeVariable(type.getFormulaType(), getIndexed(name, index));
  }

  private Formula makeNumber(VariableType type, long value) {
    return fmgr.makeNumber(type.getFormulaType(), value);
  }

  private Formula makeNumber(Number pNumber) {
    if (pNumber instanceof BigInteger) {
      return fmgr.makeNumber(VariableType.INTEGER.getFormulaType(), (BigInteger) pNumber);
    } else if (pNumber instanceof BigDecimal) {
      return fmgr.getFloatingPointFormulaManager()
          .makeNumber(
              (BigDecimal) pNumber, (FloatingPointType) VariableType.FLOAT.getFormulaType());
    } else {
      throw new AssertionError();
    }
  }

  private BooleanFormula makeVariableCondition(TaVariableCondition condition, int currentStep) {
    var expressions =
        from(condition.getExpressions())
            .transform(expr -> makeVariableExpression(expr, currentStep))
            .toList();
    return bFmgr.and(expressions);
  }

  private BooleanFormula makeVariableExpression(TaVariableExpression expression, int currentStep) {
    var variableFormula =
        makeVariable(VariableType.FLOAT, expression.getVariable().getName(), currentStep);
    var constantFormula = makeNumber(expression.getConstant());

    switch (expression.getOperator()) {
      case GREATER:
        return fmgr.makeGreaterThan(variableFormula, constantFormula, true);
      case GREATER_EQUAL:
        return fmgr.makeGreaterOrEqual(variableFormula, constantFormula, true);
      case LESS:
        return fmgr.makeLessThan(variableFormula, constantFormula, true);
      case LESS_EQUAL:
        return fmgr.makeLessOrEqual(variableFormula, constantFormula, true);
      case EQUAL:
        return fmgr.makeEqual(variableFormula, constantFormula);
      default:
        throw new AssertionError();
    }
  }

  private BooleanFormula makeVariableReset(TaVariable variable, int currentStep, boolean reset) {
    var successorVariable = makeVariable(VariableType.FLOAT, variable.getName(), currentStep);
    Formula value;
    if (reset) {
      value = makeNumber(VariableType.FLOAT, 0);
    } else {
      value = makeVariable(VariableType.FLOAT, variable.getName(), currentStep - 1);
    }

    return fmgr.makeEqual(successorVariable, value);
  }

  private static String getAutomatonLocationVariableName(String automatonName) {
    return "location#" + automatonName;
  }

  private static String getActionVariableName() {
    return "#action";
  }

  private static String getDelayVariableName() {
    return "#delay";
  }

  private static String getIndexed(String variableName, int index) {
    return variableName + "@" + index;
  }
}
