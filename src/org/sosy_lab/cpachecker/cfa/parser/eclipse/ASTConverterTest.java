/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ASTConverterTest {

  @Test
  public final void testCharacterExpression() {
    assertEquals('\000',   ASTLiteralConverter.parseCharacterLiteral("'\\000'", null));
    assertEquals('\077',   ASTLiteralConverter.parseCharacterLiteral("'\\077'", null));
    assertEquals('\u00FF', ASTLiteralConverter.parseCharacterLiteral("'\\xFF'", null));
    assertEquals('\u00BC', ASTLiteralConverter.parseCharacterLiteral("'\\xBC'", null));
    assertEquals('\u0080', ASTLiteralConverter.parseCharacterLiteral("'\\x80'", null));
    assertEquals('\u007F', ASTLiteralConverter.parseCharacterLiteral("'\\X7F'", null));
    assertEquals('\\', ASTLiteralConverter.parseCharacterLiteral("'\\\\'", null));
    assertEquals('\'', ASTLiteralConverter.parseCharacterLiteral("'\\''", null));
    assertEquals('"',  ASTLiteralConverter.parseCharacterLiteral("'\\\"'", null));
    assertEquals('\0', ASTLiteralConverter.parseCharacterLiteral("'\\0'", null));
    assertEquals(7,    ASTLiteralConverter.parseCharacterLiteral("'\\a'", null));
    assertEquals('\b', ASTLiteralConverter.parseCharacterLiteral("'\\b'", null));
    assertEquals('\f', ASTLiteralConverter.parseCharacterLiteral("'\\f'", null));
    assertEquals('\n', ASTLiteralConverter.parseCharacterLiteral("'\\n'", null));
    assertEquals('\r', ASTLiteralConverter.parseCharacterLiteral("'\\r'", null));
    assertEquals('\t', ASTLiteralConverter.parseCharacterLiteral("'\\t'", null));
    assertEquals(11,   ASTLiteralConverter.parseCharacterLiteral("'\\v'", null));
    assertEquals('a', ASTLiteralConverter.parseCharacterLiteral("'a'", null));
    assertEquals(' ', ASTLiteralConverter.parseCharacterLiteral("' '", null));
    assertEquals('9', ASTLiteralConverter.parseCharacterLiteral("'9'", null));
    assertEquals('´', ASTLiteralConverter.parseCharacterLiteral("'´'", null));
    assertEquals('´', ASTLiteralConverter.parseCharacterLiteral("'´'", null));
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression1() {
    ASTLiteralConverter.parseCharacterLiteral("", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression2() {
    ASTLiteralConverter.parseCharacterLiteral("'\\'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression3() {
    ASTLiteralConverter.parseCharacterLiteral("'aa'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression4() {
    ASTLiteralConverter.parseCharacterLiteral("'\\777'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression5() {
    ASTLiteralConverter.parseCharacterLiteral("'\\xFFF'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression6() {
    ASTLiteralConverter.parseCharacterLiteral("'\\z'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression7() {
    ASTLiteralConverter.parseCharacterLiteral("'\\0777'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression8() {
    ASTLiteralConverter.parseCharacterLiteral("'\\088'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression9() {
    ASTLiteralConverter.parseCharacterLiteral("'\\xGG'", null);
  }

  private String parseIntegerExpression(String s) {
    return ASTLiteralConverter.parseIntegerLiteral(s, null).toString();
  }

  @Test
  public final void testIntegerExpression() {
    assertEquals("0", parseIntegerExpression("0"));
    assertEquals("1", parseIntegerExpression("1"));
    assertEquals("2", parseIntegerExpression("2L"));
    assertEquals("3", parseIntegerExpression("3u"));
    assertEquals("4", parseIntegerExpression("4LL"));
    assertEquals("5", parseIntegerExpression("5uLl"));
    assertEquals("63", parseIntegerExpression("077"));

    assertEquals( "2147483647", parseIntegerExpression("2147483647"));
    assertEquals( "2147483647", parseIntegerExpression("0x7FFFFFFFL"));
    assertEquals("-2147483648", parseIntegerExpression("2147483648L"));
    assertEquals("-2147483648", parseIntegerExpression("0x80000000"));
    assertEquals("-1",          parseIntegerExpression("4294967295"));
    assertEquals("-1",          parseIntegerExpression("0xFFFFFFFFl"));
    assertEquals( "0",          parseIntegerExpression("4294967296l"));
    assertEquals("-294967296",  parseIntegerExpression("4000000000"));
    assertEquals( "4000000000", parseIntegerExpression("4000000000u"));

    assertEquals("2147483647", parseIntegerExpression("2147483647LL"));
    assertEquals("2147483647", parseIntegerExpression("0x7FFFFFFFlL"));
    assertEquals("2147483648", parseIntegerExpression("2147483648Ll"));
    assertEquals("2147483648", parseIntegerExpression("0x80000000LL"));
    assertEquals("4294967295", parseIntegerExpression("4294967295ll"));
    assertEquals("4294967295", parseIntegerExpression("0xFFFFFFFFLL"));
    assertEquals("4294967296", parseIntegerExpression("4294967296Ll"));
    assertEquals("4000000000", parseIntegerExpression("4000000000lL"));
    assertEquals("4000000000", parseIntegerExpression("4000000000uLL"));

    assertEquals("18446744073709551600", parseIntegerExpression("0xfffffffffffffff0ull"));
    assertEquals("-16",                  parseIntegerExpression("0xfffffffffffffff0ll"));

    assertEquals( "9223372036854775807", parseIntegerExpression("9223372036854775807LL"));
    assertEquals( "9223372036854775807", parseIntegerExpression("0x7FFFFFFFFFFFFFFFLL"));
    assertEquals("-9223372036854775808", parseIntegerExpression("9223372036854775808LL"));
    assertEquals("-9223372036854775808", parseIntegerExpression("0x8000000000000000LL"));
    assertEquals( "9223372036854775808", parseIntegerExpression("9223372036854775808uLL"));
    assertEquals( "9223372036854775808", parseIntegerExpression("0x8000000000000000uLL"));
    assertEquals("18446744073709551615", parseIntegerExpression("18446744073709551615uLL"));
    assertEquals("18446744073709551615", parseIntegerExpression("0xFFFFFFFFFFFFFFFFuLL"));

    assertEquals("-1", parseIntegerExpression("0xFFFFFFFFFFFFFFFF"));
    assertEquals("-1", parseIntegerExpression("0xFFFFFFFFFFFFFFFFLL"));

    assertEquals("1", parseIntegerExpression("18446744073709551617uLL"));
  }

}
