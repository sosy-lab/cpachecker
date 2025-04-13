// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SideEffectInfo {

  public enum AccessType {
    WRITE,
    READ
  }

  private final MemoryLocation memoryLocation;
  private final AccessType accessType;
  private final CFAEdge cfaEdge;

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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SideEffectInfo)) return false;
    SideEffectInfo that = (SideEffectInfo) o;
    return Objects.equals(memoryLocation, that.memoryLocation)
        && accessType == that.accessType
        && Objects.equals(cfaEdge, that.cfaEdge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(memoryLocation, accessType, cfaEdge);
  }

  @Override
  public String toString() {
    return String.format(
        "SideEffect[%s at %s, stmt=\"%s\" in %s at line %d, column %d]",
        accessType,
        memoryLocation.getExtendedQualifiedName(),
        cfaEdge.getRawStatement(),
        cfaEdge.getFileLocation().getNiceFileName(),
        cfaEdge.getLineNumber(),
        cfaEdge.getFileLocation().getStartColumnInLine());
  }

  public String toStringSimple(){
    return String.format(
        "SideEffect[%s at %s]",
        accessType,
        memoryLocation.getExtendedQualifiedName());
  }
}
