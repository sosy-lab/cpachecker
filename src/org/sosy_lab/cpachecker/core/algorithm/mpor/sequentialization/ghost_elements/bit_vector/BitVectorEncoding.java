// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;

public enum BitVectorEncoding {
  NONE(false, false),
  BINARY(true, false),
  DECIMAL(true, false),
  HEXADECIMAL(true, false),
  SPARSE(false, true);

  public final boolean isDense;

  public final boolean isSparse;

  BitVectorEncoding(boolean pIsDense, boolean pIsSparse) {
    isDense = pIsDense;
    isSparse = pIsSparse;
  }

  public boolean isEnabled() {
    return !this.equals(NONE);
  }
}
