// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;

public record DecimalBitVectorValueExpression(ImmutableSet<Integer> setBits)
    implements BitVectorValueExpression {

  public DecimalBitVectorValueExpression {
    checkArgument(setBits.isEmpty() || Collections.max(setBits) < BitVectorUtil.MAX_BINARY_LENGTH);
  }

  @Override
  public String toASTString() {
    return String.valueOf(BitVectorUtil.buildDecimalBitVector(setBits));
  }

  @Override
  public BitVectorEncoding getEncoding() {
    return BitVectorEncoding.DECIMAL;
  }

  @Override
  public boolean isZero() {
    return setBits.isEmpty();
  }
}
