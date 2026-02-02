// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.expression;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class CBitVectorLiteralExpressionTest {

  private static final ImmutableSet<Integer> setBits =
      ImmutableSet.<Integer>builder().add(0).add(1).add(6).add(7).add(13).add(14).build();

  @Test
  public void test_binary() throws UnrecognizedCodeException {
    CBitVectorBinaryLiteralExpression binaryExpression =
        new CBitVectorBinaryLiteralExpression(setBits, CNumericTypes.SHORT_INT);
    String expected = "0b0110000011000011";
    assertThat(binaryExpression.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_decimal() throws UnrecognizedCodeException {
    CBitVectorDecimalLiteralExpression decimalExpression =
        new CBitVectorDecimalLiteralExpression(setBits, CNumericTypes.INT);
    String expected = String.valueOf(1 + 2 + 64 + 128 + 8192 + 16384);
    assertThat(decimalExpression.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_hexadecimal() throws UnrecognizedCodeException {
    CBitVectorHexadecimalLiteralExpression hexadecimalExpression =
        new CBitVectorHexadecimalLiteralExpression(setBits, CNumericTypes.SHORT_INT);
    String expected = "0x60c3";
    assertThat(hexadecimalExpression.toASTString()).isEqualTo(expected);
  }
}
