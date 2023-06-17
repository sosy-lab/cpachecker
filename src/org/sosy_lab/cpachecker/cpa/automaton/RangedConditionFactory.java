// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchCFAEdgeNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition.Builder;
import org.sosy_lab.cpachecker.util.rangedconditions.CFAPath;

public class RangedConditionFactory {

  private static final String STATE_ACCEPTING = "__TRUE";
  private static final String STATE_NON_ACCEPTING = "REJECT";

  private final CFA cfa;

  public RangedConditionFactory(CFA pCfa) {
    cfa = pCfa;
  }

  public Automaton createForSmallestRange(CFAPath pLargePath) throws InvalidAutomatonException {
    CFAPath bot = new CFAPath(ImmutableList.of(cfa.getMainFunction()));
    return createForRange(bot, pLargePath);
  }

  public Automaton createForLargestRange(CFAPath pSmallPath) throws InvalidAutomatonException {
    return createForRange(pSmallPath, CFAPath.TOP);
  }

  public Automaton createForRange(CFAPath pSmallPath, CFAPath pLargePath)
      throws InvalidAutomatonException {

    Set<CFAPath> stateSet = new HashSet<>();
    stateSet.addAll(pSmallPath.getPrefixes());
    stateSet.addAll(pLargePath.getPrefixes());

    List<AutomatonInternalState> states = new ArrayList<>();

    for (CFAPath state : stateSet) {
      List<AutomatonTransition> transitions = new ArrayList<>();
      CFANode currentNode = state.getLast();
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        CFANode successor = currentNode.getLeavingEdge(i).getSuccessor();
        CFAPath newPath = new CFAPath(state);
        newPath.add(successor);

        if (stateSet.contains(newPath)) {
          transitions.add(generateTransition(currentNode, successor, newPath));
        } else if (newPath.compareTo(pSmallPath) < 0
            || (pLargePath != CFAPath.TOP && newPath.compareTo(pLargePath) > 0)) {
          transitions.add(generateTransitionAccepting(currentNode, successor));
        } else {
          transitions.add(generateTransitionNonAccepting(currentNode, successor));
        }
      }
      states.add(new AutomatonInternalState(state.toString(), transitions));
    }

    states.addAll(generateFinalStates());

    return new Automaton(
        "AssumptionAutomaton", new HashMap<>(), states, cfa.getMainFunction().toString());
  }

  private Set<AutomatonInternalState> generateFinalStates() {
    ImmutableList<AutomatonTransition> transitionsAccepting =
        ImmutableList.of(new Builder(AutomatonBoolExpr.TRUE, STATE_ACCEPTING).build());
    ImmutableList<AutomatonTransition> transitionsRejecting =
        ImmutableList.of(new Builder(AutomatonBoolExpr.TRUE, STATE_NON_ACCEPTING).build());

    AutomatonInternalState accepting =
        new AutomatonInternalState(STATE_ACCEPTING, transitionsAccepting);
    AutomatonInternalState nonAccepting =
        new AutomatonInternalState(STATE_NON_ACCEPTING, transitionsRejecting);

    return ImmutableSet.of(accepting, nonAccepting);
  }

  private AutomatonTransition generateTransitionNonAccepting(
      CFANode pPredecessorNode, CFANode pSuccessorNode) {
    return _generateTransition(pPredecessorNode, pSuccessorNode, STATE_NON_ACCEPTING);
  }

  private AutomatonTransition generateTransitionAccepting(
      CFANode pPredecessorNode, CFANode pSuccessorNode) {
    return _generateTransition(pPredecessorNode, pSuccessorNode, STATE_ACCEPTING);
  }

  private AutomatonTransition generateTransition(
      CFANode pPredecessorNode, CFANode pSuccessorNode, CFAPath pTargetState) {
    return _generateTransition(pPredecessorNode, pSuccessorNode, pTargetState.toString());
  }

  private AutomatonTransition _generateTransition(
      CFANode pPredecessorNode, CFANode pSuccessorNode, String pTargetState) {
    AutomatonBoolExpr trigger =
        new MatchCFAEdgeNodes(pPredecessorNode.getNodeNumber(), pSuccessorNode.getNodeNumber());
    AutomatonTransition.Builder transitionBuilder = new Builder(trigger, pTargetState);
    return transitionBuilder.build();
  }
}
