// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.powerset;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
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

  public PowerSetState(
      final Set<AbstractState> states, final PowerSetState state1, final PowerSetState state2) {
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
    if (this == obj) {
      return true;
    }
    return obj instanceof PowerSetState
        && Objects.equals(setOfStates, ((PowerSetState) obj).setOfStates);
  }

  @Override
  public ImmutableSet<AbstractState> getWrappedStates() {
    return setOfStates;
  }

  @Override
  public boolean isTarget() {
    return Iterables.any(setOfStates, AbstractStates::isTargetState);
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return FluentIterable.from(setOfStates)
        .filter(AbstractStates::isTargetState)
        .transformAndConcat(s -> ((Targetable) s).getTargetInformation())
        .toSet();
  }
}
