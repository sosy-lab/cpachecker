// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

public enum BitVectorEncoding {
  NONE(false),
  BINARY(true),
  HEXADECIMAL(true),
  SCALAR(false);

  public final boolean isDense;

  BitVectorEncoding(boolean pIsDense) {
    isDense = pIsDense;
  }
}
