/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states depending
 * on the number of running threads (if there are any).
 * States with fewer running threads are considered first.
 * These states are expected to avoid state explosion,
 * as they have fewer successors due to the interleaving of threads.
 */
public class ThreadingSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ThreadingSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    ThreadingState state =
      AbstractStates.extractStateByType(pState, ThreadingState.class);

    // negate size so that the highest key corresponds to the smallest map
    return (state == null) ? 0 : -state.getThreadIds().size();
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new ThreadingSortedWaitlist(pSecondaryStrategy);
  }
}
