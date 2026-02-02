// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.value_expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorEncoding;

public class SparseBitVectorValueExpression extends BitVectorValueExpression {

  private final boolean value;

  public SparseBitVectorValueExpression(boolean pValue) {
    value = pValue;
  }

  @Override
  public String toASTString() {
    return value ? ONE_BIT : ZERO_BIT;
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
