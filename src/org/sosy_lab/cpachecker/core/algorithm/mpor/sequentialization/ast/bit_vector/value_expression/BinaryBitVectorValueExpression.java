// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.value_expression;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorUtil;

public class BinaryBitVectorValueExpression extends BitVectorValueExpression {

  private static final String BINARY_LITERAL = "0b";

  private final int binaryLength;

  private final ImmutableSet<Integer> setBits;

  public BinaryBitVectorValueExpression(int pBinaryLength, ImmutableSet<Integer> pSetBits) {
    checkArgument(
        pSetBits.isEmpty() || Collections.max(pSetBits) < BitVectorUtil.MAX_BINARY_LENGTH);
    binaryLength = pBinaryLength;
    setBits = pSetBits;
  }

  @Override
  public String toASTString() {
    StringBuilder rBitVector = new StringBuilder();
    rBitVector.append(BINARY_LITERAL);
    int leftIndex = BitVectorUtil.getLeftIndexByBinaryLength(binaryLength);
    // build bit vector from left to right
    for (int i = leftIndex; i >= BitVectorUtil.RIGHT_INDEX; i--) {
      rBitVector.append(setBits.contains(i) ? ONE_BIT : ZERO_BIT);
    }
    return rBitVector.toString();
  }

  @Override
  public BitVectorEncoding getEncoding() {
    return BitVectorEncoding.BINARY;
  }

  @Override
  public boolean isZero() {
    return setBits.isEmpty();
  }
}
