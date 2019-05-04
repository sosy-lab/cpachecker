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

public class WitnessLocationInvariantsAutomaton {

  private static final String WITNESS_AUTOMATON_NAME = "WitnessLocationInvariantsAutomaton";

  private LogManager logger;
  private CFA cfa;

  public WitnessLocationInvariantsAutomaton(CFA cfa, LogManager logger) {
    this.cfa = cfa;
    this.logger = logger;
  }

  private static Set<CFANode> extractCFANodes(Set<ExpressionTreeLocationInvariant> invariants) {
    return invariants
        .stream()
        .map(ExpressionTreeLocationInvariant::getLocation)
        .collect(Collectors.toSet());
  }

  private static ExpressionTreeLocationInvariant getInvariantByLocation(
      Set<ExpressionTreeLocationInvariant> invariants, CFANode location) {
    Set<ExpressionTreeLocationInvariant> filteredInvariants =
        invariants
            .stream()
            .filter(inv -> inv.getLocation().equals(location))
            .collect(Collectors.toSet());
    assert filteredInvariants.size() == 1;
    return filteredInvariants.iterator().next();
  }

  @SuppressWarnings("unchecked")
  public Automaton buildWitnessLocationInvariantsAutomaton(
      Set<ExpressionTreeLocationInvariant> invariants) {
    try {
      String automatonName = WITNESS_AUTOMATON_NAME;
      String initialStateName = createStateName(cfa.getMainFunction());
      List<AutomatonInternalState> states = Lists.newLinkedList();
      Set<CFANode> invariantCFANodes = extractCFANodes(invariants);
      for (CFANode node : cfa.getAllNodes()) {
        if (node.getNumLeavingEdges() > 0 && node.getLeavingEdge(0) != null) {
          CFAEdge leavingEdge = node.getLeavingEdge(0);
          List<AutomatonTransition> transitions = Lists.newLinkedList();
          CFANode successor = leavingEdge.getSuccessor();
          boolean successorIsBottom = false;
          if (successor.getNumLeavingEdges() == 0) {
            successorIsBottom = true;
          }
          if (invariantCFANodes.contains(successor)) {
            ExpressionTreeLocationInvariant invariant =
                getInvariantByLocation(invariants, successor);
            ExpressionTree<AExpression> inv = invariant.getExpressionTree();
            createLocationInvariantsTransitions(
                transitions, node, successor, inv, successorIsBottom);
          } else {
            transitions.add(
                createAutomatonTransition(
                    node, Collections.<AExpression>emptyList(), successor, successorIsBottom));
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
      List<AutomatonTransition> pTransitions,
      CFANode pOrigin,
      CFANode pTarget,
      ExpressionTree<AExpression> pInvariant,
      boolean successorIsBottom)
      throws UnrecognizedCodeException {
    CExpression cExpr = pInvariant.accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));
    if (pInvariant instanceof LeafExpression<?>) {
      // we must swap the c expression when assume truth is false
      if (!((LeafExpression<?>) pInvariant).assumeTruth()) {
        cExpr =
            (new CBinaryExpressionBuilder(cfa.getMachineModel(), logger))
                .negateExpressionAndSimplify(cExpr);
      }
    }
    CExpression negCExpr =
        (new CBinaryExpressionBuilder(cfa.getMachineModel(), logger))
            .negateExpressionAndSimplify(cExpr);
    List<AExpression> assumptionWithNegCExpr = Collections.singletonList(negCExpr);
    pTransitions.add(createAutomatonInvariantErrorTransition(pOrigin, assumptionWithNegCExpr));
    List<AExpression> assumptionWithCExpr = Collections.singletonList(cExpr);
    pTransitions.add(
        createAutomatonTransition(pOrigin, assumptionWithCExpr, pTarget, successorIsBottom));
  }

  private AutomatonTransition createAutomatonTransition(
      CFANode origin, List<AExpression> assumptions, CFANode target, boolean successorIsBottom) {
    if (successorIsBottom) {
      return new AutomatonTransition(
          createQueryString(origin),
          Collections.<AutomatonBoolExpr>emptyList(),
          assumptions,
          Collections.<AutomatonAction>emptyList(),
          AutomatonInternalState.BOTTOM);
    } else {
      return new AutomatonTransition(
          createQueryString(origin),
          Collections.<AutomatonBoolExpr>emptyList(),
          assumptions,
          Collections.<AutomatonAction>emptyList(),
          createStateName(target));
    }
  }

  private AutomatonTransition createAutomatonInvariantErrorTransition(
      CFANode origin, List<AExpression> assumptions) {
    return new AutomatonTransition(
        createQueryString(origin),
        Collections.<AutomatonBoolExpr>emptyList(),
        assumptions,
        Collections.<AutomatonAction>emptyList(),
        AutomatonInternalState.ERROR,
        new StringExpression("Invariant not valid"));
  }

  private static AutomatonBoolExpr createQueryString(CFANode origin) {
    return new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + origin.getNodeNumber());
  }

  private static String createStateName(CFANode node) {
    return "S" + node.getNodeNumber();
  }
}
