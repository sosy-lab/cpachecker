// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ReversePostorderSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ReversePostorderSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  public void add(AbstractState pState) {
    assert AbstractStates.extractLocations(pState) != null;
    super.add(pState);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    int sum = 0;
    Iterable<CFANode> locations = AbstractStates.extractLocations(pState);
    for (CFANode location : locations) {
      sum += location.getReversePostorderId();
    }
    int average = sum / Iterables.size(locations);

    return average;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new ReversePostorderSortedWaitlist(pSecondaryStrategy);
  }
}
