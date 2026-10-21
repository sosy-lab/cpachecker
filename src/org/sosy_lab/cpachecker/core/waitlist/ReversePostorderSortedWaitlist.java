// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.collect.Streams;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ReversePostorderSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ReversePostorderSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  public void add(AbstractState pState) {
    assert AbstractStates.extractLocation(pState) != null
        || AbstractStates.extractLocations(pState).iterator().hasNext();
    super.add(pState);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    CFANode location = AbstractStates.extractLocation(pState);
    if (location != null) {
      return location.getReversePostorderId();
    }

    // For states with several locations, use the largest ID as the best reverse-postorder key.
    var maxId =
        Streams.stream(AbstractStates.extractLocations(pState))
            .mapToInt(CFANode::getReversePostorderId)
            .max();
    assert maxId.isPresent() : "State has no CFA locations";
    return maxId.orElseThrow(() -> new IllegalArgumentException("State has no CFA locations"));
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new ReversePostorderSortedWaitlist(pSecondaryStrategy);
  }
}
