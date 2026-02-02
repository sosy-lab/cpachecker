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

public final class CBitVectorDecimalLiteralExpression extends CBitVectorLiteralExpression {

  public CBitVectorDecimalLiteralExpression(ImmutableSet<Integer> pSetBits, CSimpleType pType) {
    super(pSetBits, pType);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    // use long to support up to 64 bits
    long rSum = 0;
    for (int oneBit : oneBits) {
      // use shift expression, equivalent to 2^oneBit
      rSum += 1L << (oneBit - RIGHT_MOST_INDEX);
    }
    return String.valueOf(rSum);
  }
}
