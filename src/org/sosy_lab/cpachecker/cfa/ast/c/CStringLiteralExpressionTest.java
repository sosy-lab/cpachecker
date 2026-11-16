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

import com.google.common.base.VerifyException;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CStringLiteralExpressionTest {

  private static CStringLiteralExpression literal(String source) {
    return CStringLiteralExpression.fromSourceCode(FileLocation.DUMMY, source);
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

  @Test
  public void testLiteralWithoutQuotes() {
    assertThrows(IllegalArgumentException.class, () -> literal("abc"));
    assertThrows(IllegalArgumentException.class, () -> literal("\""));
  }

  @Test
  public void testEncodingPrefix() {
    CStringLiteralExpression e = literal("L\"a\\x1234\"");
    assertThat(e.toASTString()).isEqualTo("L\"a\\x1234\"");
    assertThat(e.getSize()).isEqualTo(8);
    assertThrows(VerifyException.class, () -> e.getContentWithoutNullTerminator());
  }

  @Test
  public void testFromContent() {
    String content = "a\u0001\"\\\0ÿ";
    CStringLiteralExpression e = CStringLiteralExpression.fromContent(FileLocation.DUMMY, content);
    assertThat(e.toASTString()).isEqualTo("\"a\\001\\\"\\\\\\000\\377\"");
    assertThat(e.getContentWithoutNullTerminator()).isEqualTo(content);
    assertThat(e.getSize()).isEqualTo(7);
    assertThat(literal(e.toASTString())).isEqualTo(e);
  }

  @Test
  public void testFromContentWithoutBytes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> CStringLiteralExpression.fromContent(FileLocation.DUMMY, "Ā"));
  }
}
