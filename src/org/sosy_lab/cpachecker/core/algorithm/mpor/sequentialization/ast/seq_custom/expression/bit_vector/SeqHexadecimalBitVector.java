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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SeqBitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqHexadecimalBitVector implements SeqBitVector {

  private final int length;

  private final ImmutableSet<Integer> setBits;

  public SeqHexadecimalBitVector(int pLength, ImmutableSet<Integer> pSetBits) {
    checkArgument(pSetBits.isEmpty() || Collections.max(pSetBits) < BitVectorUtil.MAX_LENGTH);
    length = pLength;
    setBits = pSetBits;
  }

  @Override
  public String toASTString() {
    StringBuilder rBitVector = new StringBuilder();
    rBitVector.append(SeqToken._0x);
    // build the binary vector, then parse to long and convert to hex
    StringBuilder binaryBitVector = new StringBuilder();
    for (int i = 0; i < length; i++) {
      binaryBitVector.append(setBits.contains(i) ? SeqToken._1 : SeqToken._0);
    }
    // use long in case we have 64 length bit vectors
    long parsedLong = Long.parseLong(binaryBitVector.toString(), 2);
    // padding is not necessary, but looks nicer
    rBitVector.append(BitVectorUtil.padHexString(parsedLong));
    return rBitVector.toString();
  }

  @Override
  public SeqBitVectorEncoding getEncoding() {
    return SeqBitVectorEncoding.HEXADECIMAL;
  }
}
