// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.locationset;

import com.google.common.collect.ImmutableSortedSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.NullLocation;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.PointerTarget;

public class LocationSetFactory {
  public static ExplicitLocationSet withPointerLocation(PointerTarget pLocation) {
    ImmutableSortedSet<PointerTarget> build =
        ImmutableSortedSet.<PointerTarget>naturalOrder().add(pLocation).build();

    return new ExplicitLocationSet(build);
  }

  public static LocationSet withPointerTargets(Set<PointerTarget> pLocations) {
    if (pLocations.isEmpty()) {
      return withBot();
    }
    return new ExplicitLocationSet(ImmutableSortedSet.copyOf(pLocations));
  }

  public static LocationSet withNullLocation() {
    return new ExplicitLocationSet(ImmutableSortedSet.of(new NullLocation()));
  }

  public static LocationSet withTop() {
    return LocationSetTop.INSTANCE;
  }

  public static LocationSet withBot() {
    return LocationSetBot.INSTANCE;
  }

  static LocationSet ofTargets(Set<PointerTarget> targets) {
    if (targets == null || targets.isEmpty()) {
      return withBot();
    }
    // Keep explicit representation
    return new ExplicitLocationSet(com.google.common.collect.ImmutableSortedSet.copyOf(targets));
  }

  static LocationSet ofPointer(PointerTarget target) {
    return ofTargets(java.util.Collections.singleton(target));
  }
}
