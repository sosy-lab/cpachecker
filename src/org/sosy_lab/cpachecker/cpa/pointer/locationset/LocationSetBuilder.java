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
import java.util.SortedSet;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.NullLocation;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.PointerTarget;

public class LocationSetBuilder {
  public static ExplicitLocationSet withPointerLocation(PointerTarget pLocation) {
    SortedSet<PointerTarget> build =
        ImmutableSortedSet.<PointerTarget>naturalOrder().add(pLocation).build();

    return new ExplicitLocationSet(build);
  }

  public static LocationSet withPointerTargets(Set<PointerTarget> pLocations) {
    if (pLocations.isEmpty()) {
      return LocationSetBot.INSTANCE;
    }
    return new ExplicitLocationSet(ImmutableSortedSet.copyOf(pLocations));
  }

  public static LocationSet withNullLocation() {
    return new ExplicitLocationSet(ImmutableSortedSet.of(new NullLocation()));
  }
}
