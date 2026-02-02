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
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorUtil;

public class DecimalBitVectorValueExpression extends BitVectorValueExpression {

  private final ImmutableSet<Integer> setBits;

  public DecimalBitVectorValueExpression(ImmutableSet<Integer> pSetBits) {
    checkArgument(
        pSetBits.isEmpty() || Collections.max(pSetBits) < BitVectorUtil.MAX_BINARY_LENGTH);
    setBits = pSetBits;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
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
