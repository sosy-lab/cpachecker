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
import java.math.BigInteger;
import java.util.Collections;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorUtil;

public class HexadecimalBitVectorValueExpression extends BitVectorValueExpression {

  private static final String HEXADECIMAL_LITERAL = "0x";

  private final int hexLength;

  private final ImmutableSet<Integer> setBits;

  public HexadecimalBitVectorValueExpression(int pHexLength, ImmutableSet<Integer> pSetBits) {
    // we still use the max binary length here, because setBits represents the binary positions
    checkArgument(
        pSetBits.isEmpty() || Collections.max(pSetBits) < BitVectorUtil.MAX_BINARY_LENGTH);
    hexLength = pHexLength;
    setBits = pSetBits;
  }

  @Override
  public String toASTString() {
    StringBuilder rBitVector = new StringBuilder();
    rBitVector.append(HEXADECIMAL_LITERAL);
    // build the binary vector, then parse to long and convert to hex
    StringBuilder binaryBitVector = new StringBuilder();
    int binLength = BitVectorUtil.convertHexLengthToBinary(hexLength);
    int leftIndex = BitVectorUtil.getLeftIndexByBinaryLength(binLength);
    // build bit vector from left to right
    for (int i = leftIndex; i >= BitVectorUtil.RIGHT_INDEX; i--) {
      binaryBitVector.append(setBits.contains(i) ? ONE_BIT : ZERO_BIT);
    }
    // use long in case we have 64 length bit vectors
    BigInteger bigInteger = new BigInteger(binaryBitVector.toString(), 2);
    // padding is not necessary, but looks nicer
    rBitVector.append(BitVectorUtil.padHexString(hexLength, bigInteger));
    return rBitVector.toString();
  }

  @Override
  public BitVectorEncoding getEncoding() {
    return BitVectorEncoding.HEXADECIMAL;
  }

  @Override
  public boolean isZero() {
    return setBits.isEmpty();
  }
}
