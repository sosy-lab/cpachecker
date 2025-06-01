// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public record SideEffectInfo(
    MemoryLocation memoryLocation, AccessType accessType, CFAEdge cfaEdge) {

  public enum AccessType {
    WRITE,
    READ
  }

  public boolean isWrite() {
    return accessType == AccessType.WRITE;
  }

  public boolean isRead() {
    return accessType == AccessType.READ;
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s at %s (line %d, col %d) %s",
        memoryLocation.getExtendedQualifiedName(),
        accessType,
        cfaEdge.getFileLocation().getNiceFileName(),
        cfaEdge.getFileLocation().getStartingLineInOrigin(),
        cfaEdge.getFileLocation().getStartColumnInLine(),
        cfaEdge.getCode());
  }
}
