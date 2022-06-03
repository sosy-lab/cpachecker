// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.GREATER_EQUAL;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_THAN;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.MINUS;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.MULTIPLY;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.PLUS;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

@RunWith(Parameterized.class)
public class CBinaryExpressionBuilderTest {

  @Parameters(name = "{0}")
  public static Object[] getMachineModels() {
    return MachineModel.values();
  }

  @Parameter(0)
  public MachineModel machineModel;

  // type constants, need to be canonical
  private static final CSimpleType S_CHAR = CNumericTypes.SIGNED_CHAR;
  private static final CSimpleType U_CHAR = CNumericTypes.UNSIGNED_CHAR;

  private static final CSimpleType S_SHORT_INT = CNumericTypes.SHORT_INT.getCanonicalType();
  private static final CSimpleType U_SHORT_INT = CNumericTypes.UNSIGNED_SHORT_INT;
  private static final CSimpleType S_INT = CNumericTypes.INT.getCanonicalType();
  private static final CSimpleType U_INT = CNumericTypes.UNSIGNED_INT;
  private static final CSimpleType S_LONG_INT = CNumericTypes.LONG_INT.getCanonicalType();
  private static final CSimpleType U_LONG_INT = CNumericTypes.UNSIGNED_LONG_INT;
  private static final CSimpleType S_LONG_LONG_INT = CNumericTypes.LONG_LONG_INT.getCanonicalType();
  private static final CSimpleType U_LONG_LONG_INT = CNumericTypes.UNSIGNED_LONG_LONG_INT;

  private static final List<CSimpleType> smallTypes =
      ImmutableList.of(S_CHAR, U_CHAR, S_SHORT_INT, U_SHORT_INT, S_INT);
  private static final List<CSimpleType> bigTypes =
      ImmutableList.of(U_INT, S_LONG_INT, U_LONG_INT, S_LONG_LONG_INT, U_LONG_LONG_INT);

  private LogManager logger;
  private CBinaryExpressionBuilder c;

  @Before
  public void init() {
    logger = LogManager.createTestLogManager();

    c = new CBinaryExpressionBuilder(machineModel, logger);
  }

  @Test
  public void checkTypeForBinaryOperation() throws UnrecognizedCodeException {
    checkArithmeticTypes(PLUS);
    checkArithmeticTypes(MINUS);
    checkArithmeticTypes(MULTIPLY);

    checkArithmeticCalculationTypes(PLUS);
    checkArithmeticCalculationTypes(MINUS);
    checkArithmeticCalculationTypes(MULTIPLY);

    checkRelationalTypes(EQUALS);
    checkRelationalTypes(LESS_THAN);
    checkRelationalTypes(GREATER_EQUAL);
  }

  private void checkArithmeticTypes(BinaryOperator op) throws UnrecognizedCodeException {

    for (CSimpleType small1 : smallTypes) {
      for (CSimpleType small2 : smallTypes) {
        checkResult(op, small1, small2, S_INT);
        checkCalculation(op, small1, small2, S_INT);
      }
    }

    for (CSimpleType big : bigTypes) {
      for (CSimpleType small : smallTypes) {
        checkResult(op, big, small, big);
        checkCalculation(op, big, small, big);
      }
    }
  }

  private void checkRelationalTypes(BinaryOperator op) throws UnrecognizedCodeException {

    for (CSimpleType small1 : smallTypes) {
      for (CSimpleType small2 : smallTypes) {
        checkResult(op, small1, small2, S_INT);
        checkCalculation(op, small1, small2, S_INT);
      }
    }

    for (CSimpleType big : bigTypes) {
      for (CSimpleType small : smallTypes) {
        checkResult(op, big, small, S_INT);
        checkCalculation(op, big, small, big);
      }
      for (CSimpleType big2 : bigTypes) {
        checkResult(op, big, big2, S_INT);
      }
    }
  }

  private void checkArithmeticCalculationTypes(BinaryOperator op) throws UnrecognizedCodeException {
    checkCalculation(op, U_INT, U_INT, U_INT);
    if (machineModel == MachineModel.LINUX32 || machineModel == MachineModel.ARM) {
      checkCalculation(op, U_INT, S_LONG_INT, U_LONG_INT); // !!!!
    } else {
      checkCalculation(op, U_INT, S_LONG_INT, S_LONG_INT);
    }
    checkCalculation(op, U_INT, U_LONG_INT, U_LONG_INT);
    checkCalculation(op, U_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(op, U_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(op, S_LONG_INT, S_LONG_INT, S_LONG_INT);
    checkCalculation(op, S_LONG_INT, U_LONG_INT, U_LONG_INT);
    checkCalculation(op, S_LONG_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(op, S_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(op, U_LONG_INT, U_LONG_INT, U_LONG_INT);
    if (machineModel == MachineModel.LINUX64 || machineModel == MachineModel.ARM64) {
      checkCalculation(op, U_LONG_INT, S_LONG_LONG_INT, U_LONG_LONG_INT); // !!!!
    } else {
      checkCalculation(op, U_LONG_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    }
    checkCalculation(op, U_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(op, S_LONG_LONG_INT, S_LONG_LONG_INT, S_LONG_LONG_INT);
    checkCalculation(op, S_LONG_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);

    checkCalculation(op, U_LONG_LONG_INT, U_LONG_LONG_INT, U_LONG_LONG_INT);
  }

  private void checkCalculation(BinaryOperator op, CType t1, CType t2, CType target)
      throws UnrecognizedCodeException {
    assertThat(
            c.getCalculationTypeForBinaryOperation(
                t1, t2, op, CIntegerLiteralExpression.ZERO, CIntegerLiteralExpression.ZERO))
        .isEqualTo(target);
    assertThat(
            c.getCalculationTypeForBinaryOperation(
                t2, t1, op, CIntegerLiteralExpression.ZERO, CIntegerLiteralExpression.ZERO))
        .isEqualTo(target);
  }

  private void checkResult(BinaryOperator op, CType t1, CType t2, CType target)
      throws UnrecognizedCodeException {
    assertThat(
            c.getResultTypeForBinaryOperation(
                t1, t2, op, CIntegerLiteralExpression.ZERO, CIntegerLiteralExpression.ZERO))
        .isEqualTo(target);
    assertThat(
            c.getResultTypeForBinaryOperation(
                t2, t1, op, CIntegerLiteralExpression.ZERO, CIntegerLiteralExpression.ZERO))
        .isEqualTo(target);
  }
}
