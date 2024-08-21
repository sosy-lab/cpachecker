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
 * Waitlist implementation that sorts the abstract states by the depth of their loopstack. States
 * with a larger/smaller (depending on the used factory method) loopstack are considered first.
 */
public class LoopstackSortedWaitlist extends AbstractSortedWaitlist<Integer> {
  private final int multiplier;

  private LoopstackSortedWaitlist(WaitlistFactory pSecondaryStrategy, int pMultiplier) {
    super(pSecondaryStrategy);
    multiplier = pMultiplier;
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    LoopBoundState loopstackState = AbstractStates.extractStateByType(pState, LoopBoundState.class);
    return (loopstackState != null) ? (multiplier * loopstackState.getDepth()) : 0;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new LoopstackSortedWaitlist(pSecondaryStrategy, 1);
  }

  public static WaitlistFactory reversedFactory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new LoopstackSortedWaitlist(pSecondaryStrategy, -1);
  }
}
