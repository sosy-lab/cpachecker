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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/**
 * Building a witness invariants automaton out of the invariants from a correctness witness. The
 * resulting witness has two states: init and error. A transitions to the error state consists of
 * the invariants location and the assumption of the negated invariant
 */
public class InvariantsAutomatonBuilder {

  private static final String WITNESS_AUTOMATON_NAME = "InvariantsAutomaton";
  private static final String INITIAL_STATE_NAME = "Init";

  private final LogManager logger;
  private final CFA cfa;

  public InvariantsAutomatonBuilder(CFA pCfa, LogManager pLogger) {
    this.cfa = pCfa;
    this.logger = pLogger;
  }

  @SuppressWarnings("unchecked")
  public Automaton buildWitnessInvariantsAutomaton(
      Set<ExpressionTreeLocationInvariant> pInvariants) {
    try {
      String automatonName = WITNESS_AUTOMATON_NAME;
      String initialStateName = INITIAL_STATE_NAME;
    List<AutomatonInternalState> states = Lists.newLinkedList();
    List<AutomatonTransition> initTransitions = Lists.newLinkedList();
      for (ExpressionTreeLocationInvariant invariant : pInvariants) {
        ExpressionTree<?> inv = invariant.asExpressionTree();
        ExpressionTree<AExpression> invA = (ExpressionTree<AExpression>) inv;
        CExpression cExpr = invA.accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));
        if (invA instanceof LeafExpression<?>) {
          // we must swap the c expression when assume truth is false
          if (!((LeafExpression<?>) invA).assumeTruth()) {
            cExpr =
                new CBinaryExpressionBuilder(cfa.getMachineModel(), logger)
                    .negateExpressionAndSimplify(cExpr);
          }
        }
        CExpression negCExpr =
            new CBinaryExpressionBuilder(cfa.getMachineModel(), logger)
                .negateExpressionAndSimplify(cExpr);
        List<AExpression> assumptionWithCExpr = Collections.singletonList(cExpr);
        List<AExpression> assumptionWithNegCExpr = Collections.singletonList(negCExpr);
        initTransitions.add(
            createTransitionWithCheckLocationAndAssumptionToError(
                invariant.getLocation(), assumptionWithNegCExpr));
        initTransitions.add(
            createTransitionWithCheckLocationAndAssumptionToInit(
                invariant.getLocation(), assumptionWithCExpr));
      }
      AutomatonInternalState initState =
          new AutomatonInternalState(initialStateName, initTransitions, false, true, false);
    states.add(initState);
      Map<String, AutomatonVariable> vars = Collections.emptyMap();
      return new Automaton(automatonName, vars, states, initialStateName);
    } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
      throw new RuntimeException("The passed invariants produce an inconsistent automaton", e);
    }
  }

  private AutomatonTransition createTransitionWithCheckLocationAndAssumptionToError(
      CFANode pLocation, final List<AExpression> pAssumptions) {
    return new AutomatonTransition(
        createQueryLocationString(pLocation),
        Collections.<AutomatonBoolExpr>emptyList(),
        pAssumptions,
        Collections.<AutomatonAction>emptyList(),
        AutomatonInternalState.ERROR,
        new StringExpression("Invariant not valid"));
  }

  private static AutomatonTransition createTransitionWithCheckLocationAndAssumptionToInit(
      CFANode pLocation, final List<AExpression> pAssumptions) {
    return new AutomatonTransition(
        createQueryLocationString(pLocation),
        Collections.<AutomatonBoolExpr>emptyList(),
        pAssumptions,
        Collections.<AutomatonAction>emptyList(),
        "Init");
  }

  private static AutomatonBoolExpr createQueryLocationString(final CFANode pNode) {
    return new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + pNode.getNodeNumber());
  }

}
