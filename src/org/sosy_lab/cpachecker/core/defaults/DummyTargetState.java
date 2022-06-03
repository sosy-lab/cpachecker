// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

/**
 * This is a simple {@link AbstractState} implements that has {@link Targetable#isTarget} always
 * return <code>true</code>. It can be used by algorithms that do not naturally produce a set of
 * reachable states, but need to have a target state in the resulting reached set in order to signal
 * a found property violation.
 */
public final class DummyTargetState implements AbstractState, Targetable, Partitionable {

  private final ImmutableSet<TargetInformation> properties;

  private DummyTargetState(ImmutableSet<TargetInformation> pProperties) {
    properties = pProperties;
  }

  public static DummyTargetState withTargetInofmration(TargetInformation pProp) {
    return new DummyTargetState(ImmutableSet.of(pProp));
  }

  public static DummyTargetState withSimpleTargetInformation(String pPropText) {
    return withTargetInofmration(SimpleTargetInformation.create(pPropText));
  }

  public static DummyTargetState withoutTargetInformation() {
    return new DummyTargetState(ImmutableSet.of());
  }

  public static DummyTargetState withTargetInformation(Iterable<TargetInformation> pProperties) {
    return new DummyTargetState(ImmutableSet.copyOf(pProperties));
  }

  @Override
  public int hashCode() {
    return properties.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DummyTargetState)) {
      return false;
    }

    DummyTargetState other = (DummyTargetState) obj;
    return properties.equals(other.properties);
  }

  @Override
  public boolean isTarget() {
    return true;
  }

  @Override
  public Set<TargetInformation> getTargetInformation() {
    return properties;
  }

  @Override
  public @Nullable Object getPartitionKey() {
    return this;
  }
}
