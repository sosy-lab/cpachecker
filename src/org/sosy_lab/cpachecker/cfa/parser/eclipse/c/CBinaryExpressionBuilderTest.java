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
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.collect.Lists;


public class CBinaryExpressionBuilderTest {


  //type constants
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


  private final static List<CSimpleType> smallTypes = Lists.newArrayList(
      S_CHAR, U_CHAR, S_SHORT_INT, U_SHORT_INT, S_INT);
  private final static List<CSimpleType> bigTypes = Lists.newArrayList(
      U_INT, S_LONG_INT, U_LONG_INT, S_LONG_LONG_INT, U_LONG_LONG_INT);


  private static CSimpleType makeInt(
      boolean pIsSigned, boolean pIsShort, boolean pIsLong, boolean pIsLongLong) {
    return new CSimpleType(false, false, CBasicType.INT,
        pIsLong, pIsShort, pIsSigned, !pIsSigned, false, false, pIsLongLong);
  }


  private LogManager logger;

  private CBinaryExpressionBuilder c32;
  private CBinaryExpressionBuilder c64;


  @Before
  public void init() {
    logger = TestLogManager.getInstance();

    c32 = new CBinaryExpressionBuilder(MachineModel.LINUX32, logger);
    c64 = new CBinaryExpressionBuilder(MachineModel.LINUX64, logger);
  }


  @Test
  public void checkTypeForBinaryOperation32() {
    checkArithmetic(c32, PLUS);
    checkArithmetic(c32, MINUS);
    checkArithmetic(c32, MULTIPLY);

    checkArithmetic32(PLUS);
    checkArithmetic32(MINUS);
    checkArithmetic32(MULTIPLY);

    checkRelational(c32, EQUALS);
    checkRelational(c32, LESS_THAN);
    checkRelational(c32, GREATER_EQUAL);

  }


  @Test
  public void checkTypeForBinaryOperation64() {
    checkArithmetic(c64, PLUS);
    checkArithmetic(c64, MINUS);
    checkArithmetic(c64, MULTIPLY);

    checkArithmetic64(PLUS);
    checkArithmetic64(MINUS);
    checkArithmetic64(MULTIPLY);

    checkRelational(c64, EQUALS);
    checkRelational(c64, LESS_THAN);
    checkRelational(c64, GREATER_EQUAL);
  }


  private void checkArithmetic(CBinaryExpressionBuilder c, BinaryOperator op) {

    for (CSimpleType small1 : smallTypes) {
      for (CSimpleType small2 : smallTypes) {
        checkResult(c, op, small1, small2, S_INT);
        checkCalculation(c, op, small1, small2, S_INT);
      }
    }

    for (CSimpleType big : bigTypes) {
      for (CSimpleType small : smallTypes) {
        checkResult(c, op, big, small, big);
        checkCalculation(c, op, big, small, big);
      }
    }
  }


  private void checkRelational(CBinaryExpressionBuilder c, BinaryOperator op) {

    for (CSimpleType small1 : smallTypes) {
      for (CSimpleType small2 : smallTypes) {
        checkResult(c, op, small1, small2, S_INT);
        checkCalculation(c, op, small1, small2, S_INT);
      }
    }

    for (CSimpleType big : bigTypes) {
      for (CSimpleType small : smallTypes) {
        checkResult(c, op, big, small, S_INT);
        checkCalculation(c, op, big, small, big);
      }
      for (CSimpleType big2 : bigTypes) {
        checkResult(c, op, big, big2, S_INT);
      }
    }
  }


  private void checkArithmetic32(BinaryOperator op) {
    checkCalculation(c32, op, U_INT, U_INT, U_INT);
    checkCalculation(c32, op, U_INT, S_LONG_INT, U_LONG_INT); // !!!!
    checkCalculation(c32, op, U_INT, U_LONG_INT, U_LONG_INT);
    checkCalculation(c32, op, U_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(c32, op, U_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(c32, op, S_LONG_INT, S_LONG_INT, S_LONG_INT);
    checkCalculation(c32, op, S_LONG_INT, U_LONG_INT, U_LONG_INT);
    checkCalculation(c32, op, S_LONG_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(c32, op, S_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(c32, op, U_LONG_INT, U_LONG_INT, U_LONG_INT);
    checkCalculation(c32, op, U_LONG_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(c32, op, U_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(c32, op, S_LONG_LONG_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(c32, op, S_LONG_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(c32, op, U_LONG_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);
  }


  private void checkArithmetic64(BinaryOperator op) {
    checkCalculation(c64, op, U_INT, U_INT, U_INT);
    checkCalculation(c64, op, U_INT, S_LONG_INT, S_LONG_INT);
    checkCalculation(c64, op, U_INT, U_LONG_INT, U_LONG_INT);
    checkCalculation(c64, op, U_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(c64, op, U_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(c64, op, S_LONG_INT, S_LONG_INT, S_LONG_INT);
    checkCalculation(c64, op, S_LONG_INT, U_LONG_INT, U_LONG_INT);
    checkCalculation(c64, op, S_LONG_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(c64, op, S_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(c64, op, U_LONG_INT, U_LONG_INT, U_LONG_INT);
    checkCalculation(c64, op, U_LONG_INT, S_LONG_LONG_INT, U_LONG_LONG_INT); // !!!!
    checkCalculation(c64, op, U_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(c64, op, S_LONG_LONG_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(c64, op, S_LONG_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(c64, op, U_LONG_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);
  }


  private void checkCalculation(CBinaryExpressionBuilder c, BinaryOperator op, CType t1, CType t2, CType target) {
    assertEquals(target, c.getCalculationTypeForBinaryOperation(t1, t2, op, CNumericTypes.ZERO, CNumericTypes.ZERO));
    assertEquals(target, c.getCalculationTypeForBinaryOperation(t2, t1, op, CNumericTypes.ZERO, CNumericTypes.ZERO));
  }


  private void checkResult(CBinaryExpressionBuilder c, BinaryOperator op, CType t1, CType t2, CType target) {
    assertEquals(target, c.getResultTypeForBinaryOperation(t1, t2, op, CNumericTypes.ZERO, CNumericTypes.ZERO));
    assertEquals(target, c.getResultTypeForBinaryOperation(t2, t1, op, CNumericTypes.ZERO, CNumericTypes.ZERO));
  }
}
