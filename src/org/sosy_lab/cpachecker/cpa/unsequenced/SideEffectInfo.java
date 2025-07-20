// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import com.google.common.collect.ComparisonChain;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Records side-effect information caused by a program operation, such as a memory write or read
 * through a pointer or global variable.
 *
 * @param memoryLocation the location that was read from or written to; If sideEffectKind is
 *     POINTER_DEREFERENCE_UNRESOLVED, then memoryLocation refers to the pointer variable, not the
 *     actual target address (which may be unknown at this point).
 * @param accessType the type of access performed: either READ or WRITE.
 * @param cfaEdge the CFA edge on which the side effect occurs, useful for diagnostics.
 * @param sideEffectKind the kind of memory access: global variable, pointer dereference unresolved
 *     and pointer dereference resolved
 */
public record SideEffectInfo(
    MemoryLocation memoryLocation,
    AccessType accessType,
    CFAEdge cfaEdge,
    SideEffectKind sideEffectKind)
    implements Comparable<SideEffectInfo> {

  public enum AccessType {
    WRITE,
    READ
  }

  public enum SideEffectKind {
    GLOBAL_VARIABLE,
    POINTER_DEREFERENCE_UNRESOLVED,
    // isResolvedPointer true if this pointer dereference has already been resolved to its alias
    POINTER_DEREFERENCE_RESOLVED,
  }

  public boolean isWrite() {
    return accessType == AccessType.WRITE;
  }

  public boolean isRead() {
    return accessType == AccessType.READ;
  }

  public boolean isUnresolvedPointer() {
    return sideEffectKind == SideEffectKind.POINTER_DEREFERENCE_UNRESOLVED;
  }

  @Override
  public int compareTo(SideEffectInfo other) {
    return ComparisonChain.start()
        .compare(memoryLocation, other.memoryLocation)
        .compare(accessType, other.accessType)
        .compare(sideEffectKind, other.sideEffectKind)
        .compare(cfaEdge.getFileLocation(), other.cfaEdge.getFileLocation())
        .result();
  }

  @Override
  public String toString() {
    String locInfo;

    if (sideEffectKind == SideEffectKind.POINTER_DEREFERENCE_RESOLVED) {
      locInfo = "pointee " + memoryLocation;
    } else if (sideEffectKind == SideEffectKind.POINTER_DEREFERENCE_UNRESOLVED) {
      locInfo = "pointer " + memoryLocation;
    } else {
      locInfo = memoryLocation.toString();
    }

    return String.format(
        "[%s] %s@%s at %s (line %d, col %d) %s",
        sideEffectKind,
        locInfo,
        accessType,
        cfaEdge.getFileLocation().getNiceFileName(),
        cfaEdge.getFileLocation().getStartingLineInOrigin(),
        cfaEdge.getFileLocation().getStartColumnInLine(),
        cfaEdge.getCode());
  }
}
