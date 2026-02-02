// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.expression;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public final class CBitVectorBinaryLiteralExpression extends CBitVectorLiteralExpression {

  public CBitVectorBinaryLiteralExpression(ImmutableSet<Integer> pSetBits, CSimpleType pType) {
    super(pSetBits, pType);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder rBitVector = new StringBuilder();
    rBitVector.append(BINARY_PREFIX);
    // build bit vector from right to left and then reverse
    for (int i = RIGHT_MOST_INDEX; i < binaryLength + RIGHT_MOST_INDEX; i++) {
      rBitVector.append(oneBits.contains(i) ? ONE_BIT : ZERO_BIT);
    }
    return rBitVector.reverse().toString();
  }
}
