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

  /**
   * All overflow functions including "overflow" in their name promote the types of the first 2
   * arguments to infinite precision, then the binary operation is performed on them. The result is
   * then cast to the type of the third argument and stored in the third argument for all functions
   * except the *_p functions (signaled by having NO return of arithmetic result). If the cast
   * result is equal to the result in infinite precision, the functions return false, else true. For
   * functions without arbitrary parameter types, the input needs to be cast to the type of the
   * parameters first.
   *
   * <p>The overflow functions without "overflow" in their name add/subtract the first 3 arguments
   * with fixed types, then return the arithmetic result, storing 0 in the fourth arguments target
   * if there was no overflow in none of the arithmetic operations, 1 else.
   */
  private enum BuiltinOverflowFunction {
    /*
     * Overflow functions that return the boolean result whether the arithmetic operation
     * overflowed, as well as the arithmetic result. The arithmetic result is returned as a side
     * effect in the target of the pointer of the last argument.
     */
    ADD(BinaryOperator.PLUS, null, true, false),
    SADD(BinaryOperator.PLUS, CNumericTypes.SIGNED_INT, true, false),
    SADDL(BinaryOperator.PLUS, CNumericTypes.SIGNED_LONG_INT, true, false),
    SADDLL(BinaryOperator.PLUS, CNumericTypes.SIGNED_LONG_LONG_INT, true, false),
    UADD(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_INT, true, false),
    UADDL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_INT, true, false),
    UADDLL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, true, false),

    SUB(BinaryOperator.MINUS, null, true, false),
    SSUB(BinaryOperator.MINUS, CNumericTypes.SIGNED_INT, true, false),
    SSUBL(BinaryOperator.MINUS, CNumericTypes.SIGNED_LONG_INT, true, false),
    SSUBLL(BinaryOperator.MINUS, CNumericTypes.SIGNED_LONG_LONG_INT, true, false),
    USUB(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_INT, true, false),
    USUBL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_INT, true, false),
    USUBLL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, true, false),

    MUL(BinaryOperator.MULTIPLY, null, true, false),
    SMUL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_INT, true, false),
    SMULL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_LONG_INT, true, false),
    SMULLL(BinaryOperator.MULTIPLY, CNumericTypes.SIGNED_LONG_LONG_INT, true, false),
    UMUL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_INT, true, false),
    UMULL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_LONG_INT, true, false),
    UMULLL(BinaryOperator.MULTIPLY, CNumericTypes.UNSIGNED_LONG_LONG_INT, true, false),

    /*
     * Overflow functions that return only the boolean result whether the arithmetic operation
     * overflowed, but do not return the result of the arithmetic operation (but evaluate the side
     * effects of the argument used to retrieve the target type to cast to evaluate whether
     * it overflowed).
     */
    ADD_P(BinaryOperator.PLUS, null, false, false),
    SUB_P(BinaryOperator.MINUS, null, false, false),
    MUL_P(BinaryOperator.MULTIPLY, null, false, false),

    /*
     * "Carry out" overflow functions, returning the arithmetic result as function return,
     * storing the result whether the arithmetic operation overflowed in the "carry out"
     * (last (fourth) argument).
     * The addition functions add the first 3 unsigned arguments and set the target of the fourth
     * argument (pointer) to 1 if any of the two additions overflowed, otherwise 0.
     * These functions calculate in the given types and determine overflow in them,
     * returning the arithmetic operations result.
     * The sub functions work in the same way, subtracting the second and third argument from
     * the first, behaving equally to add above in the rest.
     */
    ADDC(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_INT, false, true),
    ADDCL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_INT, false, true),
    ADDCLL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, false, true),

    SUBC(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_INT, false, true),
    SUBCL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_INT, false, true),
    SUBCLL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, false, true);

    private final BinaryOperator operator;
    private final Optional<CSimpleType> type;

    // Function returns the result of the arithmetic calculation as side effect
    private final Boolean sideEffectArithmeticReturn;

    // Function returns the result of the arithmetic calculation as function return
    private final Boolean directArithmeticReturn;
    private final String name;

    BuiltinOverflowFunction(
        BinaryOperator pOperator,
        @Nullable CSimpleType pType,
        boolean pSideEffectArithmeticReturn,
        boolean pDirectArithmeticReturn) {
      operator = pOperator;
      type = Optional.ofNullable(pType);
      sideEffectArithmeticReturn = pSideEffectArithmeticReturn;
      directArithmeticReturn = pDirectArithmeticReturn;

      String baseName = "__builtin_" + getDataTypePrefix(pType) + getOperatorName(pOperator);
      if (pDirectArithmeticReturn) {
        name = baseName + "c" + getDataTypeSuffix(pType);
      } else {
        name =
            baseName
                + getDataTypeSuffix(pType)
                + "_overflow"
                + (pSideEffectArithmeticReturn ? "" : "_p");
      }
    }

    private static String getOperatorName(BinaryOperator pOperator) {
      if (pOperator == BinaryOperator.PLUS) {
        return "add";
      } else if (pOperator == BinaryOperator.MINUS) {
        return "sub";
      } else {
        checkState(pOperator == BinaryOperator.MULTIPLY);
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
      if (pType != null) {
        if (pType.hasLongSpecifier()) {
          return "l";
        } else if (pType.hasLongLongSpecifier()) {
          return "ll";
        }
      }

      return "";
    }
  }

  private static final Map<String, BuiltinOverflowFunction> allFunctions;

  static {
    allFunctions = from(BuiltinOverflowFunction.values()).uniqueIndex(func -> func.name);
  }

  /**
   * Resolve the expected parameter type (for all numeric parameters) of the built-in overflow
   * function by function name. This is important since the input parameters have to be cast in case
   * their input values differ from the used type.
   */
  public static Optional<CSimpleType> getParameterType(String pFunctionName) {
    checkState(allFunctions.containsKey(pFunctionName));
    return allFunctions.get(pFunctionName).type;
  }

  public static BinaryOperator getOperator(String pFunctionName) {
    checkState(allFunctions.containsKey(pFunctionName));
    return allFunctions.get(pFunctionName).operator;
  }

  /**
   * Check whether a given function is a builtin function specific to overflows based on GNU (GCC)
   * extensions according to <a
   * href="https://gcc.gnu.org/onlinedocs/gcc/Integer-Overflow-Builtins.html">...</a>.
   */
  public static boolean isBuiltinOverflowFunction(String pFunctionName) {
    return allFunctions.containsKey(pFunctionName);
  }

  /**
   * Returns true for functions whose arguments must not be cast before being promoted to infinite
   * precision.
   */
  public static boolean isFunctionWithArbitraryArgumentTypes(String pFunctionName) {
    checkState(allFunctions.containsKey(pFunctionName));
    return !allFunctions.get(pFunctionName).type.isPresent();
  }

  /**
   * Returns true if the function given returns a boolean signaling whether the arithmetic operation
   * performed in the function overflowed or not.
   */
  private static boolean functionReturnsBooleanOverflowCheck(String functionName) {
    return !allFunctions.get(functionName).directArithmeticReturn;
  }

  /**
   * Returns true if the function given stores the arithmetic result of the
   * addition/subtraction/multiplication performed in the function into the target of a pointer
   * given as parameter (i.e. as side effect).
   */
  private static boolean functionStoresArithmeticResultUsingSideEffect(String functionName) {
    return allFunctions.get(functionName).sideEffectArithmeticReturn;
  }

  /**
   * Returns true if the function given returns the arithmetic result of the
   * addition/subtraction/multiplication performed in the function as function return.
   */
  // TODO: useless due to functionReturnsBooleanOverflowCheck?
  private static boolean functionReturnsArithmeticResult(String functionName) {
    return allFunctions.get(functionName).directArithmeticReturn;
  }

  // TODO: remove this method. Either we don't know the types, or all are equal, with the last being
  // a pointer type towards this equal type.
  public static List<CType> getParameterTypes(String pFunctionName) {
    checkState(allFunctions.containsKey(pFunctionName));
    Optional<CSimpleType> type = allFunctions.get(pFunctionName).type;

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
    checkState(isBuiltinOverflowFunction(functionName));
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
    checkState(isBuiltinOverflowFunction(functionName));
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
