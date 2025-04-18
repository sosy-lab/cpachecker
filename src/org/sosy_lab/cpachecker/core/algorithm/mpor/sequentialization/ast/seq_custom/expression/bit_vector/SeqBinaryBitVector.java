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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.BitVectorInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqBinaryBitVector implements SeqBitVector {

  private final int length;

  private final ImmutableSet<Integer> setBits;

  public SeqBinaryBitVector(int pLength, ImmutableSet<Integer> pSetBits) {
    checkArgument(Collections.max(pSetBits) < BitVectorInjector.MAX_BIT_VECTOR_LENGTH);
    length = pLength;
    setBits = pSetBits;
  }

  @Override
  public String toASTString() {
    StringBuilder rBitVector = new StringBuilder();
    rBitVector.append(SeqToken._0b);
    for (int i = 0; i < length; i++) {
      rBitVector.append(setBits.contains(i) ? SeqToken._1 : SeqToken._0);
    }
    return rBitVector.toString();
  }
}
