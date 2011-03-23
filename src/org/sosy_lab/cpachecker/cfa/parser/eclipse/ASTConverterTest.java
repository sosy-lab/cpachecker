/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import static org.junit.Assert.*;
import static org.sosy_lab.cpachecker.cfa.parser.eclipse.ASTConverter.parseCharacterLiteral;

import org.junit.Test;

public class ASTConverterTest {
  
  @Test
  public final void testCharacterExpression() {
    assertEquals('\000',   parseCharacterLiteral("'\\000'", null));
    assertEquals('\u00FF', parseCharacterLiteral("'\\xFF'", null));
    assertEquals('\u00BC', parseCharacterLiteral("'\\xBC'", null));
    assertEquals('\u0080', parseCharacterLiteral("'\\x80'", null));
    assertEquals('\u007F', parseCharacterLiteral("'\\X7F'", null));
    assertEquals('\\', parseCharacterLiteral("'\\\\'", null));
    assertEquals('\'', parseCharacterLiteral("'\\''", null));
    assertEquals('"',  parseCharacterLiteral("'\\\"'", null));
    assertEquals('\0', parseCharacterLiteral("'\\0'", null));
    assertEquals('\t', parseCharacterLiteral("'\\t'", null));
    assertEquals('\n', parseCharacterLiteral("'\\n'", null));
    assertEquals('\r', parseCharacterLiteral("'\\r'", null));
    assertEquals('a', parseCharacterLiteral("'a'", null));
    assertEquals(' ', parseCharacterLiteral("' '", null));
    assertEquals('9', parseCharacterLiteral("'9'", null));
    assertEquals('´', parseCharacterLiteral("'´'", null));
    assertEquals('´', parseCharacterLiteral("'´'", null));
  }
  
  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression1() {
    parseCharacterLiteral("", null);
  }
  
  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression2() {
    parseCharacterLiteral("\\", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression3() {
    parseCharacterLiteral("aa", null);
  }
  
  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression4() {
    parseCharacterLiteral("\\777", null);
  }
  
  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression5() {
    parseCharacterLiteral("\\xFFF", null);
  }

  @Test(expected=CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression6() {
    parseCharacterLiteral("\\z", null);
  }
  
  private static String parseIntegerExpression(String s) {
    return ASTConverter.parseIntegerLiteral(s, null).toString();
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
