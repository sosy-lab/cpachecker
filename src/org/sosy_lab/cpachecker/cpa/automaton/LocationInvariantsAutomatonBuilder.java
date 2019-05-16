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
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/**
 * Builds a location invariant automaton out of the invariants from a correctness witness. The
 * structure of the automaton follows the structure of the CFA: States and transitions of the
 * automaton are built corresponding to the nodes and edges of the CFA. For each entering CFANode
 * that matches with the invariant location two transitions and successor states are built: One
 * transition with the invariant assumption that goes to the next CFA based state and another
 * transition with the negated invariant assumption that goes to the Error state.
 */
public class LocationInvariantsAutomatonBuilder {

  private static final String WITNESS_AUTOMATON_NAME = "LocationInvariantsAutomaton";

  private final LogManager logger;
  private final CFA cfa;

  public LocationInvariantsAutomatonBuilder(CFA pCfa, LogManager pLogger) {
    this.cfa = pCfa;
    this.logger = pLogger;
  }

  private static Set<CFANode> extractCFANodes(
      final Set<ExpressionTreeLocationInvariant> pInvariants) {
    return pInvariants
        .stream()
        .map(ExpressionTreeLocationInvariant::getLocation)
        .collect(Collectors.toSet());
  }

  private static ExpressionTreeLocationInvariant getInvariantByLocation(
      final Set<ExpressionTreeLocationInvariant> pInvariants, final CFANode pLocation) {
    Set<ExpressionTreeLocationInvariant> filteredInvariants =
        pInvariants
            .stream()
            .filter(inv -> inv.getLocation().equals(pLocation))
            .collect(Collectors.toSet());
    return filteredInvariants.iterator().next();
  }

  @SuppressWarnings("unchecked")
  public Automaton buildWitnessLocationInvariantsAutomaton(
      final Set<ExpressionTreeLocationInvariant> pInvariants) {
    try {
      String automatonName = WITNESS_AUTOMATON_NAME;
      String initialStateName = createStateName(cfa.getMainFunction());
      List<AutomatonInternalState> states = Lists.newLinkedList();
      Set<CFANode> invariantCFANodes = extractCFANodes(pInvariants);
      for (CFANode node : cfa.getAllNodes()) {
        if (node.getNumLeavingEdges() > 0) {
          List<AutomatonTransition> transitions = Lists.newLinkedList();
          for (int i = 0; i < node.getNumLeavingEdges(); i++) {
            CFAEdge leavingEdge = node.getLeavingEdge(i);
            CFANode successor = leavingEdge.getSuccessor();
            boolean successorIsBottom = false;
            if (successor.getNumLeavingEdges() == 0) {
              successorIsBottom = true;
            }
            if (invariantCFANodes.contains(successor)) {
              ExpressionTreeLocationInvariant invariant =
                  getInvariantByLocation(pInvariants, successor);
              ExpressionTree<?> inv = invariant.asExpressionTree();
              ExpressionTree<AExpression> invA = (ExpressionTree<AExpression>) inv;
              createLocationInvariantsTransitions(transitions, successor, invA, successorIsBottom);
            } else {
              transitions.add(
                  createAutomatonTransition(
                      successor,
                      Collections.<AExpression>emptyList(),
                      successorIsBottom));
            }
          }
          AutomatonInternalState state =
              new AutomatonInternalState(createStateName(node), transitions, false, true, false);
          states.add(state);
        }
      }
      return new Automaton(
          automatonName,
          Collections.<String, AutomatonVariable>emptyMap(),
          states,
          initialStateName);
    } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
      throw new RuntimeException("The passed invariants produce an inconsistent automaton", e);
    }
  }

  private void createLocationInvariantsTransitions(
      final List<AutomatonTransition> pTransitions,
      final CFANode pSuccessor,
      final ExpressionTree<AExpression> pInvariant,
      final boolean pSuccessorIsBottom)
      throws UnrecognizedCodeException {
    CExpression cExpr = pInvariant.accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));
    if (pInvariant instanceof LeafExpression<?>) {
      // we must swap the c expression when assume truth is false
      if (!((LeafExpression<?>) pInvariant).assumeTruth()) {
        cExpr =
            new CBinaryExpressionBuilder(cfa.getMachineModel(), logger)
                .negateExpressionAndSimplify(cExpr);
      }
    }
    CExpression negCExpr =
        new CBinaryExpressionBuilder(cfa.getMachineModel(), logger)
            .negateExpressionAndSimplify(cExpr);
    List<AExpression> assumptionWithNegCExpr = Collections.singletonList(negCExpr);
    pTransitions.add(createAutomatonInvariantErrorTransition(pSuccessor, assumptionWithNegCExpr));
    List<AExpression> assumptionWithCExpr = Collections.singletonList(cExpr);
    pTransitions.add(
        createAutomatonTransition(pSuccessor, assumptionWithCExpr, pSuccessorIsBottom));
  }

  private static AutomatonTransition createAutomatonTransition(
      final CFANode pSuccessor,
      final List<AExpression> pAssumptions,
      final boolean pSuccessorIsBottom) {
    if (pSuccessorIsBottom) {
      return new AutomatonTransition(
          createQueryLocationString(pSuccessor),
          Collections.<AutomatonBoolExpr>emptyList(),
          pAssumptions,
          Collections.<AutomatonAction>emptyList(),
          AutomatonInternalState.BOTTOM);
    } else {
      return new AutomatonTransition(
          createQueryLocationString(pSuccessor),
          Collections.<AutomatonBoolExpr>emptyList(),
          pAssumptions,
          Collections.<AutomatonAction>emptyList(),
          createStateName(pSuccessor));
    }
  }

  private static AutomatonTransition createAutomatonInvariantErrorTransition(
      final CFANode pSuccessor, final List<AExpression> pAssumptions) {
    return new AutomatonTransition(
        createQueryLocationString(pSuccessor),
        Collections.<AutomatonBoolExpr>emptyList(),
        pAssumptions,
        Collections.<AutomatonAction>emptyList(),
        AutomatonInternalState.ERROR,
        new StringExpression("Invariant not valid"));
  }

  private static AutomatonBoolExpr createQueryLocationString(final CFANode pNode) {
    return new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + pNode.getNodeNumber());
  }

  private static String createStateName(CFANode pNode) {
    return "S" + pNode.getNodeNumber();
  }
}
