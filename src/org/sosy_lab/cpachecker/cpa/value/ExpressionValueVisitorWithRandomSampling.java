// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ExpressionValueVisitorWithRandomSampling extends ExpressionValueVisitor {
  public static final String PATERN_FOR_RANDOM = "__VERIFIER_nondet_";

  private final LogManagerWithoutDuplicates logger;

  private final Random randomGenerator;

  /**
   * This Visitor returns the numeral value for an expression.
   *
   * @param pState where to get the values for variables (identifiers)
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   */
  public ExpressionValueVisitorWithRandomSampling(
      ValueAnalysisState pState,
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      Random pRandomGenerator) {
    super(pState, pFunctionName, pMachineModel, pLogger);
    logger = pLogger;
    randomGenerator = pRandomGenerator;
  }

  @Override
  public Value evaluate(CRightHandSide pExp, CType pTargetType) throws UnrecognizedCodeException {
    if (pExp instanceof CFunctionCallExpression call
        && (call.getFunctionNameExpression() instanceof CIdExpression
            && ((CIdExpression) call.getFunctionNameExpression())
                .getName()
                .startsWith(PATERN_FOR_RANDOM))) {

      Value value = newrandomValue(call);

      logger.log(
          Level.FINER,
          "Returning value at position %d, for statement " + pExp.toASTString() + " that is: ",
          value);

      return value;
    }
    return super.evaluate(pExp, pTargetType);
  }

  private Value newrandomValue(CFunctionCallExpression call) {

    // Determine the type that needs to be returned:
    if (call.getExpressionType().getCanonicalType() instanceof CSimpleType pCSimpleType) {
      CBasicType basicType = pCSimpleType.getType();
      return switch (basicType) {
        case UNSPECIFIED ->
            throw new IllegalArgumentException(
                "Cannot handle type UNSPECIFIED in random value generation");
        case BOOL -> new NumericValue(randomGenerator.nextBoolean() ? 1 : 0);
        case CHAR -> new NumericValue(randomGenerator.nextInt(-128, 127));
        case INT -> {
          if (pCSimpleType.hasLongSpecifier()) {
            yield new NumericValue(
                randomGenerator.nextLong(
                    -2 ^ getMachineModel().getSizeofLongInt(),
                    2 ^ getMachineModel().getSizeofLongInt()));
          } else if (pCSimpleType.hasShortSpecifier()) {
            yield new NumericValue(
                randomGenerator.nextLong(
                    -2 ^ getMachineModel().getSizeofShortInt(),
                    2 ^ getMachineModel().getSizeofShortInt()));
          } else {
            yield new NumericValue(randomGenerator.nextInt());
          }
        }
        // Does not use the full range of INT128, but should be okay
        case INT128 -> new NumericValue(BigInteger.valueOf(randomGenerator.nextLong()));
        case FLOAT -> new NumericValue(randomGenerator.nextFloat());
        case DOUBLE -> new NumericValue(randomGenerator.nextDouble());
        // Does not use the full range of FLOAT128, but should be okay
        case FLOAT128 -> new NumericValue(BigDecimal.valueOf(randomGenerator.nextDouble()));
      };
    } else {
      logger.log(Level.WARNING, "Cannot parse complex types, hence returning unknown");
    }
    return new UnknownValue();
  }
}
