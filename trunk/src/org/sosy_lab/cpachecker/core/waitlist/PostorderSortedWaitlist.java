// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class PostorderSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected PostorderSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  public void add(AbstractState pState) {
    assert AbstractStates.extractLocation(pState) != null;
    super.add(pState);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    return 0 - AbstractStates.extractLocation(pState).getReversePostorderId();
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new PostorderSortedWaitlist(pSecondaryStrategy);
  }
}
