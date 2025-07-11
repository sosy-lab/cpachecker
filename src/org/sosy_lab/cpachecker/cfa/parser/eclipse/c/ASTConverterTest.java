// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class ASTConverterTest {

  @BeforeClass
  public static void configureLocale() {
    Locale.setDefault(Locale.US);
  }

  private final ASTLiteralConverter converter32 =
      new ASTLiteralConverter(MachineModel.LINUX32, ParseContext.dummy());
  private final ASTLiteralConverter converter64 =
      new ASTLiteralConverter(MachineModel.LINUX64, ParseContext.dummy());

  @Test
  public final void testCharacterExpression() {
    assertThat(converter32.parseCharacterLiteral("'\\000'", null)).isEqualTo('\000');
    assertThat(converter32.parseCharacterLiteral("'\\077'", null)).isEqualTo('\077');
    assertThat(converter32.parseCharacterLiteral("'\\xFF'", null)).isEqualTo('\u00FF');
    assertThat(converter32.parseCharacterLiteral("'\\xBC'", null)).isEqualTo('\u00BC');
    assertThat(converter32.parseCharacterLiteral("'\\x80'", null)).isEqualTo('\u0080');
    assertThat(converter32.parseCharacterLiteral("'\\X7F'", null)).isEqualTo('\u007F');
    assertThat(converter32.parseCharacterLiteral("'\\\\'", null)).isEqualTo('\\');
    assertThat(converter32.parseCharacterLiteral("'\\''", null)).isEqualTo('\'');
    assertThat(converter32.parseCharacterLiteral("'\\\"'", null)).isEqualTo('"');
    assertThat(converter32.parseCharacterLiteral("'\\0'", null)).isEqualTo('\0');
    assertThat(converter32.parseCharacterLiteral("'\\a'", null)).isEqualTo(7);
    assertThat(converter32.parseCharacterLiteral("'\\b'", null)).isEqualTo('\b');
    assertThat(converter32.parseCharacterLiteral("'\\f'", null)).isEqualTo('\f');
    assertThat(converter32.parseCharacterLiteral("'\\n'", null)).isEqualTo('\n');
    assertThat(converter32.parseCharacterLiteral("'\\r'", null)).isEqualTo('\r');
    assertThat(converter32.parseCharacterLiteral("'\\t'", null)).isEqualTo('\t');
    assertThat(converter32.parseCharacterLiteral("'\\v'", null)).isEqualTo(11);
    assertThat(converter32.parseCharacterLiteral("'a'", null)).isEqualTo('a');
    assertThat(converter32.parseCharacterLiteral("' '", null)).isEqualTo(' ');
    assertThat(converter32.parseCharacterLiteral("'9'", null)).isEqualTo('9');
    assertThat(converter32.parseCharacterLiteral("'´'", null)).isEqualTo('´');
    assertThat(converter32.parseCharacterLiteral("'´'", null)).isEqualTo('´');
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression1() {
    converter32.parseCharacterLiteral("", null);
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression2() {
    converter32.parseCharacterLiteral("'\\'", null);
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression3() {
    converter32.parseCharacterLiteral("'aa'", null);
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression4() {
    converter32.parseCharacterLiteral("'\\777'", null);
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression5() {
    converter32.parseCharacterLiteral("'\\xFFF'", null);
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression6() {
    converter32.parseCharacterLiteral("'\\z'", null);
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression7() {
    converter32.parseCharacterLiteral("'\\0777'", null);
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression8() {
    converter32.parseCharacterLiteral("'\\088'", null);
  }

  @Test(expected = CFAGenerationRuntimeException.class)
  public final void testInvalidCharacterExpression9() {
    converter32.parseCharacterLiteral("'\\xGG'", null);
  }

  @Test
  public final void testInvalidIntegerExpressions() {
    ImmutableList<ASTLiteralConverter> converters = ImmutableList.of(converter32, converter64);
    ImmutableList<String> invalidValues =
        ImmutableList.of(
            "18446744073709551617u",
            "36893488147419103232",
            "36893488147419103232u",
            "100020003000400050006000700080009000u");

    for (ASTLiteralConverter c : converters) {
      for (String s : invalidValues) {
        try {
          c.parseIntegerLiteral(FileLocation.DUMMY, s, null);
          assert_().fail();
        } catch (CFAGenerationRuntimeException e) {
          assertThat(e.getMessage())
              .contains(
                  "Integer value is too large to be represented by the highest possible type"
                      + " (unsigned long long int):");
        }
      }
    }
  }

  private String parseIntegerExpression32(String s) {
    return parseIntegerExpression(s, converter32);
  }

  private String parseIntegerExpression64(String s) {
    return parseIntegerExpression(s, converter64);
  }

  private String parseIntegerExpression(String pExpression, ASTLiteralConverter pConverter) {
    CLiteralExpression exp = pConverter.parseIntegerLiteral(FileLocation.DUMMY, pExpression, null);
    return ((CIntegerLiteralExpression) exp).getValue().toString();
  }

  @Test
  public final void testIntegerExpression() {
    check("0", "0");
    check("1", "1");
    check("2", "2");
    check("3", "3u");
    check("4", "4");
    check("5", "5u");
    check("63", "077");

    check("2147483647", "2147483647");
    check("2147483647", "0x7FFFFFFF");
    check("2147483648", "2147483648");
    check("2147483648", "0x80000000");
    check("4294967295", "4294967295");
    check("4294967295", "0xFFFFFFFF");
    check("4294967296", "4294967296");
    check("4000000000", "4000000000");
    check("4000000000", "4000000000u");

    check("18446744073709551600", "0xfffffffffffffff0u");
    check("18446744073709551600", "0xfffffffffffffff0");

    check("9223372036854775807", "9223372036854775807");
    check("9223372036854775807", "0x7FFFFFFFFFFFFFFF");
    check("9223372036854775808", "9223372036854775808u");
    check("9223372036854775808", "0x8000000000000000u");
    check("9223372036854775808", "9223372036854775808u");
    check("9223372036854775808", "0x8000000000000000u");
    check("18446744073709551615", "18446744073709551615u");
    check("18446744073709551615", "0xFFFFFFFFFFFFFFFF");
    check("18446744073709551615", "0xFFFFFFFFFFFFFFFFu");
    check("18446711088360718336", "0xffffe20000000000U");
    check("18446604435732824064", "0xffff810000000000U");

    check("0", "0b0");
    check("0", "0B0");
    check("255", "0b11111111");
    check("255", "0B11111111");
  }

  private void check(String expected, String input) {
    // constant integers are always extended to the smallest matching type.
    // thus the result is always identical for L and LL
    for (String postfix : ImmutableList.of("", "l", "ll", "L", "LL", "lL", "Ll")) {
      assertThat(parseIntegerExpression32(input + postfix)).isEqualTo(expected);
      assertThat(parseIntegerExpression64(input + postfix)).isEqualTo(expected);
    }
  }

  /** Test that the parser can handle valid floating point inputs */
  @Test
  public final void testValidFloatExpressions() {
    ImmutableList<ASTLiteralConverter> converters = ImmutableList.of(converter32, converter64);

    // TestCase consists of: input value, expected output, input type for CLiteralExpression
    record TestCase(String input, String expected, CType type) {}
    ImmutableList<TestCase> input_output =
        ImmutableList.of(
            // Decimal floating point literal
            // The exponent may have a sign and floating point numbers may be followed by an "f" or
            // "l" suffix to mark them as "float" or "long double" precision
            new TestCase("5e2", "5.00000000e+02", CNumericTypes.FLOAT),
            new TestCase("5e2f", "5.00000000e+02", CNumericTypes.FLOAT),
            new TestCase("5e+2", "5.00000000e+02", CNumericTypes.FLOAT),
            new TestCase("5e+2f", "5.00000000e+02", CNumericTypes.FLOAT),

            // Hexadecimal floating point literals
            // For hexadecimal floating point literals the exponent starts with a "p" and must
            // always be included. The literal is prefixed by "0x" to mark it as hexadecimal, and
            // may be followed by an "f" or "l" suffix, just as for decimal floating point literals
            new TestCase("0xfp00", "1.5000000000000000e+01", CNumericTypes.DOUBLE),
            new TestCase("0x5e2p0", "1.50600000e+03", CNumericTypes.FLOAT),
            new TestCase("0x5e2p0f", "1.50600000e+03", CNumericTypes.FLOAT),
            new TestCase("0x5e2fp0", "2.41110000e+04", CNumericTypes.FLOAT),
            new TestCase("0x5ep2", "3.76000000e+02", CNumericTypes.FLOAT),
            new TestCase("0x5ep-2", "2.35000000e+01", CNumericTypes.FLOAT),
            new TestCase("0x308p-2F", "1.94000000e+02", CNumericTypes.FLOAT),
            new TestCase("0x30ap0l", "7.78000000000000000000e+02", CNumericTypes.LONG_DOUBLE),

            // 3.41E+38 is too large for a float, but can be represented as a double
            new TestCase("3.41E+38", "inf", CNumericTypes.FLOAT),
            new TestCase("3.41E+38", "3.4100000000000000e+38", CNumericTypes.DOUBLE),

            // Some decimal fractions like "3.08" don't have a precise representation as (binary)
            // floating point values. In this case the parser will return the floating point value
            // that lies closes to the real decimal fraction.
            new TestCase("308e-2f", "3.07999992e+00", CNumericTypes.FLOAT),
            new TestCase("308.0L", "3.08000000000000000000e+02", CNumericTypes.LONG_DOUBLE),

            // When printing a floating point number, leading zeroes are dropped, and the number of
            // decimals that are printed after the comma depends on the precision of the type
            new TestCase("0000.000e+0", "0.00000000e+00", CNumericTypes.FLOAT),

            // Try some "partial" inputs where we don't use the exponent or skip the zeroes before
            // or after the decimal point. Note that for hexadecimal floating point literals the
            // exponent must always be included.
            new TestCase("1.0", "1.00000000e+00", CNumericTypes.FLOAT),
            new TestCase(".0", "0.00000000e+00", CNumericTypes.FLOAT),
            new TestCase(".0e0", "0.00000000e+00", CNumericTypes.FLOAT),
            new TestCase("1.", "1.00000000e+00", CNumericTypes.FLOAT),
            new TestCase("1.e0", "1.00000000e+00", CNumericTypes.FLOAT),
            new TestCase("1e0", "1.00000000e+00", CNumericTypes.FLOAT),
            new TestCase("0x.0p0", "0.00000000e+00", CNumericTypes.FLOAT));

    for (ASTLiteralConverter converter : converters) {
      for (TestCase test : input_output) {
        CFloatLiteralExpression literal =
            converter.parseFloatLiteral(FileLocation.DUMMY, test.type(), test.input(), null);

        assertThat(literal.getValue().toString()).isEqualTo(test.expected());
        assertThat(literal.getExpressionType()).isSameInstanceAs(test.type());
      }
    }
  }

  /**
   * Test for floating point literals that will overflow to <code>Infinity</code> because their
   * exponent is too large for the format
   */
  @Test
  public final void testOverflowingFloatExpressions() {
    ImmutableList<ASTLiteralConverter> converters = ImmutableList.of(converter32, converter64);

    // TestCase consists of: input value, expected output, input type for CLiteralExpression
    record TestCase(String input, String expected, CType type) {}
    ImmutableList<TestCase> input_output =
        ImmutableList.of(
            new TestCase("3.41e+38f", "inf", CNumericTypes.FLOAT),
            new TestCase("1.8e+308", "inf", CNumericTypes.DOUBLE),
            new TestCase("1.2e+4932l", "inf", CNumericTypes.LONG_DOUBLE));

    for (ASTLiteralConverter converter : converters) {
      for (TestCase test : input_output) {
        CFloatLiteralExpression literal =
            converter.parseFloatLiteral(FileLocation.DUMMY, test.type(), test.input(), null);

        assertThat(literal.getValue().toString()).isEqualTo(test.expected());
        assertThat(literal.getExpressionType()).isSameInstanceAs(test.type());
      }
    }
  }

  /** Test that the parser rejects invalid floating point literals as input */
  @Test
  public final void testInvalidFloatExpressions() {
    ImmutableList<ASTLiteralConverter> converters = ImmutableList.of(converter32, converter64);
    ImmutableList<String> inputs =
        ImmutableList.of(
            "",
            "+",
            "e",
            "-+0.0",
            "++0.0",
            "0.0+",
            "0",
            "+0",
            "-0",
            "0,0",
            "0.0.",
            "0..",
            ".",
            ".e0",
            "0.0e",
            "0.0e-",
            "0e+-0",
            "0e++0",
            "0e+0+",
            "f",
            "ff",
            "0.ff",
            "0ee",
            "0y0.0p0",
            "0x0.0p0ff",
            "0xf",
            "0x0.0",
            "0x0.0e0");

    for (ASTLiteralConverter converter : converters) {
      for (String value : inputs) {
        assertThrows(
            CFAGenerationRuntimeException.class,
            () ->
                converter.parseFloatLiteral(FileLocation.DUMMY, CNumericTypes.DOUBLE, value, null));
      }
    }
  }
}
