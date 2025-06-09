// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class BitVectorExpressionTest {

  private static final ImmutableSet<Integer> setBits =
      ImmutableSet.<Integer>builder().add(1).add(2).add(7).add(8).add(14).add(15).build();

  @Test
  public void test_binary() {
    BinaryBitVectorExpression binaryExpression = new BinaryBitVectorExpression(16, setBits);
    String expected = "0b0110000011000011";
    assertEquals(expected, binaryExpression.toASTString());
  }

  @Test
  public void test_decimal() {
    DecimalBitVectorExpression decimalExpression = new DecimalBitVectorExpression(setBits);
    String expected = String.valueOf(2 + 4 + 128 + 256 + 16384 + 32768);
    assertEquals(expected, decimalExpression.toASTString());
  }

  @Test
  public void test_hexadecimal() {
    HexadecimalBitVectorExpression hexadecimalExpression =
        new HexadecimalBitVectorExpression(4, setBits);
    String expected = "0x60c3";
    assertEquals(expected, hexadecimalExpression.toASTString());
  }
}
