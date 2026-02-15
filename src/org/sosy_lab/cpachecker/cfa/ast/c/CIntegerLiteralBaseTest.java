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

  private static final BigInteger testValue =
      BigInteger.ZERO.setBit(0).setBit(1).setBit(6).setBit(7).setBit(13).setBit(14);

  @Test
  public void test_binary() {
    CIntegerLiteralExpression withBinaryBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.SHORT_INT, testValue, CIntegerLiteralBase.BINARY);
    // leading zeros are dropped, so e.g. "0b1" instead of "0b0000_0001"
    String expected = "0b110000011000011";
    assertThat(withBinaryBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_octal() {
    CIntegerLiteralExpression withOctalBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.SHORT_INT, testValue, CIntegerLiteralBase.OCTAL);
    String expected = "060303";
    assertThat(withOctalBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_decimal() {
    CIntegerLiteralExpression withDecimalBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.INT, testValue, CIntegerLiteralBase.DECIMAL);
    String expected = String.valueOf(1 + 2 + 64 + 128 + 8192 + 16384);
    assertThat(withDecimalBase.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_hexadecimal() {
    CIntegerLiteralExpression withHexadecimalBase =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            CNumericTypes.SHORT_INT,
            testValue,
            CIntegerLiteralBase.HEXADECIMAL);
    String expected = "0x60c3";
    assertThat(withHexadecimalBase.toASTString()).isEqualTo(expected);
  }
}
