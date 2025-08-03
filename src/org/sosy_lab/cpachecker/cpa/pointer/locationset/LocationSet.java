// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.locationset;

import java.util.Set;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.PointerTarget;

public interface LocationSet extends Comparable<LocationSet> {

  boolean mayPointTo(PointerTarget pLocation);

  LocationSet addElements(Set<PointerTarget> pLocations);

  LocationSet addElements(Set<PointerTarget> pLocations, boolean pContainsNull);

  LocationSet addElements(LocationSet pLocations);

  boolean isBot();

  boolean isTop();

  boolean isNull();

  boolean containsNull();

  boolean containsAll(LocationSet pLocations);
}
