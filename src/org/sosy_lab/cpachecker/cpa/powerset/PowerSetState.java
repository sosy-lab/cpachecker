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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class PowerSetState implements AbstractWrapperState, Targetable {

  private final transient PowerSetState merged1;
  private final transient PowerSetState merged2;

  private final ImmutableSet<AbstractState> setOfStates;

  public PowerSetState(final Set<AbstractState> states) {
    merged1 = merged2 = null;
    setOfStates = ImmutableSet.copyOf(states);
  }

  public PowerSetState(final Set<AbstractState> states, final PowerSetState state1, final PowerSetState state2) {
    merged1 = state1;
    merged2 = state2;
    setOfStates = ImmutableSet.copyOf(states);
  }

  public boolean isMergedInto(final PowerSetState pState) {
    return pState == merged1 || pState == merged2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(setOfStates);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    return obj instanceof PowerSetState
        && Objects.equals(setOfStates, ((PowerSetState) obj).setOfStates);
  }

  @Override
  public ImmutableSet<AbstractState> getWrappedStates() {
    return setOfStates;
  }

  @Override
  public boolean isTarget() {
    return Iterables.any(setOfStates, AbstractStates.IS_TARGET_STATE);
  }

  @Override
  public @Nonnull Set<Property> getViolatedProperties() throws IllegalStateException {
    return FluentIterable.from(setOfStates)
        .filter(AbstractStates.IS_TARGET_STATE)
        .transformAndConcat(s -> ((Targetable) s).getViolatedProperties())
        .toSet();
  }

}
