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
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BuiltinOverflowFunctions {

  // TODO: add missing functions, their handling and doc: __builtin_addc, __builtin_addcl,
  //  __builtin_addcll, __builtin_subc, __builtin_subcl, __builtin_subcll.
  //  The addition functions add all 3 unsigned values and set the target of the fourth (pointer)
  //  argument to 1 if any of the two additions overflowed, otherwise 0. They return the calculated
  //  sum. The sub functions work in the same way, subtracting the second and third argument from
  //  the first etc.
  /**
   * In all overflow functions, the types of the first 2 arguments are promoted to infinite
   * precision, then the binary operation is performed on them. The result is then cast to the type
   * of the third argument and stored in the third argument for all functions except the *_p
   * functions (signaled by having NO side effects). If the cast result is equal to the result in
   * infinite precision, the functions return false, else true.
   */
  private enum BuiltinOverflowFunction {
    ADD(BinaryOperator.PLUS, null, false),
    SADD(BinaryOperator.PLUS, CNumericTypes.SIGNED_INT, false),
    SADDL(BinaryOperator.PLUS, CNumericTypes.SIGNED_LONG_INT, false),
    SADDLL(BinaryOperator.PLUS, CNumericTypes.SIGNED_LONG_LONG_INT, false),
    UADD(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_INT, false),
    UADDL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_INT, false),
    UADDLL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, false),

    SUB(BinaryOperator.MINUS, null, false),
    SSUB(BinaryOperator.MINUS, CNumericTypes.SIGNED_INT, false),
    SSUBL(BinaryOperator.MINUS, CNumericTypes.SIGNED_LONG_INT, false),
    SSUBLL(BinaryOperator.MINUS, CNumericTypes.SIGNED_LONG_LONG_INT, false),
    USUB(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_INT, false),
    USUBL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_INT, false),
    USUBLL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, false),

    MUL(BinaryOperator.MULTIPLY, null, false),
    SMUL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_INT, false),
    SMULL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_LONG_INT, false),
    SMULLL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_LONG_LONG_INT, false),
    UMUL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_INT, false),
    UMULL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_LONG_INT, false),
    UMULLL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_LONG_LONG_INT, false),

    ADD_P(BinaryOperator.PLUS, null, true),
    SUB_P(BinaryOperator.MINUS, null, true),
    MUL_P(BinaryOperator.MULTIPLY, null, true);

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
   * Resolve the expected parameter type (for all numeric parameters) of the built-in overflow
   * function by function name. This is important since the input parameters have to be cast in case
   * their input values differ from the used type.
   */
  public static Optional<CSimpleType> getParameterType(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    return functions.get(pFunctionName).type;
  }

  public static BinaryOperator getOperator(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    return functions.get(pFunctionName).operator;
  }

  // TODO: either add the missing functions or add some way of checking for them with this class!
  // ALL CPAs assume that this class handles ALL builtin overflow functions, which it does not!
  /**
   * Check whether a given function is a builtin function specific to overflows that can be further
   * analyzed with this class.
   */
  public static boolean isBuiltinOverflowFunction(String pFunctionName) {
    return functions.containsKey(pFunctionName);
  }

  /**
   * Returns true for functions whose arguments must not be cast before being promoted to infinite
   * precision.
   */
  public static boolean isFunctionWithArbitraryArgumentTypes(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    return !functions.get(pFunctionName).type.isPresent();
  }

  // TODO: this is false! Even the functions that do not store the result of the arithmetic
  // calculation into memory pointed to by a pointer evaluate the side-effects of their inputs
  // (without integral argument promotion on that argument)! Correct would be something like "does
  // not return arithmetic value".
  public static boolean isFunctionWithoutSideEffect(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    return functions.get(pFunctionName).hasNoSideEffects;
  }

  // TODO: remove this method. Either we don't know the types, or all are equal, with the last being
  // a pointer type towards this equal type.
  public static List<CType> getParameterTypes(String pFunctionName) {
    checkState(functions.containsKey(pFunctionName));
    Optional<CSimpleType> type = functions.get(pFunctionName).type;

    if (type.isPresent()) {
      return ImmutableList.of(
          type.orElseThrow(),
          type.orElseThrow(),
          new CPointerType(CTypeQualifiers.NONE, type.orElseThrow()));
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
  public static CExpression getOverflowFunctionResult(
      OverflowAssumptionManager overflowMgr,
      CExpression var1,
      CExpression var2,
      CExpression var3,
      String functionName)
      throws UnrecognizedCodeException {
    checkState(functions.containsKey(functionName));
    CSimpleType targetType = getTargetType(functionName, var3);
    BinaryOperator operator = getOperator(functionName);
    CExpression castVar1 = var1;
    CExpression castVar2 = var2;
    if (!isFunctionWithArbitraryArgumentTypes(functionName)) {
      castVar1 = new CCastExpression(FileLocation.DUMMY, targetType, var1);
      castVar2 = new CCastExpression(FileLocation.DUMMY, targetType, var2);
    }

    CExpression result;
    if (operator == BinaryOperator.MULTIPLY) {
      result =
          overflowMgr.getConjunctionOfMultiplicationAssumptions(
              castVar1, castVar2, targetType, true);
    } else {
      result =
          overflowMgr.getConjunctionOfAdditiveAssumptions(
              castVar1, castVar2, operator, targetType, true);
    }

    return new CCastExpression(FileLocation.DUMMY, CNumericTypes.BOOL, result);
  }

  /**
   * Returns the {@link CExpression} handling the side effect for all functions with side effects.
   * E.g. in case the numeric operations result is returned via a pointer, the
   */
  // TODO: this is returning the result of the numeric calculation, but should return the
  //  assignment of this value to the dereferenced pointer!
  public static CExpression handleOverflowSideEffects(
      OverflowAssumptionManager overflowMgr,
      CExpression var1,
      CExpression var2,
      CExpression var3,
      String functionName)
      throws UnrecognizedCodeException {
    checkState(functions.containsKey(functionName));
    CSimpleType targetType = getTargetType(functionName, var3);
    BinaryOperator operator = getOperator(functionName);
    CExpression castVar1 = new CCastExpression(FileLocation.DUMMY, targetType, var1);
    CExpression castVar2 = new CCastExpression(FileLocation.DUMMY, targetType, var2);
    // TODO: This is WRONG! This does not promote to infinite precision at all!
    return overflowMgr.getResultOfOperation(castVar1, castVar2, operator);
  }

  /**
   * Returns the {@link CSimpleType} in which the numerically calculated value is to be cast to and
   * returned (either as part of the side effect of these functions in case of function with
   * "overflow" in their names, or as actual function return in case of functions without "overflow"
   * in their names).
   */
  private static CSimpleType getTargetType(String pFunctionName, CExpression thirdArgument) {
    if (!isFunctionWithArbitraryArgumentTypes(pFunctionName)) {
      return getParameterType(pFunctionName).orElseThrow();
    }

    CType targetType = thirdArgument.getExpressionType().getCanonicalType();
    if (targetType instanceof CPointerType) {
      targetType = ((CPointerType) targetType).getType();
    }
    return (CSimpleType) targetType;
  }

  // TODO: add method that does all the handling based on CExpression as the method below for Value
  //  Analysis, bundling handleOverflowSideEffects() and getOverflowFunctionResult() based on
  //  generic CExpressions that can be handled by any CPA (that can handle C) and replace the method
  //  below.

  /**
   * Evaluates the result value of the numeric operation (addition/subtraction/multiplication) of
   * the builtin overflow function. The arguments are cast to the functions parameters types (if
   * necessary), and the result of the numeric operation is computed with infinite precision. The
   * overflow is determined by casting to the type of the third parameter.
   */
  // TODO: this is missing symbolic handling.
  // TODO: this UNSOUNDLY ignores side-effects!
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
              evv.evaluate(parameters.getFirst(), parameters.getFirst().getExpressionType());
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
                      firstParameterValue, resultType, machineModel, logger);
              secondParameterValue =
                  AbstractExpressionValueVisitor.castCValue(
                      secondParameterValue, resultType, machineModel, logger);
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
                    resultValue, resultType, machineModel, logger);

            // TODO: this ignores all side-effects!
            if (resultValue.asNumericValue().bigIntegerValue().equals(resultOfComputation)) {
              return new NumericValue(0);
            } else {
              return new NumericValue(1);
            }
          }
        }
      }
    }

    // TODO: this ignores all side-effects!
    return Value.UnknownValue.getInstance();
  }
}
