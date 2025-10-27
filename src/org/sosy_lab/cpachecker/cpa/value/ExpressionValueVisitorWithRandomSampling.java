// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

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
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public class ExpressionValueVisitorWithRandomSampling extends ExpressionValueVisitor {
  public static final String PATTERN_FOR_RANDOM = "__VERIFIER_nondet_";

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
                .startsWith(PATTERN_FOR_RANDOM))) {

      Value value = newRandomValue(call);

      logger.logf(
          Level.FINER,
          "Returning value at position %s, for statement %s that is: ",
          value,
          pExp.toASTString());

      return value;
    }
    return super.evaluate(pExp, pTargetType);
  }

  /**
   * Generates a random BigInteger in the given range [min, max]
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (inclusive)
   * @return a random NumericValue in the given range
   */
  private BigInteger randomIntInRange(BigInteger min, BigInteger max) {
    BigInteger guess;
    do {
      guess = new BigInteger(max.subtract(min).bitLength(), randomGenerator);
    } while (guess.compareTo(max) >= 0 || guess.compareTo(min) <= 0);

    return guess.add(min);
  }

  private Value newRandomValue(CFunctionCallExpression call) {

    // Determine the type that needs to be returned:
    if (call.getExpressionType().getCanonicalType() instanceof CSimpleType pCSimpleType) {
      CBasicType basicType = pCSimpleType.getType();
      return switch (basicType) {
        case UNSPECIFIED ->
            throw new IllegalArgumentException(
                "Cannot handle type UNSPECIFIED in random value generation");
        case BOOL -> new NumericValue(randomGenerator.nextBoolean() ? 1 : 0);
        case CHAR, INT, INT128 ->
            new NumericValue(
                randomIntInRange(
                    getMachineModel().getMinimalIntegerValue(pCSimpleType),
                    getMachineModel().getMaximalIntegerValue(pCSimpleType)));
        case FLOAT, DOUBLE, FLOAT128 ->
            new NumericValue(
                FloatValue.randomValue(
                    FloatValue.Format.fromCType(getMachineModel(), pCSimpleType), randomGenerator));
      };
    } else {
      logger.logOnce(
          Level.WARNING,
          "Cannot instantiate complex types for call '%s' with a concrete "
              + "value for random testing, hence returning unknown.",
          call.toASTString());
    }
    return new UnknownValue();
  }
}
