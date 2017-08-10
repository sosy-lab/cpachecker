/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.powerset;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

public class PowerSetState implements AbstractWrapperState, Targetable {

  private final transient PowerSetState merged1;
  private final transient PowerSetState merged2;

  private final Set<AbstractState> setOfStates;

  public PowerSetState(final Set<AbstractState> states) {
    merged1 = merged2 = null;
    setOfStates = states;
  }

  public PowerSetState(final Set<AbstractState> states, final PowerSetState state1, final PowerSetState state2) {
    merged1 = state1;
    merged2 = state2;
    setOfStates = states;
  }

  public boolean isMergedInto(final PowerSetState pState) {
    return pState == merged1 || pState == merged2;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((setOfStates == null) ? 0 : setOfStates.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (getClass() != obj.getClass()) { return false; }
    PowerSetState other = (PowerSetState) obj;
    if (setOfStates == null) {
      if (other.setOfStates != null) { return false; }
    } else if (!setOfStates.equals(other.setOfStates)) { return false; }
    return true;
  }

  Set<AbstractState> getSet() {
    return setOfStates;
  }

  public boolean contains(final AbstractState pState) {
    return setOfStates.contains(pState);
  }

  @Override
  public Iterable<AbstractState> getWrappedStates() {
    return ImmutableList.copyOf(setOfStates);
  }

  @Override
  public boolean isTarget() {
    for (AbstractState state : setOfStates) {
      if (state instanceof Targetable && ((Targetable) state).isTarget()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nonnull Set<Property> getViolatedProperties() throws IllegalStateException {
    Set<Property> result = new HashSet<>();

    for (AbstractState state : setOfStates) {
      if (state instanceof Targetable && ((Targetable) state).isTarget()) {
        result.addAll(((Targetable) state).getViolatedProperties());
      }
    }

    return result;
  }

}
