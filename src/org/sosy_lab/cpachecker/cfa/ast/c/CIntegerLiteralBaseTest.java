// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.CIntegerLiteralBase;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

public class CIntegerLiteralBaseTest {

  /** Set bits from left to right: {@code 0110000011000011}. */
  private static final BigInteger positiveTestValue =
      BigInteger.ZERO.setBit(14).setBit(13).setBit(7).setBit(6).setBit(1).setBit(0);

  /** Set bits from left to right: {@code 10101010}. */
  private static final BigInteger negativeTestValue =
      BigInteger.ZERO.setBit(7).setBit(5).setBit(3).setBit(1).negate();

  @Test
  public void test_positive_binary() {
    CIntegerLiteralExpression withBinaryBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            positiveTestValue,
            CIntegerLiteralBase.BINARY);
    // leading zeros are dropped, so e.g. "0b1" instead of "0b0000_0001"
    String expected = "0b110000011000011";
    assertThat(withBinaryBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_negative_binary() {
    CIntegerLiteralExpression withBinaryBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            negativeTestValue,
            CIntegerLiteralBase.BINARY);
    // leading zeros are dropped, so e.g. "0b1" instead of "0b0000_0001"
    String expected = "-0b10101010";
    assertThat(withBinaryBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_positive_octal() {
    CIntegerLiteralExpression withOctalBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            positiveTestValue,
            CIntegerLiteralBase.OCTAL);
    String expected = "060303";
    assertThat(withOctalBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_negative_octal() {
    CIntegerLiteralExpression withBinaryBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            negativeTestValue,
            CIntegerLiteralBase.OCTAL);
    String expected = "-0252";
    assertThat(withBinaryBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_positive_decimal() {
    CIntegerLiteralExpression withDecimalBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            positiveTestValue,
            CIntegerLiteralBase.DECIMAL);
    String expected = String.valueOf(1 + 2 + 64 + 128 + 8192 + 16384);
    assertThat(withDecimalBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_negative_decimal() {
    CIntegerLiteralExpression withDecimalBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            negativeTestValue,
            CIntegerLiteralBase.DECIMAL);
    String expected = "-170";
    assertThat(withDecimalBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_positive_hexadecimal() {
    CIntegerLiteralExpression withHexadecimalBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            positiveTestValue,
            CIntegerLiteralBase.HEXADECIMAL);
    String expected = "0x60c3";
    assertThat(withHexadecimalBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_negative_hexadecimal() {
    CIntegerLiteralExpression withHexadecimalBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            negativeTestValue,
            CIntegerLiteralBase.HEXADECIMAL);
    String expected = "-0xaa";
    assertThat(withHexadecimalBase.toASTString()).isEqualTo(expected);
  }
}
