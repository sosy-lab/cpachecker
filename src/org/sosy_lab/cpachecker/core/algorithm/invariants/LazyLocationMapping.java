// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class LazyLocationMapping {
  private final UnmodifiableReachedSet reachedSet;

  private final AtomicReference<Multimap<CFANode, AbstractState>> statesByLocationRef =
      new AtomicReference<>();

  public LazyLocationMapping(UnmodifiableReachedSet pReachedSet) {
    reachedSet = Objects.requireNonNull(pReachedSet);
  }

  public Iterable<AbstractState> get(
      CFANode pLocation, Optional<CallstackStateEqualsWrapper> callstackInformation) {
    Iterable<AbstractState> out = get0(pLocation);
    if (!callstackInformation.isPresent()) {
      return out;
    }

    // Filter by callstack manually.
    // Number of states, once filtered by location, should be small enough to make
    // explicit filtering feasible.
    List<AbstractState> toReturn = new ArrayList<>();
    for (AbstractState returned : out) {
      Optional<CallstackStateEqualsWrapper> returnedCallstackInfo =
          AbstractStates.extractOptionalCallstackWraper(returned);

      // todo: cache?
      if (returnedCallstackInfo.equals(callstackInformation)) {
        toReturn.add(returned);
      }
    }
    return toReturn;
  }

  public Iterable<AbstractState> get0(CFANode pLocation) {
    if (reachedSet instanceof LocationMappedReachedSet) {
      return AbstractStates.filterLocation(reachedSet, pLocation);
    }
    if (statesByLocationRef.get() == null) {
      Multimap<CFANode, AbstractState> statesByLocation = HashMultimap.create();
      for (AbstractState state : reachedSet) {
        for (CFANode location : AbstractStates.extractLocations(state)) {
          statesByLocation.put(location, state);
        }
      }
      statesByLocationRef.set(statesByLocation);
      return statesByLocation.get(pLocation);
    }
    return statesByLocationRef.get().get(pLocation);
  }
}
