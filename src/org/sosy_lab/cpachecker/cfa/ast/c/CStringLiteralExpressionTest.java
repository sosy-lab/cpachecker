// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CStringLiteralExpressionTest {

  private static CStringLiteralExpression literal(String source) {
    return new CStringLiteralExpression(FileLocation.DUMMY, source);
  }

  @Test
  public void testWithoutEscapeSequences() {
    CStringLiteralExpression e = literal("\"abc\"");
    assertThat(e.getContentWithoutNullTerminator()).isEqualTo("abc");
    assertThat(e.getSize()).isEqualTo(4);
    assertThat(e.getExpressionType().getLengthAsInt()).hasValue(4);
    assertThat(e.toASTString()).isEqualTo("\"abc\"");
  }

  @Test
  public void testSimpleEscapeSequences() {
    CStringLiteralExpression e = literal("\"x\\\"y\\n\\\\\\'\\?\\t\\a\\v\"");
    assertThat(e.getContentWithoutNullTerminator()).isEqualTo("x\"y\n\\'?\t\u0007\u000B");
    assertThat(e.getSize()).isEqualTo(11);
    assertThat(e.getExpressionType().getLengthAsInt()).hasValue(11);
  }

  @Test
  public void testNumericEscapeSequences() {
    assertThat(literal("\"\\101\\x42\\0z\"").getContentWithoutNullTerminator()).isEqualTo("AB\0z");
    // octal escapes have at most three digits, hex escapes take all digits
    assertThat(literal("\"\\1012\"").getContentWithoutNullTerminator()).isEqualTo("A2");
    assertThat(literal("\"\\x0041\"").getContentWithoutNullTerminator()).isEqualTo("A");
    assertThat(literal("\"\\377\\xff\"").getContentWithoutNullTerminator())
        .isEqualTo("\u00FF\u00FF");
  }

  @Test
  public void testUniversalCharacterName() {
    assertThat(literal("\"\\u00e4\"").getContentWithoutNullTerminator()).isEqualTo("\u00C3\u00A4");
  }

  @Test
  public void testInvalidEscapeSequences() {
    assertThrows(IllegalArgumentException.class, () -> literal("\"\\x\""));
    assertThrows(IllegalArgumentException.class, () -> literal("\"\\x100\""));
    assertThrows(IllegalArgumentException.class, () -> literal("\"\\u12\""));
  }
}
