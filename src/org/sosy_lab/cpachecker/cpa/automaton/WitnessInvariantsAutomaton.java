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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.DefaultExpressionTreeVisitor;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/** Helper class to build an automaton out of the invariants from a given correctness witness */
public class WitnessInvariantsAutomaton {

  public static Automaton buildWitnessInvariantsAutomaton(
      Set<CandidateInvariant> candidates,
      ToCExpressionVisitor visitor,
      CBinaryExpressionBuilder builder) {
    try {
    String automatonName = "WitnessInvariantsAutomaton";
    String initialStateName = "Init";
    Map<String, AutomatonVariable> vars = Collections.emptyMap();
    List<AutomatonInternalState> states = Lists.newLinkedList();
    List<AutomatonTransition> initTransitions = Lists.newLinkedList();
    List<AutomatonInternalState> invStates = Lists.newLinkedList();
    for (CandidateInvariant candidate : candidates) {
      if (candidate instanceof ExpressionTreeLocationInvariant) {
        ExpressionTreeLocationInvariant inv = (ExpressionTreeLocationInvariant) candidate;
          AutomatonTransition errorTransition =
              createTransitionWithCheckLocationAndAssumptionToError(inv, visitor, builder);
          AutomatonTransition initTransition =
              createTransitionWithCheckLocationAndAssumptionToInit(inv, visitor, builder);
        initTransitions.add(errorTransition);
        initTransitions.add(initTransition);
      }
    }
    AutomatonInternalState initState =
        new AutomatonInternalState(initialStateName, initTransitions, false, true, false);
    states.add(initState);
    states.addAll(invStates);
      return new Automaton(automatonName, vars, states, initialStateName);
    } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
      throw new RuntimeException("The passed invariants prdouce an inconsistent automaton", e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <LeafType> ExpressionTree<?> applyAssumeTruth(
      ExpressionTree<?> expr, CBinaryExpressionBuilder builder) throws UnrecognizedCodeException {
    if(expr instanceof And<?>) {
      Set<ExpressionTree<LeafType>> operators = new HashSet<>();
      And<?> exprAnd = (And<?>) expr;
      Iterator<?> operandsIterator = exprAnd.iterator();
      while (operandsIterator.hasNext()) {
        ExpressionTree<?> next = (ExpressionTree<?>) operandsIterator.next();
        ExpressionTree<?> applied = applyAssumeTruth(next, builder);
        operators.add((ExpressionTree<LeafType>) applied);
      }
      return And.of(FluentIterable.from(operators));
    }
    if(expr instanceof Or<?>) {
      Set<ExpressionTree<LeafType>> operators = new HashSet<>();
      Or<?> exprAnd = (Or<?>) expr;
      Iterator<?> operandsIterator = exprAnd.iterator();
      while (operandsIterator.hasNext()) {
        ExpressionTree<?> next = (ExpressionTree<?>) operandsIterator.next();
        ExpressionTree<?> applied = applyAssumeTruth(next, builder);
        operators.add((ExpressionTree<LeafType>) applied);
      }
      return Or.of(FluentIterable.from(operators));
    }
    if(expr instanceof LeafExpression<?>) {
      LeafExpression<?> leafExpr = (LeafExpression<?>) expr;
      if(!leafExpr.assumeTruth()) {
        if(leafExpr.getExpression() instanceof CBinaryExpression) {
          CBinaryExpression exprC = (CBinaryExpression) leafExpr.getExpression();
          CBinaryExpression cExprNegated = builder.negateExpressionAndSimplify(exprC);
          return LeafExpression.fromCExpression(cExprNegated, true);
        }
      }
    }
    return expr;
  }

  @SuppressWarnings("unchecked")
  private static <LeafType> CExpression convertToCExpression(
      ExpressionTree<?> expr,
      DefaultExpressionTreeVisitor<LeafType, CExpression, UnrecognizedCodeException> visitor)
      throws UnrecognizedCodeException {
    if (expr instanceof And<?>) {
      return visitor.visit((And<LeafType>) expr);
    }
    if (expr instanceof Or<?>) {
      return visitor.visit((Or<LeafType>) expr);
    }
    if (expr instanceof LeafExpression<?>) {
      return visitor.visit((LeafExpression<LeafType>) expr);
    }
    return null;
  }

  private static CExpression negateCExpression(
      CBinaryExpression cBinExpr, CBinaryExpressionBuilder builder)
      throws UnrecognizedCodeException {
    // TODO go recursive into the formula swap all operators and set == 0
    return builder.negateExpressionAndSimplify(cBinExpr);
  }

  private static <LeafType>
      AutomatonTransition createTransitionWithCheckLocationAndAssumptionToError(
          ExpressionTreeLocationInvariant inv,
          DefaultExpressionTreeVisitor<LeafType, CExpression, UnrecognizedCodeException> visitor,
          CBinaryExpressionBuilder builder)
          throws UnrecognizedCodeException {
    AutomatonBoolExpr queryString =
        new AutomatonBoolExpr.CPAQuery(
            "location", "nodenumber==" + inv.getLocation().getNodeNumber());
    StringExpression violatedPropertyDesc = new StringExpression("Invariant not valid");
    List<AutomatonAction> stateActions = Collections.emptyList();
    List<AutomatonBoolExpr> stateAssertions = Collections.emptyList();
    AutomatonInternalState followState = AutomatonInternalState.ERROR;
    List<AExpression> assumptions = Lists.newLinkedList();
    ExpressionTree<?> expr = applyAssumeTruth(inv.asExpressionTree(), builder);
    CExpression cExpr = convertToCExpression(expr, visitor);
    CBinaryExpression cBinExpr = (CBinaryExpression) cExpr;
    CExpression negCExpr;
    negCExpr = negateCExpression(cBinExpr, builder);
    assumptions.add(negCExpr);
    return new AutomatonTransition(
        queryString, stateAssertions, assumptions, stateActions, followState, violatedPropertyDesc);
  }

  private static AutomatonTransition createTransitionWithCheckLocationAndAssumptionToInit(
      ExpressionTreeLocationInvariant inv,
      ToCExpressionVisitor visitor,
      CBinaryExpressionBuilder builder)
      throws UnrecognizedCodeException {
    AutomatonBoolExpr queryString =
        new AutomatonBoolExpr.CPAQuery(
            "location", "nodenumber==" + inv.getLocation().getNodeNumber());
    List<AutomatonAction> stateActions = Collections.emptyList();
    List<AutomatonBoolExpr> stateAssertions = Collections.emptyList();
    String followStateName = "Init";
    List<AExpression> assumptions = Lists.newLinkedList();
    ExpressionTree<?> expr = applyAssumeTruth(inv.asExpressionTree(), builder);
    CExpression cExpr = convertToCExpression(expr, visitor);
    assumptions.add(cExpr);
    return new AutomatonTransition(
        queryString, stateAssertions, assumptions, stateActions, followStateName);
  }
}
