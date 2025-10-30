// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public record SparseBitVectorValueExpression(boolean value) implements BitVectorValueExpression {

  @Override
  public String toASTString() {
    return value ? SeqToken.ONE_BIT : SeqToken.ZERO_BIT;
  }

  @Override
  public BitVectorEncoding getEncoding() {
    return BitVectorEncoding.SPARSE;
  }

  @Override
  public boolean isZero() {
    return !value;
  }
}
