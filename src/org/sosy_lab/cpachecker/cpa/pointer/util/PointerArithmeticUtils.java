// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.util;

import java.util.HashSet;
import java.util.Set;

public final class PointerArithmeticUtils {

  private PointerArithmeticUtils() {}

  public static LocationSet applyPointerArithmetic(
      LocationSet pBaseLocations, long pOffset, boolean pIsOffsetSensitive) {
    if (pBaseLocations.isTop() || pBaseLocations.isBot()) {
      return pBaseLocations;
    }

    if (pBaseLocations.isNull()) {
      return ExplicitLocationSet.from(
          InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC));
    }

    if (!(pBaseLocations instanceof ExplicitLocationSet explicitSet)) {
      return LocationSetTop.INSTANCE;
    }

    Set<PointerTarget> resultTargets = new HashSet<>();
    for (PointerTarget target : explicitSet.getExplicitLocations()) {
      resultTargets.add(applyOffsetToTarget(target, pOffset, pIsOffsetSensitive));
    }
    return ExplicitLocationSet.from(resultTargets, explicitSet.containsNull());
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
    return InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC);
  }

  private static PointerTarget applyOffsetToMemoryLocation(
      MemoryLocationPointer memPtr, long offset, boolean pIsOffsetSensitive) {
    if (offset == 0) {
      return memPtr;
    }

    if (!memPtr.getMemoryLocation().isReference()) {
      return InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC);
    }

    long newOffset = memPtr.getMemoryLocation().getOffset() + offset;
    if (newOffset < 0) {
      return InvalidLocation.forInvalidation(InvalidationReason.POINTER_ARITHMETIC);
    }
    if (pIsOffsetSensitive) {
      return new MemoryLocationPointer(memPtr.getMemoryLocation().withAddedOffset(offset));
    } else {
      return memPtr;
    }
  }

  private static PointerTarget applyOffsetToHeapLocation(
      HeapLocation heapLoc, long offset, boolean pIsOffsetSensitive) {
    if (offset == 0) {
      return heapLoc;
    }

    if (heapLoc.isReference()) {
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
