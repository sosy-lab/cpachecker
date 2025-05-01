// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SideEffectInfo {

  public enum AccessType {
    WRITE,
    READ
  }

  private final MemoryLocation memoryLocation;
  private final AccessType accessType;
  private final CFAEdge cfaEdge; // location of side effect

  public SideEffectInfo(MemoryLocation pMemoryLocation, AccessType pAccessType, CFAEdge pCfaEdge) {
    memoryLocation = pMemoryLocation;
    accessType = pAccessType;
    cfaEdge = pCfaEdge;
  }

  public MemoryLocation getMemoryLocation() {
    return memoryLocation;
  }

  public AccessType getAccessType() {
    return accessType;
  }

  public CFAEdge getCfaEdge() {
    return cfaEdge;
  }

  public boolean isWrite() {
    return accessType == AccessType.WRITE;
  }

  public boolean isRead() {
    return accessType == AccessType.READ;
  }

  @Override
  public boolean equals(@Nullable Object pOther) {
    if (this == pOther) {
      return true;
    }
    // Intentionally using instanceof instead of getClass() to comply with ErrorProne
    if (!(pOther instanceof SideEffectInfo other)) {
      return false;
    }
    return Objects.equals(memoryLocation, other.memoryLocation)
        && accessType == other.accessType
        && Objects.equals(cfaEdge.getCode(), other.cfaEdge.getCode());
  }

  @Override
  public int hashCode() {
    return Objects.hash(memoryLocation, accessType, cfaEdge.getCode());
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s at %s (line %d, col %d) %s ",
        memoryLocation.getExtendedQualifiedName(),
        accessType,
        cfaEdge.getFileLocation().getNiceFileName(),
        cfaEdge.getFileLocation().getStartingLineInOrigin(),
        cfaEdge.getFileLocation().getStartColumnInLine(),
        cfaEdge.getCode());
  }

  public String toStringSimple() {
    return String.format(
        "%s@%s:line %d",
        memoryLocation.getExtendedQualifiedName(),
        accessType,
        cfaEdge.getFileLocation().getStartingLineInOrigin());
  }
}
