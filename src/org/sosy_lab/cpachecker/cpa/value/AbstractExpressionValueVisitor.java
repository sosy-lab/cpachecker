// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLongs;
import java.io.Serial;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.types.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.ArrayValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.EnumConstantValue;
import org.sosy_lab.cpachecker.cpa.value.type.FunctionValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.RoundingMode;

/**
 * This Visitor implements an evaluation strategy of simply typed expressions. An expression is
 * defined as simply typed iff it is not an array type (vgl {@link CArrayType}), a struct or union
 * type (vgl {@link CComplexType}), an imaginary type (vgl {@link CImaginaryLiteralExpression}), or
 * a pointer type (vgl {@link CPointerType}). The key distinction between these types and simply
 * typed types is, that a value of simply typed types can be represented as a numerical value
 * without losing information.
 *
 * <p>Furthermore, the visitor abstracts from using abstract states to get values stored in the
 * memory of a program.
 */
public abstract class AbstractExpressionValueVisitor
    extends DefaultCExpressionVisitor<Value, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Value, UnrecognizedCodeException>,
        JRightHandSideVisitor<Value, NoException>,
        JExpressionVisitor<Value, NoException> {

  /** length of type LONG in Java (in bit). */
  private static final int SIZE_OF_JAVA_LONG = 64;

  // private final ValueAnalysisState state;
  private final String functionName;
  private final MachineModel machineModel;

  // for logging
  private final LogManagerWithoutDuplicates logger;

  private boolean missingFieldAccessInformation = false;

  /**
   * This Visitor returns the numeral value for an expression.
   *
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   */
  protected AbstractExpressionValueVisitor(
      String pFunctionName, MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {

    // this.state = pState;
    functionName = pFunctionName;
    machineModel = pMachineModel;
    logger = pLogger;
  }

  public boolean hasMissingFieldAccessInformation() {
    return missingFieldAccessInformation;
  }

  @Override
  protected Value visitDefault(CExpression pExp) {
    return Value.UnknownValue.getInstance();
  }

  public void reset() {
    missingFieldAccessInformation = false;
  }

  @Override
  public Value visit(final CBinaryExpression pE) throws UnrecognizedCodeException {
    final Value lVal = pE.getOperand1().accept(this);
    if (lVal.isUnknown()) {
      return Value.UnknownValue.getInstance();
    }
    final Value rVal = pE.getOperand2().accept(this);
    if (rVal.isUnknown()) {
      return Value.UnknownValue.getInstance();
    }
    return calculateBinaryOperation(lVal, rVal, pE, machineModel, logger);
  }

  /**
   * This method calculates the exact result for a binary operation. If the value can not be
   * determined, we return an {@link UnknownValue}. If an arithmetic exception happens (e.g.,
   * division by zero), we log a warning and also return {@link UnknownValue}.
   *
   * @param lVal evaluated first operand of binaryExpr
   * @param rVal evaluated second operand of binaryExpr
   * @param binaryExpr will be evaluated
   * @param machineModel information about types
   * @param logger for logging
   */
  public static Value calculateBinaryOperation(
      Value lVal,
      Value rVal,
      final CBinaryExpression binaryExpr,
      final MachineModel machineModel,
      final LogManagerWithoutDuplicates logger) {

    final BinaryOperator binaryOperator = binaryExpr.getOperator();
    final CType calculationType = binaryExpr.getCalculationType();

    lVal = castCValue(lVal, calculationType, machineModel, logger, binaryExpr.getFileLocation());
    if (binaryOperator != BinaryOperator.SHIFT_LEFT
        && binaryOperator != BinaryOperator.SHIFT_RIGHT) {
      /* For SHIFT-operations we do not cast the second operator.
       * We even do not need integer-promotion,
       * because the maximum SHIFT of 64 is lower than MAX_CHAR.
       *
       * ISO-C99 (6.5.7 #3): Bitwise shift operators
       * The integer promotions are performed on each of the operands.
       * The type of the result is that of the promoted left operand.
       * If the value of the right operand is negative or is greater than
       * or equal to the width of the promoted left operand,
       * the behavior is undefined.
       */
      rVal = castCValue(rVal, calculationType, machineModel, logger, binaryExpr.getFileLocation());
    }

    if (lVal instanceof FunctionValue || rVal instanceof FunctionValue) {
      return calculateExpressionWithFunctionValue(binaryOperator, rVal, lVal);
    }

    if (lVal instanceof SymbolicValue || rVal instanceof SymbolicValue) {
      return calculateSymbolicBinaryExpression(lVal, rVal, binaryExpr);
    }

    if (!lVal.isNumericValue() || !rVal.isNumericValue()) {
      logger.logf(
          Level.FINE,
          "Parameters to binary operation '%s %s %s' are no numeric values.",
          lVal,
          binaryOperator,
          rVal);
      return Value.UnknownValue.getInstance();
    }

    return switch (binaryOperator) {
      case PLUS,
          MINUS,
          DIVIDE,
          MODULO,
          MULTIPLY,
          SHIFT_LEFT,
          SHIFT_RIGHT,
          BINARY_AND,
          BINARY_OR,
          BINARY_XOR -> {
        Value result =
            arithmeticOperation(
                (NumericValue) lVal,
                (NumericValue) rVal,
                binaryOperator,
                calculationType,
                machineModel,
                logger);
        yield castCValue(
            result,
            binaryExpr.getExpressionType(),
            machineModel,
            logger,
            binaryExpr.getFileLocation());
      }
      case EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_EQUAL, LESS_THAN, LESS_EQUAL ->
          comparisonOperation(
              (NumericValue) lVal,
              (NumericValue) rVal,
              binaryOperator,
              calculationType,
              machineModel,
              logger);
        // we do not cast here, because 0 and 1 should be small enough for every type.
    };
  }

  /**
   * Join a symbolic expression with something else using a binary expression.
   *
   * <p>e.g. joining `a` and `5` with `+` will produce `a + 5`
   *
   * @param pLValue left hand side value
   * @param pRValue right hand side value
   * @param pExpression the binary expression with the operator
   * @return the calculated Value
   */
  public static Value calculateSymbolicBinaryExpression(
      Value pLValue, Value pRValue, final CBinaryExpression pExpression) {

    final BinaryOperator operator = pExpression.getOperator();

    final CType leftOperandType = pExpression.getOperand1().getExpressionType();
    final CType rightOperandType = pExpression.getOperand2().getExpressionType();
    final CType expressionType = pExpression.getExpressionType();
    final CType calculationType = pExpression.getCalculationType();

    return createSymbolicExpression(
        pLValue,
        leftOperandType,
        pRValue,
        rightOperandType,
        operator,
        expressionType,
        calculationType);
  }

  public static Value calculateExpressionWithFunctionValue(
      BinaryOperator binaryOperator, Value val1, Value val2) {
    if (val1 instanceof FunctionValue) {
      return calculateOperationWithFunctionValue(binaryOperator, (FunctionValue) val1, val2);
    } else if (val2 instanceof FunctionValue) {
      return calculateOperationWithFunctionValue(binaryOperator, (FunctionValue) val2, val1);
    } else {
      return new Value.UnknownValue();
    }
  }

  private static NumericValue calculateOperationWithFunctionValue(
      BinaryOperator binaryOperator, FunctionValue val1, Value val2) {
    return switch (binaryOperator) {
      case EQUALS -> new NumericValue(val1.equals(val2) ? 1 : 0);
      case NOT_EQUALS -> new NumericValue(val1.equals(val2) ? 0 : 1);
      default ->
          throw new AssertionError(
              "Operation " + binaryOperator + " is not supported for function values");
    };
  }

  private static SymbolicValue createSymbolicExpression(
      Value pLeftValue,
      CType pLeftType,
      Value pRightValue,
      CType pRightType,
      CBinaryExpression.BinaryOperator pOperator,
      CType pExpressionType,
      CType pCalculationType) {

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression leftOperand;
    SymbolicExpression rightOperand;

    leftOperand = factory.asConstant(pLeftValue, pLeftType);
    rightOperand = factory.asConstant(pRightValue, pRightType);

    return switch (pOperator) {
      case PLUS -> factory.add(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MINUS -> factory.minus(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MULTIPLY ->
          factory.multiply(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case DIVIDE -> factory.divide(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MODULO -> factory.modulo(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_LEFT ->
          factory.shiftLeft(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_RIGHT ->
          factory.shiftRightSigned(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_AND ->
          factory.binaryAnd(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_OR ->
          factory.binaryOr(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_XOR ->
          factory.binaryXor(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case EQUALS -> factory.equal(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case NOT_EQUALS ->
          factory.notEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_THAN ->
          factory.lessThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_EQUAL ->
          factory.lessThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_THAN ->
          factory.greaterThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_EQUAL ->
          factory.greaterThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
    };
  }

  /**
   * Calculate an arithmetic operation on two integer types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @param machineModel the machine model
   * @param logger logging
   * @return the resulting value
   */
  private static long arithmeticOperation(
      final long l,
      final long r,
      final BinaryOperator op,
      final CType calculationType,
      final MachineModel machineModel,
      final LogManager logger) {

    // special handling for UNSIGNED_LONGLONG (32 and 64bit), UNSIGNED_LONG (64bit)
    // because Java only has SIGNED_LONGLONG
    CSimpleType st = getArithmeticType(calculationType);
    if (st != null) {
      if (machineModel.getSizeofInBits(st) >= SIZE_OF_JAVA_LONG && st.hasUnsignedSpecifier()) {
        switch (op) {
          case DIVIDE -> {
            if (r == 0) {
              logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
              return 0;
            }
            return UnsignedLongs.divide(l, r);
          }
          case MODULO -> {
            return UnsignedLongs.remainder(l, r);
          }
          case SHIFT_RIGHT -> {
            /*
             * from http://docs.oracle.com/javase/tutorial/java/nutsandbolts/op3.html
             *
             * The unsigned right shift operator ">>>" shifts a zero
             * into the leftmost position, while the leftmost position
             * after ">>" depends on sign extension.
             */
            return l >>> r;
          }
          default -> {}
        }
      }
    }

    switch (op) {
      case PLUS -> {
        return l + r;
      }
      case MINUS -> {
        return l - r;
      }
      case DIVIDE -> {
        if (r == 0) {
          logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
          return 0;
        }
        return l / r;
      }
      case MODULO -> {
        return l % r;
      }
      case MULTIPLY -> {
        return l * r;
      }
      case SHIFT_LEFT -> {
        /* There is a difference in the SHIFT-operation in Java and C.
         * In C a SHIFT is a normal SHIFT, in Java the rVal is used as (r%64).
         *
         * http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
         *
         * If the promoted type of the left-hand operand is long, then only the
         * six lowest-order bits of the right-hand operand are used as the
         * shift distance. It is as if the right-hand operand were subjected to
         * a bitwise logical AND operator & (§15.22.1) with the mask value 0x3f.
         * The shift distance actually used is therefore always in the range 0 to 63.
         */
        return (r >= SIZE_OF_JAVA_LONG) ? 0 : l << r;
      }
      case SHIFT_RIGHT -> {
        return l >> r;
      }
      case BINARY_AND -> {
        return l & r;
      }
      case BINARY_OR -> {
        return l | r;
      }
      case BINARY_XOR -> {
        return l ^ r;
      }
      default -> throw new AssertionError("unknown binary operation: " + op);
    }
  }

  /**
   * Calculate an arithmetic operation on two int128 types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @param logger logging
   * @return the resulting value
   */
  private static BigInteger arithmeticOperation(
      final BigInteger l, final BigInteger r, final BinaryOperator op, final LogManager logger) {

    switch (op) {
      case PLUS -> {
        return l.add(r);
      }
      case MINUS -> {
        return l.subtract(r);
      }
      case DIVIDE -> {
        if (r.equals(BigInteger.ZERO)) {
          // this matches the behavior of long
          logger.logf(Level.SEVERE, "Division by Zero (%s / %s)", l.toString(), r.toString());
          return BigInteger.ZERO;
        }
        return l.divide(r);
      }
      case MODULO -> {
        return l.mod(r);
      }
      case MULTIPLY -> {
        return l.multiply(r);
      }
      case SHIFT_LEFT -> {
        // (C11, 6.5.7p3) "If the value of the right operand is negative
        // or is greater than or equal to the width of the promoted left operand,
        // the behavior is undefined"
        if (r.compareTo(BigInteger.valueOf(128)) <= 0 && r.signum() != -1) {
          return l.shiftLeft(r.intValue());
        } else {
          logger.logf(
              Level.SEVERE,
              "Right-hand side (%s) of the bitshift is larger than 128 or negative.",
              r.toString());
          return BigInteger.ZERO;
        }
      }
      case SHIFT_RIGHT -> {
        if (r.compareTo(BigInteger.valueOf(128)) <= 0 && r.signum() != -1) {
          return l.shiftRight(r.intValue());
        } else {
          return BigInteger.ZERO;
        }
      }
      case BINARY_AND -> {
        return l.and(r);
      }
      case BINARY_OR -> {
        return l.or(r);
      }
      case BINARY_XOR -> {
        return l.xor(r);
      }
      default -> throw new AssertionError("unknown binary operation: " + op);
    }
  }

  /** Cast the argument to a floating point type */
  private static FloatValue castToFloat(
      MachineModel pMachineModel, CSimpleType pTargetType, NumericValue pValue) {
    checkArgument(
        pTargetType.getType().isFloatingPointType(),
        "Target type `%s` is not a floating point type",
        pTargetType);
    FloatValue.Format precision = FloatValue.Format.fromCType(pMachineModel, pTargetType);
    return pValue.floatingPointValue(precision);
  }

  /**
   * Calculate an arithmetic operation on two floating point values.
   *
   * @param pOperation the binary operator
   * @param pArg1 left hand side value
   * @param pArg2 right hand side value
   * @return the resulting value
   */
  private static FloatValue arithmeticOperation(
      final BinaryOperator pOperation, final FloatValue pArg1, final FloatValue pArg2) {

    return switch (pOperation) {
      case PLUS -> pArg1.add(pArg2);
      case MINUS -> pArg1.subtract(pArg2);
      case DIVIDE -> pArg1.divide(pArg2);
      case MODULO -> pArg1.modulo(pArg2);
      case MULTIPLY -> pArg1.multiply(pArg2);
      case SHIFT_LEFT, SHIFT_RIGHT, BINARY_AND, BINARY_OR, BINARY_XOR ->
          throw new UnsupportedOperationException(
              "Trying to perform " + pOperation + " on floating point operands");
      default -> throw new IllegalArgumentException("Unknown binary operation: " + pOperation);
    };
  }

  /**
   * Calculate an arithmetic operation on two Value types.
   *
   * @param lNum left hand side value
   * @param rNum right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @param machineModel the machine model
   * @param logger logging
   * @return the resulting values
   */
  private static Value arithmeticOperation(
      final NumericValue lNum,
      final NumericValue rNum,
      final BinaryOperator op,
      final CType calculationType,
      final MachineModel machineModel,
      final LogManager logger) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(
          Level.FINE, "unsupported type %s for result of binary operation %s", calculationType, op);
      return Value.UnknownValue.getInstance();
    }

    try {
      switch (type.getType()) {
        case INT -> {
          // Both l and r must be of the same type, which in this case is INT, so we can cast to
          // long.
          long lVal = lNum.getNumber().longValue();
          long rVal = rNum.getNumber().longValue();
          long result = arithmeticOperation(lVal, rVal, op, calculationType, machineModel, logger);
          return new NumericValue(result);
        }
        case INT128 -> {
          BigInteger lVal = lNum.bigIntegerValue();
          BigInteger rVal = rNum.bigIntegerValue();
          BigInteger result = arithmeticOperation(lVal, rVal, op, logger);
          return new NumericValue(result);
        }
        case FLOAT, DOUBLE, FLOAT128 -> {
          return new NumericValue(
              arithmeticOperation(
                  op,
                  castToFloat(machineModel, type, lNum),
                  castToFloat(machineModel, type, rNum)));
        }
        default -> {
          logger.logf(
              Level.FINE, "unsupported type for result of binary operation %s", type.toString());
          return Value.UnknownValue.getInstance();
        }
      }
    } catch (ArithmeticException e) { // log warning and ignore expression
      logger.logf(
          Level.WARNING,
          "expression causes arithmetic exception (%s): %s %s %s",
          e.getMessage(),
          lNum,
          op.getOperator(),
          rNum);
      return Value.UnknownValue.getInstance();
    }
  }

  private static Value comparisonOperation(
      final NumericValue l,
      final NumericValue r,
      final BinaryOperator op,
      final CType calculationType,
      final MachineModel machineModel,
      final LogManager logger) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(
          Level.FINE, "unsupported type %s for result of binary operation %s", calculationType, op);
      return Value.UnknownValue.getInstance();
    }

    switch (type.getType()) {
      case INT128, CHAR, INT -> {
        CSimpleType canonicalType = type.getCanonicalType();
        int sizeInBits = machineModel.getSizeof(canonicalType) * machineModel.getSizeofCharInBits();

        // Compare the two numbers
        final int cmp;
        if ((!machineModel.isSigned(canonicalType) && sizeInBits == SIZE_OF_JAVA_LONG)
            || sizeInBits > SIZE_OF_JAVA_LONG) {
          BigInteger leftBigInt = l.bigIntegerValue();
          BigInteger rightBigInt = r.bigIntegerValue();
          cmp = leftBigInt.compareTo(rightBigInt);
        } else {
          cmp = Long.compare(l.longValue(), r.longValue());
        }

        // returns True, iff cmp fulfills the boolean operation.
        boolean result =
            switch (op) {
              case GREATER_THAN -> cmp > 0;
              case GREATER_EQUAL -> cmp >= 0;
              case LESS_THAN -> cmp < 0;
              case LESS_EQUAL -> cmp <= 0;
              case EQUALS -> cmp == 0;
              case NOT_EQUALS -> cmp != 0;
              default -> throw new AssertionError("unknown binary operation: " + op);
            };

        // return 1 if expression holds, 0 otherwise
        return new NumericValue(result ? 1 : 0);
      }
      case FLOAT, DOUBLE, FLOAT128 -> {
        boolean result =
            comparisonOperation(
                op, castToFloat(machineModel, type, l), castToFloat(machineModel, type, r));
        return new NumericValue(result ? 1 : 0);
      }
      default -> {
        logger.logf(
            Level.FINE,
            "unsupported type %s for result of binary operation %s",
            type.toString(),
            op);
        return Value.UnknownValue.getInstance();
      }
    }
  }

  /**
   * Calculate a comparison operation on two floating point values.
   *
   * @param pOperation the binary operator
   * @param pArg1 left hand side value
   * @param pArg2 right hand side value
   * @return the resulting value
   */
  private static boolean comparisonOperation(
      final BinaryOperator pOperation, final FloatValue pArg1, final FloatValue pArg2) {

    return switch (pOperation) {
      case GREATER_THAN -> pArg1.greaterThan(pArg2);
      case GREATER_EQUAL -> pArg1.greaterOrEqual(pArg2);
      case LESS_THAN -> pArg1.lessThan(pArg2);
      case LESS_EQUAL -> pArg1.lessOrEqual(pArg2);
      case EQUALS -> pArg1.equalTo(pArg2);
      case NOT_EQUALS -> !pArg1.equalTo(pArg2);
      default -> throw new AssertionError("unknown binary operation: " + pOperation);
    };
  }

  @Override
  public Value visit(CCastExpression pE) throws UnrecognizedCodeException {
    return castCValue(
        pE.getOperand().accept(this),
        pE.getExpressionType(),
        machineModel,
        logger,
        pE.getFileLocation());
  }

  @Override
  public Value visit(CComplexCastExpression pE) throws UnrecognizedCodeException {
    // evaluation of complex numbers is not supported by now
    return Value.UnknownValue.getInstance();
  }

  /**
   * Helper method to handle unary builtin function in {@link
   * AbstractExpressionValueVisitor#visit(CFunctionCallExpression)}
   */
  private Value handleBuiltinFunction1(
      String pName, List<Value> pArguments, Function<FloatValue, Value> pOperation) {
    final Value parameter = Iterables.getOnlyElement(pArguments);
    if (parameter.isExplicitlyKnown()) {
      // Cast the argument to match the function type
      FloatValue value =
          castToFloat(
              machineModel,
              BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pName),
              (NumericValue) parameter);
      return pOperation.apply(value);
    }
    return Value.UnknownValue.getInstance();
  }

  /**
   * Helper method to handle binary builtin function in {@link
   * AbstractExpressionValueVisitor#visit(CFunctionCallExpression)}
   */
  private Value handleBuiltinFunction2(
      String pName, List<Value> pArguments, BiFunction<FloatValue, FloatValue, Value> pOperation) {
    checkArgument(pArguments.size() == 2);
    Value parameter1 = pArguments.get(0);
    Value parameter2 = pArguments.get(1);

    if (parameter1.isExplicitlyKnown() && parameter2.isExplicitlyKnown()) {
      // Cast both arguments to match the function type
      CSimpleType targetType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pName);
      FloatValue value1 = castToFloat(machineModel, targetType, (NumericValue) parameter1);
      FloatValue value2 = castToFloat(machineModel, targetType, (NumericValue) parameter2);

      return pOperation.apply(value1, value2);
    }
    return Value.UnknownValue.getInstance();
  }

  @Override
  public Value visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCodeException {
    CExpression functionNameExp = pIastFunctionCallExpression.getFunctionNameExpression();

    // We only handle builtin functions
    if (functionNameExp instanceof CIdExpression) {
      String calledFunctionName = ((CIdExpression) functionNameExp).getName();

      if (BuiltinFunctions.isBuiltinFunction(calledFunctionName)) {
        CType functionType = BuiltinFunctions.getFunctionType(calledFunctionName);

        if (isUnspecifiedType(functionType)) {
          // unsupported formula
          return Value.UnknownValue.getInstance();
        }

        List<CExpression> parameterExpressions =
            pIastFunctionCallExpression.getParameterExpressions();
        List<Value> parameterValues = new ArrayList<>(parameterExpressions.size());

        for (CExpression currParamExp : parameterExpressions) {
          Value newValue = currParamExp.accept(this);
          parameterValues.add(newValue);
        }

        if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(calledFunctionName)) {
          return BuiltinOverflowFunctions.evaluateFunctionCall(
              pIastFunctionCallExpression, this, machineModel, logger);

        } else if (BuiltinFloatFunctions.matchesAbsolute(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName, parameterValues, (FloatValue arg) -> new NumericValue(arg.abs()));

        } else if (BuiltinFloatFunctions.matchesHugeVal(calledFunctionName)
            || BuiltinFloatFunctions.matchesInfinity(calledFunctionName)) {
          checkArgument(parameterValues.isEmpty());
          FloatValue.Format precision =
              FloatValue.Format.fromCType(
                  machineModel,
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName));
          return new NumericValue(FloatValue.infinity(precision));

        } else if (BuiltinFloatFunctions.matchesNaN(calledFunctionName)) {
          // FIXME: Add support for NaN payloads
          checkArgument(parameterValues.size() < 2);
          FloatValue.Format precision =
              FloatValue.Format.fromCType(
                  machineModel,
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName));
          return new NumericValue(FloatValue.nan(precision));

        } else if (BuiltinFloatFunctions.matchesIsNaN(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> new NumericValue(arg.isNan() ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsInfinity(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> new NumericValue(arg.isInfinite() ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsInfinitySign(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) ->
                  new NumericValue(arg.isInfinite() ? (arg.isNegative() ? -1 : 1) : 0));

        } else if (BuiltinFloatFunctions.matchesFinite(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> new NumericValue((arg.isInfinite() || arg.isNan()) ? 0 : 1));

        } else if (BuiltinFloatFunctions.matchesFloor(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> new NumericValue(arg.round(RoundingMode.FLOOR)));

        } else if (BuiltinFloatFunctions.matchesCeil(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> new NumericValue(arg.round(RoundingMode.CEILING)));

        } else if (BuiltinFloatFunctions.matchesRound(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> new NumericValue(arg.round(RoundingMode.NEAREST_AWAY)));

        } else if (BuiltinFloatFunctions.matchesLround(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> {
                FloatValue value = arg.round(RoundingMode.NEAREST_AWAY);
                return switch (machineModel.getSizeofLongInt()) {
                  case Integer.BYTES -> new NumericValue(value.integerValue());
                  case Long.BYTES -> new NumericValue(value.longValue());
                  default -> Value.UnknownValue.getInstance();
                };
              });

        } else if (BuiltinFloatFunctions.matchesLlround(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> {
                FloatValue value = arg.round(RoundingMode.NEAREST_AWAY);
                return switch (machineModel.getSizeofLongLongInt()) {
                  case Integer.BYTES -> new NumericValue(value.integerValue());
                  case Long.BYTES -> new NumericValue(value.longValue());
                  default -> Value.UnknownValue.getInstance();
                };
              });

        } else if (BuiltinFloatFunctions.matchesTrunc(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> new NumericValue(arg.round(RoundingMode.TRUNCATE)));

        } else if (BuiltinFloatFunctions.matchesFdim(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(
                      arg1.lessOrEqual(arg2)
                          ? FloatValue.zero(arg1.getFormat())
                          : arg1.subtract(arg2)));

        } else if (BuiltinFloatFunctions.matchesFmax(calledFunctionName)) {
          // TODO: Add a warning message for fmax(0.0,-0.0) and fmax(-0.0, 0.0)
          // The value is undefined and we simply pick 0.0 in those cases, but gcc will always
          // return the first argument.
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(
                      switch (arg1.compareWithTotalOrder(arg2)) {
                        case -1 -> arg2.isNan() ? arg1 : arg2;
                        case +1 -> arg1.isNan() ? arg2 : arg1;
                        default -> arg1;
                      }));

        } else if (BuiltinFloatFunctions.matchesFmin(calledFunctionName)) {
          // FIXME: Add a warning message for fmin(0.0,-0.0) and fmin(-0.0, 0.0)
          // The value is undefined and we pick -0.0 in those cases, but gcc will return the first
          // argument for `float` or `double` and the second for `long double`
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(
                      switch (arg1.compareWithTotalOrder(arg2)) {
                        case -1 -> arg1.isNan() ? arg2 : arg1;
                        case +1 -> arg2.isNan() ? arg1 : arg2;
                        default -> arg1;
                      }));

        } else if (BuiltinFloatFunctions.matchesSignbit(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> new NumericValue(arg.isNegative() ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesCopysign(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) -> new NumericValue(arg1.copySign(arg2)));

        } else if (BuiltinFloatFunctions.matchesFloatClassify(calledFunctionName)) {
          return handleBuiltinFunction1(
              calledFunctionName,
              parameterValues,
              (FloatValue arg) -> {
                int fpClass;
                if (arg.isNan()) {
                  fpClass = 0;
                } else if (arg.isInfinite()) {
                  fpClass = 1;
                } else if (arg.isZero()) {
                  fpClass = 2;
                } else if (arg.isSubnormal()) {
                  fpClass = 3;
                } else {
                  // Normal number
                  fpClass = 4;
                }
                return new NumericValue(fpClass);
              });

        } else if (BuiltinFloatFunctions.matchesModf(calledFunctionName)) {
          // We only need the return value and can ignore the integer part that needs to be written
          // to the pointer in the 2nd argument
          if (parameterValues.size() == 2) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              FloatValue arg =
                  castToFloat(
                      machineModel,
                      BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName),
                      (NumericValue) value);

              if (arg.isInfinite()) {
                // Return zero if the number is infinite
                return new NumericValue(
                    arg.isNegative()
                        ? FloatValue.negativeZero(arg.getFormat())
                        : FloatValue.zero(arg.getFormat()));
              } else {
                // Otherwise, get the fractional part
                return new NumericValue(arg.modulo(FloatValue.one(arg.getFormat())));
              }
            }
          }

        } else if (BuiltinFloatFunctions.matchesFremainder(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) -> new NumericValue(arg1.remainder(arg2)));

        } else if (BuiltinFloatFunctions.matchesFmod(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) -> new NumericValue(arg1.modulo(arg2)));

        } else if (BuiltinFloatFunctions.matchesIsgreater(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(arg1.greaterThan(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsgreaterequal(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(arg1.greaterOrEqual(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsless(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) -> new NumericValue(arg1.lessThan(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIslessequal(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(arg1.lessOrEqual(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIslessgreater(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue(arg1.lessOrGreater(arg2) ? 1 : 0));

        } else if (BuiltinFloatFunctions.matchesIsunordered(calledFunctionName)) {
          return handleBuiltinFunction2(
              calledFunctionName,
              parameterValues,
              (FloatValue arg1, FloatValue arg2) ->
                  new NumericValue((arg1.isNan() || arg2.isNan()) ? 1 : 0));
        }
      }
    }
    // Return 'unknown' if it's not a builtin function that we support
    return Value.UnknownValue.getInstance();
  }

  private boolean isUnspecifiedType(CType pType) {
    return pType instanceof CSimpleType
        && ((CSimpleType) pType).getType() == CBasicType.UNSPECIFIED;
  }

  @Override
  public Value visit(CCharLiteralExpression pE) throws UnrecognizedCodeException {
    return new NumericValue((long) pE.getCharacter());
  }

  @Override
  public Value visit(CFloatLiteralExpression pE) throws UnrecognizedCodeException {
    return new NumericValue(pE.getValue());
  }

  @Override
  public Value visit(CIntegerLiteralExpression pE) throws UnrecognizedCodeException {
    return new NumericValue(pE.getValue());
  }

  @Override
  public Value visit(CImaginaryLiteralExpression pE) throws UnrecognizedCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public Value visit(CStringLiteralExpression pE) throws UnrecognizedCodeException {
    return Value.UnknownValue.getInstance();
  }

  @Override
  public Value visit(final CTypeIdExpression pE) {
    final TypeIdOperator idOperator = pE.getOperator();
    final CType innerType = pE.getType();

    switch (idOperator) {
      case SIZEOF -> {
        if (innerType.hasKnownConstantSize()) {
          BigInteger size = machineModel.getSizeof(innerType);
          return new NumericValue(size);
        }
        return Value.UnknownValue.getInstance();
      }
      case ALIGNOF -> {
        return new NumericValue(machineModel.getAlignof(innerType));
      }
      default -> {
        // TODO support more operators
        return Value.UnknownValue.getInstance();
      }
    }
  }

  /**
   * Computes size of a type. Result can be an unknown value! Prefer this method over {@link
   * MachineModel#getSizeof(CType)} because it works for variable-length arrays if the current
   * visitor instance is able to evalue the length expression.
   */
  protected Value sizeof(CType pType) throws UnrecognizedCodeException {
    return new SizeofVisitor().evaluateSizeof(pType);
  }

  private final class SizeofVisitor extends BaseSizeofVisitor<UnrecognizedCodeException> {

    private boolean sizeKnown = true;

    SizeofVisitor() {
      super(machineModel);
    }

    Value evaluateSizeof(CType pType) throws UnrecognizedCodeException {
      BigInteger size = machineModel.getSizeof(pType, this);
      if (sizeKnown) {
        return new NumericValue(size);
      } else {
        return Value.UnknownValue.getInstance();
      }
    }

    @Override
    protected BigInteger evaluateArrayLength(CExpression pLength, CArrayType pArrayType)
        throws UnrecognizedCodeException {
      // This covers the most common and relevant case, handling other cases could be unsound
      // because we would need to use variable values from the declaration time of the array.
      // cf. https://gitlab.com/sosy-lab/software/cpachecker/-/issues/1146
      if (pLength instanceof CIdExpression idExpression
          && idExpression.getExpressionType().isConst()) {
        Value lengthValue = pLength.accept(AbstractExpressionValueVisitor.this);
        if (lengthValue.isNumericValue()) {
          return lengthValue.asNumericValue().bigIntegerValue();
        }
      }

      sizeKnown = false;
      return BigInteger.ZERO; // dummy value, will be ignored in evaluateSizeof()
    }
  }

  @Override
  public Value visit(CIdExpression idExp) throws UnrecognizedCodeException {
    if (idExp.getDeclaration() instanceof CEnumerator enumerator) {
      return new NumericValue(enumerator.getValue());
    }

    return evaluateCIdExpression(idExp);
  }

  @Override
  public Value visit(CUnaryExpression unaryExpression) throws UnrecognizedCodeException {
    final UnaryOperator unaryOperator = unaryExpression.getOperator();
    final CExpression unaryOperand = unaryExpression.getOperand();

    return switch (unaryOperator) {
      case SIZEOF -> sizeof(unaryOperand.getExpressionType());

      case ALIGNOF -> new NumericValue(machineModel.getAlignof(unaryOperand.getExpressionType()));

      case AMPER -> {
        // We can handle &((struct foo*)0)->field
        if (unaryOperand instanceof CFieldReference fieldRef
            && fieldRef.isPointerDereference()
            && fieldRef.getFieldOwner() instanceof CCastExpression cast
            && cast.getCastType().getCanonicalType() instanceof CPointerType pointerType
            && pointerType.getType().getCanonicalType() instanceof CCompositeType structType) {
          Value baseAddress = cast.getOperand().accept(this);
          if (baseAddress.isNumericValue()) {
            Optional<BigInteger> offset =
                machineModel.getFieldOffsetInBytes(structType, fieldRef.getFieldName());
            if (offset.isPresent()) {
              yield new NumericValue(
                  baseAddress.asNumericValue().bigIntegerValue().add(offset.orElseThrow()));
            }
          }
        }
        yield Value.UnknownValue.getInstance();
      }

      default -> {
        final Value value = unaryOperand.accept(this);

        if (value.isUnknown()) {
          yield Value.UnknownValue.getInstance();
        }

        if (value instanceof SymbolicValue) {
          final CType expressionType = unaryExpression.getExpressionType();
          final CType operandType = unaryOperand.getExpressionType();

          yield createSymbolicExpression(value, operandType, unaryOperator, expressionType);

        } else if (!value.isNumericValue()) {
          logger.logf(
              Level.FINE, "Invalid argument %s for unary operator %s.", value, unaryOperator);
          yield Value.UnknownValue.getInstance();
        }

        final NumericValue numericValue = (NumericValue) value;
        yield switch (unaryOperator) {
          case MINUS -> numericValue.negate();
          case TILDE -> new NumericValue(~numericValue.longValue());
          default -> throw new AssertionError("unknown operator: " + unaryOperator);
        };
      }
    };
  }

  private Value createSymbolicExpression(
      Value pValue, CType pOperandType, UnaryOperator pUnaryOperator, CType pExpressionType) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression operand = factory.asConstant(pValue, pOperandType);

    return switch (pUnaryOperator) {
      case MINUS -> factory.negate(operand, pExpressionType);
      case TILDE -> factory.binaryNot(operand, pExpressionType);
      default -> throw new AssertionError("Unhandled unary operator " + pUnaryOperator);
    };
  }

  @Override
  public Value visit(CPointerExpression pointerExpression) throws UnrecognizedCodeException {
    return evaluateCPointerExpression(pointerExpression);
  }

  @Override
  public Value visit(CFieldReference fieldReferenceExpression) throws UnrecognizedCodeException {
    return evaluateCFieldReference(fieldReferenceExpression);
  }

  @Override
  public Value visit(CArraySubscriptExpression pE) throws UnrecognizedCodeException {
    return evaluateCArraySubscriptExpression(pE);
  }

  @Override
  public Value visit(JCharLiteralExpression pE) {
    return new NumericValue((long) pE.getCharacter());
  }

  @Override
  public Value visit(JBinaryExpression pE) {
    JBinaryExpression.BinaryOperator binaryOperator = pE.getOperator();
    JExpression lVarInBinaryExp = pE.getOperand1();
    JExpression rVarInBinaryExp = pE.getOperand2();
    JType lValType = lVarInBinaryExp.getExpressionType();
    JType rValType = rVarInBinaryExp.getExpressionType();
    JType expressionType = pE.getExpressionType();

    // Get the concrete values of the lefthandside and righthandside
    final Value lValue = lVarInBinaryExp.accept(this);
    if (lValue.isUnknown()) {
      return UnknownValue.getInstance();
    }

    final Value rValue = rVarInBinaryExp.accept(this);
    if (rValue.isUnknown()) {
      return UnknownValue.getInstance();
    }

    try {
      return calculateBinaryOperation(
          binaryOperator, lValue, lValType, rValue, rValType, expressionType, pE);

    } catch (IllegalOperationException e) {
      logger.logUserException(Level.SEVERE, e, pE.getFileLocation().toString());
      return UnknownValue.getInstance();
    }
  }

  private Value calculateBinaryOperation(
      JBinaryExpression.BinaryOperator pOperator,
      Value pLValue,
      JType pLType,
      Value pRValue,
      JType pRType,
      JType pExpType,
      JBinaryExpression pExpression)
      throws IllegalOperationException {

    assert !pLValue.isUnknown() && !pRValue.isUnknown();

    if (pLValue instanceof SymbolicValue || pRValue instanceof SymbolicValue) {
      final JType expressionType = pExpression.getExpressionType();

      return createSymbolicExpression(
          pLValue, pLType, pRValue, pRType, pOperator, expressionType, expressionType);

    } else if (pLValue instanceof NumericValue) {

      assert pRValue instanceof NumericValue;
      assert pLType instanceof JSimpleType && pRType instanceof JSimpleType;
      assert pExpType instanceof JSimpleType;

      if (isFloatType(pLType) || isFloatType(pRType)) {
        return calculateFloatOperation(
            (NumericValue) pLValue,
            (NumericValue) pRValue,
            pOperator,
            (JSimpleType) pLType,
            (JSimpleType) pRType);

      } else {
        return calculateIntegerOperation(
            (NumericValue) pLValue,
            (NumericValue) pRValue,
            pOperator,
            (JSimpleType) pLType,
            (JSimpleType) pRType);
      }

    } else if (pLValue instanceof BooleanValue) {
      assert pRValue instanceof BooleanValue;

      boolean lVal = ((BooleanValue) pLValue).isTrue();
      boolean rVal = ((BooleanValue) pRValue).isTrue();

      return calculateBooleanOperation(lVal, rVal, pOperator);

    } else if (pOperator == JBinaryExpression.BinaryOperator.EQUALS
        || pOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS) {
      // true if EQUALS & (lValue == rValue) or if NOT_EQUALS & (lValue != rValue). False
      // otherwise. This is equivalent to an XNOR.
      return calculateComparison(pLValue, pRValue, pOperator);
    }

    return UnknownValue.getInstance();
  }

  private Value createSymbolicExpression(
      Value pLeftValue,
      JType pLeftType,
      Value pRightValue,
      JType pRightType,
      JBinaryExpression.BinaryOperator pOperator,
      JType pExpressionType,
      JType pCalculationType) {
    assert pLeftValue instanceof SymbolicValue || pRightValue instanceof SymbolicValue;

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression leftOperand = factory.asConstant(pLeftValue, pLeftType);
    SymbolicExpression rightOperand = factory.asConstant(pRightValue, pRightType);

    return switch (pOperator) {
      case PLUS -> factory.add(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MINUS -> factory.minus(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MULTIPLY ->
          factory.multiply(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case DIVIDE -> factory.divide(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MODULO -> factory.modulo(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_LEFT ->
          factory.shiftLeft(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_RIGHT_SIGNED ->
          factory.shiftRightSigned(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_RIGHT_UNSIGNED ->
          factory.shiftRightUnsigned(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_AND ->
          factory.binaryAnd(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LOGICAL_AND ->
          factory.logicalAnd(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_OR ->
          factory.binaryOr(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LOGICAL_OR ->
          factory.logicalOr(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_XOR, LOGICAL_XOR ->
          factory.binaryXor(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case EQUALS -> factory.equal(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case NOT_EQUALS ->
          factory.notEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_THAN ->
          factory.lessThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_EQUAL ->
          factory.lessThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_THAN ->
          factory.greaterThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_EQUAL ->
          factory.greaterThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      default -> throw new AssertionError("Unhandled binary operation " + pOperator);
    };
  }

  /*
   * Calculates the result of the given operation for the given integer values.
   * The given values have to be of a Java integer type, that is long, int, short, or byte.
   */
  private Value calculateIntegerOperation(
      NumericValue pLeftValue,
      NumericValue pRightValue,
      JBinaryExpression.BinaryOperator pBinaryOperator,
      JSimpleType pLeftType,
      JSimpleType pRightType)
      throws IllegalOperationException {

    checkNotNull(pLeftType);
    checkNotNull(pRightType);

    final long lVal = pLeftValue.longValue();
    final long rVal = pRightValue.longValue();

    switch (pBinaryOperator) {
      case PLUS,
          MINUS,
          DIVIDE,
          MULTIPLY,
          SHIFT_LEFT,
          BINARY_AND,
          BINARY_OR,
          BINARY_XOR,
          MODULO,
          SHIFT_RIGHT_SIGNED,
          SHIFT_RIGHT_UNSIGNED -> {
        long numResult =
            switch (pBinaryOperator) {
              case PLUS -> lVal + rVal;

              case MINUS -> lVal - rVal;

              case DIVIDE -> {
                if (rVal == 0) {
                  throw new IllegalOperationException("Division by zero: " + lVal + " / " + rVal);
                }

                yield lVal / rVal;
              }
              case MULTIPLY -> lVal * rVal;

              case BINARY_AND -> lVal & rVal;

              case BINARY_OR -> lVal | rVal;

              case BINARY_XOR -> lVal ^ rVal;

              case MODULO -> lVal % rVal;

              // shift operations' behaviour is determined by whether the left hand side value is of
              // type int or long, so we have to cast if the actual type is int.
              case SHIFT_LEFT -> {
                if (pLeftType != JSimpleType.LONG && pRightType != JSimpleType.LONG) {
                  final int intResult = ((int) lVal) << rVal;
                  yield intResult;
                } else {
                  yield lVal << rVal;
                }
              }
              case SHIFT_RIGHT_SIGNED -> {
                if (pLeftType != JSimpleType.LONG && pRightType != JSimpleType.LONG) {
                  final int intResult = ((int) lVal) >> rVal;
                  yield intResult;
                } else {
                  yield lVal >> rVal;
                }
              }
              case SHIFT_RIGHT_UNSIGNED -> {
                if (pLeftType != JSimpleType.LONG && pRightType != JSimpleType.LONG) {
                  final int intResult = ((int) lVal) >>> rVal;
                  yield intResult;
                } else {
                  yield lVal >>> rVal;
                }
              }
              default -> throw new AssertionError("Unhandled operator " + pBinaryOperator);
            };

        if (pLeftType != JSimpleType.LONG && pRightType != JSimpleType.LONG) {
          int intNumResult = (int) numResult;
          numResult = intNumResult;
        }

        return new NumericValue(numResult);
      }
      case EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_EQUAL, LESS_THAN, LESS_EQUAL -> {
        final boolean result =
            switch (pBinaryOperator) {
              case EQUALS -> (lVal == rVal);
              case NOT_EQUALS -> (lVal != rVal);
              case GREATER_THAN -> (lVal > rVal);
              case GREATER_EQUAL -> (lVal >= rVal);
              case LESS_THAN -> (lVal < rVal);
              case LESS_EQUAL -> (lVal <= rVal);
              default -> throw new AssertionError("Unhandled operation " + pBinaryOperator);
            };
        return BooleanValue.valueOf(result);
      }
      default -> {
        // TODO check which cases can be handled
        return UnknownValue.getInstance();
      }
    }
  }

  /*
   * Calculates the result of the given operation for the given floating point values.
   * The given values have to be of Java types float or double.
   */
  private Value calculateFloatOperation(
      NumericValue pLeftValue,
      NumericValue pRightValue,
      JBinaryExpression.BinaryOperator pBinaryOperator,
      JSimpleType pLeftOperand,
      JSimpleType pRightOperand)
      throws IllegalOperationException {

    final double lVal;
    final double rVal;

    if (pLeftOperand != JSimpleType.DOUBLE && pRightOperand != JSimpleType.DOUBLE) {
      lVal = pLeftValue.floatValue();
      rVal = pRightValue.floatValue();
    } else {
      lVal = pLeftValue.doubleValue();
      rVal = pRightValue.doubleValue();
    }

    switch (pBinaryOperator) {
      case PLUS, MINUS, DIVIDE, MULTIPLY, MODULO -> {
        return switch (pBinaryOperator) {
          case PLUS -> new NumericValue(lVal + rVal);

          case MINUS -> new NumericValue(lVal - rVal);

          case DIVIDE -> {
            if (rVal == 0) {
              throw new IllegalOperationException("Division by zero: " + lVal + " / " + rVal);
            }
            yield new NumericValue(lVal / rVal);
          }

          case MULTIPLY -> new NumericValue(lVal * rVal);

          case MODULO -> new NumericValue(lVal % rVal);

          default ->
              throw new AssertionError(
                  "Unsupported binary operation " + pBinaryOperator + " on double values");
        };
      }
      case EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_EQUAL, LESS_THAN, LESS_EQUAL -> {
        final boolean result =
            switch (pBinaryOperator) {
              case EQUALS -> (lVal == rVal);
              case NOT_EQUALS -> (lVal != rVal);
              case GREATER_THAN -> (lVal > rVal);
              case GREATER_EQUAL -> (lVal >= rVal);
              case LESS_THAN -> (lVal < rVal);
              case LESS_EQUAL -> (lVal <= rVal);
              default ->
                  throw new AssertionError(
                      "Unsupported binary operation "
                          + pBinaryOperator
                          + " on floating point values");
            };
        // return 1 if expression holds, 0 otherwise
        return BooleanValue.valueOf(result);
      }
      default -> {
        // TODO check which cases can be handled
        return UnknownValue.getInstance();
      }
    }
  }

  private Value calculateBooleanOperation(
      boolean lVal, boolean rVal, JBinaryExpression.BinaryOperator operator) {

    return switch (operator) {
      case CONDITIONAL_AND,
          LOGICAL_AND -> // we do not care about sideeffects through evaluation of the
          // righthandside at this point -
          // this must be handled
          // earlier
          BooleanValue.valueOf(lVal && rVal);
      case CONDITIONAL_OR,
          LOGICAL_OR -> // we do not care about sideeffects through evaluation of the
          // righthandside at this point
          BooleanValue.valueOf(lVal || rVal);
      case LOGICAL_XOR -> BooleanValue.valueOf(lVal ^ rVal);
      case EQUALS -> BooleanValue.valueOf(lVal == rVal);
      case NOT_EQUALS -> BooleanValue.valueOf(lVal != rVal);
      default ->
          throw new AssertionError("Unhandled operator " + operator + " for boolean expression");
    };
  }

  private Value calculateComparison(
      Value pLeftValue, Value pRightValue, JBinaryExpression.BinaryOperator pOperator) {
    assert pOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS
        || pOperator == JBinaryExpression.BinaryOperator.EQUALS;

    return BooleanValue.valueOf(
        pOperator != JBinaryExpression.BinaryOperator.EQUALS ^ pLeftValue.equals(pRightValue));
  }

  @Override
  public Value visit(JIdExpression idExp) {

    ASimpleDeclaration decl = idExp.getDeclaration();

    // Java IdExpression could not be resolved
    if (decl == null) {
      return UnknownValue.getInstance();
    }

    if (decl instanceof JFieldDeclaration && !((JFieldDeclaration) decl).isStatic()) {
      missingFieldAccessInformation = true;

      return UnknownValue.getInstance();
    }

    return evaluateJIdExpression(idExp);
  }

  @Override
  public Value visit(JUnaryExpression unaryExpression) {

    JUnaryExpression.UnaryOperator unaryOperator = unaryExpression.getOperator();
    JExpression unaryOperand = unaryExpression.getOperand();
    final Value valueObject = unaryOperand.accept(this);

    // possible error msg if no case fits
    final String errorMsg =
        "Invalid argument [" + valueObject + "] for unary operator [" + unaryOperator + "].";

    if (valueObject.isUnknown()) {
      return UnknownValue.getInstance();

    } else if (valueObject.isNumericValue()) {
      NumericValue value = (NumericValue) valueObject;

      return switch (unaryOperator) {
        case MINUS -> value.negate();
        case COMPLEMENT -> evaluateComplement(unaryOperand, value);
        case PLUS -> value;
        default -> {
          logger.log(Level.FINE, errorMsg);
          yield UnknownValue.getInstance();
        }
      };

    } else if (valueObject instanceof BooleanValue
        && unaryOperator == JUnaryExpression.UnaryOperator.NOT) {
      return ((BooleanValue) valueObject).negate();

    } else if (valueObject instanceof SymbolicValue) {
      final JType expressionType = unaryExpression.getExpressionType();
      final JType operandType = unaryOperand.getExpressionType();

      return createSymbolicExpression(valueObject, operandType, unaryOperator, expressionType);

    } else {
      logger.log(Level.FINE, errorMsg);
      return UnknownValue.getInstance();
    }
  }

  private Value createSymbolicExpression(
      Value pValue,
      JType pOperandType,
      JUnaryExpression.UnaryOperator pUnaryOperator,
      JType pExpressionType) {

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression operand = factory.asConstant(pValue, pOperandType);

    return switch (pUnaryOperator) {
      case COMPLEMENT -> factory.binaryNot(operand, pExpressionType);
      case NOT -> factory.logicalNot(operand, pExpressionType);
      case MINUS -> factory.negate(operand, pExpressionType);
      case PLUS -> pValue;
    };
  }

  private Value evaluateComplement(JExpression pExpression, NumericValue value) {
    JType type = pExpression.getExpressionType();

    if (isIntegerType(type)) {
      return new NumericValue(~value.longValue());

    } else {
      logger.logf(Level.FINE, "Invalid argument %s for unary operator ~.", value);
      return Value.UnknownValue.getInstance();
    }
  }

  private static boolean isIntegerType(JType type) {
    return type instanceof JSimpleType && ((JSimpleType) type).isIntegerType();
  }

  private static boolean isFloatType(JType type) {
    return type instanceof JSimpleType && ((JSimpleType) type).isFloatingPointType();
  }

  @Override
  public Value visit(JIntegerLiteralExpression pE) {
    return new NumericValue(pE.asLong());
  }

  @Override
  public Value visit(JBooleanLiteralExpression pE) {
    return BooleanValue.valueOf(pE.getBoolean());
  }

  @Override
  public Value visit(JArraySubscriptExpression pJArraySubscriptExpression) {
    Value subscriptValue = pJArraySubscriptExpression.getSubscriptExpression().accept(this);
    JExpression arrayExpression = pJArraySubscriptExpression.getArrayExpression();
    Value idValue = arrayExpression.accept(this);

    if (!idValue.isUnknown() && subscriptValue.isNumericValue()) {
      ArrayValue innerMostArray = (ArrayValue) arrayExpression.accept(this);
      assert ((NumericValue) subscriptValue).longValue() >= 0
          && ((NumericValue) subscriptValue).longValue() <= Integer.MAX_VALUE;
      return innerMostArray.getValueAt((int) ((NumericValue) subscriptValue).longValue());

    } else {
      return Value.UnknownValue.getInstance();
    }
  }

  @Override
  public Value visit(JArrayLengthExpression pJArrayLengthExpression) {
    final JExpression arrayId = pJArrayLengthExpression.getQualifier();

    Value array = arrayId.accept(this);

    if (!array.isExplicitlyKnown()) {
      return UnknownValue.getInstance();

    } else {
      assert array instanceof ArrayValue;
      return new NumericValue(((ArrayValue) array).getArraySize());
    }
  }

  @Override
  public Value visit(JEnumConstantExpression pJEnumConstantExpression) {
    String fullName = pJEnumConstantExpression.getConstantName();

    return new EnumConstantValue(fullName);
  }

  @Override
  public Value visit(JCastExpression pJCastExpression) {
    JExpression operand = pJCastExpression.getOperand();
    JType castType = pJCastExpression.getCastType();

    return castJValue(
        operand.accept(this),
        operand.getExpressionType(),
        castType,
        logger,
        pJCastExpression.getFileLocation());
  }

  @Override
  public Value visit(JMethodInvocationExpression pAFunctionCallExpression) {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JClassInstanceCreation pJClassInstanceCreation) {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JStringLiteralExpression pPaStringLiteralExpression) {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JFloatLiteralExpression pJBooleanLiteralExpression) {
    return new NumericValue(pJBooleanLiteralExpression.getValue());
  }

  @Override
  public Value visit(JArrayCreationExpression pJArrayCreationExpression) {
    Value lastArrayValue;
    Value currentArrayValue = null;
    int currentDimension = 0;
    long concreteArraySize;
    final JType elementType = pJArrayCreationExpression.getExpressionType().getElementType();

    for (JExpression sizeExpression : Lists.reverse(pJArrayCreationExpression.getLength())) {
      currentDimension++;
      lastArrayValue = currentArrayValue;
      Value sizeValue = sizeExpression.accept(this);

      if (sizeValue.isUnknown()) {
        currentArrayValue = UnknownValue.getInstance();

      } else {
        concreteArraySize = ((NumericValue) sizeValue).longValue();
        currentArrayValue =
            createArrayValue(new JArrayType(elementType, currentDimension), concreteArraySize);

        if (lastArrayValue != null) {
          Value newValue = lastArrayValue;

          for (int index = 0; index < concreteArraySize; index++) {
            ((ArrayValue) currentArrayValue).setValue(newValue, index);

            // do not put the same ArrayValue instance in each slot
            // - this would mess up later value assignments because of call by reference
            if (lastArrayValue instanceof ArrayValue) {
              newValue = ArrayValue.copyOf((ArrayValue) lastArrayValue);
            }
          }
        }
      }
    }

    return currentArrayValue;
  }

  private ArrayValue createArrayValue(JArrayType pType, long pArraySize) {

    if (pArraySize < 0 || pArraySize > Integer.MAX_VALUE) {
      throw new AssertionError(
          "Trying to create array of size "
              + pArraySize
              + ". Java arrays can't be smaller than 0 or bigger than the max int value.");
    }

    return new ArrayValue(pType, (int) pArraySize);
  }

  @Override
  public Value visit(JArrayInitializer pJArrayInitializer) {
    final JArrayType arrayType = pJArrayInitializer.getExpressionType();
    final List<JExpression> initializerExpressions = pJArrayInitializer.getInitializerExpressions();

    // this list stores the values in the array's slots, in occurring order
    List<Value> slotValues = new ArrayList<>();

    for (JExpression currentExpression : initializerExpressions) {
      slotValues.add(currentExpression.accept(this));
    }

    return new ArrayValue(arrayType, slotValues);
  }

  @Override
  public Value visit(JVariableRunTimeType pJThisRunTimeType) {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JNullLiteralExpression pJNullLiteralExpression) {
    return NullValue.getInstance();
  }

  @Override
  public Value visit(JThisExpression pThisExpression) {
    return UnknownValue.getInstance();
  }

  /* abstract methods */

  protected abstract Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
      throws UnrecognizedCodeException;

  protected abstract Value evaluateCIdExpression(CIdExpression pCIdExpression)
      throws UnrecognizedCodeException;

  protected abstract Value evaluateJIdExpression(JIdExpression varName);

  protected abstract Value evaluateCFieldReference(CFieldReference pLValue)
      throws UnrecognizedCodeException;

  protected abstract Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
      throws UnrecognizedCodeException;

  /* additional methods */

  public String getFunctionName() {
    return functionName;
  }

  protected MachineModel getMachineModel() {
    return machineModel;
  }

  protected LogManagerWithoutDuplicates getLogger() {
    return logger;
  }

  /**
   * This method returns the value of an expression, reduced to match the type. This method handles
   * overflows and casts. If necessary warnings for the user are printed.
   *
   * @param pExp expression to evaluate
   * @param pTargetType the type of the left side of an assignment
   * @return if evaluation successful, then value, else null
   */
  public Value evaluate(final CRightHandSide pExp, final CType pTargetType)
      throws UnrecognizedCodeException {
    return castCValue(pExp.accept(this), pTargetType, machineModel, logger, pExp.getFileLocation());
  }

  /**
   * This method returns the value of an expression, reduced to match the given target type. This
   * method handles overflows and casts. If necessary warnings for the user are printed.
   *
   * @param pExp the expression to evaluate
   * @param pTargetType the target type of the assignment (the type of the left side of the
   *     assignment)
   * @return the corresponding value of the given expression, if the evaluation was successful.
   *     <code>Null</code>, otherwise
   */
  public Value evaluate(final JRightHandSide pExp, final JType pTargetType) {
    return castJValue(
        pExp.accept(this),
        (JType) pExp.getExpressionType(),
        pTargetType,
        logger,
        pExp.getFileLocation());
  }

  /**
   * This method returns the input-value, casted to match the type. If the value matches the type,
   * it is returned unchanged. This method handles overflows and print warnings for the user.
   * Example: This method is called, when an value of type 'integer' is assigned to a variable of
   * type 'char'.
   *
   * @param value will be casted.
   * @param targetType value will be casted to targetType.
   * @param machineModel contains information about types
   * @param logger for logging
   * @param fileLocation the location of the corresponding code in the source file
   * @return the casted Value
   */
  public static Value castCValue(
      @NonNull final Value value,
      final CType targetType,
      final MachineModel machineModel,
      final LogManagerWithoutDuplicates logger,
      final FileLocation fileLocation) {

    if (!value.isExplicitlyKnown()) {
      return castIfSymbolic(value, targetType);
    }

    // For now can only cast numeric value's
    if (!value.isNumericValue()) {
      logger.logf(
          Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
      return value;
    }
    NumericValue numericValue = (NumericValue) value;

    CType type = targetType.getCanonicalType();
    final int size;
    if (type instanceof CSimpleType st) {
      size = machineModel.getSizeofInBits(st);
    } else if (type instanceof CBitFieldType) {
      size = ((CBitFieldType) type).getBitFieldSize();
      type = ((CBitFieldType) type).getType();

    } else {
      return value;
    }

    return castNumeric(numericValue, type, machineModel, size);
  }

  private static Value castNumeric(
      @NonNull final NumericValue numericValue,
      final CType type,
      final MachineModel machineModel,
      final int size) {

    if (!(type instanceof CSimpleType)) {
      return numericValue;
    }

    final CSimpleType st = (CSimpleType) type;

    switch (st.getType()) {
      case BOOL -> {
        return convertToBool(numericValue);
      }
      case INT128, INT, CHAR -> {
        // TODO: look more closely at the INT/CHAR cases, especially at the loggedEdges stuff
        // TODO: check for overflow(source larger than the highest number we can store in target
        // etc.)

        boolean targetIsSigned = machineModel.isSigned(st);
        BigInteger integerValue;

        // Convert the value to integer
        if (numericValue.hasFloatType()) {
          // Casting from a floating point value to BigInteger
          Optional<BigInteger> maybeInteger = numericValue.getFloatValue().toInteger();
          if (maybeInteger.isEmpty()) {
            // If the value was NaN or Infinity the result of the conversion is undefined
            return UnknownValue.getInstance();
          } else {
            integerValue = maybeInteger.orElseThrow();
          }
        } else {
          // Casting from Rational or one of the integer types
          integerValue = numericValue.bigIntegerValue();
        }

        // Calculate bounds for overflow
        final BigInteger maxValue = BigInteger.ONE.shiftLeft(size); // 2^size
        BigInteger signedUpperBound;
        BigInteger signedLowerBound;
        if (targetIsSigned) {
          // signed value must be put in interval [-(maxValue/2), (maxValue/2)-1]
          // upper bound maxValue / 2 - 1
          signedUpperBound = maxValue.divide(BigInteger.valueOf(2)).subtract(BigInteger.ONE);
          // lower bound -maxValue / 2
          signedLowerBound = maxValue.divide(BigInteger.valueOf(2)).negate();
        } else {
          signedUpperBound = maxValue.subtract(BigInteger.ONE);
          signedLowerBound = BigInteger.ZERO;
        }

        BigInteger result;

        // Check for overflows
        if (numericValue.hasFloatType()) {
          // Casting from a floating point value
          if (isGreaterThan(integerValue, signedUpperBound)
              || isLessThan(integerValue, signedLowerBound)) {
            // If the number does not fit into the target type the result is undefined
            return UnknownValue.getInstance();
          } else {
            result = integerValue;
          }
        } else {
          // Casting from Rational or an integer type
          result = integerValue.remainder(maxValue); // shrink to number of bits

          if (isGreaterThan(result, signedUpperBound)) {
            // if result overflows, let it 'roll around' and add overflow to lower bound
            result = result.subtract(maxValue);
          } else if (isLessThan(result, signedLowerBound)) {
            result = result.add(maxValue);
          }
        }

        // transform result to a long and fail if it doesn't fit
        if (size < SIZE_OF_JAVA_LONG || (size == SIZE_OF_JAVA_LONG && targetIsSigned)) {
          return new NumericValue(result.longValueExact());
        } else {
          return new NumericValue(result);
        }
      }

      case FLOAT, DOUBLE, FLOAT128 -> {
        FloatValue.Format target;

        // Find the target format
        final int bitPerByte = machineModel.getSizeofCharInBits();
        if (size == machineModel.getSizeofFloat() * bitPerByte) {
          target = FloatValue.Format.Float32;
        } else if (size == machineModel.getSizeofDouble() * bitPerByte) {
          target = FloatValue.Format.Float64;
        } else if (size == machineModel.getSizeofLongDouble() * bitPerByte) {
          // Must be Linux32 or Linux64, otherwise the second clause would have matched
          target = FloatValue.Format.Float80;
        } else if (size == machineModel.getSizeofFloat128() * bitPerByte) {
          target = FloatValue.Format.Float128;
        } else {
          // Unsupported target format
          throw new AssertionError(
              String.format(
                  "Unsupported target format. Value `%s` with bit width %d can't be cast to type"
                      + "`%s`",
                  numericValue, size, st.getType()));
        }

        Number result;

        // Convert to target format
        // TODO: Add warnings for lossy conversions?
        if (numericValue.hasFloatType()) {
          // Casting from a floating point value
          if (numericValue.getNumber() instanceof FloatValue floatValue) {
            // Already a FloatValue
            // We just need to adjust the precision
            result = floatValue.withPrecision(target);
          } else {
            // Either Double or Float
            // Cast to double and then convert
            result = FloatValue.fromDouble(numericValue.doubleValue()).withPrecision(target);
          }
        } else if (numericValue.hasIntegerType()) {
          // Casting from an integer
          result = FloatValue.fromInteger(target, numericValue.bigIntegerValue());
        } else if (numericValue.getNumber() instanceof Rational rationalValue) {
          // Casting from a rational
          result = FloatValue.fromRational(target, rationalValue);
        } else {
          // Unsupported value type
          throw new AssertionError(
              String.format(
                  "Unsupported type. Value `%s` has type `%s`, but only integers, floating points"
                      + "and rationals are allowed.",
                  numericValue, numericValue.getNumber().getClass().getSimpleName()));
        }

        return new NumericValue(result);
      }

      default -> throw new AssertionError("Unhandled type: " + type);
    }
  }

  private static Value convertToBool(final NumericValue pValue) {
    Number n = pValue.getNumber();
    if (isBooleanFalseRepresentation(n)) {
      return new NumericValue(0);
    } else {
      return new NumericValue(1);
    }
  }

  private static boolean isBooleanFalseRepresentation(final Number n) {
    return ((n instanceof Float || n instanceof Double) && 0 == n.doubleValue())
        || (n instanceof BigInteger && BigInteger.ZERO.equals(n))
        || (n instanceof FloatValue && ((FloatValue) n).isZero())
        || 0 == n.longValue();
  }

  /** Returns whether first integer is greater than second integer */
  private static boolean isGreaterThan(BigInteger i1, BigInteger i2) {
    return i1.compareTo(i2) > 0;
  }

  /** Returns whether first integer is less than second integer */
  private static boolean isLessThan(BigInteger i1, BigInteger i2) {
    return i1.compareTo(i2) < 0;
  }

  private static Value castIfSymbolic(Value pValue, Type pTargetType) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    if (pValue instanceof SymbolicValue
        && (pTargetType instanceof JSimpleType || pTargetType instanceof CSimpleType)) {

      return factory.cast((SymbolicValue) pValue, pTargetType);
    }

    // If the value is not symbolic, just return it.
    return pValue;
  }

  /**
   * Casts the given value to the specified Java type. This also handles overflows.
   *
   * <p>In Java, numeric values are the only primitive types that can be cast. In consequence, all
   * values of other primitive types (and not explicitly known values) will simply be returned in
   * their original form.
   *
   * @param value the value to cast
   * @param sourceType the original type of the given value
   * @param targetType the type the given value should be cast to
   * @param logger the logger error and warning messages will be logged to
   * @param fileLocation the location of the corresponding code in the source file
   * @return the cast value, if a cast from the source to the target type is possible. Otherwise,
   *     the given value will be returned without a change
   */
  public static Value castJValue(
      @NonNull final Value value,
      JType sourceType,
      JType targetType,
      final LogManagerWithoutDuplicates logger,
      final FileLocation fileLocation) {

    if (!value.isExplicitlyKnown()) {
      return castIfSymbolic(value, targetType);
    }

    // Other than symbolic values, we can only cast numeric values, for now.
    if (!value.isNumericValue()) {
      logger.logf(
          Level.FINE, "Can not cast Java value %s to %s", value.toString(), targetType.toString());
      return value;
    }

    NumericValue numericValue = (NumericValue) value;

    if (targetType instanceof JSimpleType st) {
      if (isIntegerType(sourceType)) {
        long longValue = numericValue.longValue();

        return createValue(longValue, st);

      } else if (isFloatType(sourceType)) {
        double doubleValue = numericValue.doubleValue();

        return createValue(doubleValue, st);

      } else {
        throw new AssertionError(
            "Cast from " + sourceType + " to " + targetType + " not possible.");
      }
    } else {
      return value; // TODO handle casts between object types
    }
  }

  private static Value createValue(long value, JSimpleType targetType) {
    switch (targetType) {
      case BYTE -> {
        return new NumericValue((byte) value);
      }
      case CHAR -> {
        char castedValue = (char) value;
        return new NumericValue((int) castedValue);
      }
      case SHORT -> {
        return new NumericValue((short) value);
      }
      case INT -> {
        return new NumericValue((int) value);
      }
      case LONG -> {
        return new NumericValue(value);
      }
      case FLOAT -> {
        return new NumericValue((float) value);
      }
      case DOUBLE -> {
        return new NumericValue((double) value);
      }
      default -> throw new AssertionError("Trying to cast to unsupported type " + targetType);
    }
  }

  private static Value createValue(double value, JSimpleType targetType) {
    return switch (targetType) {
      case BYTE -> new NumericValue((byte) value);
      case CHAR, SHORT -> new NumericValue((short) value);
      case INT -> new NumericValue((int) value);
      case LONG -> new NumericValue(value);
      case FLOAT -> new NumericValue((float) value);
      case DOUBLE -> new NumericValue(value);
      default -> throw new AssertionError("Trying to cast to unsupported type " + targetType);
    };
  }

  /**
   * Returns a numeric type that can be used to perform arithmetics on an instance of the type
   * directly, or null if none.
   *
   * <p>Most notably, CPointerType will be converted to the unsigned integer type of correct size.
   *
   * @param type the input type
   */
  public static CSimpleType getArithmeticType(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      // TODO: introduce an integer type of the right size, similar to intptr_t
      return CNumericTypes.INT;
    } else if (type instanceof CSimpleType) {
      return (CSimpleType) type;
    } else {
      return null;
    }
  }

  /**
   * Exception for illegal operations that cannot be reflected by the analysis methods return values
   * (For example division by zero)
   */
  protected static class IllegalOperationException extends CPAException {

    @Serial private static final long serialVersionUID = 5420891133452817345L;

    public IllegalOperationException(String msg) {
      super(msg);
    }

    public IllegalOperationException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
