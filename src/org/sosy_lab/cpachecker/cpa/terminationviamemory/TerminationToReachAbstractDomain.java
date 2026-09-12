// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.DynamicMemoryHandler;

public class TerminationToReachAbstractDomain implements AbstractDomain {

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
    return newTerminationState.equals(reachedTerminationState)
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
    ImmutableList<CFANode> lastIterationOfTheBranch = newPath.subList(reachedPath.size(), newPath.size());

    return newPath.subList(0, reachedPath.size()).equals(reachedPath)
        // Only cover the state, if it is covered by the previous abstract state at a loop head.
        // In other words, we check that no CFANode repeats in the last iteration between the
        // reachedPath and the newPath.
        && lastIterationOfTheBranch.stream().distinct().count() == lastIterationOfTheBranch.size();
  }
}
