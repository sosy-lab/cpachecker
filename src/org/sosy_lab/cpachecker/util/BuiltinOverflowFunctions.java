// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cfa.types.c.CBasicType.INT128;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

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
    SADD(BinaryOperator.PLUS, CNumericTypes.INT, true, false),
    SADDL(BinaryOperator.PLUS, CNumericTypes.LONG_INT, true, false),
    SADDLL(BinaryOperator.PLUS, CNumericTypes.LONG_LONG_INT, true, false),
    UADD(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_INT, true, false),
    UADDL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_INT, true, false),
    UADDLL(BinaryOperator.PLUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, true, false),

    SUB(BinaryOperator.MINUS, null, true, false),
    SSUB(BinaryOperator.MINUS, CNumericTypes.INT, true, false),
    SSUBL(BinaryOperator.MINUS, CNumericTypes.LONG_INT, true, false),
    SSUBLL(BinaryOperator.MINUS, CNumericTypes.LONG_LONG_INT, true, false),
    USUB(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_INT, true, false),
    USUBL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_INT, true, false),
    USUBLL(BinaryOperator.MINUS, CNumericTypes.UNSIGNED_LONG_LONG_INT, true, false),

    MUL(BinaryOperator.MULTIPLY, null, true, false),
    SMUL(BinaryOperator.MULTIPLY, CNumericTypes.INT, true, false),
    SMULL(BinaryOperator.MULTIPLY, CNumericTypes.LONG_INT, true, false),
    SMULLL(BinaryOperator.MULTIPLY, CNumericTypes.LONG_LONG_INT, true, false),
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

      String baseName = "__builtin_";
      if (pDirectArithmeticReturn) {
        name = baseName + getOperatorName(pOperator) + "c" + getDataTypeSuffix(pType);
      } else {
        name =
            baseName
                + getDataTypePrefix(pType)
                + getOperatorName(pOperator)
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

      if (pType.hasSignedSpecifier() || !pType.hasUnsignedSpecifier()) {
        // This is technically not good, but works in this limited case
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

  @VisibleForTesting
  static Map<String, BuiltinOverflowFunction> getAllFunctions() {
    return allFunctions;
  }

  /**
   * Resolve the expected parameter type (for all numeric parameters) of the built-in overflow
   * function by function name. This is important since the input parameters have to be cast in case
   * their input values differ from the used type.
   */
  private static Optional<CSimpleType> getParameterType(String pFunctionName) {
    checkState(allFunctions.containsKey(pFunctionName));
    return allFunctions.get(pFunctionName).type;
  }

  public static CSimpleType getReturnType(String pFunctionName) {
    checkState(allFunctions.containsKey(pFunctionName));
    BuiltinOverflowFunction fun = allFunctions.get(pFunctionName);
    if (fun.directArithmeticReturn) {
      return fun.type.orElseThrow();
    } else {
      return CNumericTypes.BOOL;
    }
  }

  private static BinaryOperator getOperator(String pFunctionName) {
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
  private static boolean isFunctionWithArbitraryArgumentTypes(String pFunctionName) {
    checkState(allFunctions.containsKey(pFunctionName));
    return !allFunctions.get(pFunctionName).type.isPresent();
  }

  /**
   * Returns true if the function given returns a boolean signaling whether the arithmetic operation
   * performed in the function overflowed or not.
   */
  public static boolean functionReturnsBooleanOverflowCheck(String functionName) {
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
   * Returns true if the function checked is an overflow function that does not return a value using
   * a side effect. False else.
   */
  public static boolean functionReturnsNoValuesUsingSideEffects(String functionName) {
    return !(functionStoresArithmeticResultUsingSideEffect(functionName)
        || allFunctions.get(functionName).directArithmeticReturn);
  }

  /**
   * Returns true if the function given returns the arithmetic result of the
   * addition/subtraction/multiplication performed in the function as function return.
   */
  private static boolean functionReturnsArithmeticResult(String functionName) {
    return !functionReturnsBooleanOverflowCheck(functionName);
  }

  public static List<CType> getParameterTypes(String pFunctionName) {
    checkState(allFunctions.containsKey(pFunctionName));
    Optional<CSimpleType> maybeType = allFunctions.get(pFunctionName).type;

    if (maybeType.isPresent()) {
      ImmutableList.Builder<CType> types = ImmutableList.builder();
      CType type = maybeType.orElseThrow();
      types.add(type);
      types.add(type);
      if (functionReturnsArithmeticResult(pFunctionName)) {
        types.add(type);
      }
      types.add(new CPointerType(CTypeQualifiers.NONE, type));
      return types.build();
    } else {
      // TODO: is this correct? We know that for unknown types there have to be 3 parameters, with
      // unknown (but explicit integer derived) types, that may be distinct in between the params.
      return ImmutableList.of();
    }
  }

  /**
   * Returns the {@link CSimpleType} in which the numerically calculated value is to be cast to and
   * returned (either as part of the side effect of these functions in case of function with
   * "overflow" in their names, or as actual function return in case of functions without "overflow"
   * in their names).
   */
  private static CSimpleType getTargetType(
      String pFunctionName,
      CExpression pArgumentToRetrieveTypeFrom,
      boolean abortForSideEffects,
      CFAEdge edge)
      throws UnsupportedCodeException {
    if (!isFunctionWithArbitraryArgumentTypes(pFunctionName)) {
      return getParameterType(pFunctionName).orElseThrow();
    }

    CExpression unpackedArg = pArgumentToRetrieveTypeFrom;
    if (abortForSideEffects) {
      // TODO: we did not evaluate the side effect of the third argument of functions not returning
      // the arithmetic result yet, but we need to according to the definition. We disallow
      // non-variable/literal input in the third arg for now.
      while (unpackedArg instanceof CCastExpression castArg) {
        unpackedArg = castArg.getOperand();
      }
      // This is by no means complete! Just the most common cases where we can be sure that there
      // are not side effects!
      if (unpackedArg instanceof CUnaryExpression unaryExpr) {
        unpackedArg = unaryExpr.getOperand();
      }
      if (!(unpackedArg instanceof CIdExpression || unpackedArg instanceof CLiteralExpression)) {
        throw new UnsupportedCodeException(
            "Builtin overflow function "
                + pFunctionName
                + " can not evaluate argument "
                + pArgumentToRetrieveTypeFrom,
            edge);
      }
    }

    // CType targetType = pArgumentToRetrieveTypeFrom.getExpressionType();
    CType targetType = pArgumentToRetrieveTypeFrom.getExpressionType().getCanonicalType();
    if (targetType instanceof CPointerType) {
      targetType = ((CPointerType) targetType).getType();
    }
    return (CSimpleType) targetType;
  }

  /**
   * Returns the result of the called builtin overflow function, as well as possible side effects as
   * assignments as {@link BuiltinOverflowFunctionStatementsToApply}. GNU (GCC) builtin overflow
   * functions are documented <a
   * href="https://gcc.gnu.org/onlinedocs/gcc/Integer-Overflow-Builtins.html">here</a>. You can
   * check whether a function is one of these functions with {@link
   * #isBuiltinOverflowFunction(String)}.
   */
  public static BuiltinOverflowFunctionStatementsToApply handleBuiltinOverflowFunction(
      final CFunctionCallExpression funCallExpr,
      final String builtinOverflowFunctionName,
      boolean useOverflowManager,
      final CFAEdge edge,
      MachineModel pMachineModel,
      LogManager pLogger)
      throws UnrecognizedCodeException {
    checkArgument(BuiltinOverflowFunctions.isBuiltinOverflowFunction(builtinOverflowFunctionName));

    if (functionReturnsArithmeticResult(builtinOverflowFunctionName)) {
      // __builtin_addc{l|ll}, __builtin_subc{l|ll}
      verify(funCallExpr.getParameterExpressions().size() == 4);
      return handleOverflowFunctionReturningArithmeticResult(
          useOverflowManager,
          funCallExpr,
          builtinOverflowFunctionName,
          edge,
          pMachineModel,
          pLogger);
    } else if (functionStoresArithmeticResultUsingSideEffect(builtinOverflowFunctionName)) {
      // __builtin_{s|u}add{l|ll}_overflow,
      // __builtin_{s|u}sub{l|ll}_overflow,
      // __builtin_{s|u}mul{l|ll}_overflow
      verify(funCallExpr.getParameterExpressions().size() == 3);
      return handleOverflowFunctionStoringArithmeticResultAsSideEffect(
          useOverflowManager,
          funCallExpr,
          builtinOverflowFunctionName,
          edge,
          pMachineModel,
          pLogger);
    } else {
      // __builtin_add_overflow_p, __builtin_sub_overflow_p, __builtin_mul_overflow_p
      verify(funCallExpr.getParameterExpressions().size() == 3);
      return handleOverflowFunctionReturningOnlyBooleanResult(
          useOverflowManager,
          funCallExpr,
          builtinOverflowFunctionName,
          edge,
          pMachineModel,
          pLogger);
    }
  }

  private static BuiltinOverflowFunctionStatementsToApply
      handleOverflowFunctionReturningOnlyBooleanResult(
          boolean useOverflowManager,
          CFunctionCallExpression pFunCall,
          String pFunctionName,
          CFAEdge edge,
          MachineModel pMachineModel,
          LogManager pLogger)
          throws UnrecognizedCodeException {
    // Calculate arithmetic result and return an expression that checks whether the cast value is
    // equal to the uncast

    CExpression arg0 = getArgumentCastToParameterType(0, pFunCall, pFunctionName);
    CExpression arg1 = getArgumentCastToParameterType(1, pFunCall, pFunctionName);
    CExpression targetArgument = pFunCall.getParameterExpressions().get(2);
    CExpression booleanFunctionResult;
    if (useOverflowManager) {
      booleanFunctionResult =
          checkOverflowUsingOverflowManager(
              new OverflowAssumptionManager(pMachineModel, pLogger),
              arg0,
              arg1,
              targetArgument,
              pFunctionName,
              false,
              edge,
              pMachineModel,
              pLogger);

    } else {
      CExpression uncastArithmeticResult =
          getUncastArithmeticResultWithCastArguments(
              arg0, Optional.empty(), arg1, pFunctionName, edge, pMachineModel, pLogger);

      CExpression castArithmeticResult =
          castExpression(
              uncastArithmeticResult, getTargetType(pFunctionName, targetArgument, true, edge));

      booleanFunctionResult =
          getOverflowExpression(
              uncastArithmeticResult, castArithmeticResult, pFunctionName, pMachineModel, pLogger);
    }
    // TODO: we did not evaluate the side effect of the third argument yet, and will not for now,
    //  but we need to according to the definition. We disallow non-variable/literal input in the
    //  third arg for now.
    return BuiltinOverflowFunctionStatementsToApply.of(booleanFunctionResult);
  }

  /**
   * Transforms the {@link CExpression} that is an overflow functions return into a {@link
   * CStatement} (either a {@link CExpressionAssignmentStatement} or a {@link CExpressionStatement})
   * depending on the original function call. The result can be handled as a regular statement edge.
   */
  public static CStatement getResultStatementFromExpression(
      CFunctionCall pFunCall, CExpression functionResult) {
    CStatement resultStatement;
    if (pFunCall instanceof CFunctionCallAssignmentStatement originalAssignment) {
      CLeftHandSide lhs = originalAssignment.getLeftHandSide();
      resultStatement =
          new CExpressionAssignmentStatement(pFunCall.getFileLocation(), lhs, functionResult);
    } else {
      assert pFunCall instanceof CFunctionCallStatement;
      resultStatement = new CExpressionStatement(pFunCall.getFileLocation(), functionResult);
    }
    return resultStatement;
  }

  /**
   * Takes the original function-call, as well as the expression that is to be assigned as side
   * effect, and will build an assignment towards the variable used in the original input.
   */
  private static CExpressionAssignmentStatement getSideEffectAssignment(
      CFunctionCallExpression pFunCall,
      CExpression sideEffectResultToAssign,
      int indexForSideEffectTarget) {
    checkArgument(indexForSideEffectTarget == 2 || indexForSideEffectTarget == 3);
    checkArgument(
        indexForSideEffectTarget != 2 || sideEffectResultToAssign instanceof CCastExpression);
    checkArgument(
        indexForSideEffectTarget != 3
            || (sideEffectResultToAssign instanceof CCastExpression castExpr
                && castExpr.getOperand() instanceof CBinaryExpression binExprFromCast
                && binExprFromCast.getOperator() == BinaryOperator.NOT_EQUALS));

    CExpression sideEffectTargetArgument =
        pFunCall.getParameterExpressions().get(indexForSideEffectTarget);
    checkArgument(
        sideEffectTargetArgument.getExpressionType().getCanonicalType() instanceof CPointerType);
    // TODO: canonical type or not?
    CPointerType lhsType = (CPointerType) sideEffectTargetArgument.getExpressionType();

    CPointerExpression lhs =
        new CPointerExpression(pFunCall.getFileLocation(), lhsType, sideEffectTargetArgument);

    return new CExpressionAssignmentStatement(
        pFunCall.getFileLocation(), lhs, sideEffectResultToAssign);
  }

  private static BuiltinOverflowFunctionStatementsToApply
      handleOverflowFunctionStoringArithmeticResultAsSideEffect(
          boolean useOverflowManager,
          CFunctionCallExpression pFunCall,
          String pFunctionName,
          final CFAEdge edge,
          MachineModel pMachineModel,
          LogManager pLogger)
          throws UnrecognizedCodeException {
    // Calculate arithmetic result and return an expression that checks whether the cast value is
    // equal to the uncast

    CExpression booleanFunctionResult;
    CExpression castArithmeticResult;
    CExpression arg0 = getArgumentCastToParameterType(0, pFunCall, pFunctionName);
    CExpression arg1 = getArgumentCastToParameterType(1, pFunCall, pFunctionName);
    CExpression targetArgument = pFunCall.getParameterExpressions().get(2);

    if (useOverflowManager) {
      booleanFunctionResult =
          checkOverflowUsingOverflowManager(
              new OverflowAssumptionManager(pMachineModel, pLogger),
              arg0,
              arg1,
              targetArgument,
              pFunctionName,
              false,
              edge,
              pMachineModel,
              pLogger);

      CBinaryExpression sideEffectValueToAssign =
          new CBinaryExpressionBuilder(pMachineModel, pLogger)
              .buildBinaryExpression(arg0, arg1, getOperator(pFunctionName));
      castArithmeticResult =
          castExpression(
              sideEffectValueToAssign, getTargetType(pFunctionName, targetArgument, false, edge));

    } else {
      CExpression uncastArithmeticResult =
          getUncastArithmeticResultWithCastArguments(
              arg0, Optional.empty(), arg1, pFunctionName, edge, pMachineModel, pLogger);
      castArithmeticResult =
          castExpression(
              uncastArithmeticResult, getTargetType(pFunctionName, targetArgument, false, edge));

      booleanFunctionResult =
          getOverflowExpression(
              uncastArithmeticResult, castArithmeticResult, pFunctionName, pMachineModel, pLogger);
    }
    // TODO: we did not evaluate the side effect of the third argument yet, and will not for now,
    //  but we need to according to the definition. We disallow non-variable/literal input in the
    //  third arg for now. Depending on need we can allow it with a warning or handle it as
    //  statement expr.

    CExpressionAssignmentStatement sideEffectAssignment =
        getSideEffectAssignment(pFunCall, castArithmeticResult, 2);

    return BuiltinOverflowFunctionStatementsToApply.of(booleanFunctionResult, sideEffectAssignment);
  }

  private static BuiltinOverflowFunctionStatementsToApply
      handleOverflowFunctionReturningArithmeticResult(
          boolean useOverflowManager,
          CFunctionCallExpression funCallExpr,
          String functionName,
          final CFAEdge edge,
          MachineModel pMachineModel,
          LogManager pLogger)
          throws UnrecognizedCodeException {
    // All of these method have parameters (a, b, carry_in, *carry_out)
    //  with exclusively unsigned types, either unsigned int, unsigned long, or unsigned long long
    // uniformly.

    // They can be built using a composition of the other methods defined in this class like:
    //  ({ __typeof__ (a) s; \
    //      __typeof__ (a) c1 = __builtin_sub/add_overflow(a, b, &s); \
    //      __typeof__ (a) c2 = __builtin_sub/add_overflow(s, carry_in, &s); \
    //      *(carry_out) = c1 | c2; \
    //      s; })

    CExpression booleanOverflowResult;
    CExpression castArithmeticResult;
    CExpression arg0 = getArgumentCastToParameterType(0, funCallExpr, functionName);
    CExpression arg1 = getArgumentCastToParameterType(1, funCallExpr, functionName);
    CExpression arg2 = getArgumentCastToParameterType(2, funCallExpr, functionName);
    CExpression targetArgument = funCallExpr.getParameterExpressions().get(3);
    if (useOverflowManager) {

      // They all have the same types, so casting of types and the return type does not matter
      CBinaryExpression firstArithmeticResult =
          new CBinaryExpressionBuilder(pMachineModel, pLogger)
              .buildBinaryExpression(arg0, arg1, getOperator(functionName));
      CBinaryExpression secondArithmeticResult =
          new CBinaryExpressionBuilder(pMachineModel, pLogger)
              .buildBinaryExpression(firstArithmeticResult, arg2, getOperator(functionName));
      castArithmeticResult =
          castExpression(
              secondArithmeticResult, getTargetType(functionName, targetArgument, false, edge));

      OverflowAssumptionManager ofaMgr = new OverflowAssumptionManager(pMachineModel, pLogger);
      CExpression firstBooleanOverflowResult =
          checkOverflowUsingOverflowManager(
              ofaMgr,
              arg0,
              arg1,
              targetArgument,
              functionName,
              false,
              edge,
              pMachineModel,
              pLogger);
      CExpression secondBooleanOverflowResult =
          checkOverflowUsingOverflowManager(
              ofaMgr,
              firstArithmeticResult,
              arg2,
              targetArgument,
              functionName,
              false,
              edge,
              pMachineModel,
              pLogger);

      CBinaryExpressionBuilder binExprBuilder =
          new CBinaryExpressionBuilder(pMachineModel, pLogger);
      // The boolean overflow result is just a bitwise OR on the 2 results, wrapped in a logical
      // expression so that all CPAs can handle it
      booleanOverflowResult =
          binExprBuilder.buildBinaryExpression(
              CIntegerLiteralExpression.ZERO,
              binExprBuilder.buildBinaryExpression(
                  firstBooleanOverflowResult,
                  secondBooleanOverflowResult,
                  BinaryOperator.BINARY_OR),
              BinaryOperator.NOT_EQUALS);

    } else {
      // For now, we simply calculate using int128 and see what happens.
      // Our type restriction gets worse here as long long * long long * long long can exceed
      // int128!
      List<CExpression> params = funCallExpr.getParameterExpressions();
      CType typeArg1 = params.get(0).getExpressionType().getCanonicalType();
      CType typeArg2 = params.get(1).getExpressionType().getCanonicalType();
      CType typeArg3 = params.get(2).getExpressionType().getCanonicalType();
      int maxTypeBitSize =
          Integer.max(
              Integer.max(
                  pMachineModel.getSizeofInBits(typeArg1).intValueExact(),
                  pMachineModel.getSizeofInBits(typeArg2).intValueExact()),
              pMachineModel.getSizeofInBits(typeArg3).intValueExact());
      // We can't have multiplication for these functions, so we need (I think) 2 times 2 bits
      // buffer for +/-. Use 10 as buffer to make sure.
      if (maxTypeBitSize + 10 >= 128) {
        // Unsupported for now
        throw new UnsupportedCodeException(
            "Builtin overflow function "
                + functionName
                + " is currently not supported with input types "
                + typeArg1
                + ", "
                + typeArg2
                + ", and "
                + typeArg3
                + ", as we can't evaluate types this large",
            edge);
      }

      CExpression resOfFirstTwoArgs =
          getUncastArithmeticResultWithCastArguments(
              arg0, Optional.empty(), arg1, functionName, edge, pMachineModel, pLogger);
      CExpression uncastArithmeticResult =
          getUncastArithmeticResultWithCastArguments(
              resOfFirstTwoArgs,
              Optional.of(ImmutableList.of(arg0, arg1)),
              arg2,
              functionName,
              edge,
              pMachineModel,
              pLogger);
      castArithmeticResult =
          castExpression(
              uncastArithmeticResult, getTargetType(functionName, targetArgument, false, edge));

      booleanOverflowResult =
          getOverflowExpression(
              uncastArithmeticResult, castArithmeticResult, functionName, pMachineModel, pLogger);
    }

    // These functions return the boolean result in the side effect
    CExpressionAssignmentStatement sideEffectAssignment =
        getSideEffectAssignment(funCallExpr, booleanOverflowResult, 3);

    // These functions return the arithmetic result as function return
    return BuiltinOverflowFunctionStatementsToApply.of(castArithmeticResult, sideEffectAssignment);
  }

  /**
   * Gets the argument at the given index, and if the overflow function used has fixed input types,
   * casts the argument to that type.
   */
  private static CExpression getArgumentCastToParameterType(
      int index, CFunctionCallExpression pFunCallExpr, String pFunctionName) {
    CExpression argument = pFunCallExpr.getParameterExpressions().get(index);
    if (!isFunctionWithArbitraryArgumentTypes(pFunctionName)) {
      CType paramType = getParameterType(pFunctionName).orElseThrow().getCanonicalType();
      CType funDefParamType =
          pFunCallExpr.getDeclaration().getParameters().get(index).getType().getCanonicalType();
      checkArgument(paramType.equals(funDefParamType));
      if (!argument.getExpressionType().equals(paramType)) {
        argument = new CCastExpression(pFunCallExpr.getFileLocation(), paramType, argument);
      }
    }
    return argument;
  }

  /**
   * Returns a CExpression that is a logical expression representing whether the input operation
   * overflowed cast to either a bool or an unsigned int/long/long long type, depending on the used
   * function
   */
  private static CExpression checkOverflowUsingOverflowManager(
      OverflowAssumptionManager ofMgr,
      CExpression operandLeft,
      CExpression operandRight,
      CExpression targetTypeArgument,
      String pFunctionName,
      boolean abortForSideEffects,
      CFAEdge edge,
      MachineModel machineModel,
      LogManager logger)
      throws UnrecognizedCodeException {

    CSimpleType targetType =
        getTargetType(pFunctionName, targetTypeArgument, abortForSideEffects, edge);
    BinaryOperator operator = getOperator(pFunctionName);
    /*CExpression castOperandLeft = operandLeft;
    CExpression castOperandRight = operandRight;
    if (!isFunctionWithArbitraryArgumentTypes(pFunctionName)) {
      // castOperandLeft = new CCastExpression(FileLocation.DUMMY, targetType, operandLeft);
      // castOperandRight = new CCastExpression(FileLocation.DUMMY, targetType, operandRight);
    }*/

    CExpression result;
    if (operator == BinaryOperator.MULTIPLY) {
      result =
          ofMgr.getConjunctionOfMultiplicationAssumptions(
              operandLeft, operandRight, targetType, true);
    } else {
      result =
          ofMgr.getConjunctionOfAdditiveAssumptions(
              operandLeft, operandRight, operator, targetType, true);
    }

    if (!(result instanceof CBinaryExpression binExpr)
        || !binExpr.getOperator().isLogicalOperator()) {
      // Transform result into logical form expected by most CPAs
      result =
          new CBinaryExpressionBuilder(machineModel, logger)
              .buildBinaryExpression(
                  CIntegerLiteralExpression.ZERO, result, BinaryOperator.NOT_EQUALS);
    }
    return getCastReturnForBooleanOverflowExpression(pFunctionName, result);
  }

  private static CCastExpression getCastReturnForBooleanOverflowExpression(
      String pFunctionName, CExpression booleanResult) {
    if (functionReturnsArithmeticResult(pFunctionName)) {
      // The carry_out is of the same type as the other parameters for functions returning the
      // arithmetic value directly
      return new CCastExpression(
          FileLocation.DUMMY, getParameterType(pFunctionName).orElseThrow(), booleanResult);
    } else {
      return new CCastExpression(FileLocation.DUMMY, CNumericTypes.BOOL, booleanResult);
    }
  }

  /**
   * Casts the arguments to int128 and calculates the result of the operation. The result is of type
   * int128 and still needs to be cast back to the target type! Can not be used with arguments of
   * types larger than 64 bits! originalLeftArgs is only to be set if the left argument is not a
   * parameter, but built by this class somehow, and originalLeftArgs is then supposed to include
   * the original arguments!
   */
  private static CExpression getUncastArithmeticResultWithCastArguments(
      CExpression left,
      Optional<List<CExpression>> originalLeftArgs,
      CExpression right,
      String functionName,
      CFAEdge edge,
      MachineModel pMachineModel,
      LogManager pLogger)
      throws UnrecognizedCodeException {
    BinaryOperator op = getOperator(functionName);
    // The definition says that we promote to an infinite precision signed type.
    // Promote the type to a much larger (signed) type and just cast back.
    CType leftType = left.getExpressionType().getCanonicalType();
    CType rightType = right.getExpressionType().getCanonicalType();
    // Signage is only relevant for 2 types whose bit size added is equal to int128s, as all others
    // fit nicely
    boolean signedCalculationType = true;
    int typeBitSizesAdded =
        pMachineModel
            .getSizeofInBits(leftType)
            .add(pMachineModel.getSizeofInBits(rightType))
            .intValueExact();
    int maxTypeBitSize =
        Integer.max(
            pMachineModel.getSizeofInBits(leftType).intValueExact(),
            pMachineModel.getSizeofInBits(rightType).intValueExact());

    // Special case when left is (arg0 +/- arg1) already
    if (originalLeftArgs.isPresent()) {
      typeBitSizesAdded = pMachineModel.getSizeofInBits(rightType).intValueExact();
      maxTypeBitSize = pMachineModel.getSizeofInBits(rightType).intValueExact();
      for (CExpression origLeft : originalLeftArgs.orElseThrow()) {
        CType origLeftType = origLeft.getExpressionType().getCanonicalType();
        typeBitSizesAdded += pMachineModel.getSizeofInBits(origLeftType).intValueExact();
        maxTypeBitSize =
            Integer.max(
                pMachineModel.getSizeofInBits(origLeftType).intValueExact(), maxTypeBitSize);
      }
    }
    if (typeBitSizesAdded == 128 && op == BinaryOperator.MULTIPLY) {
      // Additions and subtractions of smaller types can not hit this limit, only multiplications
      // can, in the wurst case, add their bit-counts.
      // Since we can hit that limit with 2 long longs, the signage is important in this case!
      CSimpleType leftUsedType = getUsedType(leftType, edge);
      boolean leftSignedCalculationType = pMachineModel.isSigned(leftUsedType);
      CSimpleType rightUsedType = getUsedType(rightType, edge);
      boolean rightSignedCalculationType = pMachineModel.isSigned(rightUsedType);
      if (leftSignedCalculationType != rightSignedCalculationType || originalLeftArgs.isPresent()) {
        throw new UnsupportedCodeException(
            "Builtin overflow function "
                + functionName
                + " is currently not supported with input types "
                + leftType
                + " and "
                + rightType
                + ", as we can't evaluate types with distinct signage this large",
            edge);
      }
      signedCalculationType = leftSignedCalculationType;

    } else if ((typeBitSizesAdded > 128 && op == BinaryOperator.MULTIPLY)
        || maxTypeBitSize + 10 >= 128) {
      // Multiplication adds the bit sizes, while +/- only adds 2 (?) bits (we assume 10 as buffer
      // as there might be 3 arguments). We can not exceed 128 ever with this technique.
      throw new UnsupportedCodeException(
          "Builtin overflow function "
              + functionName
              + " is currently not supported with input types "
              + leftType
              + " and "
              + rightType
              + ", as we can't evaluate types this large",
          edge);
    }
    // TODO: check if this is correct and works in all cases! If not, create a TRULY larger type or
    // maybe use the OverflowCPA.
    CSimpleType calculationType =
        new CSimpleType(
            CTypeQualifiers.NONE,
            INT128,
            false,
            false,
            signedCalculationType,
            !signedCalculationType,
            false,
            false,
            false);

    CExpression castLeft = new CCastExpression(FileLocation.DUMMY, calculationType, left);
    CExpression castRight = new CCastExpression(FileLocation.DUMMY, calculationType, right);

    return new CBinaryExpressionBuilder(pMachineModel, pLogger)
        .buildBinaryExpression(castLeft, castRight, op);
  }

  private static CSimpleType getUsedType(CType type, CFAEdge edge) throws UnsupportedCodeException {
    CType returnType;
    if (type instanceof CSimpleType simpleType) {
      returnType = simpleType.getCanonicalType();
    } else if (type instanceof CArrayType arrayType) {
      returnType = arrayType.getType().getCanonicalType();
    } else if (type instanceof CVoidType voidType) {
      returnType = voidType.getCanonicalType();
    } else if (type instanceof CTypedefType typeDefType) {
      returnType = typeDefType.getRealType().getCanonicalType();
    } else if (type instanceof CPointerType ptrType) {
      returnType = ptrType.getType().getCanonicalType();
    } else if (type instanceof CFunctionType funType) {
      returnType = funType.getReturnType().getCanonicalType();
    } else if (type instanceof CBitFieldType bitFieldType) {
      returnType = bitFieldType.getType().getCanonicalType();
    } else {
      throw new UnsupportedCodeException(
          "Unhandled type " + type + " when resolving types for builtin overflow function", edge);
    }
    if (returnType instanceof CSimpleType simpleType) {
      return simpleType;
    }
    throw new UnsupportedCodeException(
        "Unhandled type " + type + " when resolving types for builtin overflow function", edge);
  }

  /**
   * Returns 0 for no overflow and 1 for overflow by returning the negated equality of the cast and
   * uncast arithmetic result, cast to the correct type for the function (bool or unsigned
   * int/long/long long).
   */
  private static CCastExpression getOverflowExpression(
      CExpression uncastArithmeticResult,
      CExpression castArithmeticResult,
      String functionName,
      MachineModel pMachineModel,
      LogManager pLogger)
      throws UnrecognizedCodeException {
    return getCastReturnForBooleanOverflowExpression(
        functionName,
        new CBinaryExpressionBuilder(pMachineModel, pLogger)
            .buildBinaryExpression(
                uncastArithmeticResult, castArithmeticResult, BinaryOperator.NOT_EQUALS));
  }

  private static CExpression castExpression(CExpression expressionToCast, CType targetType) {
    return new CCastExpression(FileLocation.DUMMY, targetType, expressionToCast);
  }

  /**
   * Builtin overflow functions often have a side effect that needs to be assigned additionally to
   * their returned expression.
   */
  public static final class BuiltinOverflowFunctionStatementsToApply {

    private final CExpression functionReturn;
    private final Optional<CExpressionAssignmentStatement> sideEffectAssignment;

    private BuiltinOverflowFunctionStatementsToApply(
        CExpression pFunctionReturn,
        @Nullable CExpressionAssignmentStatement pSideEffectAssignment) {
      functionReturn = checkNotNull(pFunctionReturn);
      sideEffectAssignment = Optional.ofNullable(pSideEffectAssignment);
    }

    static BuiltinOverflowFunctionStatementsToApply of(CExpression pFunctionReturn) {
      return new BuiltinOverflowFunctionStatementsToApply(pFunctionReturn, null);
    }

    static BuiltinOverflowFunctionStatementsToApply of(
        CExpression pFunctionReturn, CExpressionAssignmentStatement pSideEffectAssignment) {
      return new BuiltinOverflowFunctionStatementsToApply(
          pFunctionReturn, checkNotNull(pSideEffectAssignment));
    }

    /**
     * Returns the expression that is the return of the overflow function called. If {@link
     * #hasSideEffectAssignment()} is true, the side effect returned in {@link
     * #getSideEffectAssignmentExpression()} needs to be assigned additionally to evaluating the
     * expression returned here!
     */
    public CExpression getFunctionReturnExpression() {
      return functionReturn;
    }

    /**
     * Returns true if a side effect assignment is part of the function evaluation and {@link
     * #getSideEffectAssignmentExpression()} is non-empty.
     */
    public boolean hasSideEffectAssignment() {
      return sideEffectAssignment.isPresent();
    }

    /**
     * Returns an optional, that if filled, is the assignment expression for the side effect of the
     * evaluated overflow function.
     */
    public Optional<CExpressionAssignmentStatement> getSideEffectAssignmentExpression() {
      return sideEffectAssignment;
    }
  }
}
