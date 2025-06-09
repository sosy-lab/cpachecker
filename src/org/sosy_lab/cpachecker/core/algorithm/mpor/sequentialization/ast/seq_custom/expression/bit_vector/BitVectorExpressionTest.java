// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class BitVectorExpressionTest {

  private static final ImmutableSet<Integer> setBits =
      ImmutableSet.<Integer>builder().add(0).add(1).add(6).add(7).add(13).add(14).build();

  @Test
  public void test_binary() {
    BinaryBitVectorExpression binaryExpression = new BinaryBitVectorExpression(16, setBits);
    String expected = "0b0110000011000011";
    assertThat(binaryExpression.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_decimal() {
    DecimalBitVectorExpression decimalExpression = new DecimalBitVectorExpression(setBits);
    String expected = String.valueOf(1 + 2 + 64 + 128 + 8192 + 16384);
    assertThat(decimalExpression.toASTString()).isEqualTo(expected);
  }

  @Test
  public void test_hexadecimal() {
    HexadecimalBitVectorExpression hexadecimalExpression =
        new HexadecimalBitVectorExpression(4, setBits);
    String expected = "0x60c3";
    assertThat(hexadecimalExpression.toASTString()).isEqualTo(expected);
  }
}
