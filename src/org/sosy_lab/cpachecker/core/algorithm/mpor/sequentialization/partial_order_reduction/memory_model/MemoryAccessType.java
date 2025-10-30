// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public enum MemoryAccessType {
  NONE(SeqSyntax.EMPTY_STRING, SeqSyntax.EMPTY_STRING),
  /** For both read and write. */
  ACCESS(SeqToken.ACCESS_BIT_VECTOR_PREFIX, SeqToken.ACCESS),
  READ(SeqToken.READ_BIT_VECTOR_PREFIX, SeqToken.READ),
  WRITE(SeqToken.WRITE_BIT_VECTOR_PREFIX, SeqToken.WRITE);

  public final String shortName;

  public final String longName;

  MemoryAccessType(String pShortName, String pLongName) {
    shortName = pShortName;
    longName = pLongName;
  }

  public boolean in(MemoryAccessType... pMemoryAccessTypes) {
    for (MemoryAccessType accessType : pMemoryAccessTypes) {
      if (accessType.equals(this)) {
        return true;
      }
    }
    return false;
  }
}
