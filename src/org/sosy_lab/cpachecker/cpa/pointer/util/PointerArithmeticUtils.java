// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.pointer.location.DeclaredVariableLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.HeapLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.InvalidationReason;
import org.sosy_lab.cpachecker.cpa.pointer.location.PointerLocation;
import org.sosy_lab.cpachecker.cpa.pointer.location.StructLocation;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;

public final class PointerArithmeticUtils {

  private PointerArithmeticUtils() {}

  public static LocationSet applyPointerArithmetic(
      LocationSet pBaseLocations, long pOffset, boolean pIsOffsetSensitive) {
    if (pBaseLocations.isTop() || pBaseLocations.isBot()) {
      return pBaseLocations;
    }

    if (pBaseLocations.containsAllNulls()) {
      return LocationSetFactory.withPointerLocation(
          new InvalidLocation(InvalidationReason.POINTER_ARITHMETIC));
    }

    if (pBaseLocations instanceof ExplicitLocationSet pExplicitLocationSet) {
      Set<PointerLocation> resultTargets =
          pExplicitLocationSet.sortedPointerLocations().stream()
              .map(target -> applyOffsetToTarget(target, pOffset, pIsOffsetSensitive))
              .collect(Collectors.toSet());

      return LocationSetFactory.withPointerTargets(resultTargets);
    }

    return LocationSetFactory.withTop();
  }

  private static PointerLocation applyOffsetToTarget(
      PointerLocation target, long offset, boolean pIsOffsetSensitive) {
    if (target instanceof InvalidLocation || target instanceof StructLocation) {
      return new InvalidLocation(InvalidationReason.POINTER_ARITHMETIC);
    }
    if (target instanceof DeclaredVariableLocation memPtr) {
      return applyOffsetToMemoryLocation(memPtr, offset, pIsOffsetSensitive);
    }
    if (target instanceof HeapLocation heapLoc) {
      return applyOffsetToHeapLocation(heapLoc, offset, pIsOffsetSensitive);
    }
    // We cannot apply pointer arithmetic to null target, so we proceed with invalidation.
    return new InvalidLocation(InvalidationReason.POINTER_ARITHMETIC);
  }

  private static PointerLocation applyOffsetToMemoryLocation(
      DeclaredVariableLocation memPtr, long offset, boolean pIsOffsetSensitive) {
    if (offset == 0) {
      return memPtr;
    }

    if (!memPtr.memoryLocation().isReference()) {
      return new InvalidLocation(InvalidationReason.POINTER_ARITHMETIC);
    }

    long newOffset = memPtr.memoryLocation().getOffset() + offset;
    if (newOffset < 0) {
      return new InvalidLocation(InvalidationReason.POINTER_ARITHMETIC);
    }
    if (pIsOffsetSensitive) {
      return new DeclaredVariableLocation(memPtr.memoryLocation().withAddedOffset(offset));
    } else {
      return memPtr;
    }
  }

  private static PointerLocation applyOffsetToHeapLocation(
      HeapLocation heapLoc, long offset, boolean pIsOffsetSensitive) {
    if (offset == 0) {
      return heapLoc;
    }

    if (heapLoc.hasOffset()) {
      long newOffset = heapLoc.getOffset() + offset;
      if (newOffset < 0) {
        return new InvalidLocation(InvalidationReason.POINTER_ARITHMETIC);
      }
      if (pIsOffsetSensitive) {
        return heapLoc.withAddedOffset(offset);
      } else {
        return heapLoc;
      }
    }
    return heapLoc;
  }
}
