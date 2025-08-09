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
import org.sosy_lab.cpachecker.cpa.pointer.locationset.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer.locationset.LocationSetFactory;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.HeapLocation;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.InvalidLocation;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.InvalidationReason;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.MemoryLocationPointer;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.PointerTarget;
import org.sosy_lab.cpachecker.cpa.pointer.pointertarget.StructLocation;

public final class PointerArithmeticUtils {

  private PointerArithmeticUtils() {}

  public static LocationSet applyPointerArithmetic(
      LocationSet pBaseLocations, long pOffset, boolean pIsOffsetSensitive) {
    if (pBaseLocations.isTop() || pBaseLocations.isBot()) {
      return pBaseLocations;
    }

    if (pBaseLocations.containsAllNulls()) {
      return LocationSetFactory.withPointerLocation(
          InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC));
    }

    if (pBaseLocations instanceof ExplicitLocationSet pExplicitLocationSet) {
      Set<PointerTarget> resultTargets =
          pExplicitLocationSet.sortedPointerTargets().stream()
              .map(target -> applyOffsetToTarget(target, pOffset, pIsOffsetSensitive))
              .collect(Collectors.toSet());

      return LocationSetFactory.withPointerTargets(resultTargets);
    }

    return LocationSetFactory.withTop();
  }

  private static PointerTarget applyOffsetToTarget(
      PointerTarget target, long offset, boolean pIsOffsetSensitive) {
    if (target instanceof InvalidLocation || target instanceof StructLocation) {
      return InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC);
    }
    if (target instanceof MemoryLocationPointer memPtr) {
      return applyOffsetToMemoryLocation(memPtr, offset, pIsOffsetSensitive);
    }
    if (target instanceof HeapLocation heapLoc) {
      return applyOffsetToHeapLocation(heapLoc, offset, pIsOffsetSensitive);
    }
    // We cannot apply pointer arithmetic to null target, so we proceed with invalidation.
    return InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC);
  }

  private static PointerTarget applyOffsetToMemoryLocation(
      MemoryLocationPointer memPtr, long offset, boolean pIsOffsetSensitive) {
    if (offset == 0) {
      return memPtr;
    }

    if (!memPtr.memoryLocation().isReference()) {
      return InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC);
    }

    long newOffset = memPtr.memoryLocation().getOffset() + offset;
    if (newOffset < 0) {
      return InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC);
    }
    if (pIsOffsetSensitive) {
      return new MemoryLocationPointer(memPtr.memoryLocation().withAddedOffset(offset));
    } else {
      return memPtr;
    }
  }

  private static PointerTarget applyOffsetToHeapLocation(
      HeapLocation heapLoc, long offset, boolean pIsOffsetSensitive) {
    if (offset == 0) {
      return heapLoc;
    }

    if (heapLoc.hasOffset()) {
      long newOffset = heapLoc.getOffset() + offset;
      if (newOffset < 0) {
        return InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC);
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
