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
    return newPath.size() >= reachedPath.size()
        && newPath.subList(0, reachedPath.size() - 1).equals(reachedPath);
  }
}
