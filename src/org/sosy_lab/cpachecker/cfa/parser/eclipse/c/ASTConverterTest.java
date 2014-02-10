/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

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

  private String parseIntegerExpression32(String s) {
    return ASTLiteralConverter.parseIntegerLiteral(s, null, MachineModel.LINUX32).toString();
  }

  @Test
  public final void testIntegerExpression() {
    assertEquals("0", parseIntegerExpression32("0"));
    assertEquals("1", parseIntegerExpression32("1"));
    assertEquals("2", parseIntegerExpression32("2L"));
    assertEquals("3", parseIntegerExpression32("3u"));
    assertEquals("4", parseIntegerExpression32("4LL"));
    assertEquals("5", parseIntegerExpression32("5uLl"));
    assertEquals("63", parseIntegerExpression32("077"));

    assertEquals( "2147483647", parseIntegerExpression32("2147483647"));
    assertEquals( "2147483647", parseIntegerExpression32("0x7FFFFFFFL"));
    assertEquals("-2147483648", parseIntegerExpression32("2147483648L"));
    assertEquals("-2147483648", parseIntegerExpression32("0x80000000"));
    assertEquals("-1",          parseIntegerExpression32("4294967295"));
    assertEquals("-1",          parseIntegerExpression32("0xFFFFFFFFl"));
    assertEquals( "0",          parseIntegerExpression32("4294967296l"));
    assertEquals("-294967296",  parseIntegerExpression32("4000000000"));
    assertEquals( "4000000000", parseIntegerExpression32("4000000000u"));

    assertEquals("2147483647", parseIntegerExpression32("2147483647LL"));
    assertEquals("2147483647", parseIntegerExpression32("0x7FFFFFFFlL"));
    assertEquals("2147483648", parseIntegerExpression32("2147483648Ll"));
    assertEquals("2147483648", parseIntegerExpression32("0x80000000LL"));
    assertEquals("4294967295", parseIntegerExpression32("4294967295ll"));
    assertEquals("4294967295", parseIntegerExpression32("0xFFFFFFFFLL"));
    assertEquals("4294967296", parseIntegerExpression32("4294967296Ll"));
    assertEquals("4000000000", parseIntegerExpression32("4000000000lL"));
    assertEquals("4000000000", parseIntegerExpression32("4000000000uLL"));

    assertEquals("18446744073709551600", parseIntegerExpression32("0xfffffffffffffff0ull"));
    assertEquals("-16",                  parseIntegerExpression32("0xfffffffffffffff0ll"));

    assertEquals( "9223372036854775807", parseIntegerExpression32("9223372036854775807LL"));
    assertEquals( "9223372036854775807", parseIntegerExpression32("0x7FFFFFFFFFFFFFFFLL"));
    assertEquals("-9223372036854775808", parseIntegerExpression32("9223372036854775808LL"));
    assertEquals("-9223372036854775808", parseIntegerExpression32("0x8000000000000000LL"));
    assertEquals( "9223372036854775808", parseIntegerExpression32("9223372036854775808uLL"));
    assertEquals( "9223372036854775808", parseIntegerExpression32("0x8000000000000000uLL"));
    assertEquals("18446744073709551615", parseIntegerExpression32("18446744073709551615uLL"));
    assertEquals("18446744073709551615", parseIntegerExpression32("0xFFFFFFFFFFFFFFFFuLL"));

    assertEquals("-1", parseIntegerExpression32("0xFFFFFFFFFFFFFFFF"));
    assertEquals("-1", parseIntegerExpression32("0xFFFFFFFFFFFFFFFFLL"));

    assertEquals("1", parseIntegerExpression32("18446744073709551617uLL"));
    assertEquals("0", parseIntegerExpression32("0xffffe20000000000UL"));
    assertEquals("0", parseIntegerExpression32("0xffff810000000000UL"));
  }

  private String parseIntegerExpression64(String s) {
    return ASTLiteralConverter.parseIntegerLiteral(s, null, MachineModel.LINUX64).toString();
  }

  @Test
  public final void testIntegerExpression64() {
    assertEquals("0", parseIntegerExpression64("0"));
    assertEquals("1", parseIntegerExpression64("1"));
    assertEquals("2", parseIntegerExpression64("2L"));
    assertEquals("3", parseIntegerExpression64("3u"));
    assertEquals("4", parseIntegerExpression64("4LL"));
    assertEquals("5", parseIntegerExpression64("5uLl"));
    assertEquals("63", parseIntegerExpression64("077"));

    assertEquals( "2147483647", parseIntegerExpression64("2147483647"));
    assertEquals( "2147483647", parseIntegerExpression64("0x7FFFFFFFL"));
    assertEquals( "2147483648", parseIntegerExpression64("2147483648L"));
    assertEquals("-2147483648", parseIntegerExpression64("0x80000000"));
    assertEquals("-1",          parseIntegerExpression64("4294967295"));
    assertEquals( "4294967295", parseIntegerExpression64("0xFFFFFFFFl"));
    assertEquals( "4294967296", parseIntegerExpression64("4294967296l"));
    assertEquals("-294967296",  parseIntegerExpression64("4000000000"));
    assertEquals( "4000000000", parseIntegerExpression64("4000000000u"));

    assertEquals("2147483647", parseIntegerExpression64("2147483647LL"));
    assertEquals("2147483647", parseIntegerExpression64("0x7FFFFFFFlL"));
    assertEquals("2147483648", parseIntegerExpression64("2147483648Ll"));
    assertEquals("2147483648", parseIntegerExpression64("0x80000000LL"));
    assertEquals("4294967295", parseIntegerExpression64("4294967295ll"));
    assertEquals("4294967295", parseIntegerExpression64("0xFFFFFFFFLL"));
    assertEquals("4294967296", parseIntegerExpression64("4294967296Ll"));
    assertEquals("4000000000", parseIntegerExpression64("4000000000lL"));
    assertEquals("4000000000", parseIntegerExpression64("4000000000uLL"));

    assertEquals("18446744073709551600", parseIntegerExpression64("0xfffffffffffffff0ull"));
    assertEquals("-16",                  parseIntegerExpression64("0xfffffffffffffff0ll"));

    assertEquals( "9223372036854775807", parseIntegerExpression64("9223372036854775807LL"));
    assertEquals( "9223372036854775807", parseIntegerExpression64("0x7FFFFFFFFFFFFFFFLL"));
    assertEquals("-9223372036854775808", parseIntegerExpression64("9223372036854775808LL"));
    assertEquals("-9223372036854775808", parseIntegerExpression64("0x8000000000000000LL"));
    assertEquals( "9223372036854775808", parseIntegerExpression64("9223372036854775808uLL"));
    assertEquals( "9223372036854775808", parseIntegerExpression64("0x8000000000000000uLL"));
    assertEquals("18446744073709551615", parseIntegerExpression64("18446744073709551615uLL"));
    assertEquals("18446744073709551615", parseIntegerExpression64("0xFFFFFFFFFFFFFFFFuLL"));

    assertEquals("-1", parseIntegerExpression64("0xFFFFFFFFFFFFFFFF"));
    assertEquals("-1", parseIntegerExpression64("0xFFFFFFFFFFFFFFFFLL"));

    assertEquals("1", parseIntegerExpression64("18446744073709551617uLL"));
    assertEquals("18446711088360718336", parseIntegerExpression64("0xffffe20000000000UL"));
    assertEquals("18446604435732824064", parseIntegerExpression64("0xffff810000000000UL"));
  }

}
