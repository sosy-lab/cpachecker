// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Advanced implementation of ReachedSet. It groups states by location and allows fast access to all
 * states with the same location as a given one.
 */
public class LocationMappedReachedSet extends PartitionedReachedSet {

  public LocationMappedReachedSet(
      ConfigurableProgramAnalysis pCpa, WaitlistFactory waitlistFactory) {
    super(pCpa, waitlistFactory);
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    checkNotNull(location);
    return getReachedForKey(location);
  }

  @Override
  protected Object getPartitionKey(AbstractState pState) {
    CFANode location = AbstractStates.extractLocation(pState);
    checkNotNull(location, "Location information necessary for LocationMappedReachedSet");
    return location;
  }

  @SuppressWarnings("unchecked")
  public Set<CFANode> getLocations() {
    // generic cast is safe because we only put CFANodes into it
    return (Set<CFANode>) super.getKeySet();
  }
}
