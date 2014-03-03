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
package org.sosy_lab.cpachecker.cpa.value;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.collect.Lists;


public class ExpressionValueVisitorTest {


  // we need some dummy-values.
  private final ValueAnalysisState state = new ValueAnalysisState();
  private final String functionName = "dummy_function";
  private final FileLocation loc = FileLocation.DUMMY;


  // constants for C
  private final int MAX_CHAR = 256;
  private final int MAX_SHORT = 65536;
  private final long MAX_INT = 4294967296L;


  // type constants
  private final static CSimpleType S_CHAR =
      new CSimpleType(false, false, CBasicType.CHAR, false, false, true, false, false, false, false);
  private final static CSimpleType U_CHAR =
      new CSimpleType(false, false, CBasicType.CHAR, false, false, false, true, false, false, false);

  private final static CSimpleType S_SHORT_INT = makeInt(true, true, false, false);
  private final static CSimpleType U_SHORT_INT = makeInt(false, true, false, false);
  private final static CSimpleType S_INT = makeInt(true, false, false, false);
  private final static CSimpleType U_INT = makeInt(false, false, false, false);
  private final static CSimpleType S_LONG_INT = makeInt(true, false, true, false);
  private final static CSimpleType U_LONG_INT = makeInt(false, false, true, false);
  private final static CSimpleType S_LONG_LONG_INT = makeInt(true, false, false, true);
  private final static CSimpleType U_LONG_LONG_INT = makeInt(false, false, false, true);


  private static CSimpleType makeInt(
      boolean pIsSigned, boolean pIsShort, boolean pIsLong, boolean pIsLongLong) {
    return new CSimpleType(false, false, CBasicType.INT,
        pIsLong, pIsShort, pIsSigned, !pIsSigned, false, false, pIsLongLong);
  }


  private Configuration config;
  private StringBuildingLogHandler stringLogHandler;
  private LogManager logger;

  private ExpressionValueVisitor evv32;
  private ExpressionValueVisitor evv64;


  @Before
  public void init() throws InvalidConfigurationException {
    config = Configuration.builder().build();
    stringLogHandler = new StringBuildingLogHandler();
    logger = new BasicLogManager(config, stringLogHandler);

    evv32 = new ExpressionValueVisitor(
        state, functionName, MachineModel.LINUX32, logger, null);
    evv64 = new ExpressionValueVisitor(
        state, functionName, MachineModel.LINUX64, logger, null);

  }

  @Test
  public void checkSimpleCasts32() throws Exception {
    checkSimpleCasts(evv32);
    checkCastsDirect(evv32);
  }

  @Test
  public void checkSimpleCasts64() throws Exception {
    checkSimpleCasts(evv64);
    checkCastsDirect(evv64);
  }

  @Test
  public void checkCasts32() throws Exception {
    checkCasts32(evv32);
  }

  @Test
  public void checkCasts64() throws Exception {
    checkCasts64(evv64);
  }


  /**
   * this test checks the casts of CHAR, SHORT_INT and (normal) INT,
   * it does not use LONG_INT or LONG_LONG_INT
   */
  private void checkSimpleCasts(ExpressionValueVisitor evv) throws Exception {

    for (int i = -MAX_CHAR / 2; i < MAX_CHAR; i += 50) {
      checkCast(evv, i, i < MAX_CHAR / 2 ? i : (-MAX_CHAR + i), S_CHAR);
      checkCast(evv, i, i < 0 ? (MAX_CHAR + i) : i, U_CHAR);
    }

    for (int i = MAX_SHORT / 2; i < MAX_SHORT; i += 5000) {
      checkCast(evv, i, i < MAX_SHORT / 2 ? i : (-MAX_SHORT + i), S_SHORT_INT);
      checkCast(evv, i, i < 0 ? (MAX_SHORT + i) : i, U_SHORT_INT);
    }

    for (long i = MAX_INT / 2; i < MAX_INT; i += 10 * 1000 * 1000) {
      checkCast(evv, i, i < MAX_INT / 2 ? i : (-MAX_INT + i), S_INT);
      checkCast(evv, i, i < 0 ? (MAX_INT + i) : i, U_INT);
    }
  }


  /**
   * this test checks the casts of CHAR, SHORT_INT and (normal) INT,
   * it does not use LONG_INT or LONG_LONG_INT
   */
  private void checkCastsDirect(ExpressionValueVisitor evv) throws Exception {

    checkCast(evv, 4, 4, S_CHAR);
    checkCast(evv, -4, -4, S_CHAR);
    checkCast(evv, MAX_CHAR, 0, S_CHAR);
    checkCast(evv, 260, 4, S_CHAR);
    checkCast(evv, 127, 127, S_CHAR);
    checkCast(evv, 128, -128, S_CHAR);
    checkCast(evv, -127, -127, S_CHAR);
    checkCast(evv, -128, -128, S_CHAR);
    checkCast(evv, 1050, 26, S_CHAR);
    checkCast(evv, -1050, -26, S_CHAR);

    checkCast(evv, 4, 4, U_CHAR);
    checkCast(evv, -4, 252, U_CHAR);
    checkCast(evv, 256, 0, U_CHAR);
    checkCast(evv, 260, 4, U_CHAR);
    checkCast(evv, 127, 127, U_CHAR);
    checkCast(evv, -128, 128, U_CHAR);
    checkCast(evv, -127, 129, U_CHAR);
    checkCast(evv, -1, 255, U_CHAR);
    checkCast(evv, 1050, 26, U_CHAR);
    checkCast(evv, -1050, 230, U_CHAR);

    checkCast(evv, 4L, 4L, S_INT);
    checkCast(evv, -4L, -4L, S_INT);
    checkCast(evv, 4294967296L, 0L, S_INT);
    checkCast(evv, 4294967300L, 4L, S_INT);
    checkCast(evv, 2147483647L, 2147483647L, S_INT);
    checkCast(evv, -2147483648L, -2147483648L, S_INT);
    checkCast(evv, 2147483648L, -2147483648L, S_INT);
    checkCast(evv, -2147483649L, 2147483647L, S_INT);
    checkCast(evv, 2147483649L, -2147483647L, S_INT);

    checkCast(evv, 4L, 4L, U_INT);
    checkCast(evv, -4L, 4294967292L, U_INT);
    checkCast(evv, 4294967296L, 0L, U_INT);
    checkCast(evv, 4294967300L, 4L, U_INT);
    checkCast(evv, 2147483647L, 2147483647L, U_INT);
    checkCast(evv, 2147483649L, 2147483649L, U_INT);
    checkCast(evv, -2147483648L, 2147483648L, U_INT);
    checkCast(evv, -2147483649L, 2147483647L, U_INT);
    checkCast(evv, -1L, 4294967295L, U_INT);
  }


  /** this test checks casting, we assume long_int == int == int32 */
  private void checkCasts32(ExpressionValueVisitor evv) throws Exception {

    for (long i = -(MAX_INT / 2L); i < MAX_INT; i += 10L * 1000L * 1000L) {

      checkCast(evv, i, i < MAX_INT / 2 ? i : (-MAX_INT + i), S_INT);
      checkCast(evv, i, i < 0 ? (MAX_INT + i) : i, U_INT);

      checkCast(evv, i, i < MAX_INT / 2 ? i : (-MAX_INT + i), S_LONG_INT);
      checkCast(evv, i, i < 0 ? (MAX_INT + i) : i, U_LONG_INT);

      checkCast(evv, i, i, S_LONG_LONG_INT);
      checkCast(evv, i, i, U_LONG_LONG_INT);
    }

    for (CType t : Lists.newArrayList(S_INT, S_LONG_INT)) {
      checkCast(evv, 4L, 4L, t);
      checkCast(evv, -4L, -4L, t);
      checkCast(evv, 4294967296L, 0L, t);
      checkCast(evv, 4294967300L, 4L, t);
      checkCast(evv, 2147483647L, 2147483647L, t);
      checkCast(evv, -2147483648L, -2147483648L, t);
      checkCast(evv, 2147483648L, -2147483648L, t);
      checkCast(evv, -2147483649L, 2147483647L, t);
      checkCast(evv, 2147483649L, -2147483647L, t);
    }

    for (CType t : Lists.newArrayList(U_INT, U_LONG_INT)) {
      checkCast(evv, 4L, 4L, t);
      checkCast(evv, -4L, 4294967292L, t);
      checkCast(evv, 4294967296L, 0L, t);
      checkCast(evv, 4294967300L, 4L, t);
      checkCast(evv, 2147483647L, 2147483647L, t);
      checkCast(evv, 2147483649L, 2147483649L, t);
      checkCast(evv, -2147483648L, 2147483648L, t);
      checkCast(evv, -2147483649L, 2147483647L, t);
      checkCast(evv, -1L, 4294967295L, t);
    }
  }


  /** this test checks casting, we assume long_int == long_long_int == int64 */
  private void checkCasts64(ExpressionValueVisitor evv) throws Exception {

    for (long i = -(MAX_INT / 2L); i < MAX_INT; i += 10L * 1000L * 1000L) {

      checkCast(evv, i, i < MAX_INT / 2 ? i : (-MAX_INT + i), S_INT);
      checkCast(evv, i, i < 0 ? (MAX_INT + i) : i, U_INT);

      checkCast(evv, i, i, S_LONG_INT);
      checkCast(evv, i, i, U_LONG_INT);

      checkCast(evv, i, i, S_LONG_LONG_INT);
      checkCast(evv, i, i, U_LONG_LONG_INT);
    }

    checkCast(evv, 4L, 4L, S_INT);
    checkCast(evv, -4L, -4L, S_INT);
    checkCast(evv, 4294967296L, 0L, S_INT);
    checkCast(evv, 4294967300L, 4L, S_INT);
    checkCast(evv, 2147483647L, 2147483647L, S_INT);
    checkCast(evv, -2147483648L, -2147483648L, S_INT);
    checkCast(evv, 2147483648L, -2147483648L, S_INT);
    checkCast(evv, -2147483649L, 2147483647L, S_INT);
    checkCast(evv, 2147483649L, -2147483647L, S_INT);

    checkCast(evv, 4L, 4L, U_INT);
    checkCast(evv, -4L, 4294967292L, U_INT);
    checkCast(evv, 4294967296L, 0L, U_INT);
    checkCast(evv, 4294967300L, 4L, U_INT);
    checkCast(evv, 2147483647L, 2147483647L, U_INT);
    checkCast(evv, 2147483649L, 2147483649L, U_INT);
    checkCast(evv, -2147483648L, 2147483648L, U_INT);
    checkCast(evv, -2147483649L, 2147483647L, U_INT);
    checkCast(evv, -1L, 4294967295L, U_INT);

    checkCast(evv, 4L, 4L, S_LONG_INT);
    checkCast(evv, -4L, -4L, S_LONG_INT);
    checkCast(evv, 2147483649L, 2147483649L, S_LONG_INT);
    checkCast(evv, -2147483649L, -2147483649L, S_LONG_INT);
    checkCast(evv, 4294967296L, 4294967296L, S_LONG_INT);
    checkCast(evv, 4294967300L, 4294967300L, S_LONG_INT);

    checkCast(evv, Long.MAX_VALUE, Long.MAX_VALUE, S_LONG_INT);
    checkCast(evv, Long.MIN_VALUE, Long.MIN_VALUE, S_LONG_INT);

    checkCast(evv, 4L, 4L, U_LONG_INT);
    checkCast(evv, 2147483649L, 2147483649L, U_LONG_INT);
    checkCast(evv, 4294967300L, 4294967300L, U_LONG_INT);
    checkCast(evv, -2147483626L, -2147483626L, U_LONG_INT);

    // for U_LONG we cannot make tests with negative values or values > Long.Max_Value,
    // because Java-long is too small.

  }


  private void checkCast(ExpressionValueVisitor evv, long in, long expectedOut, CType outType)
      throws UnrecognizedCCodeException {

    // we use NULL as inputType, because it is not needed
    final Value value = evv.evaluate(
        new CIntegerLiteralExpression(loc, null, BigInteger.valueOf(in)),
        outType);

    System.out.println(String.format("(%s) %d == %d == %s", outType, in, expectedOut, value.toString()));

    // TODO explicitfloat: add floats to the test
    // We know it's of type int since we manually created a CIntegerLiteralExpression
    Assert.assertTrue(expectedOut == value.asLong(CNumericTypes.INT));
  }
}
