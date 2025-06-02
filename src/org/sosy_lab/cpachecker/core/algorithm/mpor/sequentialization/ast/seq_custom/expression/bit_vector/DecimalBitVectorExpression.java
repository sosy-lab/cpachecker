// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;

public class DecimalBitVectorExpression implements BitVectorExpression {

  private final ImmutableSet<Integer> setBits;

  public DecimalBitVectorExpression(ImmutableSet<Integer> pSetBits) {
    checkArgument(
        pSetBits.isEmpty() || Collections.max(pSetBits) < BitVectorUtil.MAX_BINARY_LENGTH);
    setBits = pSetBits;
  }

  @Override
  public String toASTString() {
    int bitSum = setBits.stream().mapToInt(Integer::intValue).sum();
    return String.valueOf(bitSum);
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
