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
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchCFAEdgeNodes;
import org.sosy_lab.cpachecker.util.rangedconditions.CFAPath;

public class RangedConditionFactory {

  private static final String STATE_ACCEPTING = "__TRUE";
  private static final String STATE_NON_ACCEPTING = "REJECT";

  private final CFA cfa;

  public RangedConditionFactory(CFA pCfa) {
    cfa = pCfa;
  }

  public Automaton createForSmallestRange(CFAPath pLargePath, Set<CFAPath> otherPaths)
      throws InvalidAutomatonException {
    CFAPath bot = new CFAPath(ImmutableList.of(cfa.getMainFunction()));
    return createForRange(bot, pLargePath, otherPaths);
  }

  public Automaton createForLargestRange(CFAPath pSmallPath, Set<CFAPath> otherPaths)
      throws InvalidAutomatonException {
    return createForRange(pSmallPath, CFAPath.TOP, otherPaths);
  }

  public Automaton createForRange(CFAPath pSmallPath, CFAPath pLargePath, Set<CFAPath> otherPaths)
      throws InvalidAutomatonException {

    ImmutableSet.Builder<CFAPath> prefixBuilder = new ImmutableSet.Builder<>();
    prefixBuilder.addAll(pSmallPath.getPrefixes());
    prefixBuilder.addAll(pLargePath.getPrefixes());

    Set<CFAPath> rangePrefixes = prefixBuilder.build();

    ImmutableSet.Builder<AutomatonInternalState> statesBuilder = new ImmutableSet.Builder<>();

    for (CFAPath state : rangePrefixes) {
      List<AutomatonTransition> transitions = new ArrayList<>();
      CFANode currentNode = state.getLast();
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        CFANode successor = currentNode.getLeavingEdge(i).getSuccessor();
        CFAPath currentPath = CFAPath.append(state, successor);

        if (rangePrefixes.contains(currentPath)) {
          transitions.add(generateTransition(currentNode, successor, currentPath));
        } else if (currentPath.compareTo(pSmallPath) < 0
            || (pLargePath != CFAPath.TOP && currentPath.compareTo(pLargePath) > 0)) {
          transitions.add(generateTransitionAccepting(currentNode, successor));
        } else {
          transitions.add(generateTransitionNonAccepting(currentNode, successor));
        }
      }
      statesBuilder.add(new AutomatonInternalState(state.toString(), transitions));
    }

    // Add other paths prefixes as states. This is necessary for the PartialARGCombiner
    otherPaths.forEach(path -> prefixBuilder.addAll(path.getPrefixes()));
    Set<CFAPath> allPrefixes = prefixBuilder.build();
    Sets.difference(allPrefixes, rangePrefixes)
        .forEach(
            prefix ->
                statesBuilder.add(
                    new AutomatonInternalState(prefix.toString(), ImmutableList.of())));

    statesBuilder.addAll(generateFinalStates());

    return new Automaton(
        "AssumptionAutomaton",
        new HashMap<>(),
        statesBuilder.build().asList(),
        cfa.getMainFunction().toString());
  }

  private Set<AutomatonInternalState> generateFinalStates() {
    ImmutableList<AutomatonTransition> transitionsAccepting =
        ImmutableList.of(
            new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, STATE_ACCEPTING).build());
    ImmutableList<AutomatonTransition> transitionsRejecting =
        ImmutableList.of(
            new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, STATE_NON_ACCEPTING).build());

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
    AutomatonTransition.Builder transitionBuilder =
        new AutomatonTransition.Builder(trigger, pTargetState);
    return transitionBuilder.build();
  }
}
