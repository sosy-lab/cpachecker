// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class OverapproximatingMemoryLocation {

  // A set of potential memory locations,
  // this is not guaranteed to include all potential locations if isPrecise is false
  private final ImmutableSet<MemoryLocation> memoryLocations;
  private final CType type;
  private final boolean isAmbiguous;
  private final boolean isPrecise;
  // Whether a pointer may point to this location, i.e., its address is taken or it is an array
  private final boolean mayBePointedTo;

  /**
   * Create an {@link OverapproximatingMemoryLocation} that may represent any memory location of the
   * given type.
   */
  public OverapproximatingMemoryLocation(CType pType) {
    this(ImmutableSet.of(), pType, true, false, true);
  }

  public OverapproximatingMemoryLocation(
      Set<MemoryLocation> pMemoryLocations,
      CType pType,
      boolean pIsAmbiguous,
      boolean pIsPrecise,
      boolean pMayBePointedTo) {
    memoryLocations = ImmutableSet.copyOf(pMemoryLocations);
    type = pType;
    isAmbiguous = pIsAmbiguous;
    isPrecise = pIsPrecise;
    mayBePointedTo = pMayBePointedTo;
  }

  public Set<MemoryLocation> getMemoryLocations() {
    return memoryLocations;
  }

  public CType getType() {
    return type;
  }

  public boolean isAmbiguous() {
    return isAmbiguous;
  }

  public boolean isPrecise() {
    return isPrecise;
  }

  public boolean mayBePointedTo() {
    return mayBePointedTo;
  }

  @Override
  public String toString() {
    if (isPrecise) {
      return memoryLocations.toString();
    }
    return "OverapproximatingMemoryLocation{" + type + "}";
  }
}
