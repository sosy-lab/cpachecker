// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.cpa.smg2.SMGCPAValueVisitor.extractFloatFromNumericValueOrThrow;
import static org.sosy_lab.cpachecker.util.BuiltinFunctions.getParameterTypeOfBuiltinPopcountFunction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.RoundingMode;

/** Helper for math.h-related C functions and numerical builtins in SMG2. */
final class SMGMathBuiltins {

  private SMGMathBuiltins() {}

  /**
   * Evaluates arguments via {@link SMGCPAValueVisitor} starting from {@code pState}, threading the
   * newest state and returning values in argument order. Throws if an evaluation returns anything
   * other than one {@link ValueAndSMGState}, including multiple results from abstraction
   * materialization.
   */
  private static ListOfValuesAndSMGState readArgumentsSimplified(
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    ImmutableList.Builder<Value> parameterValuesBuilder = ImmutableList.builder();
    // Evaluate all parameters
    SMGState currentState = pState;
    for (CExpression currParamExp : pParameterExpressions) {
      // Here we expect only 1 result value
      SMGCPAValueVisitor vv = new SMGCPAValueVisitor(pEvaluator, currentState, pCfaEdge, pLogger);
      List<ValueAndSMGState> newValuesAndStates =
          vv.evaluate(currParamExp, SMGCPAExpressionEvaluator.getCanonicalType(currParamExp));
      checkArgument(newValuesAndStates.size() == 1);
      Value newValue = newValuesAndStates.getFirst().getValue();
      // CPA access has side effects! Always take the newest state!
      currentState = newValuesAndStates.getFirst().getState();
      parameterValuesBuilder.add(newValue);
    }
    return new ListOfValuesAndSMGState(parameterValuesBuilder.build(), currentState);
  }

  /**
   * {@code double fabs(double x);}
   *
   * <p>{@code float fabsf(float x);}
   *
   * <p>{@code long double fabsl(long double x);} Returns the absolute value of x.
   */
  static List<ValueAndSMGState> handleBuiltinFabsFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(ValueAndSMGState.of(new NumericValue(arg.abs()), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double __builtin_inf(void);} Returns positive infinity if supported by double.
   * Otherwise, returns DBL_MAX and generates a compiler warning.
   *
   * <p>{@code double __builtin_huge_val(void);} Returns positive infinity if supported by double.
   * Otherwise, returns DBL_MAX.
   */
  static List<ValueAndSMGState> handleBuiltinInfinityFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      MachineModel pMachineModel) {

    checkArgument(pParameterExpressions.isEmpty());
    Format precision =
        Format.fromCType(
            pMachineModel, BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName));
    return ImmutableList.of(
        ValueAndSMGState.of(new NumericValue(FloatValue.infinity(precision)), pState));
  }

  /**
   * {@code double nan(const char *tagp);}
   *
   * <p>{@code float nanf(const char *tagp);}
   *
   * <p>{@code long double nanl(const char *tagp);} Returns NaN, optionally using the character
   * string tagp to select the stored NaN representation. Returns zero if quiet NaNs are
   * unsupported. Undefined behavior: tagp is NULL or an invalid pointer. The string-argument
   * requirements were clarified by WG14 DR 477.
   *
   * <p>FIXME: Add support for NaN payloads
   */
  static List<ValueAndSMGState> handleBuiltinNanFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    checkArgument(parameterValues.size() < 2);
    Format precision =
        Format.fromCType(
            pMachineModel, BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName));
    return ImmutableList.of(
        ValueAndSMGState.of(new NumericValue(FloatValue.nan(precision)), currentState));
  }

  /**
   * {@code int isnan(x)} x may be float, double, or long double. Returns nonzero if x is NaN.
   * Undefined behavior: x is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIsnanFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg.isNan() ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int isinf(x)} x may be float, double, or long double. Returns nonzero if x is positive
   * or negative infinity. Undefined behavior: x is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIsinfFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg.isInfinite() ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int __builtin_isinf_sign(x)} x may be float, double, or long double. Returns -1 for
   * negative infinity, +1 for positive infinity, or 0 if x is not infinite. GCC extension; not an
   * ISO C11 function or macro.
   */
  static List<ValueAndSMGState> handleBuiltinIsinfSignFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(
              new NumericValue(arg.isInfinite() ? (arg.isNegative() ? -1 : 1) : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int isfinite(x)} x may be float, double, or long double. Returns nonzero if x is neither
   * infinity nor NaN. Undefined behavior: x is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIsfiniteFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(
              new NumericValue((arg.isInfinite() || arg.isNan()) ? 0 : 1), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double floor(double x);}
   *
   * <p>{@code float floorf(float x);}
   *
   * <p>{@code long double floorl(long double x);} Returns the largest integer-valued floating-point
   * number &lt;= x.
   */
  static List<ValueAndSMGState> handleBuiltinFloorFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg.round(RoundingMode.FLOOR)), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double ceil(double x);}
   *
   * <p>{@code float ceilf(float x);}
   *
   * <p>{@code long double ceill(long double x);} Returns the smallest integer-valued floating-point
   * number &gt;= x.
   */
  static List<ValueAndSMGState> handleBuiltinCeilFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg.round(RoundingMode.CEILING)), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double round(double x);}
   *
   * <p>{@code float roundf(float x);}
   *
   * <p>{@code long double roundl(long double x);} Rounds x to the nearest integer-valued
   * floating-point number. Exact halfway cases round away from zero. Ignores the current rounding
   * mode.
   */
  static List<ValueAndSMGState> handleBuiltinRoundFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(
              new NumericValue(arg.round(RoundingMode.NEAREST_AWAY)), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code long int lround(double x);}
   *
   * <p>{@code long int lroundf(float x);}
   *
   * <p>{@code long int lroundl(long double x);} Rounds x to the nearest integer, with halfway cases
   * away from zero. Returns long int. Unspecified numeric result if outside long int range. Domain
   * or range error may occur for an unrepresentable result. Annex F: an unrepresentable result
   * raises FE_INVALID.
   */
  static List<ValueAndSMGState> handleBuiltinLroundFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      FloatValue value = arg.round(RoundingMode.NEAREST_AWAY);
      Value result =
          switch (pMachineModel.getSizeofLongInt()) {
            case Integer.BYTES -> new NumericValue(value.integerValue());
            case Long.BYTES -> new NumericValue(value.longValue());
            default -> {
              pState.logUnknownValue(
                  "Returned unknown value due to undefined conversion of float value "
                      + value
                      + " in builtin function "
                      + pFunctionName
                      + " ",
                  pCfaEdge);
              yield UnknownValue.getInstance();
            }
          };
      return ImmutableList.of(ValueAndSMGState.of(result, currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code long long int llround(double x);}
   *
   * <p>{@code long long int llroundf(float x);}
   *
   * <p>{@code long long int llroundl(long double x);} Rounds x to the nearest integer, with halfway
   * cases away from zero. Returns long long int. Unspecified numeric result if outside long long
   * int range. Domain or range error may occur for an unrepresentable result. Annex F: an
   * unrepresentable result raises FE_INVALID.
   */
  static List<ValueAndSMGState> handleBuiltinLlroundFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      FloatValue value = arg.round(RoundingMode.NEAREST_AWAY);
      Value result =
          switch (pMachineModel.getSizeofLongLongInt()) {
            case Integer.BYTES -> new NumericValue(value.integerValue());
            case Long.BYTES -> new NumericValue(value.longValue());
            default -> {
              pState.logUnknownValue(
                  "Returned unknown value due to undefined conversion of float value "
                      + value
                      + " in builtin function "
                      + pFunctionName
                      + " ",
                  pCfaEdge);
              yield UnknownValue.getInstance();
            }
          };
      return ImmutableList.of(ValueAndSMGState.of(result, currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double trunc(double x);}
   *
   * <p>{@code float truncf(float x);}
   *
   * <p>{@code long double truncl(long double x);} Removes the fractional part of x, rounding toward
   * zero. Returns the result as a floating-point value.
   */
  static List<ValueAndSMGState> handleBuiltinTruncFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg.round(RoundingMode.TRUNCATE)), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double fdim(double x, double y);}
   *
   * <p>{@code float fdimf(float x, float y);}
   *
   * <p>{@code long double fdiml(long double x, long double y);} Returns x-y when x &gt; y,
   * otherwise positive zero. Range error may occur if the positive difference is too large.
   */
  static List<ValueAndSMGState> handleBuiltinFdimFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(
              new NumericValue(
                  arg1.lessOrEqual(arg2) ? FloatValue.zero(arg1.getFormat()) : arg1.subtract(arg2)),
              currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double fmax(double x, double y);}
   *
   * <p>{@code float fmaxf(float x, float y);}
   *
   * <p>{@code long double fmaxl(long double x, long double y);} Returns the greater of x and y. If
   * only one argument is NaN, returns the other argument. Annex F: returns NaN if both arguments
   * are NaN.
   */
  static List<ValueAndSMGState> handleBuiltinFmaxFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    // TODO: Add a warning message for fmax(0.0,-0.0) and fmax(-0.0, 0.0) The value is undefined
    //   and we simply pick 0.0 in those cases, but gcc will always return the first argument.

    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(
              new NumericValue(
                  switch (arg1.compareWithTotalOrder(arg2)) {
                    case -1 -> arg2.isNan() ? arg1 : arg2;
                    case +1 -> arg1.isNan() ? arg2 : arg1;
                    default -> arg1;
                  }),
              currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double fmin(double x, double y);}
   *
   * <p>{@code float fminf(float x, float y);}
   *
   * <p>{@code long double fminl(long double x, long double y);} Returns the smaller of x and y. If
   * only one argument is NaN, returns the other argument. Annex F: returns NaN if both arguments
   * are NaN.
   */
  static List<ValueAndSMGState> handleBuiltinFminFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {

    // FIXME: Add a warning message for fmin(0.0,-0.0) and fmin(-0.0, 0.0) The value is undefined
    //   and we pick -0.0 in those cases, but gcc will return the first argument for `float` or
    //   `double` and the second for `long double`
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(
              new NumericValue(
                  switch (arg1.compareWithTotalOrder(arg2)) {
                    case -1 -> arg1.isNan() ? arg2 : arg1;
                    case +1 -> arg2.isNan() ? arg1 : arg2;
                    default -> arg1;
                  }),
              currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int signbit(x)} x may be float, double, or long double. Returns nonzero if x has a
   * negative sign, including negative zero. Undefined behavior: x is not a real floating-point
   * expression.
   */
  static List<ValueAndSMGState> handleBuiltinSignbitFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg.isNegative() ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double copysign(double x, double y);}
   *
   * <p>{@code float copysignf(float x, float y);}
   *
   * <p>{@code long double copysignl(long double x, long double y);} Returns the magnitude of x with
   * the sign of y. Supports infinities, NaNs, and signed zeros where available.
   */
  static List<ValueAndSMGState> handleBuiltinCopysignFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg1.copySign(arg2)), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int fpclassify(x)} x may be float, double, or long double. Returns FP_NAN, FP_INFINITE,
   * FP_ZERO, FP_SUBNORMAL, FP_NORMAL, or an implementation-defined classification. Undefined
   * behavior: x is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinFpclassifyFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      int fpClass;
      if (arg.isNan()) {
        fpClass = 0;
      } else if (arg.isInfinite()) {
        fpClass = 1;
      } else if (arg.isZero()) {
        fpClass = 2;
      } else if (arg.abs().lessThan(FloatValue.minNormal(arg.getFormat()))) {
        // Subnormal numbers
        fpClass = 3;
      } else {
        fpClass = 4;
      }
      return ImmutableList.of(ValueAndSMGState.of(new NumericValue(fpClass), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double modf(double value, double *iptr);}
   *
   * <p>{@code float modff(float value, float *iptr);}
   *
   * <p>{@code long double modfl(long double value, long double *iptr);} Splits value into signed
   * integral and fractional parts. Stores the integral part in *iptr and returns the fractional
   * part. UB: iptr is NULL, invalid, or does not identify writable storage of the corresponding
   * floating-point type.
   *
   * <p>FIXME: Assign the integer part to the pointer from the second parameter. The current
   * solution is UNSOUND!!!!
   */
  static List<ValueAndSMGState> handleBuiltinModfFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value value = parameterValues.getFirst();

    pLogger.logOnce(
        Level.WARNING,
        "Ignored side-effect of function " + pFunctionName + ". This is potentially unsound.");

    if (value instanceof NumericValue numValue1) {
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numValue1);
      if (arg.isInfinite()) {
        // Return zero if the number is infinite
        return ImmutableList.of(
            ValueAndSMGState.of(
                new NumericValue(
                    arg.isNegative()
                        ? FloatValue.negativeZero(arg.getFormat())
                        : FloatValue.zero(arg.getFormat())),
                currentState));
      } else {
        // Otherwise, get the fractional part
        return ImmutableList.of(
            ValueAndSMGState.of(
                new NumericValue(arg.modulo(FloatValue.one(arg.getFormat()))), currentState));
      }
    }

    return ImmutableList.of(
        ValueAndSMGState.ofUnknownValue(
            currentState,
            "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
            pCfaEdge));
  }

  /**
   * {@code double remainder(double x, double y);}
   *
   * <p>{@code float remainderf(float x, float y);}
   *
   * <p>{@code long double remainderl(long double x, long double y);} Returns x - n*y, where n is
   * the integer nearest to x/y. Exact halfway quotient cases select the even integer.
   * Implementation-defined: when y == 0, either a domain error occurs or zero is returned.
   */
  static List<ValueAndSMGState> handleBuiltinRemainderFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg1.remainder(arg2)), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double fmod(double x, double y);}
   *
   * <p>{@code float fmodf(float x, float y);}
   *
   * <p>{@code long double fmodl(long double x, long double y);} Returns the remainder of x divided
   * by y, using a quotient rounded toward zero. Implementation-defined: when y == 0, either a
   * domain error occurs or zero is returned. Annex F: when x is infinite or y is zero, returns NaN
   * and raises FE_INVALID, unless either argument is NaN.
   */
  static List<ValueAndSMGState> handleBuiltinFmodFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg1.modulo(arg2)), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code double sqrt(double x);}
   *
   * <p>{@code float sqrtf(float x);}
   *
   * <p>{@code long double sqrtl(long double x);} Returns the nonnegative square root of x. Domain
   * error: x &lt; 0; negative zero is permitted.
   */
  static List<ValueAndSMGState> handleBuiltinSqrtFunctionFamily(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 1);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    final Value parameter = Iterables.getOnlyElement(parameterValues);
    if (parameter instanceof NumericValue numParameter) {
      // Cast the argument to match the function type
      FloatValue arg =
          extractFloatFromNumericValueOrThrow(
              pMachineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName),
              numParameter);
      return ImmutableList.of(ValueAndSMGState.of(new NumericValue(arg.sqrt()), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int isgreater(x, y)} x and y may be float, double, or long double independently. Usual
   * arithmetic conversions apply. Returns nonzero if x &gt; y, otherwise zero. Undefined behavior:
   * either argument is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIsgreaterFunction(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg1.greaterThan(arg2) ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int isgreaterequal(x, y)} x and y may be float, double, or long double independently.
   * Usual arithmetic conversions apply. Returns nonzero if x &gt;= y, otherwise zero. Undefined
   * behavior: either argument is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIsgreaterequalFunction(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg1.greaterOrEqual(arg2) ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int isless(x, y)} x and y may be float, double, or long double independently. Usual
   * arithmetic conversions apply. Returns nonzero if x &lt; y, otherwise zero. Undefined behavior:
   * either argument is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIslessFunction(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg1.lessThan(arg2) ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int islessequal(x, y)} x and y may be float, double, or long double independently. Usual
   * arithmetic conversions apply. Returns nonzero if x &lt;= y, otherwise zero. Undefined behavior:
   * either argument is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIslessequalFunction(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg1.lessOrEqual(arg2) ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int islessgreater(x, y)} x and y may be float, double, or long double independently.
   * Usual arithmetic conversions apply. Returns nonzero if x &lt; y or x &gt; y, otherwise zero.
   * Returns zero if the arguments are equal or either is NaN. Undefined behavior: either argument
   * is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIslessgreaterFunction(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(arg1.lessOrGreater(arg2) ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * {@code int isunordered(x, y)} x and y may be float, double, or long double independently. Usual
   * arithmetic conversions apply. Returns nonzero if x or y is NaN, otherwise zero. Undefined
   * behavior: either argument is not a real floating-point expression.
   */
  static List<ValueAndSMGState> handleBuiltinIsunorderedFunction(
      String pFunctionName,
      List<CExpression> pParameterExpressions,
      SMGState pState,
      CFAEdge pCfaEdge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger)
      throws CPATransferException {
    checkArgument(pParameterExpressions.size() == 2);
    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(pParameterExpressions, pState, pCfaEdge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();
    Value parameter1 = parameterValues.getFirst();
    Value parameter2 = parameterValues.get(1);
    if (parameter1 instanceof NumericValue numericValue1
        && parameter2 instanceof NumericValue numericValue2) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      FloatValue arg1 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue1);
      FloatValue arg2 =
          extractFloatFromNumericValueOrThrow(pMachineModel, targetType, numericValue2);
      return ImmutableList.of(
          ValueAndSMGState.of(
              new NumericValue((arg1.isNan() || arg2.isNan()) ? 1 : 0), currentState));
    } else {
      return ImmutableList.of(
          ValueAndSMGState.ofUnknownValue(
              currentState,
              "Returned unknown value due to unhandled builtin function " + pFunctionName + " in ",
              pCfaEdge));
    }
  }

  /**
   * Handle calls to:
   *
   * <p>{@code int __builtin_popcount(unsigned int x);}
   *
   * <p>{@code int __builtin_popcountl(unsigned long int x);}
   *
   * <p>{@code int __builtin_popcountll(unsigned long long int x);}
   *
   * <p>Popcount sums up all 1-bits in an unsigned int, unsigned long int or unsigned long long int
   * number given.
   */
  static List<ValueAndSMGState> handlePopcount(
      String pFunctionName,
      SMGState pState,
      CFunctionCallExpression functionCall,
      CFAEdge edge,
      SMGCPAExpressionEvaluator pEvaluator,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      SMGOptions pOptions)
      throws CPATransferException {

    if (functionCall.getParameterExpressions().size() != 1) {
      throw new UnrecognizedCodeException(
          "Function "
              + pFunctionName
              + " received "
              + functionCall.getParameterExpressions().size()
              + " parameters"
              + " instead of the expected "
              + 1,
          functionCall);
    }

    CSimpleType paramType = getParameterTypeOfBuiltinPopcountFunction(pFunctionName);
    assert functionCall.getExpressionType() instanceof CSimpleType simpleType
        && simpleType.getCanonicalType().getType().isIntegerType()
        && simpleType.getCanonicalType().hasSignedSpecifier();
    assert paramType.hasUnsignedSpecifier();

    ListOfValuesAndSMGState arguments =
        readArgumentsSimplified(
            functionCall.getParameterExpressions(), pState, edge, pEvaluator, pLogger);
    SMGState currentState = arguments.state();
    List<Value> parameterValues = arguments.values();

    // Cast to unsigned target type
    Value castParamValue =
        new SMGCPAValueVisitor(pEvaluator, currentState, edge, pLogger)
            .castCValue(Iterables.getOnlyElement(parameterValues), paramType.getCanonicalType());

    if (castParamValue instanceof NumericValue numCastParamValue) {
      BigInteger numericParam = numCastParamValue.bigIntegerValue();

      // Check that the cast function parameter is really unsigned, as defined by the function and
      // needed by Java BigInteger.bitcount() to be correct, as negative values give distinct
      // results
      verify(
          numericParam.signum() >= 0,
          "Evaluated parameter for C function %s is negative, but the function defines unsigned"
              + " parameters only",
          pFunctionName);

      return ImmutableList.of(
          ValueAndSMGState.of(new NumericValue(numericParam.bitCount()), currentState));

    } else if (pOptions.trackPredicates() && !castParamValue.isUnknown()) {

      checkArgument(castParamValue instanceof SymbolicExpression);

      SymbolicExpression symbolicParam = (SymbolicExpression) castParamValue;
      int parameterBitSize = pMachineModel.getSizeofInBits(paramType);

      SymbolicExpression one =
          ConstantSymbolicExpression.of(new NumericValue(1), CNumericTypes.INT);
      SymbolicExpression constraint =
          BinaryAndExpression.of(symbolicParam, one, CNumericTypes.INT, paramType);

      // Add up the bits one by one
      // (castParamValue >> 0) & 1 + (castParamValue >> 1) & 1 + ...
      for (int i = 1; i < parameterBitSize; i++) {
        SymbolicExpression countOfBitAtIndex =
            BinaryAndExpression.of(
                ShiftRightExpression.ofUnsigned(
                    symbolicParam,
                    ConstantSymbolicExpression.of(new NumericValue(i), CNumericTypes.INT),
                    paramType,
                    paramType),
                one,
                CNumericTypes.INT,
                paramType);
        constraint =
            AdditionExpression.of(
                constraint, countOfBitAtIndex, CNumericTypes.INT, CNumericTypes.INT);
      }

      return ImmutableList.of(ValueAndSMGState.of(constraint, currentState));
    } else {

      // TODO: we could associate a symbolic output with the input used to assure ==
      currentState.logUnknownValue(
          "Assumed unknown return value for function call to builtin popcountX() function in ",
          edge);
      return ImmutableList.of(ValueAndSMGState.of(UnknownValue.getInstance(), currentState));
    }
  }

  private record ListOfValuesAndSMGState(List<Value> values, SMGState state) {

    private ListOfValuesAndSMGState {
      values = ImmutableList.copyOf(values);
      checkArgument(!values.isEmpty());
      checkNotNull(state);
    }
  }
}
