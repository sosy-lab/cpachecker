// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states depending on the number of SMG-objects (if
 * there are any). States with fewer objects are considered first.
 */
public class SMGSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected SMGSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    SMGState state = AbstractStates.extractStateByType(pState, SMGState.class);

    // negate size so that the highest key corresponds to the smallest map
    return (state == null) ? 0 : -state.getHeap().getHeapObjects().size();
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new SMGSortedWaitlist(pSecondaryStrategy);
  }
}
