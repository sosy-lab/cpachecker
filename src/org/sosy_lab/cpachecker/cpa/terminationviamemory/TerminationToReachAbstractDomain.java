// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class TerminationToReachAbstractDomain implements AbstractDomain {

  private final LoopStructure loopStructure;

  public TerminationToReachAbstractDomain(LoopStructure pLoopStructure) {
    loopStructure = pLoopStructure;
  }

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    throw new UnsupportedOperationException("These states cannot be joined.");
  }

  @Override
  public boolean isLessOrEqual(AbstractState newState, AbstractState reachedState)
      throws CPAException {
    TerminationToReachState newTerminationState =
        AbstractStates.extractStateByType(newState, TerminationToReachState.class);
    TerminationToReachState reachedTerminationState =
        AbstractStates.extractStateByType(reachedState, TerminationToReachState.class);

    // An abstract state in this domain expresses paths.
    // Therefore, one abstract state can cover other only if they are on the same path.
    // A state with more transition invariants represents fewer pairs of states.
    if (newTerminationState.isTarget() != reachedTerminationState.isTarget()
        || !newTerminationState
            .getTransitionInvariants()
            .containsAll(reachedTerminationState.getTransitionInvariants())) {
      return false;
    }
    if (newTerminationState.getPathSequence().equals(reachedTerminationState.getPathSequence())) {
      return newTerminationState
          .getTransitionPredicates()
          .equals(reachedTerminationState.getTransitionPredicates());
    }
    return !reachedTerminationState.getTransitionInvariants().isEmpty()
        && isSubsequence(
            newTerminationState.getPathSequence(), reachedTerminationState.getPathSequence());
  }

  private boolean isSubsequence(
      ImmutableList<CFANode> newPath, ImmutableList<CFANode> reachedPath) {
    if (newPath.size() < reachedPath.size()) {
      return false;
    }
    // Taking one more state away with reachPath.size() instead of reachedPath.size() - 1, because
    // the previous loop head is included twice at the end.
    ImmutableList<CFANode> lastIterationOfTheBranch =
        newPath.subList(reachedPath.size(), newPath.size());

    // Only cover the state by the abstract state of the previous visit of its loop head, i.e., the
    // path between them is exactly one iteration of the loop. It may contain iterations of
    // nested loops, but it must not leave the loop, e.g., through the head of an outer loop.
    return newPath.subList(0, reachedPath.size()).equals(reachedPath)
        && !lastIterationOfTheBranch.isEmpty()
        && lastIterationOfTheBranch.indexOf(newPath.getLast())
            == lastIterationOfTheBranch.size() - 1
        && getLoopNodes(newPath.getLast()).containsAll(lastIterationOfTheBranch);
  }

  private Set<CFANode> getLoopNodes(CFANode pLoopHead) {
    Set<CFANode> nodes = new HashSet<>();
    for (Loop loop : loopStructure.getLoopsForLoopHead(pLoopHead)) {
      nodes.addAll(loop.getLoopNodes());
    }
    return nodes;
  }
}
