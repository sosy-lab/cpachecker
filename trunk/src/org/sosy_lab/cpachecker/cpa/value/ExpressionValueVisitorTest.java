// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Unit tests for {@link ExpressionValueVisitor} and {@link AbstractExpressionValueVisitor}. */
@RunWith(Parameterized.class)
public class ExpressionValueVisitorTest {

  @Parameters(name = "{0}, symbolicValues={1}")
  public static List<Object[]> getParameters() {
    List<Object[]> result = new ArrayList<>();
    for (MachineModel model : MachineModel.values()) {
      result.add(new Object[] {model, false});
      result.add(new Object[] {model, true});
    }
    return result;
  }

  @Parameter(0)
  public MachineModel machineModel;

  @Parameter(1)
  public boolean symbolicValues;

  // constants for C
  private static final int MAX_CHAR = 256;
  private static final int MAX_SHORT = 65536;
  private static final long MAX_INT = 4294967296L;

  // type constants
  private static final CSimpleType S_CHAR = CNumericTypes.SIGNED_CHAR;
  private static final CSimpleType U_CHAR = CNumericTypes.UNSIGNED_CHAR;

  private static final CSimpleType S_SHORT_INT = CNumericTypes.SHORT_INT;
  private static final CSimpleType U_SHORT_INT = CNumericTypes.UNSIGNED_SHORT_INT;
  private static final CSimpleType S_INT = CNumericTypes.INT;
  private static final CSimpleType U_INT = CNumericTypes.UNSIGNED_INT;
  private static final CSimpleType S_LONG_INT = CNumericTypes.LONG_INT;
  private static final CSimpleType U_LONG_INT = CNumericTypes.UNSIGNED_LONG_INT;
  private static final CSimpleType S_LONG_LONG_INT = CNumericTypes.LONG_LONG_INT;
  private static final CSimpleType U_LONG_LONG_INT = CNumericTypes.UNSIGNED_LONG_LONG_INT;

  private static final CSimpleType FLOAT = CNumericTypes.FLOAT;
  private static final CSimpleType DOUBLE = CNumericTypes.DOUBLE;
  private static final CSimpleType LONG_DOUBLE = CNumericTypes.LONG_DOUBLE;

  private LogManagerWithoutDuplicates logger;
  private ExpressionValueVisitor evv;

  @Before
  public void init() {
    logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

    evv =
        new ExpressionValueVisitor(
            new ValueAnalysisState(machineModel), "dummy_function", machineModel, logger);
  }

  /**
   * this test checks the casts of CHAR, SHORT_INT and (normal) INT, it does not use LONG_INT or
   * LONG_LONG_INT
   */
  @Test
  public void checkSimpleCasts() throws Exception {

    for (int i = -MAX_CHAR / 2; i < MAX_CHAR; i += 50) {
      checkCast(i, i < MAX_CHAR / 2 ? i : (-MAX_CHAR + i), S_CHAR);
      checkCast(i, i < 0 ? (MAX_CHAR + i) : i, U_CHAR);
    }

    for (int i = MAX_SHORT / 2; i < MAX_SHORT; i += 5000) {
      checkCast(i, i < MAX_SHORT / 2 ? i : (-MAX_SHORT + i), S_SHORT_INT);
      checkCast(i, i < 0 ? (MAX_SHORT + i) : i, U_SHORT_INT);
    }

    for (long i = MAX_INT / 2; i < MAX_INT; i += 10 * 1000 * 1000) {
      checkCast(i, i < MAX_INT / 2 ? i : (-MAX_INT + i), S_INT);
      checkCast(i, i < 0 ? (MAX_INT + i) : i, U_INT);
    }
  }

  /**
   * this test checks the casts of CHAR, SHORT_INT and (normal) INT, it does not use LONG_INT or
   * LONG_LONG_INT
   */
  @Test
  public void checkCastsDirect() throws Exception {

    checkCast(4, 4, S_CHAR);
    checkCast(-4, -4, S_CHAR);
    checkCast(MAX_CHAR, 0, S_CHAR);
    checkCast(260, 4, S_CHAR);
    checkCast(127, 127, S_CHAR);
    checkCast(128, -128, S_CHAR);
    checkCast(-127, -127, S_CHAR);
    checkCast(-128, -128, S_CHAR);
    checkCast(1050, 26, S_CHAR);
    checkCast(-1050, -26, S_CHAR);

    checkCast(4, 4, U_CHAR);
    checkCast(-4, 252, U_CHAR);
    checkCast(256, 0, U_CHAR);
    checkCast(260, 4, U_CHAR);
    checkCast(127, 127, U_CHAR);
    checkCast(-128, 128, U_CHAR);
    checkCast(-127, 129, U_CHAR);
    checkCast(-1, 255, U_CHAR);
    checkCast(1050, 26, U_CHAR);
    checkCast(-1050, 230, U_CHAR);

    checkCast(4L, 4L, S_INT);
    checkCast(-4L, -4L, S_INT);
    checkCast(4294967296L, 0L, S_INT);
    checkCast(4294967300L, 4L, S_INT);
    checkCast(2147483647L, 2147483647L, S_INT);
    checkCast(-2147483648L, -2147483648L, S_INT);
    checkCast(2147483648L, -2147483648L, S_INT);
    checkCast(-2147483649L, 2147483647L, S_INT);
    checkCast(2147483649L, -2147483647L, S_INT);

    checkCast(4L, 4L, U_INT);
    checkCast(-4L, 4294967292L, U_INT);
    checkCast(4294967296L, 0L, U_INT);
    checkCast(4294967300L, 4L, U_INT);
    checkCast(2147483647L, 2147483647L, U_INT);
    checkCast(2147483649L, 2147483649L, U_INT);
    checkCast(-2147483648L, 2147483648L, U_INT);
    checkCast(-2147483649L, 2147483647L, U_INT);
    checkCast(-1L, 4294967295L, U_INT);
  }

  /** this test checks casting, we assume long_int == int == int32 */
  @Test
  public void checkIntegerCasts32() throws Exception {
    assume().that(machineModel).isSameInstanceAs(MachineModel.LINUX32);

    for (long i = -(MAX_INT / 2L); i < MAX_INT; i += 10L * 1000L * 1000L) {

      checkCast(i, i < MAX_INT / 2 ? i : (-MAX_INT + i), S_INT);
      checkCast(i, i < 0 ? (MAX_INT + i) : i, U_INT);

      checkCast(i, i < MAX_INT / 2 ? i : (-MAX_INT + i), S_LONG_INT);
      checkCast(i, i < 0 ? (MAX_INT + i) : i, U_LONG_INT);

      checkCast(i, i, S_LONG_LONG_INT);
      checkCast(i, i, U_LONG_LONG_INT);
    }

    for (CType t : ImmutableList.of(S_INT, S_LONG_INT)) {
      checkCast(4L, 4L, t);
      checkCast(-4L, -4L, t);
      checkCast(4294967296L, 0L, t);
      checkCast(4294967300L, 4L, t);
      checkCast(2147483647L, 2147483647L, t);
      checkCast(-2147483648L, -2147483648L, t);
      checkCast(2147483648L, -2147483648L, t);
      checkCast(-2147483649L, 2147483647L, t);
      checkCast(2147483649L, -2147483647L, t);
    }

    for (CType t : ImmutableList.of(U_INT, U_LONG_INT)) {
      checkCast(4L, 4L, t);
      checkCast(-4L, 4294967292L, t);
      checkCast(4294967296L, 0L, t);
      checkCast(4294967300L, 4L, t);
      checkCast(2147483647L, 2147483647L, t);
      checkCast(2147483649L, 2147483649L, t);
      checkCast(-2147483648L, 2147483648L, t);
      checkCast(-2147483649L, 2147483647L, t);
      checkCast(-1L, 4294967295L, t);
    }
  }

  /** this test checks casting, we assume long_int == long_long_int == int64 */
  @Test
  public void checkIntegerCasts64() throws Exception {
    assume().that(machineModel).isSameInstanceAs(MachineModel.LINUX64);

    for (long i = -(MAX_INT / 2L); i < MAX_INT; i += 10L * 1000L * 1000L) {

      checkCast(i, i < MAX_INT / 2 ? i : (-MAX_INT + i), S_INT);
      checkCast(i, i < 0 ? (MAX_INT + i) : i, U_INT);

      checkCast(i, i, S_LONG_INT);
      checkCast(i, i, U_LONG_INT);

      checkCast(i, i, S_LONG_LONG_INT);
      checkCast(i, i, U_LONG_LONG_INT);
    }

    checkCast(4L, 4L, S_INT);
    checkCast(-4L, -4L, S_INT);
    checkCast(4294967296L, 0L, S_INT);
    checkCast(4294967300L, 4L, S_INT);
    checkCast(2147483647L, 2147483647L, S_INT);
    checkCast(-2147483648L, -2147483648L, S_INT);
    checkCast(2147483648L, -2147483648L, S_INT);
    checkCast(-2147483649L, 2147483647L, S_INT);
    checkCast(2147483649L, -2147483647L, S_INT);

    checkCast(4L, 4L, U_INT);
    checkCast(-4L, 4294967292L, U_INT);
    checkCast(4294967296L, 0L, U_INT);
    checkCast(4294967300L, 4L, U_INT);
    checkCast(2147483647L, 2147483647L, U_INT);
    checkCast(2147483649L, 2147483649L, U_INT);
    checkCast(-2147483648L, 2147483648L, U_INT);
    checkCast(-2147483649L, 2147483647L, U_INT);
    checkCast(-1L, 4294967295L, U_INT);

    checkCast(4L, 4L, S_LONG_INT);
    checkCast(-4L, -4L, S_LONG_INT);
    checkCast(2147483649L, 2147483649L, S_LONG_INT);
    checkCast(-2147483649L, -2147483649L, S_LONG_INT);
    checkCast(4294967296L, 4294967296L, S_LONG_INT);
    checkCast(4294967300L, 4294967300L, S_LONG_INT);

    checkCast(Long.MAX_VALUE, Long.MAX_VALUE, S_LONG_INT);
    checkCast(Long.MIN_VALUE, Long.MIN_VALUE, S_LONG_INT);

    checkCast(4L, 4L, U_LONG_INT);
    checkCast(2147483649L, 2147483649L, U_LONG_INT);
    checkCast(4294967300L, 4294967300L, U_LONG_INT);
    checkCast(-2147483626L, -2147483626L, U_LONG_INT);

    // for U_LONG we cannot make tests with negative values or values > Long.Max_Value,
    // because Java-long is too small.

  }

  @Test
  public void checkFloatCasts32() {
    assume().that(machineModel).isSameInstanceAs(MachineModel.LINUX32);

    performMachineModelAgnosticChecksForFloats();
  }

  @Test
  public void checkFloatCasts64() {
    assume().that(machineModel).isSameInstanceAs(MachineModel.LINUX64);

    performMachineModelAgnosticChecksForFloats();
  }

  @Test
  public void checkFloatToIntCasts32() {
    assume().that(machineModel).isSameInstanceAs(MachineModel.LINUX32);

    for (long i = -MAX_INT; i < MAX_INT; i += 10L * 1000L * 1000L) {
      double d;
      if (i < 0) {
        d = i - 0.5;
      } else {
        d = i + 0.5;
      }
      checkCast(d, i, S_LONG_LONG_INT);
    }
  }

  @Test
  public void checkFloatToIntCasts64() {
    assume().that(machineModel).isSameInstanceAs(MachineModel.LINUX64);

    for (long i = -MAX_INT; i < MAX_INT; i += 10L * 1000L * 1000L) {
      double d;
      if (i < 0) {
        d = i - 0.5;
      } else {
        d = i + 0.5;
      }
      checkCast(d, i, S_LONG_INT);
      checkCast(d, i, S_LONG_LONG_INT);
    }
  }

  private void performMachineModelAgnosticChecksForFloats() {
    for (CType type : ImmutableList.of(FLOAT, DOUBLE)) {
      for (Float n :
          ImmutableList.of(0.0f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)) {
        checkCast(n, n, type);
      }

      for (Double n :
          ImmutableList.of(0.0d, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
        checkCast(n, n, type);
      }
    }

    checkCast(-Double.NaN, -Float.NaN, FLOAT);
    checkCast(-Float.NaN, -Double.NaN, DOUBLE);
    checkCast(0.0d, 0.0d, FLOAT);

    // check that max and min float values are represented as same values in double and long double
    checkCast(Float.MAX_VALUE, Float.MAX_VALUE, DOUBLE);
    checkCast(Float.MIN_VALUE, Float.MIN_VALUE, DOUBLE);
    checkCast(Float.MAX_VALUE, Float.MAX_VALUE, LONG_DOUBLE);
    checkCast(Float.MIN_VALUE, Float.MIN_VALUE, LONG_DOUBLE);
    checkCast(Double.MAX_VALUE, Double.MAX_VALUE, LONG_DOUBLE);
    checkCast(Double.MIN_VALUE, Double.MIN_VALUE, LONG_DOUBLE);
  }

  private void checkCast(long in, long expectedOut, CType outType)
      throws UnrecognizedCodeException {

    final Value value =
        evv.evaluate(
            new CIntegerLiteralExpression(
                FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(in)),
            outType);

    // We know it's of type int since we manually created a CIntegerLiteralExpression
    assertThat(value.asLong(CNumericTypes.INT)).isEqualTo(expectedOut);
  }

  private void checkCast(double in, double expectedOut, CType outType) {
    NumericValue inValue = new NumericValue(in);

    final Value value =
        AbstractExpressionValueVisitor.castCValue(
            inValue, outType, machineModel, logger, FileLocation.DUMMY);

    assertThat(value).isInstanceOf(NumericValue.class);
    assertThat(((NumericValue) value).doubleValue()).isEqualTo(expectedOut);
  }

  private void checkCast(float in, double expectedOut, CType outType) {
    NumericValue inValue = new NumericValue(in);

    final Value value =
        AbstractExpressionValueVisitor.castCValue(
            inValue, outType, machineModel, logger, FileLocation.DUMMY);

    assertThat(value).isInstanceOf(NumericValue.class);
    assertThat(((NumericValue) value).doubleValue()).isEqualTo(expectedOut);
  }

  private void checkCast(float in, float expectedOut, CType outType) {
    NumericValue inValue = new NumericValue(in);

    final Value value =
        AbstractExpressionValueVisitor.castCValue(
            inValue, outType, machineModel, logger, FileLocation.DUMMY);

    assertThat(value).isInstanceOf(NumericValue.class);
    assertThat(((NumericValue) value).floatValue()).isEqualTo(expectedOut);
  }

  private void checkCast(double in, long expectedOut, CType outType) {
    NumericValue inValue = new NumericValue(in);

    final Value value =
        AbstractExpressionValueVisitor.castCValue(
            inValue, outType, machineModel, logger, FileLocation.DUMMY);

    assertThat(value).isInstanceOf(NumericValue.class);
    assertThat(((NumericValue) value).longValue()).isEqualTo(expectedOut);
  }
}
