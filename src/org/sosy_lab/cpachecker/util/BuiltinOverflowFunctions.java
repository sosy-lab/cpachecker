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
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cfa.types.c.CBasicType.INT128;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
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
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
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
  private static boolean functionReturnsArithmeticResult(String functionName) {
    return !functionReturnsBooleanOverflowCheck(functionName);
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
   * Returns the {@link CSimpleType} in which the numerically calculated value is to be cast to and
   * returned (either as part of the side effect of these functions in case of function with
   * "overflow" in their names, or as actual function return in case of functions without "overflow"
   * in their names).
   */
  private static CSimpleType getTargetType(String pFunctionName, CExpression pArgumentToRetrieveTypeFrom, boolean abortForSideEffects, CFAEdge edge)
      throws UnsupportedCodeException {
    if (!isFunctionWithArbitraryArgumentTypes(pFunctionName)) {
      return getParameterType(pFunctionName).orElseThrow();
    }

    CExpression unpackedArg = pArgumentToRetrieveTypeFrom;
    if (abortForSideEffects) {
      // TODO: we did not evaluate the side effect of the third argument of functions not returning the arithmetic result yet, but we need to according to the definition. We disallow non-variable/literal input in the third arg for now.
      while (unpackedArg instanceof CCastExpression castArg) {
      unpackedArg = castArg.getOperand();
    }
    // This is by no means complete! Just the most common cases where we can be sure that there are not side effects!
      if (unpackedArg instanceof CUnaryExpression unaryExpr) {
        unpackedArg = unaryExpr.getOperand();
      }
    if (!(unpackedArg instanceof CIdExpression || unpackedArg instanceof CLiteralExpression )) {
      throw new UnsupportedCodeException("Builtin overflow function " + pFunctionName + " can not evaluate argument " + pArgumentToRetrieveTypeFrom, edge);
    }
    }

    CType targetType = pArgumentToRetrieveTypeFrom.getExpressionType().getCanonicalType();
    if (targetType instanceof CPointerType) {
      targetType = ((CPointerType) targetType).getType();
    }
    return (CSimpleType) targetType;
  }

  /**
   * Returns the result of the called builtin overflow function, as well as possible side effects as assignments in {@link BuiltinOverflowFunctionReturn}.
   */
  public static BuiltinOverflowFunctionReturn handleOverflowFunction(
      final CFunctionCallExpression funCallExpr, final String functionName, final CFAEdge edge, MachineModel pMachineModel, LogManager pLogger)
      throws UnrecognizedCodeException {
    checkArgument(BuiltinOverflowFunctions.isBuiltinOverflowFunction(functionName));

    if (functionReturnsArithmeticResult(functionName)) {
      return handleOverflowFunctionReturningArithmeticResult(funCallExpr, functionName, edge, pMachineModel, pLogger);
    } else if (functionStoresArithmeticResultUsingSideEffect(functionName)) {
      return handleOverflowFunctionStoringArithmeticResultAsSideEffect(funCallExpr, functionName, edge, pMachineModel, pLogger);
    } else {
      return handleOverflowFunctionReturningOnlyBooleanResult(funCallExpr, functionName, edge, pMachineModel, pLogger);
    }
  }

  private static BuiltinOverflowFunctionReturn handleOverflowFunctionReturningOnlyBooleanResult(
      CFunctionCallExpression pFunCallExpr, String pFunctionName, CFAEdge edge, MachineModel pMachineModel, LogManager pLogger)
      throws UnrecognizedCodeException {
    // Calculate arithmetic result and return an expression that checks whether the cast value is equal to the uncast
    // A possible alternative would be the overflow CPAs overflow check
    CExpression uncastArithmeticResult = getUncastArithmeticResultOfFirstTwoArguments(pFunCallExpr, pFunctionName, edge, pMachineModel, pLogger);
    CExpression thirdArg = pFunCallExpr.getParameterExpressions().get(2);
    // TODO: we did not evaluate the side effect of the third argument yet, and will not for now, but we need to according to the definition. We disallow non-variable/literal input in the third arg for now.
    CExpression castArithmeticResult = castExpression(uncastArithmeticResult, getTargetType(pFunctionName, thirdArg, true, edge));

    return BuiltinOverflowFunctionReturn.of(getEqualityExpression(uncastArithmeticResult, castArithmeticResult, pMachineModel, pLogger));
  }

  private static BuiltinOverflowFunctionReturn handleOverflowFunctionStoringArithmeticResultAsSideEffect(
      CFunctionCallExpression pFunCallExpr, String pFunctionName, final CFAEdge edge, MachineModel pMachineModel, LogManager pLogger)
      throws UnrecognizedCodeException {
// Calculate arithmetic result and return an expression that checks whether the cast value is equal to the uncast
    // A possible alternative would be the overflow CPAs overflow check
    CExpression uncastArithmeticResult = getUncastArithmeticResultOfFirstTwoArguments(pFunCallExpr, pFunctionName, edge, pMachineModel, pLogger);
    CExpression thirdArg = pFunCallExpr.getParameterExpressions().get(2);
    // TODO: we did not evaluate the side effect of the third argument yet, and will not for now, but we need to according to the definition. We disallow non-variable/literal input in the third arg for now.
    CExpression castArithmeticResult = castExpression(uncastArithmeticResult, getTargetType(pFunctionName, thirdArg, true, edge));

    // TODO:
    CExpressionAssignmentStatement sideEffectAssignment = null;

    CExpression functionReturn = getEqualityExpression(uncastArithmeticResult, castArithmeticResult, pMachineModel, pLogger);
    return BuiltinOverflowFunctionReturn.of(functionReturn, sideEffectAssignment);
  }

  private static BuiltinOverflowFunctionReturn handleOverflowFunctionReturningArithmeticResult(
      CFunctionCallExpression pFunCallExpr, String functionName, final CFAEdge edge, MachineModel pMachineModel, LogManager pLogger)
      throws UnsupportedCodeException {
    // All of these method have parameters (a, b, carry_in, *carry_out)
    //  with exclusively unsigned types (either unsigned int, unsigned long, or unsigned long long).
    // They can be built using a composition of the other methods defined in this class like:
    //  ({ __typeof__ (a) s; \
    //      __typeof__ (a) c1 = __builtin_sub/add_overflow(a, b, &s); \
    //      __typeof__ (a) c2 = __builtin_sub/add_overflow(s, carry_in, &s); \
    //      *(carry_out) = c1 | c2; \
    //      s; })

    // Our type restriction gets worse here as long long * long long * long long can exceed int128!
    List<CExpression> params = pFunCallExpr.getParameterExpressions();
    CType typeArg1 = params.get(0).getExpressionType().getCanonicalType();
    CType typeArg2 = params.get(1).getExpressionType().getCanonicalType();
    CType typeArg3 = params.get(2).getExpressionType().getCanonicalType();
    int typeBitSizesAdded = pMachineModel.getSizeofInBits(typeArg1).add(pMachineModel.getSizeofInBits(typeArg2)).add(pMachineModel.getSizeofInBits(typeArg3)).intValueExact();
    if (typeBitSizesAdded > 128) {
      // Unsupported for now
      throw new UnsupportedCodeException("Builtin overflow function " + functionName + " is currently not supported with input types " + typeArg1 + ", " + typeArg2 + ", and " + typeArg3 + ", as we can't evaluate types this large", edge);
    }

    // TODO:
    // These functions return the boolean result in the side effect
    // The boolean overflow result is just a bitwise OR on the 2 results
    CExpressionAssignmentStatement sideEffectAssignment = null;

    // TODO:
    // These functions return the arithmetic result as function return
    CExpression functionReturn = null;

    return BuiltinOverflowFunctionReturn.of(functionReturn, sideEffectAssignment);
  }

  /**
   * Takes the first 2 arguments of the {@link CFunctionCallExpression} and applies the arithmetic binary operation of the overflow function.
   */
  private static CExpression getUncastArithmeticResultOfFirstTwoArguments(CFunctionCallExpression pFunCallExpr, String pFunctionName, CFAEdge edge, MachineModel pMachineModel, LogManager pLogger)
      throws UnrecognizedCodeException {
    List<CExpression> params = pFunCallExpr.getParameterExpressions();
    BinaryOperator operator = getOperator(pFunctionName);
    CExpression castArg1 = params.get(1);
    CExpression castArg2 = params.get(2);
    if (!isFunctionWithArbitraryArgumentTypes(pFunctionName)) {
      CType paramType = getParameterType(pFunctionName).orElseThrow().getCanonicalType();
      checkArgument(paramType.equals(pFunCallExpr.getDeclaration().getParameters().get(0).getType().getCanonicalType()));
      checkArgument(paramType.equals(pFunCallExpr.getDeclaration().getParameters().get(1).getType().getCanonicalType()));
      castArg1 = new CCastExpression(FileLocation.DUMMY, paramType, castArg1);
      castArg2 = new CCastExpression(FileLocation.DUMMY, paramType, castArg2);
    }

    return getUncastArithmeticResultOf(castArg1, castArg2, operator, pFunctionName, edge, pMachineModel, pLogger);
  }

  private static CExpression getUncastArithmeticResultOf(CExpression left, CExpression right, BinaryOperator op, String functionName, CFAEdge edge, MachineModel pMachineModel, LogManager pLogger)
      throws UnrecognizedCodeException {
    // The definition says that we promote to an infinite precision signed type.
    // Promote the type to a much larger (signed) type and just cast back.
    CType leftType = left.getExpressionType().getCanonicalType();
    CType rightType = right.getExpressionType().getCanonicalType();
    // Signage is only relevant for 2 types whose bit size added is equal to int128s, as all others fit nicely
    boolean signedCalculationType = true;
    int typeBitSizesAdded = pMachineModel.getSizeofInBits(leftType).add(pMachineModel.getSizeofInBits(rightType)).intValueExact();
    if (typeBitSizesAdded == 128 && op == BinaryOperator.MULTIPLY) {
      // Additions and subtractions of smaller types can not hit this limit, only multiplications can, in the wurst case, add their bit-counts.
      // Since we can hit that limit with 2 long longs, the signage is important in this case!
      CSimpleType leftUsedType = getUsedType(leftType, edge);
      boolean leftSignedCalculationType = leftUsedType.hasSignedSpecifier();
      checkState(signedCalculationType != leftUsedType.hasUnsignedSpecifier());
      CSimpleType rightUsedType = getUsedType(rightType, edge);
      boolean rightSignedCalculationType = rightUsedType.hasSignedSpecifier();
      checkState(signedCalculationType != rightUsedType.hasUnsignedSpecifier());
      if (leftSignedCalculationType == rightSignedCalculationType) {
        signedCalculationType = leftSignedCalculationType;
      } else {
        throw new UnsupportedCodeException("Builtin overflow function " + functionName + " is currently not supported with input types " + leftType + " and " + rightType+ ", as we can't evaluate types with distinct signage this large", edge);
      }

    } else if (typeBitSizesAdded > 128) {
      // Unsupported for now
      throw new UnsupportedCodeException("Builtin overflow function " + functionName + " is currently not supported with input types " + leftType + " and " + rightType+ ", as we can't evaluate types this large", edge);
    }
    // TODO: check if this is correct and works in all cases! If not, create a TRULY larger type or maybe use the OverflowCPA.
    CSimpleType calculationType = new CSimpleType(
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

    return new CBinaryExpressionBuilder(pMachineModel, pLogger).buildBinaryExpression(castLeft, castRight, op);
  }

  private static CSimpleType getUsedType(CType type, CFAEdge edge)
      throws UnsupportedCodeException {
    CType returnType;
    if (type instanceof CSimpleType simpleType) {
      returnType = simpleType.getCanonicalType();
    } else if (type instanceof CArrayType arrayType) {
      returnType =  arrayType.getType().getCanonicalType();
    } else if (type instanceof CVoidType voidType) {
      returnType =  voidType.getCanonicalType();
    } else if (type instanceof CTypedefType typeDefType) {
      returnType =  typeDefType.getRealType().getCanonicalType();
    } else if (type instanceof CPointerType ptrType) {
      returnType =  ptrType.getType().getCanonicalType();
    } else if (type instanceof CFunctionType funType) {
      returnType =  funType.getReturnType().getCanonicalType();
    } else if (type instanceof CBitFieldType bitFieldType) {
      returnType =  bitFieldType.getType().getCanonicalType();
    } else {
      throw new UnsupportedCodeException("Unhandled type "+ type +" when resolving types for builtin overflow function",
          edge);
    }
    if (returnType instanceof CSimpleType simpleType) {
      return simpleType;
    }
    throw new UnsupportedCodeException("Unhandled type "+ type +" when resolving types for builtin overflow function",
        edge);
  }

  /**
   * Returns 0 for no overflow and 1 for overflow by returning the negated equality of the cast and uncast arithmetic result.
   */
  private static CExpression getEqualityExpression(CExpression uncastArithmeticResult, CExpression castArithmeticResult, MachineModel pMachineModel, LogManager pLogger)
      throws UnrecognizedCodeException {
    return new CBinaryExpressionBuilder(pMachineModel, pLogger).buildBinaryExpression(uncastArithmeticResult, castArithmeticResult, BinaryOperator.NOT_EQUALS);
  }

  private static CExpression castExpression(CExpression expressionToCast, CType targetType) {
    return new CCastExpression(FileLocation.DUMMY, targetType, expressionToCast);
  }

  /**
   * Builtin overflow functions often have a side effect that needs to be assigned additionally to their returned expression.
   */
  public static final class BuiltinOverflowFunctionReturn {

    private final CExpression functionReturn;
    private final Optional<CExpressionAssignmentStatement> sideEffectAssignment;

    private BuiltinOverflowFunctionReturn(CExpression pFunctionReturn, @Nullable CExpressionAssignmentStatement pSideEffectAssignment) {
      functionReturn = checkNotNull(pFunctionReturn);
      sideEffectAssignment = Optional.ofNullable(pSideEffectAssignment);
    }

    static BuiltinOverflowFunctionReturn of(CExpression pFunctionReturn) {
      return new BuiltinOverflowFunctionReturn(pFunctionReturn, null);
    }

    static BuiltinOverflowFunctionReturn of(CExpression pFunctionReturn, CExpressionAssignmentStatement pSideEffectAssignment) {
      return new BuiltinOverflowFunctionReturn(pFunctionReturn, checkNotNull(pSideEffectAssignment));
    }

    /**
     * Returns the function return of the overflow function called. If {@link #hasSideEffectAssignment()} is true, the side effect returned in {@link #getSideEffectAssignmentExpression()} needs to be assigned additionally to evaluating the expression returned here!
     */
    public CExpression getFunctionReturnExpression() {
      return functionReturn;
    }

    /**
     * Returns true if a side effect assignment is part of the function evaluation and {@link #getSideEffectAssignmentExpression()} is non-empty.
     */
    public boolean hasSideEffectAssignment() {
      return sideEffectAssignment.isPresent();
    }

    /**
     * Returns an optional, that if filled, is the assignment expression for the side effect of the evaluated overflow function.
     */
    public Optional<CExpressionAssignmentStatement> getSideEffectAssignmentExpression() {
      return sideEffectAssignment;
    }
  }
}
