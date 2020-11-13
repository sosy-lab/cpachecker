/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BuiltinOverflowFunctions {
  // https://gcc.gnu.org/onlinedocs/gcc/Integer-Overflow-Builtins.html
  private static final String ADD = of("add");
  private static final String SADD = of("sadd");
  private static final String SADDL = of("saddl");
  private static final String SADDLL = of("saddll");
  private static final String UADD = of("uadd");
  private static final String UADDL = of("uaddl");
  private static final String UADDLL = of("uaddll");

  private static final String SUB = of("sub");
  private static final String SSUB = of("ssub");
  private static final String SSUBL = of("ssubl");
  private static final String SSUBLL = of("ssubll");
  private static final String USUB = of("usub");
  private static final String USUBL = of("usubl");
  private static final String USUBLL = of("usubll");

  // TODO: add missing overflow functions like multiplication; also add more tests to
  // test/programs/simple/builtin_overflow_functions/

  private static final String PREFIX = "__builtin_";
  private static final String SUFFIX = "_overflow";

  private static final ImmutableList<String> possibleIdentifiers =
      ImmutableList.<String>builder()
          .add(ADD)
          .add(SADD)
          .add(SADDL)
          .add(SADDLL)
          .add(UADD)
          .add(UADDL)
          .add(UADDLL)
          .add(SUB)
          .add(SSUB)
          .add(SSUBL)
          .add(SSUBLL)
          .add(USUB)
          .add(USUBL)
          .add(USUBLL)
          .build();

  private static String of(String identifier) {
    return PREFIX + identifier + SUFFIX;
  }

  private static String getShortIdentifiers(String identifier) {
    // TODO: replace this by a compiled regex
    return identifier.replaceFirst(PREFIX, "").replaceFirst(SUFFIX, "");
  }

  /**
   * resolve the type of the built-yin overflow function. This is important since the input
   * parameters have to be casted in case their type differs TODO: solve this with an enum of the
   * different function names instead.
   */
  public static CSimpleType getType(String functionName) {
    String shortIdentifier = getShortIdentifiers(functionName);
    boolean unsigned = (shortIdentifier.startsWith("u"));
    int size;
    if (shortIdentifier.endsWith("ll")) {
      size = 2;
    } else if (shortIdentifier.endsWith("l")) {
      size = 1;
    } else {
      size = 0;
    }
    if (unsigned) {
      switch (size) {
        case 0:
          return CNumericTypes.UNSIGNED_INT;
        case 1:
          return CNumericTypes.UNSIGNED_LONG_INT;
        case 2:
          return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      }
    } else {
      switch (size) {
        case 0:
          return CNumericTypes.SIGNED_INT;
        case 1:
          return CNumericTypes.SIGNED_LONG_INT;
        case 2:
          return CNumericTypes.SIGNED_LONG_LONG_INT;
      }
    }
    return null;
  }

  public static BinaryOperator getOperator(String functionName) {
    String shortIdentifier = getShortIdentifiers(functionName);
    if (shortIdentifier.contains("add")) {
      return BinaryOperator.PLUS;
    } else {
      return BinaryOperator.MINUS;
    }
  }

  /**
   * Check whether a given function is a builtin function specific to overflows that can be further
   * analyzed with this class.
   */
  public static boolean isBuiltinOverflowFunction(String pFunctionName) {
    return possibleIdentifiers.contains(pFunctionName);
  }

  /* Functions without prefix and suffix have arbitrary argument types */
  public static boolean isFunctionWithArbitraryArgumentTypes(String pFunctionName) {
    return pFunctionName.equals(ADD) || pFunctionName.equals(SUB);
  }

  /**
   * This method returns a {@link CExpression} that represents the truth value of checking whether
   * an arithmetic operation performed on the input expressions var1 and var1 overflows.
   *
   * @throws UnrecognizedCodeException when building the result fails due to unrecognized code
   */
  public static CExpression handleOverflow(
      OverflowAssumptionManager ofmgr, CExpression var1, CExpression var2, String pFunctionName)
      throws UnrecognizedCodeException {
    // TODO: make this more efficient (but probably not worth the effort):
    checkState(possibleIdentifiers.contains(pFunctionName));
    CSimpleType type = getType(pFunctionName);
    BinaryOperator operator = getOperator(pFunctionName);
    CExpression castedVar1 = new CCastExpression(FileLocation.DUMMY, type, var1);
    CExpression castedVar2 = new CCastExpression(FileLocation.DUMMY, type, var2);
    return ofmgr.getConjunctionOfAdditiveAssumptions(
        castedVar1, castedVar2, operator, type, true);
  }

  public static CExpression handleOverflowSideeffects(
      OverflowAssumptionManager ofmgr, CExpression var1, CExpression var2, String pFunctionName)
      throws UnrecognizedCodeException {
    // TODO: make this more efficient (but probably not worth the effort):
    checkState(possibleIdentifiers.contains(pFunctionName));
    // TODO: remove code duplication between handleOverflowSideeffects and handleOverflow
    String shortIdentifier = getShortIdentifiers(pFunctionName);
    CSimpleType type = getType(shortIdentifier);
    BinaryOperator operator = getOperator(shortIdentifier);
    CExpression castedVar1 = new CCastExpression(FileLocation.DUMMY, type, var1);
    CExpression castedVar2 = new CCastExpression(FileLocation.DUMMY, type, var2);
    return ofmgr.getResultOfAdditiveOperation(castedVar1, castedVar2, operator);
  }

  /* This class represents the result of a function evalutation */
  public static class BuiltinOverflowFunctionResult {
    public Value resultOfComputation;
    public Value didOverflow;

    public BuiltinOverflowFunctionResult() {
      resultOfComputation = Value.UnknownValue.getInstance();
      didOverflow = Value.UnknownValue.getInstance();
    }
  }

  /*
   * Calcualtes the result of a builtin overflow function. The arguments are converted to respective
   * types (if necessary), the result of the operation is computed with infinite precision, and the
   * overflow is determined by casting to the type of the third parameter.
   */
  public static BuiltinOverflowFunctionResult evaluateFunctionCall(
      AFunctionCallExpression functionCallExpression,
      AbstractExpressionValueVisitor evv,
      MachineModel machineModel,
      LogManagerWithoutDuplicates logger)
      throws UnrecognizedCodeException {
    BuiltinOverflowFunctionResult result = new BuiltinOverflowFunctionResult();

    AExpression nameExpressionOfCalledFunc = functionCallExpression.getFunctionNameExpression();
    if (nameExpressionOfCalledFunc instanceof AIdExpression) {
      String nameOfCalledFunc = ((AIdExpression) nameExpressionOfCalledFunc).getName();
      if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(nameOfCalledFunc)) {
        List<? extends AExpression> parameters = functionCallExpression.getParameterExpressions();
        if (parameters.size() == 3 && parameters.get(2) instanceof CExpression) {
          Value firstParameterValue =
              evv.evaluate(
                  (CRightHandSide) parameters.get(0),
                  (CType) parameters.get(0).getExpressionType());
          Value secondParameterValue =
              evv.evaluate(
                  (CRightHandSide) parameters.get(1),
                  (CType) parameters.get(1).getExpressionType());
          CSimpleType resultType =
              (CSimpleType) ((CPointerType) parameters.get(2).getExpressionType()).getType();

          if (resultType.getType().isIntegerType()
              && firstParameterValue.isExplicitlyKnown()
              && secondParameterValue.isExplicitlyKnown()) {
            // cast arguments to matching values
            if (!BuiltinOverflowFunctions.isFunctionWithArbitraryArgumentTypes(nameOfCalledFunc)) {
              firstParameterValue =
                  AbstractExpressionValueVisitor.castCValue(
                      firstParameterValue,
                      resultType,
                      machineModel,
                      logger,
                      functionCallExpression.getFileLocation());
              secondParameterValue =
                  AbstractExpressionValueVisitor.castCValue(
                      secondParameterValue,
                      resultType,
                      machineModel,
                      logger,
                      functionCallExpression.getFileLocation());
            }

            // perform operation with infinite precision
            BigInteger p1 = firstParameterValue.asNumericValue().bigInteger();
            BigInteger p2 = secondParameterValue.asNumericValue().bigInteger();

            BigInteger resultOfComputation;
            BinaryOperator operator = BuiltinOverflowFunctions.getOperator(nameOfCalledFunc);
            switch (operator) {
              case PLUS:
                resultOfComputation = p1.add(p2);
                break;

              case MINUS:
                resultOfComputation = p1.subtract(p2);
                break;

              default:
                return result;
            }

            // cast result type of third parameter
            Value resultValue = new NumericValue(resultOfComputation);
            resultValue =
                AbstractExpressionValueVisitor.castCValue(
                    resultValue,
                    resultType,
                    machineModel,
                    logger,
                    functionCallExpression.getFileLocation());

            result.resultOfComputation = resultValue;
            if (resultValue.asNumericValue().bigInteger().equals(resultOfComputation)) {
              result.didOverflow = new NumericValue(0);
            } else {
              result.didOverflow = new NumericValue(1);
            }
          }
        }
      }
    }
    return result;
  }
}
