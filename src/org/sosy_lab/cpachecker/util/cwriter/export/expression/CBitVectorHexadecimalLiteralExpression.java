// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.expression;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class CBitVectorHexadecimalLiteralExpression extends CBitVectorLiteralExpression {

  private static final String HEXADECIMAL_PREFIX = "0x";

  public CBitVectorHexadecimalLiteralExpression(ImmutableSet<Integer> pOneBits, CSimpleType pType) {
    super(pOneBits, pType);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringBuilder rBitVector = new StringBuilder();
    rBitVector.append(HEXADECIMAL_PREFIX);

    // build a binary vector, then parse to long and convert to hex
    CBitVectorBinaryLiteralExpression binaryBitVector =
        new CBitVectorBinaryLiteralExpression(oneBits, type);

    // create the string of the binary bit vector, and strip its '0b' prefix
    String binaryString =
        binaryBitVector.toASTString(pAAstNodeRepresentation).substring(BINARY_PREFIX.length());

    // use long in case we have 64 length bit vectors
    BigInteger bigInteger = new BigInteger(binaryString, 2);
    int hexadecimalLength = binaryLength / 4;

    // pad the string to exactly the hexadecimal length, e.g. '0x0' -> '0x00' for length 2
    rBitVector.append(String.format("%0" + hexadecimalLength + "x", bigInteger));

    return rBitVector.toString();
  }
}
