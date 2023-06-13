// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states by the number of their loop iterations.
 * States with a more/less (depending on the used factory method) loop iterations are considered
 * first.
 */
public class LoopIterationSortedWaitlist extends AbstractSortedWaitlist<Integer> {
  private final int multiplier;

  private LoopIterationSortedWaitlist(WaitlistFactory pSecondaryStrategy, int pMultiplier) {
    super(pSecondaryStrategy);
    multiplier = pMultiplier;
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    LoopBoundState loopBoundState = AbstractStates.extractStateByType(pState, LoopBoundState.class);
    return (loopBoundState != null)
        ? (multiplier * loopBoundState.getMaxNumberOfIterationsInLoopstackFrame())
        : 0;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new LoopIterationSortedWaitlist(pSecondaryStrategy, 1);
  }

  public static WaitlistFactory reversedFactory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new LoopIterationSortedWaitlist(pSecondaryStrategy, -1);
  }
}
