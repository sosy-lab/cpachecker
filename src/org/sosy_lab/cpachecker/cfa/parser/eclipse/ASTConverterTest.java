/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

public class ASTConverterTest {

  private ASTConverter converter;

  @Before
  public final void setup() throws InvalidConfigurationException {
    converter = new ASTConverter(new Scope(), false, new LogManager(Configuration.defaultConfiguration()));
  }

  @Test
  public final void testCharacterExpression() {
    assertEquals('\000',   converter.parseCharacterLiteral("'\\000'", null));
    assertEquals('\077',   converter.parseCharacterLiteral("'\\077'", null));
    assertEquals('\u00FF', converter.parseCharacterLiteral("'\\xFF'", null));
    assertEquals('\u00BC', converter.parseCharacterLiteral("'\\xBC'", null));
    assertEquals('\u0080', converter.parseCharacterLiteral("'\\x80'", null));
    assertEquals('\u007F', converter.parseCharacterLiteral("'\\X7F'", null));
    assertEquals('\\', converter.parseCharacterLiteral("'\\\\'", null));
    assertEquals('\'', converter.parseCharacterLiteral("'\\''", null));
    assertEquals('"',  converter.parseCharacterLiteral("'\\\"'", null));
    assertEquals('\0', converter.parseCharacterLiteral("'\\0'", null));
    assertEquals('\t', converter.parseCharacterLiteral("'\\t'", null));
    assertEquals('\n', converter.parseCharacterLiteral("'\\n'", null));
    assertEquals('\r', converter.parseCharacterLiteral("'\\r'", null));
    assertEquals('a', converter.parseCharacterLiteral("'a'", null));
    assertEquals(' ', converter.parseCharacterLiteral("' '", null));
    assertEquals('9', converter.parseCharacterLiteral("'9'", null));
    assertEquals('´', converter.parseCharacterLiteral("'´'", null));
    assertEquals('´', converter.parseCharacterLiteral("'´'", null));
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression1() {
    converter.parseCharacterLiteral("", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression2() {
    converter.parseCharacterLiteral("'\\'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression3() {
    converter.parseCharacterLiteral("'aa'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression4() {
    converter.parseCharacterLiteral("'\\777'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression5() {
    converter.parseCharacterLiteral("'\\xFFF'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression6() {
    converter.parseCharacterLiteral("'\\z'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression7() {
    converter.parseCharacterLiteral("'\\0777'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression8() {
    converter.parseCharacterLiteral("'\\088'", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression9() {
    converter.parseCharacterLiteral("'\\xGG'", null);
  }

  private String parseIntegerExpression(String s) {
    return converter.parseIntegerLiteral(s, null).toString();
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
