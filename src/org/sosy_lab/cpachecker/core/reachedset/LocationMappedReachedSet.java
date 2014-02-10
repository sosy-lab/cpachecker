/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.reachedset;

import java.util.Collection;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Advanced implementation of ReachedSet.
 * It groups states by location and allows fast access to all states with
 * the same location as a given one.
 */
public class LocationMappedReachedSet extends PartitionedReachedSet {

  public LocationMappedReachedSet(WaitlistFactory waitlistFactory) {
    super(waitlistFactory);
  }

  @Override
  public Collection<AbstractState> getReached(CFANode location) {
    return getReachedForKey(location);
  }

  @Override
  protected Object getPartitionKey(AbstractState pState) {
    CFANode location = AbstractStates.extractLocation(pState);
    assert location != null : "Location information necessary for LocationMappedReachedSet";
    return location;
  }

  @SuppressWarnings("unchecked")
  public Set<CFANode> getLocations() {
    // generic cast is safe because we only put CFANodes into it
    return (Set<CFANode>)super.getKeySet();
  }
}
