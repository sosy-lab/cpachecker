// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

public enum SeqMemoryAccessType {
  NONE("", ""),
  /** For both read and write. */
  ACCESS("a", "ACCESS"),
  READ("r", "READ"),
  WRITE("w", "WRITE");

  public final String shortName;

  public final String longName;

  SeqMemoryAccessType(String pShortName, String pLongName) {
    shortName = pShortName;
    longName = pLongName;
  }

  public boolean in(SeqMemoryAccessType... pMemoryAccessTypes) {
    for (SeqMemoryAccessType accessType : pMemoryAccessTypes) {
      if (accessType.equals(this)) {
        return true;
      }
    }
    return false;
  }
}
