// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer2.util;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public interface LocationSet {

  boolean mayPointTo(MemoryLocation pLocation);

  LocationSet addElement(MemoryLocation pLocation);

  LocationSet removeElement(MemoryLocation pLocation);

  LocationSet addElements(Iterable<MemoryLocation> pLocations);

  LocationSet addElements(LocationSet pLocations);

  boolean isBot();

  boolean isTop();

  boolean containsAll(LocationSet pLocations);
}
