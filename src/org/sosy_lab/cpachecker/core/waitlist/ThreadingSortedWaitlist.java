// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithThreads;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states depending on the number of running threads
 * (if there are any). States with fewer running threads are considered first. These states are
 * expected to avoid state explosion, as they have fewer successors due to the interleaving of
 * threads.
 */
public class ThreadingSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ThreadingSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    AbstractStateWithThreads state =
        AbstractStates.extractStateByType(pState, AbstractStateWithThreads.class);

    // negate size so that the highest key corresponds to the smallest map
    return (state == null) ? 0 : state.getNumberOfActiveThreads();
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new ThreadingSortedWaitlist(pSecondaryStrategy);
  }
}
