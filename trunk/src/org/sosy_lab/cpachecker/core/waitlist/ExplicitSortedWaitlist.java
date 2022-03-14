// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states depending on the content of the
 * ExplicitState (if there is any). Stas where less variables have a value assigned are considered
 * first. This states are expected to cover a bigger part of the state space, so states with more
 * variables will probably be covered later.
 */
public class ExplicitSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ExplicitSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    ValueAnalysisState explicitState =
        AbstractStates.extractStateByType(pState, ValueAnalysisState.class);

    // negate size so that the highest key corresponds to the smallest map
    return (explicitState != null) ? -explicitState.getSize() : 0;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new ExplicitSortedWaitlist(pSecondaryStrategy);
  }
}
