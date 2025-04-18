// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public enum SeqBitVectorType {
  __UINT8_T(8),
  __UINT16_T(16),
  __UINT32_T(32),
  __UINT64_T(64);

  public final int size;

  SeqBitVectorType(int pSize) {
    size = pSize;
  }

  public String toASTString() {
    return SeqToken.__uint + size + SeqToken._t;
  }
}
