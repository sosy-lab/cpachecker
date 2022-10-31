// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class OverapproximatingMemoryLocation {

  // The set of potential memory locations
  private final Set<MemoryLocation> memoryLocations;

  private final CType type;

  public OverapproximatingMemoryLocation(Set<MemoryLocation> pMemoryLocations, CType pType) {
    memoryLocations = pMemoryLocations;
    type = pType;
  }

  public Set<MemoryLocation> getMemoryLocations() {
    return memoryLocations;
  }

  public CType getType() {
    return type;
  }

  @Override
  public String toString() {
    return memoryLocations.toString();
  }
}
