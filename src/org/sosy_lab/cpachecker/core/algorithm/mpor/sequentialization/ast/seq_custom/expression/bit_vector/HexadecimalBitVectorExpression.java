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
import java.math.BigInteger;
import java.util.Collections;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorGlobalVariable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class HexadecimalBitVectorExpression implements BitVectorExpression {

  private final int hexLength;

  private final ImmutableSet<Integer> setBits;

  public HexadecimalBitVectorExpression(
      int pHexLength, ImmutableSet<BitVectorGlobalVariable> pSetBits) {
    ImmutableSet<Integer> intBits =
        pSetBits.stream()
            .map(BitVectorGlobalVariable::getId)
            .collect(ImmutableSet.toImmutableSet());
    // we still use the max binary length here, because setBits represents the binary positions
    checkArgument(pSetBits.isEmpty() || Collections.max(intBits) < BitVectorUtil.MAX_BINARY_LENGTH);
    hexLength = pHexLength;
    setBits = intBits;
  }

  @Override
  public String toASTString() {
    StringBuilder rBitVector = new StringBuilder();
    rBitVector.append(SeqToken._0x);
    // build the binary vector, then parse to long and convert to hex
    StringBuilder binaryBitVector = new StringBuilder();
    for (int i = 0; i < BitVectorUtil.convertHexLengthToBinary(hexLength); i++) {
      binaryBitVector.append(setBits.contains(i) ? SeqToken._1 : SeqToken._0);
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
