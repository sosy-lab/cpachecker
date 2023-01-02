// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states by the depth of their call stack. States
 * with a bigger callstack are considered first. A secondary strategy needs to be given that decides
 * what to do with states of the same callstack depth.
 */
public class CallstackSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected CallstackSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    CallstackState callstackState = AbstractStates.extractStateByType(pState, CallstackState.class);

    return (callstackState != null) ? callstackState.getDepth() : 0;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new CallstackSortedWaitlist(pSecondaryStrategy);
  }
}
