// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

// TODO: run with more machine models
/* Test all SMGCPAValueVisitor visits. Some will be tested indirectly, for example value creation. */
public class SMGCPAValueVisitorTest {

  private LogManagerWithoutDuplicates logger;
  private SMGCPAValueExpressionEvaluator evaluator;
  private SMGState emptyState;
  private SMGCPAValueVisitor visitor;

  private static final CType CHAR_TYPE = CNumericTypes.CHAR;
  private static final CType SHORT_TYPE = CNumericTypes.SHORT_INT;
  private static final CType UNSIGNED_SHORT_TYPE = CNumericTypes.UNSIGNED_SHORT_INT;
  private static final CType INT_TYPE = CNumericTypes.INT;
  private static final CType UNISGNED_INT_TYPE = CNumericTypes.UNSIGNED_INT;
  private static final CType LONG_TYPE = CNumericTypes.LONG_INT;
  private static final CType UNISGNED_LONG_TYPE = CNumericTypes.UNSIGNED_LONG_INT;

  // Float/Double is currently not supported by SMG2
  @SuppressWarnings("unused")
  private static final CType FLOAT_TYPE = CNumericTypes.FLOAT;

  @SuppressWarnings("unused")
  private static final CType DOUBLE_TYPE = CNumericTypes.DOUBLE;

  private static final CType[] BIT_FIELD_TYPES =
      new CType[] {
        CHAR_TYPE,
        SHORT_TYPE,
        UNSIGNED_SHORT_TYPE,
        INT_TYPE,
        UNISGNED_INT_TYPE,
        LONG_TYPE,
        UNISGNED_LONG_TYPE
      };

  @Before
  public void init() throws InvalidConfigurationException {
    logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

    evaluator = new SMGCPAValueExpressionEvaluator(MachineModel.LINUX64, logger);

    emptyState =
        SMGState.of(
            MachineModel.LINUX64, logger, new SMGOptions(Configuration.defaultConfiguration()));

    visitor = new SMGCPAValueVisitor(evaluator, emptyState, new DummyCFAEdge(null, null), logger);
  }

  /*
   * Test casting of char concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * char to char (char is 1 byte; signed -128 to 127 unsigned 0 to 255)
   * char to signed short (short is 2 byte; -32,768 to 32,767 or 0 to 65,535)
   * char to unsigned short
   * char to signed int (int is byte; signed -2,147,483,648 to 2,147,483,647)
   * char to unsigned int (unsigned 0 to 4,294,967,295)
   * char to signed long (long is bytes; signed -9223372036854775808 to 9223372036854775807)
   * char to unsigned long (unsigned 0 to 18446744073709551615)
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castCharTest() throws CPATransferException {
    // 255 is max for unsigned. Java chars are unsigned!
    // According to the C 99 standard, char, unsigned char and signed char should behave the same.
    // As we use the numeric values, they essentially do.
    char[] testChars = new char[] {((char) 0), ((char) 1), 'a', 'A', ((char) 127), ((char) 255)};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (char testChar : testChars) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CCharLiteralExpression(FileLocation.DUMMY, CNumericTypes.CHAR, testChar));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();


        assertThat(value).isInstanceOf(NumericValue.class);
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testChar), typeToTest));
      }
    }


  }

  /*
   * Test casting of signed short concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * signed short to char
   * signed short to signed short
   * signed short to unsigned short
   * signed short to signed int
   * signed short to unsigned int
   * signed short to signed long
   * signed short to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castSignedShortTest() throws CPATransferException {
    // Min value, -1, 0, 1, max value
    short[] testShorts = new short[] {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (short testShort : testShorts) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, SHORT_TYPE, BigInteger.valueOf(testShort)));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testShort), typeToTest));
      }
    }
  }

  /*
   * Test casting of unsigned short concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * unsigned short to char
   * unsigned short to signed short
   * unsigned short to unsigned short
   * unsigned short to signed int
   * unsigned short to unsigned int
   * unsigned short to signed long
   * unsigned short to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castUnsignedShortTest() throws CPATransferException {
    // 0, 1, max value signed short, max value signed short * 2, max value unsigned short
    int[] testShorts =
        new int[] {0, 1, Short.MAX_VALUE, Short.MAX_VALUE * 2, Short.MAX_VALUE * 2 + 1};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (int testShort : testShorts) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, UNSIGNED_SHORT_TYPE, BigInteger.valueOf(testShort)));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testShort), typeToTest));
      }
    }
  }

  /*
   * Test casting of signed int concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * signed int to char
   * signed int to signed short
   * signed int to unsigned short
   * signed int to signed int
   * signed int to unsigned int
   * signed int to signed long
   * signed int to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castSignedIntTest() throws CPATransferException {
    // Min value, -1, 0, 1, max value
    int[] testShorts = new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (int testShort : testShorts) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, UNSIGNED_SHORT_TYPE, BigInteger.valueOf(testShort)));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testShort), typeToTest));
      }
    }
  }

  /*
   * Test casting of unsigned int concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * unsigned int to char
   * unsigned int to signed short
   * unsigned int to unsigned short
   * unsigned int to signed int
   * unsigned int to unsigned int
   * unsigned int to signed long
   * unsigned int to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castUnsignedIntTest() throws CPATransferException {
    // 0, 1, max value signed int, double max v signed, max unsigned
    long[] testShorts =
        new long[] {0, 1, Integer.MAX_VALUE, Integer.MAX_VALUE * 2, Integer.MAX_VALUE * 2 + 1};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (long testShort : testShorts) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, UNSIGNED_SHORT_TYPE, BigInteger.valueOf(testShort)));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testShort), typeToTest));
      }
    }
  }

  /**
   * Assuming that the input is a signed value that may be smaller or bigger than the type entered.
   */
  private BigInteger convertToType(BigInteger value, CType type) {
    if (value.compareTo(BigInteger.ZERO) == 0) {
      return value;
    }
    //int byteSize = MachineModel.LINUX64.getSizeofInBits(type.getCanonicalType()).intValueExact() / 8;
    if (type == CHAR_TYPE) {
      return BigInteger.valueOf(value.byteValue());
    } else if (type == SHORT_TYPE) {
      return BigInteger.valueOf(value.shortValue());
    } else if (type == UNSIGNED_SHORT_TYPE) {
      if (value.shortValue() < 0) {
        return BigInteger.valueOf(value.shortValue() & 0xFFFF);
      }
      return BigInteger.valueOf(value.shortValue());
    } else if (type == INT_TYPE) {
      return BigInteger.valueOf(value.intValue());
    } else if (type == UNISGNED_INT_TYPE) {
      return BigInteger.valueOf(Integer.toUnsignedLong(value.intValue()));
    } else if (type == LONG_TYPE) {
      return BigInteger.valueOf(value.longValue());
    } else if (type == UNISGNED_LONG_TYPE) {
      BigInteger longValue = BigInteger.valueOf(value.longValue());
      if (longValue.signum() < 0) {
        return longValue.add(BigInteger.ONE.shiftLeft(64));
      }
      return longValue;
    }
    // TODO: float/double
    return BigInteger.ZERO;
  }
}
