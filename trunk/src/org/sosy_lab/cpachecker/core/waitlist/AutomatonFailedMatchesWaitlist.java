// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class AutomatonFailedMatchesWaitlist extends AbstractSortedWaitlist<Integer> {

  protected AutomatonFailedMatchesWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    int sortKey = 0;
    for (AutomatonState automatonState :
        AbstractStates.asIterable(pState).filter(AutomatonState.class)) {
      sortKey = Math.max(sortKey, automatonState.getFailedMatches());
    }

    return sortKey;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new AutomatonFailedMatchesWaitlist(pSecondaryStrategy);
  }
}
