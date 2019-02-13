/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;

/** Helper class to build an automaton out of the invariants from a given correctness witness */
public class WitnessInvariantsAutomaton {

  public static Automaton buildWitnessInvariantsAutomaton(Set<CandidateInvariant> candidates) {
    String automatonName = "WitnessInvariantsAutomaton";
    String initialStateName = "Init";
    Map<String, AutomatonVariable> vars = Collections.emptyMap();
    List<AutomatonInternalState> states = Lists.newLinkedList();
    List<AutomatonTransition> initTransitions = Lists.newLinkedList();
    List<AutomatonInternalState> invStates = Lists.newLinkedList();
    for (CandidateInvariant candidate : candidates) {
      if (candidate instanceof ExpressionTreeLocationInvariant) {
        ExpressionTreeLocationInvariant inv = (ExpressionTreeLocationInvariant) candidate;
        AutomatonTransition errorTransition = createTransitionWithAssumptionToError(inv);
        AutomatonTransition initTransition = createTransitionWithAssumptionToInit(inv);
        List<AutomatonTransition> transitions = Lists.newLinkedList();
        transitions.add(errorTransition);
        transitions.add(initTransition);
        AutomatonInternalState invState =
            new AutomatonInternalState(
                "inv" + inv.getLocation().getNodeNumber(), transitions, false, true, false);
        invStates.add(invState);
        initTransitions.add(
            createInitTransitionTriggerWithLocation(inv.getLocation().getNodeNumber()));
      }
    }
    AutomatonInternalState initState =
        new AutomatonInternalState(initialStateName, initTransitions, false, true, false);
    states.add(initState);
    states.addAll(invStates);
    try {
      return new Automaton(automatonName, vars, states, initialStateName);
    } catch (InvalidAutomatonException e) {
      throw new RuntimeException("The passed invariants prdouce an inconsistent automaton", e);
    }
  }

  private static AutomatonTransition createInitTransitionTriggerWithLocation(int nodeNumber) {
    String followStateName = "inv" + nodeNumber;
    AutomatonBoolExpr queryString =
        new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + nodeNumber);
    List<AutomatonAction> stateActions = Collections.emptyList();
    List<AutomatonBoolExpr> stateAssertions = Collections.emptyList();
    List<AExpression> assumptions = Collections.emptyList();
    return new AutomatonTransition(
        queryString, stateAssertions, assumptions, stateActions, followStateName);
  }

  private static BinaryOperator determineBinaryOperator(
      BinaryOperator originalOpertator, boolean assumeTruth, boolean negate) {
    BinaryOperator result = originalOpertator;
    if (!assumeTruth) {
      result = result.getOppositLogicalOperator();
    }
    if (negate) {
      result = result.getOppositLogicalOperator();
    }
    return result;
  }

  private static AutomatonTransition createTransitionWithAssumptionToError(
      ExpressionTreeLocationInvariant inv) {
    StringExpression violatedPropertyDesc = new StringExpression("Invariant not valid");
    AutomatonBoolExpr trigger = AutomatonBoolExpr.TRUE;
    List<AutomatonAction> stateActions = Collections.emptyList();
    List<AutomatonBoolExpr> stateAssertions = Collections.emptyList();
    AutomatonInternalState followState = AutomatonInternalState.ERROR;
    List<AExpression> assumptions = Lists.newLinkedList();
    ExpressionTree<Object> expr = inv.asExpressionTree();
    CBinaryExpression cExpr = (CBinaryExpression) ((LeafExpression<?>) expr).getExpression();
    CBinaryExpression cExprNegated =
        new CBinaryExpression(
            cExpr.getFileLocation(),
            cExpr.getExpressionType(),
            cExpr.getCalculationType(),
            cExpr.getOperand1(),
            cExpr.getOperand2(),
            determineBinaryOperator(
                cExpr.getOperator(), ((LeafExpression<?>) expr).assumeTruth(), true));
    assumptions.add(cExprNegated);
    return new AutomatonTransition(
        trigger, stateAssertions, assumptions, stateActions, followState, violatedPropertyDesc);
  }

  private static AutomatonTransition createTransitionWithAssumptionToInit(
      ExpressionTreeLocationInvariant inv) {
    AutomatonBoolExpr trigger = AutomatonBoolExpr.TRUE;
    List<AutomatonAction> stateActions = Collections.emptyList();
    List<AutomatonBoolExpr> stateAssertions = Collections.emptyList();
    String followStateName = "Init";
    List<AExpression> assumptions = Lists.newLinkedList();
    ExpressionTree<Object> expr = inv.asExpressionTree();
    CBinaryExpression cExpr = (CBinaryExpression) ((LeafExpression<?>) expr).getExpression();
    CBinaryExpression cExprNotNegated =
        new CBinaryExpression(
            cExpr.getFileLocation(),
            cExpr.getExpressionType(),
            cExpr.getCalculationType(),
            cExpr.getOperand1(),
            cExpr.getOperand2(),
            determineBinaryOperator(
                cExpr.getOperator(), ((LeafExpression<?>) expr).assumeTruth(), false));
    assumptions.add(cExprNotNegated);
    return new AutomatonTransition(
        trigger, stateAssertions, assumptions, stateActions, followStateName);
  }
}
