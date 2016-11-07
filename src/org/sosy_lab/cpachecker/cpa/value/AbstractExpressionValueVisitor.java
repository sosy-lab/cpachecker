/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.primitives.UnsignedLongs;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.ArrayValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.EnumConstantValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;


/**
 * This Visitor implements an evaluation strategy
 * of simply typed expressions. An expression is
 * defined as simply typed iff it is not an
 * array type (vgl {@link CArrayType}), a struct or
 * union type (vgl {@link CComplexType}),
 * an imaginary type (vgl {@link CImaginaryLiteralExpression}),
 * or a pointer type (vgl {@link CPointerType}).
 * The key distinction between these types and simply typed types is,
 * that a value of simply typed types can be represented as a numerical
 * value without losing information.
 *
 * Furthermore, the visitor abstracts from using abstract states
 * to get values stored in the memory of a program.
 */
public abstract class AbstractExpressionValueVisitor
    extends DefaultCExpressionVisitor<Value, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Value, UnrecognizedCCodeException>,
    JRightHandSideVisitor<Value, RuntimeException>,
    JExpressionVisitor<Value, RuntimeException> {

  /** length of type LONG in Java (in bit). */
  private final static int SIZE_OF_JAVA_LONG = 64;

  /** Length of type FLOAT in Java (in bit). */
  private static final int SIZE_OF_JAVA_FLOAT = 32;

  /** Length of type DOUBLE in Java (in bit). */
  private static final int SIZE_OF_JAVA_DOUBLE = 64;

  //private final ValueAnalysisState state;
  private final String functionName;
  private final MachineModel machineModel;


  // for logging
  private final LogManagerWithoutDuplicates logger;

  private boolean missingFieldAccessInformation = false;

  /** This Visitor returns the numeral value for an expression.
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   */
  public AbstractExpressionValueVisitor(String pFunctionName,
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {

    //this.state = pState;
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
  public Value visit(final CBinaryExpression pE) throws UnrecognizedCCodeException {
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
   * This method calculates the exact result for a binary operation.
   *
   * @param lVal evaluated first operand of binaryExpr
   * @param rVal evaluated second operand of binaryExpr
   * @param binaryExpr will be evaluated
   * @param machineModel information about types
   * @param logger for logging
   */
  public static Value calculateBinaryOperation(Value lVal, Value rVal,
      final CBinaryExpression binaryExpr,
      final MachineModel machineModel, final LogManagerWithoutDuplicates logger) {

    final BinaryOperator binaryOperator = binaryExpr.getOperator();
    final CType calculationType = binaryExpr.getCalculationType();

    lVal = castCValue(lVal, calculationType, machineModel, logger, binaryExpr.getFileLocation());
    if (binaryOperator != BinaryOperator.SHIFT_LEFT && binaryOperator != BinaryOperator.SHIFT_RIGHT) {
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
      rVal =
          castCValue(rVal, calculationType, machineModel, logger, binaryExpr.getFileLocation());
    }

    if (lVal instanceof SymbolicValue || rVal instanceof SymbolicValue) {
      return calculateSymbolicBinaryExpression(lVal, rVal, binaryExpr);
    }

    if (!lVal.isNumericValue() || !rVal.isNumericValue()) {
      logger.logf(Level.FINE, "Parameters to binary operation '%s %s %s' are no numeric values.", lVal, binaryOperator, rVal);
      return Value.UnknownValue.getInstance();
    }

    Value result;

    switch (binaryOperator) {
    case PLUS:
    case MINUS:
    case DIVIDE:
    case MODULO:
    case MULTIPLY:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR: {
      result = arithmeticOperation((NumericValue)lVal, (NumericValue)rVal, binaryOperator, calculationType, machineModel, logger);
      result = castCValue(result, binaryExpr.getExpressionType(), machineModel, logger, binaryExpr.getFileLocation());

      break;
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {
      result = booleanOperation((NumericValue) lVal,
          (NumericValue) rVal, binaryOperator, calculationType, machineModel, logger);
      // we do not cast here, because 0 and 1 should be small enough for every type.

      break;
    }

    default:
      throw new AssertionError("unhandled binary operator");
    }

    return result;
  }

  /**
   * Join a symbolic expression with something else using a binary expression.
   *
   * e.g. joining `a` and `5` with `+` will produce `a + 5`
   *
   * @param pLValue left hand side value
   * @param pRValue right hand side value
   * @param pExpression the binary expression with the operator
   * @return the calculated Value
   */
  public static Value calculateSymbolicBinaryExpression(Value pLValue, Value pRValue,
      final CBinaryExpression pExpression) {

    final BinaryOperator operator = pExpression.getOperator();

    final CType leftOperandType = pExpression.getOperand1().getExpressionType();
    final CType rightOperandType = pExpression.getOperand2().getExpressionType();
    final CType expressionType = pExpression.getExpressionType();
    final CType calculationType = pExpression.getCalculationType();

    return createSymbolicExpression(pLValue, leftOperandType, pRValue, rightOperandType, operator,
        expressionType, calculationType);
  }

  private static SymbolicValue createSymbolicExpression(Value pLeftValue, CType pLeftType, Value pRightValue,
      CType pRightType, CBinaryExpression.BinaryOperator pOperator, CType pExpressionType, CType pCalculationType) {

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression leftOperand;
    SymbolicExpression rightOperand;

    leftOperand = factory.asConstant(pLeftValue, pLeftType);
    rightOperand = factory.asConstant(pRightValue, pRightType);


    switch (pOperator) {
      case PLUS:
        return factory.add(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MINUS:
        return factory.minus(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MULTIPLY:
        return factory.multiply(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case DIVIDE:
        return factory.divide(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MODULO:
        return factory.modulo(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_LEFT:
        return factory.shiftLeft(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_RIGHT:
        return factory.shiftRightSigned(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_AND:
        return factory.binaryAnd(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_OR:
        return factory.binaryOr(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_XOR:
        return factory.binaryXor(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case EQUALS:
        return factory.equal(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case NOT_EQUALS:
        return factory.notEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_THAN:
        return factory.lessThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_EQUAL:
        return factory.lessThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_THAN:
        return factory.greaterThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      default:
        throw new AssertionError("Unhandled binary operation " + pOperator);
    }
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
  private static long arithmeticOperation(final long l, final long r,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel, final LogManager logger) {

    // special handling for UNSIGNED_LONGLONG (32 and 64bit), UNSIGNED_LONG (64bit)
    // because Java only has SIGNED_LONGLONG
    CSimpleType st = getArithmeticType(calculationType);
    if (st != null) {
      if (machineModel.getSizeofInBits(st) >= SIZE_OF_JAVA_LONG
          && st.isUnsigned()) {
        switch (op) {
        case DIVIDE:
          if (r == 0) {
            logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
            return 0;
          }
          return UnsignedLongs.divide(l, r);
        case MODULO:
          return UnsignedLongs.remainder(l, r);
        case SHIFT_RIGHT:
          /*
           * from http://docs.oracle.com/javase/tutorial/java/nutsandbolts/op3.html
           *
           * The unsigned right shift operator ">>>" shifts a zero
           * into the leftmost position, while the leftmost position
           * after ">>" depends on sign extension.
           */
          return l >>> r;
        default:
          // fall-through, calculation is done correct as SINGED_LONG_LONG
        }
      }
    }

    switch (op) {
    case PLUS:
      return l + r;
    case MINUS:
      return l - r;
    case DIVIDE:
      if (r == 0) {
        logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
        return 0;
      }
      return l / r;
    case MODULO:
      return l % r;
    case MULTIPLY:
      return l * r;
    case SHIFT_LEFT:
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
    case SHIFT_RIGHT:
      return l >> r;
    case BINARY_AND:
      return l & r;
    case BINARY_OR:
      return l | r;
    case BINARY_XOR:
      return l ^ r;

    default:
      throw new AssertionError("unknown binary operation: " + op);
    }

  }

  /**
   * Calculate an arithmetic operation on two double types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @param machineModel the machine model
   * @param logger logging
   * @return the resulting value
   */
  private static double arithmeticOperation(final double l, final double r,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel, final LogManager logger) {

    switch (op) {
    case PLUS:
      return l + r;
    case MINUS:
      return l - r;
    case DIVIDE:
      return l / r;
    case MODULO:
      return l % r;
    case MULTIPLY:
      return l * r;
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
      throw new AssertionError("trying to perform " + op + " on floating point operands");
    default:
      throw new AssertionError("unknown binary operation: " + op);
    }

  }

  /**
   * Calculate an arithmetic operation on two float types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @param machineModel the machine model
   * @param logger logging
   * @return the resulting value
   */
  private static float arithmeticOperation(final float l, final float r,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel, final LogManager logger) {

    switch (op) {
    case PLUS:
      return l + r;
    case MINUS:
      return l - r;
    case DIVIDE:
      return l / r;
    case MODULO:
      return l % r;
    case MULTIPLY:
      return l * r;
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
      throw new AssertionError("trying to perform " + op + " on floating point operands");
    default:
      throw new AssertionError("unknown binary operation: " + op);
    }

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
  private static Value arithmeticOperation(final NumericValue lNum, final NumericValue rNum,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel, final LogManager logger) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(Level.FINE, "unsupported type %s for result of binary operation %s", calculationType, op);
      return Value.UnknownValue.getInstance();
    }

    switch (type.getType()) {
      case INT: {
        // Both l and r must be of the same type, which in this case is INT, so we can cast to long.
        long lVal = lNum.getNumber().longValue();
        long rVal = rNum.getNumber().longValue();
        long result = arithmeticOperation(lVal, rVal, op, calculationType, machineModel, logger);
        return new NumericValue(result);
      }
      case DOUBLE: {
        double lVal = lNum.doubleValue();
        double rVal = rNum.doubleValue();
        double result = arithmeticOperation(lVal, rVal, op, calculationType, machineModel, logger);
        return new NumericValue(result);
      }
      case FLOAT: {
        float lVal = lNum.floatValue();
        float rVal = rNum.floatValue();
        float result = arithmeticOperation(lVal, rVal, op, calculationType, machineModel, logger);
        return new NumericValue(result);
      }
      default: {
        logger.logf(Level.FINE, "unsupported type for result of binary operation %s", type.toString());
        return Value.UnknownValue.getInstance();
      }
    }
  }

  private static Value booleanOperation(final NumericValue l, final NumericValue r,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel, final LogManager logger) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(Level.FINE, "unsupported type %s for result of binary operation %s", calculationType, op);
      return Value.UnknownValue.getInstance();
    }

    final int cmp;
    switch (type.getType()) {
      case INT: {
        CSimpleType canonicalType = type.getCanonicalType();
        int sizeInBits = machineModel.getSizeof(canonicalType) * machineModel.getSizeofCharInBits();
        if ((!machineModel.isSigned(canonicalType) && sizeInBits == SIZE_OF_JAVA_LONG)
            || sizeInBits > SIZE_OF_JAVA_LONG) {
          BigInteger leftBigInt = l.getNumber() instanceof BigInteger ? (BigInteger) l.getNumber() : BigInteger.valueOf(l.longValue());
          BigInteger rightBigInt = r.getNumber() instanceof BigInteger ? (BigInteger) r.getNumber() : BigInteger.valueOf(r.longValue());
          cmp = leftBigInt.compareTo(rightBigInt);
          break;
        }
        cmp = Long.compare(l.longValue(), r.longValue());
        break;
      }
      case FLOAT: {
        float lVal = l.floatValue();
        float rVal = r.floatValue();

        if (Float.isNaN(lVal) || Float.isNaN(rVal)) {
          return new NumericValue(op == BinaryOperator.NOT_EQUALS ? 1L : 0L);
        }

        cmp = Float.compare(lVal, rVal);
        break;
      }
      case DOUBLE: {
        double lVal = l.doubleValue();
        double rVal = r.doubleValue();

        if (Double.isNaN(lVal) || Double.isNaN(rVal)) {
          return new NumericValue(op == BinaryOperator.NOT_EQUALS ? 1L : 0L);
        }

        cmp = Double.compare(lVal, rVal);
        break;
      }
      default: {
        logger.logf(Level.FINE, "unsupported type %s for result of binary operation %s", type.toString(), op);
        return Value.UnknownValue.getInstance();
      }
    }

    // return 1 if expression holds, 0 otherwise
    return new NumericValue(matchBooleanOperation(op, cmp) ? 1L : 0L);
  }

  /** returns True, iff cmp fulfills the boolean operation. */
  private static boolean matchBooleanOperation(final BinaryOperator op, final int cmp) {
    switch (op) {
    case GREATER_THAN:
      return cmp > 0;
    case GREATER_EQUAL:
      return cmp >= 0;
    case LESS_THAN:
      return cmp < 0;
    case LESS_EQUAL:
      return cmp <= 0;
    case EQUALS:
      return cmp == 0;
    case NOT_EQUALS:
      return cmp != 0;
    default:
      throw new AssertionError("unknown binary operation: " + op);
    }
  }

  @Override
  public Value visit(CCastExpression pE) throws UnrecognizedCCodeException {
    return castCValue(pE.getOperand().accept(this), pE.getExpressionType(), machineModel,
        logger, pE.getFileLocation());
  }

  @Override
  public Value visit(CComplexCastExpression pE) throws UnrecognizedCCodeException {
    // evaluation of complex numbers is not supported by now
    return Value.UnknownValue.getInstance();
  }

  @Override
  public Value visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
    CExpression functionNameExp = pIastFunctionCallExpression.getFunctionNameExpression();

    // We only handle builtin functions
    if (functionNameExp instanceof CIdExpression) {
      String functionName = ((CIdExpression) functionNameExp).getName();

      if (BuiltinFunctions.isBuiltinFunction(functionName)) {
        CType functionType = BuiltinFunctions.getFunctionType(functionName);

        if (isUnspecifiedType(functionType)) {
          // unsupported formula
          return Value.UnknownValue.getInstance();
        }

        List<CExpression> parameterExpressions = pIastFunctionCallExpression.getParameterExpressions();
        List<Value> parameterValues = new ArrayList<>(parameterExpressions.size());

        for (CExpression currParamExp : parameterExpressions) {
          Value newValue = currParamExp.accept(this);

          parameterValues.add(newValue);
        }

        if (BuiltinFloatFunctions.matchesAbsolute(functionName)) {
          assert parameterValues.size() == 1;

          final CType parameterType = parameterExpressions.get(0).getExpressionType();
          final Value parameter = parameterValues.get(0);

          if (parameterType instanceof CSimpleType && !((CSimpleType) parameterType).isSigned()) {
            return parameter;

          } else if (parameter.isExplicitlyKnown()) {
            assert parameter.isNumericValue();
            final double absoluteValue = Math.abs(((NumericValue) parameter).doubleValue());

            // absolute value for INT_MIN is undefined behaviour, so we do not bother handling it
            // in any specific way
            return new NumericValue(absoluteValue);
          }

        } else if (BuiltinFloatFunctions.matchesHugeVal(functionName)
            || BuiltinFloatFunctions.matchesInfinity(functionName)) {

          assert parameterValues.isEmpty();
          if (BuiltinFloatFunctions.matchesHugeValFloat(functionName)
              || BuiltinFloatFunctions.matchesInfinityFloat(functionName)) {

            return new NumericValue(Float.POSITIVE_INFINITY);

          } else {
            assert BuiltinFloatFunctions.matchesInfinityDouble(functionName)
                || BuiltinFloatFunctions.matchesInfinityLongDouble(functionName)
                || BuiltinFloatFunctions.matchesHugeValDouble(functionName)
                || BuiltinFloatFunctions.matchesHugeValLongDouble(functionName)
                : " Unhandled builtin function for infinity: " + functionName;

            return new NumericValue(Double.POSITIVE_INFINITY);
          }

        } else if (BuiltinFloatFunctions.matchesNaN(functionName)) {
          assert parameterValues.isEmpty() || parameterValues.size() == 1;

          if (BuiltinFloatFunctions.matchesNaNFloat(functionName)) {
            return new NumericValue(Float.NaN);
          } else {
            assert BuiltinFloatFunctions.matchesNaNDouble(functionName)
                || BuiltinFloatFunctions.matchesNaNLongDouble(functionName)
                : "Unhandled builtin function for NaN: " + functionName;

            return new NumericValue(Double.NaN);
          }
        } else if (BuiltinFloatFunctions.matchesIsNaN(functionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
              case FLOAT:
                return Float.isNaN(numericValue.floatValue()) ? new NumericValue(1) : new NumericValue(0);
              case DOUBLE:
                return Double.isNaN(numericValue.doubleValue()) ? new NumericValue(1) : new NumericValue(0);
              default:
                break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesIsInfinity(functionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
              case FLOAT:
                return Float.isInfinite(numericValue.floatValue()) ? new NumericValue(1) : new NumericValue(0);
              case DOUBLE:
                return Double.isInfinite(numericValue.doubleValue()) ? new NumericValue(1) : new NumericValue(0);
              default:
                break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFinite(functionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
              case FLOAT:
                return Float.isInfinite(numericValue.floatValue()) ? new NumericValue(0) : new NumericValue(1);
              case DOUBLE:
                return Double.isInfinite(numericValue.doubleValue()) ? new NumericValue(0) : new NumericValue(1);
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFloor(functionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValue(((BigDecimal) number).setScale(0, BigDecimal.ROUND_FLOOR));
              } else if (number instanceof Float) {
                return new NumericValue(Math.floor(number.floatValue()));
              } else if (number instanceof Double) {
                return new NumericValue(Math.floor(number.doubleValue()));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return parameter;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesCeil(functionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValue(((BigDecimal) number).setScale(0, BigDecimal.ROUND_CEILING));
              } else if (number instanceof Float) {
                return new NumericValue(Math.ceil(number.floatValue()));
              } else if (number instanceof Double) {
                return new NumericValue(Math.ceil(number.doubleValue()));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return parameter;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesRound(functionName)
            || BuiltinFloatFunctions.matchesLround(functionName)
            || BuiltinFloatFunctions.matchesLlround(functionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);
            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValue(
                    ((BigDecimal) number).setScale(0, BigDecimal.ROUND_HALF_UP));
              } else if (number instanceof Float) {
                float f = number.floatValue();
                if (0 == f || Float.isInfinite(f)) {
                  return parameter;
                }
                return new NumericValue(Math.round(f));
              } else if (number instanceof Double) {
                double d = number.doubleValue();
                if (0 == d || Double.isInfinite(d)) {
                  return parameter;
                }
                return new NumericValue(Math.round(d));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return parameter;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesTrunc(functionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);
            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValue(((BigDecimal) number).setScale(0, BigDecimal.ROUND_DOWN));
              } else if (number instanceof Float) {
                float f = number.floatValue();
                if (0 == f || Float.isInfinite(f) || Float.isNaN(f)) {
                  // +/-0.0 and +/-INF and +/-NaN are returned unchanged
                  return parameter;
                }
                return new NumericValue(
                    BigDecimal.valueOf(number.floatValue())
                        .setScale(0, BigDecimal.ROUND_DOWN)
                        .floatValue());
              } else if (number instanceof Double) {
                double d = number.doubleValue();
                if (0 == d || Double.isInfinite(d) || Double.isNaN(d)) {
                  // +/-0.0 and +/-INF and +/-NaN are returned unchanged
                  return parameter;
                }
                return new NumericValue(
                    BigDecimal.valueOf(number.doubleValue())
                        .setScale(0, BigDecimal.ROUND_DOWN)
                        .doubleValue());
              } else if (number instanceof NumericValue.NegativeNaN) {
                return parameter;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFdim(functionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              Value result = fdim(op1, op2, functionName);
              if (!Value.UnknownValue.getInstance().equals(result)) {
                return result;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmax(functionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              return fmax(op1, op2);
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmin(functionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              return fmin(op1, op2);
            }
          }
        } else if (BuiltinFloatFunctions.matchesSignbit(functionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              Optional<Boolean> isNegative = isNegative(number);
              if (isNegative.isPresent()) {
                return new NumericValue(isNegative.get() ? 1 : 0);
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesCopysign(functionName)) {
          if (parameterValues.size() == 2) {
            Value target = parameterValues.get(0);
            Value source = parameterValues.get(1);
            if (target.isExplicitlyKnown() && source.isExplicitlyKnown()) {
              assert target.isNumericValue();
              assert source.isNumericValue();
              Number targetNumber = target.asNumericValue().getNumber();
              Number sourceNumber = source.asNumericValue().getNumber();
              Optional<Boolean> sourceNegative = isNegative(sourceNumber);
              Optional<Boolean> targetNegative = isNegative(targetNumber);
              if (sourceNegative.isPresent() && targetNegative.isPresent()) {
                if (sourceNegative.get() == targetNegative.get()) {
                  return new NumericValue(targetNumber);
                }
                return target.asNumericValue().negate();
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFloatClassify(functionName)) {

          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
              case FLOAT: {
                float v = numericValue.floatValue();
                if (Float.isNaN(v)) {
                  return new NumericValue(0);
                }
                if (Float.isInfinite(v)) {
                  return new NumericValue(1);
                }
                if (v == 0.0) {
                  return new NumericValue(2);
                }
                if (Float.toHexString(v).startsWith("0x0.")) {
                  return new NumericValue(3);
                }
                return new NumericValue(4);
              }
              case DOUBLE: {
                double v = numericValue.doubleValue();
                if (Double.isNaN(v)) {
                  return new NumericValue(0);
                }
                if (Double.isInfinite(v)) {
                  return new NumericValue(1);
                }
                if (v == 0.0) {
                  return new NumericValue(2);
                }
                if (Double.toHexString(v).startsWith("0x0.")) {
                  return new NumericValue(3);
                }
                return new NumericValue(4);
              }
              default:
                break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmodf(functionName)) {
          if (parameterValues.size() == 2) {
            throw new UnsupportedCCodeException(
                "Function 'fmodf' not supported due to missing pointer analysis.", null);
          }
        } else if (BuiltinFloatFunctions.matchesFremainder(functionName)) {
          if (parameterValues.size() == 2) {
            Value numer = parameterValues.get(0);
            Value denom = parameterValues.get(1);
            if (numer.isExplicitlyKnown() && denom.isExplicitlyKnown()) {
              NumericValue numerValue = numer.asNumericValue();
              NumericValue denomValue = denom.asNumericValue();
              switch (BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName).getType()) {
                case FLOAT:
                  {
                    float num = numerValue.floatValue();
                    float den = denomValue.floatValue();
                    if (Float.isNaN(num) || Float.isNaN(den) || Float.isInfinite(num) || den == 0) {
                      return new NumericValue(Float.NaN);
                    }
                    // TODO computations on float/double are imprecise! Use epsilon environment?
                    return new NumericValue((float)Math.IEEEremainder(num,  den));
                  }
                case DOUBLE:
                  {
                    double num = numerValue.doubleValue();
                    double den = denomValue.doubleValue();
                    if (Double.isNaN(num)
                        || Double.isNaN(den)
                        || Double.isInfinite(num)
                        || den == 0) {
                      return new NumericValue(Double.NaN);
                    }
                    // TODO computations on float/double are imprecise! Use epsilon environment?
                    return new NumericValue(Math.IEEEremainder(num,  den));
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmod(functionName)) {
          if (parameterValues.size() == 2) {
            Value numer = parameterValues.get(0);
            Value denom = parameterValues.get(1);
            if (numer.isExplicitlyKnown() && denom.isExplicitlyKnown()) {
              NumericValue numerValue = numer.asNumericValue();
              NumericValue denomValue = denom.asNumericValue();
              switch (BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName).getType()) {
                case FLOAT:
                  {
                    float num = numerValue.floatValue();
                    float den = denomValue.floatValue();
                    if (Float.isNaN(num) || Float.isNaN(den) || Float.isInfinite(num) || den == 0) {
                      return new NumericValue(Float.NaN);
                    }
                    if (num == 0 && den != 0) {
                      // keep the sign on +0 and -0
                      return numer;
                    }
                    // TODO computations on float/double are imprecise! Use epsilon environment?
                    return new NumericValue(num % den);
                  }
                case DOUBLE:
                  {
                    double num = numerValue.doubleValue();
                    double den = denomValue.doubleValue();
                    if (Double.isNaN(num)
                        || Double.isNaN(den)
                        || Double.isInfinite(num)
                        || den == 0) {
                      return new NumericValue(Double.NaN);
                    }
                    if (num == 0 && den != 0) {
                      // keep the sign on +0 and -0
                      return numer;
                    }
                    // TODO computations on float/double are imprecise! Use epsilon environment?
                    return new NumericValue(num % den);
                  }
                default:
                  break;
              }
            }
          }
        }
      }
    }

    return Value.UnknownValue.getInstance();
  }

  private Value fmax(Number pOp1, Number pOp2) {
    if (Double.isNaN(pOp1.doubleValue())
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() < 0)
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() > 0)) {
      return new NumericValue(pOp2);
    }
    if (Double.isNaN(pOp2.doubleValue())
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() < 0)
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() > 0)) {
      return new NumericValue(pOp1);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }

    if (op1bd.compareTo(op2bd) > 0) {
      return new NumericValue(op1bd);
    }
    return new NumericValue(op2bd);
  }

  private Value fmin(Number pOp1, Number pOp2) {
    if (Double.isNaN(pOp1.doubleValue())
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() > 0)
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() < 0)) {
      return new NumericValue(pOp2);
    }
    if (Double.isNaN(pOp2.doubleValue())
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() > 0)
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() < 0)) {
      return new NumericValue(pOp1);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }

    if (op1bd.compareTo(op2bd) < 0) {
      return new NumericValue(op1bd);
    }
    return new NumericValue(op2bd);
  }

  private Value fdim(Number pOp1, Number pOp2, String pFunctionName) {
    if (Double.isNaN(pOp1.doubleValue()) || Double.isNaN(pOp2.doubleValue())) {
      return new NumericValue(Double.NaN);
    }

    if (Double.isInfinite(pOp1.doubleValue())) {
      if (Double.isInfinite(pOp2.doubleValue())) {
        if (pOp1.doubleValue() > pOp2.doubleValue()) {
          return new NumericValue(pOp1.doubleValue() - pOp2.doubleValue());
        }
        return new NumericValue(0.0);
      }
      if (pOp1.doubleValue() < 0) {
        return new NumericValue(0.0);
      }
      return new NumericValue(pOp1);
    }
    if (Double.isInfinite(pOp2.doubleValue())) {
      if (pOp2.doubleValue() < 0) {
        return new NumericValue(Double.NaN);
      }
      return new NumericValue(0.0);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }
    if (op1bd.compareTo(op2bd) > 0) {
      BigDecimal difference = op1bd.subtract(op2bd);

      CSimpleType type = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      BigDecimal maxValue;
      switch (type.getType()) {
        case FLOAT:
          maxValue = BigDecimal.valueOf(Float.MAX_VALUE);
          break;
        case DOUBLE:
          maxValue = BigDecimal.valueOf(Double.MAX_VALUE);
          break;
        default:
          return Value.UnknownValue.getInstance();
      }
      if (difference.compareTo(maxValue) > 0) {
        return new NumericValue(Double.POSITIVE_INFINITY);
      }
      return new NumericValue(difference);
    }
    return new NumericValue(0.0);
  }

  private Optional<Boolean> isNegative(Number pNumber) {
    if (pNumber instanceof BigDecimal) {
      return Optional.of(((BigDecimal) pNumber).signum() < 0);
    } else if (pNumber instanceof Float) {
      float number = pNumber.floatValue();
      if (Float.isNaN(number)) {
        return Optional.of(false);
      }
      return Optional.of(number < 0 || 1 / number < 0);
    } else if (pNumber instanceof Double) {
      double number = pNumber.doubleValue();
      if (Double.isNaN(number)) {
        return Optional.of(false);
      }
      return Optional.of(number < 0 || 1 / number < 0);
    } else if (pNumber instanceof NegativeNaN) {
      return Optional.of(true);
    }
    return Optional.empty();
  }

  private boolean isUnspecifiedType(CType pType) {
    return pType instanceof CSimpleType
        && ((CSimpleType) pType).getType() == CBasicType.UNSPECIFIED;
  }

  @Override
  public Value visit(CCharLiteralExpression pE) throws UnrecognizedCCodeException {
    return new NumericValue((long) pE.getCharacter());
  }

  @Override
  public Value visit(CFloatLiteralExpression pE) throws UnrecognizedCCodeException {
    return new NumericValue(pE.getValue());
  }

  @Override
  public Value visit(CIntegerLiteralExpression pE) throws UnrecognizedCCodeException {
    return new NumericValue(pE.getValue());
  }

  @Override
  public Value visit(CImaginaryLiteralExpression pE) throws UnrecognizedCCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public Value visit(CStringLiteralExpression pE) throws UnrecognizedCCodeException {
    return Value.UnknownValue.getInstance();
  }

  @Override
  public Value visit(final CTypeIdExpression pE) {
    final TypeIdOperator idOperator = pE.getOperator();
    final CType innerType = pE.getType();

    switch (idOperator) {
    case SIZEOF:
      int size = machineModel.getSizeof(innerType);
      return new NumericValue(size);

    default: // TODO support more operators
      return Value.UnknownValue.getInstance();
    }
  }

  @Override
  public Value visit(CIdExpression idExp) throws UnrecognizedCCodeException {
    if (idExp.getDeclaration() instanceof CEnumerator) {
      CEnumerator enumerator = (CEnumerator) idExp.getDeclaration();
      if (enumerator.hasValue()) {
        // TODO rewrite CEnumerator to handle abstract type Value and not just Long
        return new NumericValue(enumerator.getValue());
      } else {
        return Value.UnknownValue.getInstance();
      }
    }

    return evaluateCIdExpression(idExp);
  }

  @Override
  public Value visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
    final UnaryOperator unaryOperator = unaryExpression.getOperator();
    final CExpression unaryOperand = unaryExpression.getOperand();

    if (unaryOperator == UnaryOperator.SIZEOF) {
      return new NumericValue(machineModel.getSizeof(unaryOperand.getExpressionType()));
    }
    if (unaryOperator == UnaryOperator.ALIGNOF) {
      return new NumericValue(machineModel.getAlignof(unaryOperand.getExpressionType()));
    }
    if (unaryOperator == UnaryOperator.AMPER) {
      return Value.UnknownValue.getInstance();
    }

    final Value value = unaryOperand.accept(this);

    if (value.isUnknown()) {
      return Value.UnknownValue.getInstance();
    }

    if (value instanceof SymbolicValue) {
      final CType expressionType = unaryExpression.getExpressionType();
      final CType operandType = unaryOperand.getExpressionType();

      return createSymbolicExpression(value, operandType, unaryOperator, expressionType);

    } else if (!value.isNumericValue()) {
      logger.logf(Level.FINE, "Invalid argument %s for unary operator %s.", value, unaryOperator);
      return Value.UnknownValue.getInstance();
    }

    final NumericValue numericValue = (NumericValue) value;
    switch (unaryOperator) {
    case MINUS:
      return numericValue.negate();

    case TILDE:
      return new NumericValue(~(numericValue).longValue());

    default:
      throw new AssertionError("unknown operator: " + unaryOperator);
    }
  }

  private Value createSymbolicExpression(Value pValue, CType pOperandType, UnaryOperator pUnaryOperator,
      CType pExpressionType) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression operand = factory.asConstant(pValue, pOperandType);

    switch (pUnaryOperator) {
      case MINUS:
        return factory.negate(operand, pExpressionType);
      case TILDE:
        return factory.binaryNot(operand, pExpressionType);
      default:
        throw new AssertionError("Unhandled unary operator " + pUnaryOperator);
    }
  }

  @Override
  public Value visit(CPointerExpression pointerExpression) throws UnrecognizedCCodeException {
    return evaluateCPointerExpression(pointerExpression);
  }

  @Override
  public Value visit(CFieldReference fieldReferenceExpression) throws UnrecognizedCCodeException {
    return evaluateCFieldReference(fieldReferenceExpression);
  }

  @Override
  public Value visit(CArraySubscriptExpression pE)
      throws UnrecognizedCCodeException {
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
      return calculateBinaryOperation(binaryOperator, lValue, lValType, rValue, rValType,
          expressionType, pE);

    } catch (IllegalOperationException e) {
      logger.logUserException(Level.SEVERE, e, pE.getFileLocation().toString());
      return UnknownValue.getInstance();
    }
  }

  private Value calculateBinaryOperation(JBinaryExpression.BinaryOperator pOperator,
      Value pLValue, JType pLType, Value pRValue, JType pRType, JType pExpType, JBinaryExpression pExpression)
      throws IllegalOperationException {

    assert !pLValue.isUnknown() && !pRValue.isUnknown();

    if (pLValue instanceof SymbolicValue || pRValue instanceof SymbolicValue) {
      final JType expressionType = pExpression.getExpressionType();

      return createSymbolicExpression(pLValue, pLType, pRValue, pRType, pOperator, expressionType, expressionType);

    } else if (pLValue instanceof NumericValue) {

      assert pRValue instanceof NumericValue;
      assert pLType instanceof JSimpleType && pRType instanceof JSimpleType;
      assert pExpType instanceof JSimpleType;

      if (isFloatType(pLType) || isFloatType(pRType)) {
        return calculateFloatOperation((NumericValue) pLValue, (NumericValue) pRValue,
            pOperator, ((JSimpleType) pLType).getType(), ((JSimpleType) pRType).getType());

      } else {
        return calculateIntegerOperation((NumericValue) pLValue, (NumericValue )pRValue,
            pOperator, ((JSimpleType) pLType).getType(), ((JSimpleType) pRType).getType());
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

  private Value createSymbolicExpression(Value pLeftValue, JType pLeftType, Value pRightValue,
      JType pRightType, JBinaryExpression.BinaryOperator pOperator, JType pExpressionType, JType pCalculationType) {
    assert pLeftValue instanceof SymbolicValue || pRightValue instanceof SymbolicValue;

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression leftOperand = factory.asConstant(pLeftValue, pLeftType);
    SymbolicExpression rightOperand = factory.asConstant(pRightValue, pRightType);

    switch (pOperator) {
      case PLUS:
        return factory.add(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MINUS:
        return factory.minus(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MULTIPLY:
        return factory.multiply(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case DIVIDE:
        return factory.divide(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MODULO:
        return factory.modulo(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_LEFT:
        return factory.shiftLeft(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_RIGHT_SIGNED:
        return factory.shiftRightSigned(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_RIGHT_UNSIGNED:
        return factory.shiftRightUnsigned(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_AND:
        return factory.binaryAnd(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LOGICAL_AND:
        return factory.logicalAnd(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_OR:
        return factory.binaryOr(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LOGICAL_OR:
        return factory.logicalOr(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_XOR:
      case LOGICAL_XOR:
        return factory.binaryXor(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case EQUALS:
        return factory.equal(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case NOT_EQUALS:
        return factory.notEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_THAN:
        return factory.lessThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_EQUAL:
        return factory.lessThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_THAN:
        return factory.greaterThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      default:
        throw new AssertionError("Unhandled binary operation " + pOperator);
    }
  }

  /*
   * Calculates the result of the given operation for the given integer values.
   * The given values have to be of a Java integer type, that is long, int, short, or byte.
   */
  private Value calculateIntegerOperation(NumericValue pLeftValue, NumericValue pRightValue,
      JBinaryExpression.BinaryOperator pBinaryOperator, JBasicType pLeftType,
      JBasicType pRightType) throws IllegalOperationException {

    checkNotNull(pLeftType);
    checkNotNull(pRightType);

    final long lVal = pLeftValue.longValue();
    final long rVal = pRightValue.longValue();
    long numResult;

    switch (pBinaryOperator) {
    case PLUS:
    case MINUS:
    case DIVIDE:
    case MULTIPLY:
    case SHIFT_LEFT:
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
    case MODULO:
    case SHIFT_RIGHT_SIGNED:
    case SHIFT_RIGHT_UNSIGNED: {

      switch (pBinaryOperator) {
      case PLUS:
        numResult = lVal + rVal;
        break;

      case MINUS:
        numResult = lVal - rVal;
        break;

      case DIVIDE:
        if (rVal == 0) {
          throw new IllegalOperationException("Division by zero: " + lVal + " / " + rVal);
        }

        numResult = lVal / rVal;
        break;

      case MULTIPLY:
        numResult = lVal * rVal;
        break;

      case BINARY_AND:
        numResult = lVal & rVal;
        break;

      case BINARY_OR:
        numResult = lVal | rVal;
        break;

      case BINARY_XOR:
        numResult = lVal ^ rVal;
        break;

      case MODULO:
        numResult = lVal % rVal;
        break;

      // shift operations' behaviour is determined by whether the left hand side value is of type
      // int or long, so we have to cast if the actual type is int.
      case SHIFT_LEFT:
        if (pLeftType != JBasicType.LONG && pRightType != JBasicType.LONG) {
          numResult = ((int) lVal) << rVal;
        } else {
          numResult = lVal << rVal;
        }
        break;

      case SHIFT_RIGHT_SIGNED:
        if (pLeftType != JBasicType.LONG && pRightType != JBasicType.LONG) {
          numResult = ((int) lVal) >> rVal;
        } else {
          numResult = lVal >> rVal;
        }
        break;

      case SHIFT_RIGHT_UNSIGNED:
        if (pLeftType != JBasicType.LONG && pRightType != JBasicType.LONG) {
          numResult = ((int) lVal) >>> rVal;
        } else {
          numResult = lVal >>> rVal;
        }
        break;

      default:
        throw new AssertionError("Unhandled operator " + pBinaryOperator);
      }

      if (pLeftType != JBasicType.LONG && pRightType != JBasicType.LONG) {
        numResult = (int) numResult;
      }

      return new NumericValue(numResult);
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {

      final boolean result;
      switch (pBinaryOperator) {
      case EQUALS:
        result = (lVal == rVal);
        break;
      case NOT_EQUALS:
        result = (lVal != rVal);
        break;
      case GREATER_THAN:
        result = (lVal > rVal);
        break;
      case GREATER_EQUAL:
        result = (lVal >= rVal);
        break;
      case LESS_THAN:
        result = (lVal < rVal);
        break;
      case LESS_EQUAL:
        result = (lVal <= rVal);
        break;

      default:
        throw new AssertionError("Unhandled operation " + pBinaryOperator);
      }

      return BooleanValue.valueOf(result);
    }
    default:
      // TODO check which cases can be handled
      return UnknownValue.getInstance();
    }
  }

  /*
   * Calculates the result of the given operation for the given floating point values.
   * The given values have to be of Java types float or double.
   */
  private Value calculateFloatOperation(NumericValue pLeftValue, NumericValue pRightValue,
      JBinaryExpression.BinaryOperator pBinaryOperator,
      JBasicType pLeftOperand, JBasicType pRightOperand) throws IllegalOperationException {

    final double lVal;
    final double rVal;

    if (pLeftOperand != JBasicType.DOUBLE && pRightOperand != JBasicType.DOUBLE) {
      lVal = pLeftValue.floatValue();
      rVal = pRightValue.floatValue();
    } else {
      lVal = pLeftValue.doubleValue();
      rVal = pRightValue.doubleValue();
    }

    switch (pBinaryOperator) {
    case PLUS:
    case MINUS:
    case DIVIDE:
    case MULTIPLY:
    case MODULO: {

      switch (pBinaryOperator) {
      case PLUS:
        return new NumericValue(lVal + rVal);

      case MINUS:
        return new NumericValue(lVal - rVal);

      case DIVIDE:
        if (rVal == 0) {
          throw new IllegalOperationException("Division by zero: " + lVal + " / " + rVal);
        }
        return new NumericValue(lVal / rVal);

      case MULTIPLY:
        return new NumericValue(lVal * rVal);

      case MODULO:
        return new NumericValue(lVal % rVal);

      default:
        throw new AssertionError("Unsupported binary operation " + pBinaryOperator.toString() + " on double values");
      }
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {

      final boolean result;
      switch (pBinaryOperator) {
      case EQUALS:
        result = (lVal == rVal);
        break;
      case NOT_EQUALS:
        result = (lVal != rVal);
        break;
      case GREATER_THAN:
        result = (lVal > rVal);
        break;
      case GREATER_EQUAL:
        result = (lVal >= rVal);
        break;
      case LESS_THAN:
        result = (lVal < rVal);
        break;
      case LESS_EQUAL:
        result = (lVal <= rVal);
        break;

      default:
        throw new AssertionError("Unsupported binary operation " + pBinaryOperator.toString()
            + " on floating point values");
      }

      // return 1 if expression holds, 0 otherwise
      return BooleanValue.valueOf(result);
    }
    default:
      // TODO check which cases can be handled
      return UnknownValue.getInstance();
    }
  }

  private Value calculateBooleanOperation(boolean lVal, boolean rVal,
      JBinaryExpression.BinaryOperator operator) {

    switch (operator) {
    case CONDITIONAL_AND:
    case LOGICAL_AND:
      // we do not care about sideeffects through evaluation of the righthandside at this point - this must be handled
      // earlier
      return BooleanValue.valueOf(lVal && rVal);

    case CONDITIONAL_OR:
    case LOGICAL_OR:
      // we do not care about sideeffects through evaluation of the righthandside at this point
      return BooleanValue.valueOf(lVal || rVal);

    case LOGICAL_XOR:
      return BooleanValue.valueOf(lVal ^ rVal);

    case EQUALS:
      return BooleanValue.valueOf(lVal == rVal);
    case NOT_EQUALS:
      return BooleanValue.valueOf(lVal != rVal);
    default:
      throw new AssertionError("Unhandled operator " + operator + " for boolean expression");
    }
  }

  private Value calculateComparison(Value pLeftValue, Value pRightValue,
      JBinaryExpression.BinaryOperator pOperator) {
    assert pOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS
        || pOperator == JBinaryExpression.BinaryOperator.EQUALS;

    return BooleanValue.valueOf(pOperator != JBinaryExpression.BinaryOperator.EQUALS
        ^ pLeftValue.equals(pRightValue));
  }

  @Override
  public Value visit(JIdExpression idExp) {


    ASimpleDeclaration decl = idExp.getDeclaration();

    // Java IdExpression could not be resolved
    if (decl == null) { return UnknownValue.getInstance(); }

    if (decl instanceof JFieldDeclaration
        && !((JFieldDeclaration) decl).isStatic()) {
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
    final String errorMsg
      = "Invalid argument [" + valueObject + "] for unary operator [" + unaryOperator + "].";

    if (valueObject.isUnknown()) {
      return UnknownValue.getInstance();

    } else if (valueObject.isNumericValue()) {
      NumericValue value = (NumericValue)valueObject;

      switch (unaryOperator) {
        case MINUS:
          return value.negate();

        case COMPLEMENT:
          return evaluateComplement(unaryOperand, value);

        case PLUS:
          return value;

        default:
          logger.log(Level.FINE, errorMsg);
          return UnknownValue.getInstance();
      }

    } else if (valueObject instanceof BooleanValue
        && unaryOperator == JUnaryExpression.UnaryOperator.NOT) {
      return ((BooleanValue)valueObject).negate();

    } else if (valueObject instanceof SymbolicValue) {
      final JType expressionType = unaryExpression.getExpressionType();
      final JType operandType = unaryOperand.getExpressionType();

      return createSymbolicExpression(valueObject, operandType, unaryOperator, expressionType);

    } else {
      logger.log(Level.FINE, errorMsg);
      return UnknownValue.getInstance();
    }
  }

  private Value createSymbolicExpression(Value pValue, JType pOperandType,
      JUnaryExpression.UnaryOperator pUnaryOperator, JType pExpressionType) {

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression operand = factory.asConstant(pValue, pOperandType);

    switch (pUnaryOperator) {
      case COMPLEMENT:
        return factory.binaryNot(operand, pExpressionType);
      case NOT:
        return factory.logicalNot(operand, pExpressionType);
      case MINUS:
        return factory.negate(operand, pExpressionType);
      case PLUS:
        return pValue;
      default:
        throw new AssertionError("Unhandled unary operator " + pUnaryOperator);
    }
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
    return type instanceof JSimpleType && ((JSimpleType) type).getType().isIntegerType();

  }

  private static boolean isFloatType(JType type) {
    return type instanceof JSimpleType && ((JSimpleType) type).getType().isFloatingPointType();
  }

  @Override
  public Value visit(JIntegerLiteralExpression pE) {
    return new NumericValue(pE.asLong());
  }

  @Override
  public Value visit(JBooleanLiteralExpression pE) {
    return BooleanValue.valueOf(pE.getValue());
  }

  @Override
  public Value visit(JArraySubscriptExpression pJArraySubscriptExpression) {
    NumericValue subscriptValue = (NumericValue) pJArraySubscriptExpression.getSubscriptExpression().accept(this);
    JExpression arrayExpression = pJArraySubscriptExpression.getArrayExpression();
    Value idValue = arrayExpression.accept(this);

    if (!idValue.isUnknown()) {
      ArrayValue innerMostArray = (ArrayValue) arrayExpression.accept(this);

      assert subscriptValue.longValue() >= 0 && subscriptValue.longValue() <= Integer.MAX_VALUE;
      return innerMostArray.getValueAt((int) subscriptValue.longValue());

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

    return castJValue(operand.accept(this), operand.getExpressionType(), castType, logger,
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
    List<JExpression> arraySizeExpressions = new ArrayList<>(pJArrayCreationExpression.getLength());
    Value lastArrayValue;
    Value currentArrayValue = null;
    int currentDimension = 0;
    long concreteArraySize;
    final JType elementType = pJArrayCreationExpression.getExpressionType().getElementType();

    Collections.reverse(arraySizeExpressions);
    for (JExpression sizeExpression : arraySizeExpressions) {
      currentDimension++;
      lastArrayValue = currentArrayValue;
      Value sizeValue = sizeExpression.accept(this);

      if (sizeValue.isUnknown()) {
        currentArrayValue = UnknownValue.getInstance();

      } else {
        concreteArraySize = ((NumericValue) sizeValue).longValue();
        currentArrayValue = createArrayValue(new JArrayType(elementType, currentDimension), concreteArraySize);

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
      throw new AssertionError("Trying to create array of size " + pArraySize
          + ". Java arrays can't be smaller than 0 or bigger than the max int value.");
    }

    return new ArrayValue(pType, (int) pArraySize);
  }

  @Override
  public Value visit(JArrayInitializer pJArrayInitializer) {
    final JArrayType arrayType = pJArrayInitializer.getExpressionType();
    final List<JExpression> initializerExpressions = pJArrayInitializer.getInitializerExpressions();

    // this list stores the values in the array's slots, in occurring order
    List<Value> slotValues = new LinkedList<>();

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
      throws UnrecognizedCCodeException;

  protected abstract Value evaluateCIdExpression(CIdExpression pCIdExpression)
      throws UnrecognizedCCodeException;

  protected abstract Value evaluateJIdExpression(JIdExpression varName);

  protected abstract Value evaluateCFieldReference(CFieldReference pLValue)
      throws UnrecognizedCCodeException;

  protected abstract Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
      throws UnrecognizedCCodeException;

  /* additional methods */

  public String getFunctionName() {
    return functionName;
  }

  public long getSizeof(CType pType) {
    return machineModel.getSizeof(pType);
  }

  protected MachineModel getMachineModel() {
    return machineModel;
  }

  protected LogManagerWithoutDuplicates getLogger() {
    return logger;
  }

  /**
   * This method returns the value of an expression, reduced to match the type.
   * This method handles overflows and casts.
   * If necessary warnings for the user are printed.
   *
   * @param pExp expression to evaluate
   * @param pTargetType the type of the left side of an assignment
   * @return if evaluation successful, then value, else null
   */
  public Value evaluate(final CExpression pExp, final CType pTargetType)
      throws UnrecognizedCCodeException {
    return castCValue(pExp.accept(this), pTargetType, machineModel, logger,
        pExp.getFileLocation());
  }

  /**
   * This method returns the value of an expression, reduced to match the type.
   * This method handles overflows and casts.
   * If necessary warnings for the user are printed.
   *
   * @param pExp expression to evaluate
   * @param pTargetType the type of the left side of an assignment
   * @return if evaluation successful, then value, else null
   */
  public Value evaluate(final CRightHandSide pExp, final CType pTargetType)
      throws UnrecognizedCCodeException {
    return castCValue(pExp.accept(this), pTargetType, machineModel, logger,
        pExp.getFileLocation());
  }

  /**
   * This method returns the value of an expression, reduced to match the given target type.
   * This method handles overflows and casts.
   * If necessary warnings for the user are printed.
   *
   * @param pExp the expression to evaluate
   * @param pTargetType the target type of the assignment (the type of the left side of the assignment)
   * @return the corresponding value of the given expression, if the evaluation was successful.
   *        <code>Null</code>, otherwise
   */
  public Value evaluate(final JRightHandSide pExp, final JType pTargetType) {
    return castJValue(pExp.accept(this), (JType) pExp.getExpressionType(), pTargetType, logger,
        pExp.getFileLocation());
  }

  /**
   * This method returns the input-value, casted to match the type.
   * If the value matches the type, it is returned unchanged.
   * This method handles overflows and print warnings for the user.
   * Example:
   * This method is called, when an value of type 'integer'
   * is assigned to a variable of type 'char'.
   *
   * @param value will be casted.
   * @param targetType value will be casted to targetType.
   * @param machineModel contains information about types
   * @param logger for logging
   * @param fileLocation the location of the corresponding code in the source file
   * @return the casted Value
   */
  public static Value castCValue(@Nonnull final Value value, final CType targetType,
      final MachineModel machineModel, final LogManagerWithoutDuplicates logger,
      final FileLocation fileLocation) {

    if (!value.isExplicitlyKnown()) {
      return castIfSymbolic(value, targetType, Optional.of(machineModel));
    }

    // For now can only cast numeric value's
    if (!value.isNumericValue()) {
      logger.logf(Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
      return value;
    }
    NumericValue numericValue = (NumericValue) value;

    final CType type = targetType.getCanonicalType();
    if (type instanceof CSimpleType) {
      final CSimpleType st = (CSimpleType) type;

      switch (st.getType()) {

      case INT:
      case CHAR: {
        final int size = machineModel.getSizeofInBits(st);
        final long longValue = numericValue.longValue();
        final boolean targetIsSigned = machineModel.isSigned(st);

        if (size < SIZE_OF_JAVA_LONG) {
          // we can handle this with java-type "long" as normal number

          final long maxValue = 1L << size; // 2^size
          long result = longValue % maxValue; // shrink to number of bits

          if (targetIsSigned) {
            // signed value must be put in interval [-(maxValue/2), (maxValue/2)-1]
            if (result > (maxValue / 2) - 1) {
              result -= maxValue;
            } else if (result < -(maxValue / 2)) {
              result += maxValue;
            }
          } else {
            // unsigned value must be put in interval [0, maxValue-1]
            if (longValue < 0) {
              // value is negative, so adding maxValue makes it positive
              result += maxValue;
            }
          }

          return new NumericValue(result);

        } else if (size == SIZE_OF_JAVA_LONG) {
          // we can handle this with java-type "long", because the bitwise representation is correct.
          // but for unsigned long we need BigInteger
          if (!targetIsSigned && longValue < 0) {
            return new NumericValue(BigInteger.valueOf(longValue).andNot(BigInteger.valueOf(-1).shiftLeft(size)));
          }
          return new NumericValue(longValue);

        } else {
          // java-type "long" is too small for really big types like 'int128',
          // however we do currently not support such types.
          // so we do nothing here and trust the analysis, that handles it later
          // TODO should we handle it as BigInteger?
          logger.logfOnce(Level.INFO,
              "%s: value %s of c-type '%s' is too big for java-type 'long'.",
              fileLocation,
              value, targetType);

          return value;
        }
      }

      case FLOAT: {
        // TODO: look more closely at the INT/CHAR cases, especially at the loggedEdges stuff
        // TODO: check for overflow(source larger than the highest number we can store in target etc.)

        // casting to FLOAT, if value is INT or DOUBLE. This is sound, if we would also do this cast in C.
        float floatValue = numericValue.floatValue();
        Value result;

        final int bitPerByte = machineModel.getSizeofCharInBits();
        final int numBytes = machineModel.getSizeof(st);
        final int size = bitPerByte * numBytes;

        if (NumericValue.NegativeNaN.VALUE.equals(numericValue.getNumber())) {
          result = numericValue;
        } else if (size == SIZE_OF_JAVA_FLOAT) {
          // 32 bit means Java float
          result = new NumericValue(floatValue);
        } else if (size == SIZE_OF_JAVA_DOUBLE) {
          // 64 bit means Java double
          result = new NumericValue(floatValue);
        } else {
          throw new AssertionError("Trying to cast to unsupported floating point type: " + st);
        }

        return result;
      }
      case DOUBLE: {
        // TODO: look more closely at the INT/CHAR cases, especially at the loggedEdges stuff
        // TODO: check for overflow(source larger than the highest number we can store in target etc.)

        // casting to DOUBLE, if value is INT or FLOAT. This is sound, if we would also do this cast in C.
        double doubleValue = numericValue.doubleValue();
        Value result;

        final int bitPerByte = machineModel.getSizeofCharInBits();
        final int numBytes = machineModel.getSizeof(st);
        final int size = bitPerByte * numBytes;

        if (NumericValue.NegativeNaN.VALUE.equals(numericValue.getNumber())) {
          result = numericValue;
        } else if (size == SIZE_OF_JAVA_FLOAT) {
          // 32 bit means Java float
          result = new NumericValue((float) doubleValue);
        } else if (size == SIZE_OF_JAVA_DOUBLE) {
          // 64 bit means Java double
          result = new NumericValue(doubleValue);
        } else {
          throw new AssertionError("Trying to cast to unsupported floating point type: " + st);
        }

        return result;
      }

      default:
        return value; // currently we do not handle floats, doubles or voids
      }

    } else {
      return value; // pointer like (void)*, (struct s)*, ...
    }
  }

  private static Value castIfSymbolic(Value pValue, Type pTargetType, Optional<MachineModel> pMachineModel) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    if (pValue instanceof SymbolicValue
        && (pTargetType instanceof JSimpleType || pTargetType instanceof CSimpleType)) {

      return factory.cast((SymbolicValue) pValue, pTargetType, pMachineModel);
    }

    // If the value is not symbolic, just return it.
    return pValue;
  }

  /**
   * <p>Casts the given value to the specified Java type. This also handles overflows.</p>
   *
   * <p>
   * In Java, numeric values are the only primitive types that can be cast. In consequence, all
   * values of other primitive types (and not explicitly known values) will simply be returned
   * in their original form.
   * </p>
   *
   * @param value the value to cast
   * @param sourceType the original type of the given value
   * @param targetType the type the given value should be cast to
   * @param logger the logger error and warning messages will be logged to
   * @param fileLocation the location of the corresponding code in the source file
   * @return the cast value, if a cast from the source to the target type is possible. Otherwise,
   *         the given value will be returned without a change
   */
  public static Value castJValue(@Nonnull final Value value, JType sourceType,
      JType targetType, final LogManagerWithoutDuplicates logger, final FileLocation fileLocation) {

    if (!value.isExplicitlyKnown()) {
      return castIfSymbolic(value, targetType, Optional.empty());
    }

    // Other than symbolic values, we can only cast numeric values, for now.
    if (!value.isNumericValue()) {
      logger.logf(Level.FINE, "Can not cast Java value %s to %s", value.toString(), targetType.toString());
      return value;
    }

    NumericValue numericValue = (NumericValue) value;

    if (targetType instanceof JSimpleType) {
      final JSimpleType st = (JSimpleType) targetType;

      if (isIntegerType(sourceType)) {
        long longValue = numericValue.longValue();

        return createValue(longValue, st.getType());

      } else if (isFloatType(sourceType)) {
        double doubleValue = numericValue.doubleValue();

        return createValue(doubleValue, st.getType());

      } else {
        throw new AssertionError("Cast from " + sourceType.toString() + " to "
            + targetType.toString() + " not possible.");
      }
    } else {
      return value; // TODO handle casts between object types
    }
  }

  private static Value createValue(long value, JBasicType targetType) {
    switch (targetType) {
    case BYTE:
      return new NumericValue((byte) value);

    case CHAR:
      char castedValue = (char) value;
      return new NumericValue((int) castedValue);
    case SHORT:
      return new NumericValue((short) value);

    case INT:
      return new NumericValue((int) value);

    case LONG:
      return new NumericValue(value);

    case FLOAT:
      return new NumericValue((float) value);

    case DOUBLE:
      return new NumericValue((double) value);

    default:
      throw new AssertionError("Trying to cast to unsupported type " + targetType);
    }
  }

  private static Value createValue(double value, JBasicType targetType) {
    switch (targetType) {
    case BYTE:
      return new NumericValue((byte) value);

    case CHAR:
    case SHORT:
      return new NumericValue((short) value);

    case INT:
      return new NumericValue((int) value);

    case LONG:
      return new NumericValue(value);

    case FLOAT:
      return new NumericValue((float) value);

    case DOUBLE:
      return new NumericValue(value);

    default:
      throw new AssertionError("Trying to cast to unsupported type " + targetType);
    }
  }

  /**
   *
   * @param type the input type
   * @return A numeric type that can be used to perform arithmetics on an instance
   *         of the type directly, or null if none.
   *
   *         Most notably, CPointerType will be converted to the unsigned integer type
   *         of correct size.
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

    private static final long serialVersionUID = 5420891133452817345L;

    public IllegalOperationException(String msg) {
      super(msg);
    }

    public IllegalOperationException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }
}
