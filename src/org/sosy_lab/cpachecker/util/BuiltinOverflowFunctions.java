// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
  private enum BuiltinOverflowFunction {
    ADD(BinaryOperator.PLUS, null, false),
    ADD_P(BinaryOperator.PLUS, null, true),
    SADD(BinaryOperator.PLUS, CNumericTypes.SIGNED_INT, false),
    SADDL(BinaryOperator.PLUS, CNumericTypes.SIGNED_LONG_INT, false),
    SADDLL(BinaryOperator.PLUS, CNumericTypes.SIGNED_LONG_LONG_INT, false),
    UADD(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_INT, false),
    UADDL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_INT, false),
    UADDLL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, false),

    SUB(BinaryOperator.MINUS, null, false),
    SUB_P(BinaryOperator.MINUS, null, true),
    SSUB(BinaryOperator.MINUS, CNumericTypes.SIGNED_INT, false),
    SSUBL(BinaryOperator.MINUS, CNumericTypes.SIGNED_LONG_INT, false),
    SSUBLL(BinaryOperator.MINUS, CNumericTypes.SIGNED_LONG_LONG_INT, false),
    USUB(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_INT, false),
    USUBL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_INT, false),
    USUBLL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, false),

    MUL(BinaryOperator.MULTIPLY, null, false),
    MUL_P(BinaryOperator.MULTIPLY, null, true),
    SMUL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_INT, false),
    SMULL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_LONG_INT, false),
    SMULLL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_LONG_LONG_INT, false),
    UMUL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_INT, false),
    UMULL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_LONG_INT, false),
    UMULLL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_LONG_LONG_INT, false);

    final BinaryOperator operator;
    final Optional<CSimpleType> type;
    final Boolean hasNoSideEffects;
    final String name;

    BuiltinOverflowFunction(
        BinaryOperator pOperator, @Nullable CSimpleType pType, Boolean pHasNoSideEffect) {
      operator = pOperator;
      type = Optional.ofNullable(pType);
      hasNoSideEffects = pHasNoSideEffect;

      StringBuilder sb = new StringBuilder();
      sb.append("__builtin_")
          .append(getDataTypePrefix(pType))
          .append(getOperatorName(pOperator))
          .append(getDataTypeSuffix(pType))
          .append("_overflow")
          .append(pHasNoSideEffect ? "_p" : "");
      name = sb.toString();
    }

    private static String getOperatorName(BinaryOperator pOperator) {
      if (pOperator == BinaryOperator.PLUS) {
        return "add";
      } else if (pOperator == BinaryOperator.MINUS) {
        return "sub";
      } else {
        return "mul";
      }
    }

    private static String getDataTypePrefix(@Nullable CSimpleType pType) {
      if (pType == null) {
        return "";
      }

      if (pType.hasSignedSpecifier()) {
        return "s";
      }

      return "u";
    }

    private static String getDataTypeSuffix(@Nullable CSimpleType pType) {
      if (pType == null) {
        return "";
      }

      if (pType.hasLongSpecifier()) {
        return "l";
      } else if (pType.hasLongLongSpecifier()) {
        return "ll";
      }

      return "";
    }
  }

  private static final Map<String, BuiltinOverflowFunction> functions;

  static {
    functions = from(BuiltinOverflowFunction.values()).uniqueIndex(func -> func.name);
  }

  /**
   * resolve the type of the built-yin overflow function. This is important since the input
   * parameters have to be casted in case their type differs
   */
  public static Optional<CSimpleType> getType(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    return functions.get(pFunctionName).type;
  }

  public static BinaryOperator getOperator(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    return functions.get(pFunctionName).operator;
  }

  /**
   * Check whether a given function is a builtin function specific to overflows that can be further
   * analyzed with this class.
   */
  public static boolean isBuiltinOverflowFunction(String pFunctionName) {
    return functions.containsKey(pFunctionName);
  }

  /* Functions without prefix and suffix have arbitrary argument types */
  public static boolean isFunctionWithArbitraryArgumentTypes(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    return !functions.get(pFunctionName).type.isPresent();
  }

  public static boolean isFunctionWithoutSideEffect(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    return functions.get(pFunctionName).hasNoSideEffects;
  }

  public static List<CType> getParameterTypes(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    Optional<CSimpleType> type = functions.get(pFunctionName).type;

    if (type.isPresent()) {
      return ImmutableList.of(
          type.orElseThrow(),
          type.orElseThrow(),
          new CPointerType(false, false, type.orElseThrow()));
    } else {
      return ImmutableList.of();
    }
  }

  /**
   * This method returns a {@link CExpression} that represents the truth value of checking whether
   * an arithmetic operation performed on the input expressions var1 and var1 overflows.
   *
   * @throws UnrecognizedCodeException when building the result fails due to unrecognized code
   */
  public static CExpression handleOverflow(
      OverflowAssumptionManager ofmgr,
      CExpression var1,
      CExpression var2,
      CExpression var3,
      String pFunctionName)
      throws UnrecognizedCodeException {
    checkState(functions.containsKey(pFunctionName));
    CSimpleType targetType = getTargetType(pFunctionName, var3);
    BinaryOperator operator = getOperator(pFunctionName);
    CExpression castedVar1 = var1;
    CExpression castedVar2 = var2;
    if (!isFunctionWithArbitraryArgumentTypes(pFunctionName)) {
      castedVar1 = new CCastExpression(FileLocation.DUMMY, targetType, var1);
      castedVar2 = new CCastExpression(FileLocation.DUMMY, targetType, var2);
    }

    CExpression result;
    if (operator == BinaryOperator.MULTIPLY) {
      result =
          ofmgr.getConjunctionOfMultiplicationAssumptions(castedVar1, castedVar2, targetType, true);
    } else {
      result =
          ofmgr.getConjunctionOfAdditiveAssumptions(
              castedVar1, castedVar2, operator, targetType, true);
    }

    return new CCastExpression(FileLocation.DUMMY, CNumericTypes.BOOL, result);
  }

  public static CExpression handleOverflowSideeffects(
      OverflowAssumptionManager ofmgr,
      CExpression var1,
      CExpression var2,
      CExpression var3,
      String pFunctionName)
      throws UnrecognizedCodeException {
    checkState(functions.containsKey(pFunctionName));
    CSimpleType targetType = getTargetType(pFunctionName, var3);
    BinaryOperator operator = getOperator(pFunctionName);
    CExpression castedVar1 = new CCastExpression(FileLocation.DUMMY, targetType, var1);
    CExpression castedVar2 = new CCastExpression(FileLocation.DUMMY, targetType, var2);
    return ofmgr.getResultOfOperation(castedVar1, castedVar2, operator);
  }

  private static CSimpleType getTargetType(String pFunctionName, CExpression thirdArgument) {
    if (!isFunctionWithArbitraryArgumentTypes(pFunctionName)) {
      return getType(pFunctionName).orElseThrow();
    }

    CType targetType = thirdArgument.getExpressionType().getCanonicalType();
    if (targetType instanceof CPointerType) {
      targetType = ((CPointerType) targetType).getType();
    }
    return (CSimpleType) targetType;
  }

  /*
   * Calcualtes the result of a builtin overflow function (e.g. for value analysis). The arguments are converted to respective
   * types (if necessary), the result of the operation is computed with infinite precision, and the
   * overflow is determined by casting to the type of the third parameter.
   */
  public static Value evaluateFunctionCall(
      CFunctionCallExpression functionCallExpression,
      AbstractExpressionValueVisitor evv,
      MachineModel machineModel,
      LogManagerWithoutDuplicates logger)
      throws UnrecognizedCodeException {
    CExpression nameExpressionOfCalledFunc = functionCallExpression.getFunctionNameExpression();
    if (nameExpressionOfCalledFunc instanceof AIdExpression) {
      String nameOfCalledFunc = ((CIdExpression) nameExpressionOfCalledFunc).getName();
      if (isBuiltinOverflowFunction(nameOfCalledFunc)) {
        List<CExpression> parameters = functionCallExpression.getParameterExpressions();
        if (parameters.size() == 3) {
          Value firstParameterValue =
              evv.evaluate(parameters.get(0), parameters.get(0).getExpressionType());
          Value secondParameterValue =
              evv.evaluate(parameters.get(1), parameters.get(1).getExpressionType());
          CSimpleType resultType = getTargetType(nameOfCalledFunc, parameters.get(2));

          if (resultType.getType().isIntegerType()
              && firstParameterValue.isExplicitlyKnown()
              && secondParameterValue.isExplicitlyKnown()) {
            // cast arguments to matching values
            if (!isFunctionWithArbitraryArgumentTypes(nameOfCalledFunc)) {
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
            BigInteger p1 = firstParameterValue.asNumericValue().bigIntegerValue();
            BigInteger p2 = secondParameterValue.asNumericValue().bigIntegerValue();

            BigInteger resultOfComputation;
            BinaryOperator operator = getOperator(nameOfCalledFunc);
            resultOfComputation =
                switch (operator) {
                  case PLUS -> p1.add(p2);
                  case MINUS -> p1.subtract(p2);
                  case MULTIPLY -> p1.multiply(p2);
                  default ->
                      throw new UnrecognizedCodeException(
                          "Can not determine operator of function " + nameOfCalledFunc, null, null);
                };

            // cast result type of third parameter
            Value resultValue = new NumericValue(resultOfComputation);
            resultValue =
                AbstractExpressionValueVisitor.castCValue(
                    resultValue,
                    resultType,
                    machineModel,
                    logger,
                    functionCallExpression.getFileLocation());

            if (resultValue.asNumericValue().bigIntegerValue().equals(resultOfComputation)) {
              return new NumericValue(0);
            } else {
              return new NumericValue(1);
            }
          }
        }
      }
    }

    return Value.UnknownValue.getInstance();
  }
}
