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
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface.UnknownValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.ArrayValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.EnumConstantValue;
import org.sosy_lab.cpachecker.cpa.value.type.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValueCreator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
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
    extends DefaultCExpressionVisitor<NumberInterface, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<NumberInterface, UnrecognizedCCodeException>,
    JRightHandSideVisitor<NumberInterface, RuntimeException>,
    JExpressionVisitor<NumberInterface, RuntimeException> {

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
  protected NumberInterface visitDefault(CExpression pExp) {
    return NumberInterface.UnknownValue.getInstance();
  }

  public void reset() {
    missingFieldAccessInformation = false;
  }

  @Override
  public NumberInterface visit(final CBinaryExpression pE) throws UnrecognizedCCodeException {
    final NumberInterface lVal = pE.getOperand1().accept(this);
    if (lVal.isUnknown()) { return NumberInterface.UnknownValue.getInstance(); }
    final NumberInterface rVal = pE.getOperand2().accept(this);
    if (rVal.isUnknown()) { return NumberInterface.UnknownValue.getInstance(); }
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
  public static NumberInterface calculateBinaryOperation(NumberInterface lVal, NumberInterface rVal,
      final CBinaryExpression binaryExpr,
      final MachineModel machineModel, final LogManagerWithoutDuplicates logger) {

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
      rVal =
          castCValue(rVal, calculationType, machineModel, logger, binaryExpr.getFileLocation());
    }
    //SymbolicValue implements NumberInterface!?
    if (lVal instanceof SymbolicValue
        || rVal instanceof SymbolicValue) { return calculateSymbolicBinaryExpression(lVal, rVal,
            binaryExpr); }

    if (!lVal.isNumericValue() || !rVal.isNumericValue()) {
      logger.logf(Level.FINE, "Parameters to binary operation '%s %s %s' are no numeric values.",
          lVal, binaryOperator, rVal);
      return NumberInterface.UnknownValue.getInstance();
    }

    NumberInterface result;

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
        result =
            arithmeticOperation(lVal, rVal, binaryOperator, calculationType, machineModel, logger);
        result = castCValue(result, binaryExpr.getExpressionType(), machineModel, logger,
            binaryExpr.getFileLocation());

        break;
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {
        result = booleanOperation(lVal,
            rVal, binaryOperator, calculationType, machineModel, logger);
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
  public static NumberInterface calculateSymbolicBinaryExpression(NumberInterface pLValue,
      NumberInterface pRValue,
      final CBinaryExpression pExpression) {

    final BinaryOperator operator = pExpression.getOperator();

    final CType leftOperandType = pExpression.getOperand1().getExpressionType();
    final CType rightOperandType = pExpression.getOperand2().getExpressionType();
    final CType expressionType = pExpression.getExpressionType();
    final CType calculationType = pExpression.getCalculationType();

    return createSymbolicExpression(pLValue, leftOperandType, pRValue, rightOperandType, operator,
        expressionType, calculationType);
  }

  private static SymbolicValue createSymbolicExpression(NumberInterface pLeftValue, CType pLeftType,
      NumberInterface pRightValue,
      CType pRightType, CBinaryExpression.BinaryOperator pOperator, CType pExpressionType,
      CType pCalculationType) {

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
        return factory.shiftRightSigned(leftOperand, rightOperand, pExpressionType,
            pCalculationType);
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
        return factory.lessThanOrEqual(leftOperand, rightOperand, pExpressionType,
            pCalculationType);
      case GREATER_THAN:
        return factory.greaterThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(leftOperand, rightOperand, pExpressionType,
            pCalculationType);
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
  //  private static long arithmeticOperation(final long l, final long r,
  //      final BinaryOperator op, final CType calculationType,
  //      final MachineModel machineModel, final LogManager logger) {

  // special handling for UNSIGNED_LONGLONG (32 and 64bit), UNSIGNED_LONG (64bit)
  // because Java only has SIGNED_LONGLONG
  //    CSimpleType st = getArithmeticType(calculationType);
  //    if (st != null) {
  //      if (machineModel.getSizeofInBits(st) >= SIZE_OF_JAVA_LONG
  //          && st.isUnsigned()) {
  //        switch (op) {
  //        case DIVIDE:
  //          if (r == 0) {
  //            logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
  //            return 0;
  //          }
  //          return UnsignedLongs.divide(l, r);
  //        case MODULO:
  //          return UnsignedLongs.remainder(l, r);
  //        case SHIFT_RIGHT:
  //          /*
  //           * from http://docs.oracle.com/javase/tutorial/java/nutsandbolts/op3.html
  //           *
  //           * The unsigned right shift operator ">>>" shifts a zero
  //           * into the leftmost position, while the leftmost position
  //           * after ">>" depends on sign extension.
  //           */
  //          return l >>> r;
  //        default:
  //          // fall-through, calculation is done correct as SINGED_LONG_LONG
  //        }
  //      }
  //    }

  //    switch (op) {
  //    case PLUS:
  //      return l + r;
  //    case MINUS:
  //      return l - r;
  //    case DIVIDE:
  //      if (r == 0) {
  //        logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
  //        return 0;
  //      }
  //      return l / r;
  //    case MODULO:
  //      return l % r;
  //    case MULTIPLY:
  //      return l * r;
  //    case SHIFT_LEFT:
  //      /* There is a difference in the SHIFT-operation in Java and C.
  //       * In C a SHIFT is a normal SHIFT, in Java the rVal is used as (r%64).
  //       *
  //       * http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
  //       *
  //       * If the promoted type of the left-hand operand is long, then only the
  //       * six lowest-order bits of the right-hand operand are used as the
  //       * shift distance. It is as if the right-hand operand were subjected to
  //       * a bitwise logical AND operator & (§15.22.1) with the mask value 0x3f.
  //       * The shift distance actually used is therefore always in the range 0 to 63.
  //       */
  //      return (r >= SIZE_OF_JAVA_LONG) ? 0 : l << r;
  //    case SHIFT_RIGHT:
  //      return l >> r;
  //    case BINARY_AND:
  //      return l & r;
  //    case BINARY_OR:
  //      return l | r;
  //    case BINARY_XOR:
  //      return l ^ r;
  //
  //    default:
  //      throw new AssertionError("unknown binary operation: " + op);
  //    }
  //
  //  }

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
  //  private static double arithmeticOperation(final double l, final double r,
  //      final BinaryOperator op, final CType calculationType,
  //      final MachineModel machineModel, final LogManager logger) {
  //
  //    switch (op) {
  //    case PLUS:
  //      return l + r;
  //    case MINUS:
  //      return l - r;
  //    case DIVIDE:
  //      return l / r;
  //    case MODULO:
  //      return l % r;
  //    case MULTIPLY:
  //      return l * r;
  //    case SHIFT_LEFT:
  //    case SHIFT_RIGHT:
  //    case BINARY_AND:
  //    case BINARY_OR:
  //    case BINARY_XOR:
  //      throw new AssertionError("trying to perform " + op + " on floating point operands");
  //    default:
  //      throw new AssertionError("unknown binary operation: " + op);
  //    }
  //
  //  }

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
  //  private static float arithmeticOperation(final float l, final float r,
  //      final BinaryOperator op, final CType calculationType,
  //      final MachineModel machineModel, final LogManager logger) {
  //
  //    switch (op) {
  //    case PLUS:
  //      return l + r;
  //    case MINUS:
  //      return l - r;
  //    case DIVIDE:
  //      return l / r;
  //    case MODULO:
  //      return l % r;
  //    case MULTIPLY:
  //      return l * r;
  //    case SHIFT_LEFT:
  //    case SHIFT_RIGHT:
  //    case BINARY_AND:
  //    case BINARY_OR:
  //    case BINARY_XOR:
  //      throw new AssertionError("trying to perform " + op + " on floating point operands");
  //    default:
  //      throw new AssertionError("unknown binary operation: " + op);
  //    }

  //  }

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
  private static NumberInterface arithmeticOperation(final NumberInterface lNum,
      final NumberInterface rNum,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel, final LogManager logger) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(Level.FINE, "unsupported type %s for result of binary operation %s",
          calculationType, op);
      return NumberInterface.UnknownValue.getInstance();
    }
    CSimpleType st = getArithmeticType(calculationType);
    switch (type.getType()) {
      case INT:
      case DOUBLE:
      case FLOAT:
        switch (op) {
          case PLUS:
            return lNum.plus(rNum);
          case MINUS:
            return lNum.minus(rNum);
          case DIVIDE:
            if (lNum.getNumber().longValue() == 0 || lNum.getNumber().doubleValue() == 0.0) {
              //TODO resolve the case with different types of values
              logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", lNum.getNumber().longValue(), lNum.getNumber().longValue());
              return lNum.ZERO();
            }
            // special handling for UNSIGNED_LONGLONG (32 and 64bit), UNSIGNED_LONG (64bit)
            // because Java only has SIGNED_LONGLONG

            if (st != null) {
              if (machineModel.getSizeofInBits(st) >= SIZE_OF_JAVA_LONG
                  && st.isUnsigned()) { return lNum.unsignedDivide(rNum); }
            }
            return lNum.divide(rNum);
          case MODULO:
            if (st != null) {
              if (machineModel.getSizeofInBits(st) >= SIZE_OF_JAVA_LONG
                  && st.isUnsigned()) { return lNum.unsignedModulo(rNum); }
            }
            return lNum.modulo(rNum);
          case MULTIPLY:
            return lNum.times(rNum);
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
            return (rNum.getNumber().longValue() >= SIZE_OF_JAVA_LONG)
                ? new NumericValueCreator().factoryMethod(0) : lNum.shiftLeft(rNum);
          case SHIFT_RIGHT:
            if (st != null) {
              if (machineModel.getSizeofInBits(st) >= SIZE_OF_JAVA_LONG
                  && st.isUnsigned()) { return lNum.unsignedShiftRight(rNum); }
            }
            return lNum.shiftRight(rNum);
          case BINARY_AND:
            return lNum.binaryAnd(rNum);
          case BINARY_OR:
            return lNum.binaryOr(rNum);
          case BINARY_XOR:
            return lNum.binaryXor(rNum);
          default:
            throw new AssertionError("unknown binary operation: " + op);
        }
      default:
        logger.logf(Level.FINE, "unsupported type for result of binary operation %s",
            type.toString());
        return NumberInterface.UnknownValue.getInstance();
    }
  }

  private static NumberInterface booleanOperation(final NumberInterface l, final NumberInterface r,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel, final LogManager logger) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(Level.FINE, "unsupported type %s for result of binary operation %s",
          calculationType, op);
      return NumberInterface.UnknownValue.getInstance();
    }

    final int cmp;
    switch (type.getType()) {
      case INT: {
        CSimpleType canonicalType = type.getCanonicalType();
        int sizeInBits = machineModel.getSizeof(canonicalType) * machineModel.getSizeofCharInBits();
        if ((!machineModel.isSigned(canonicalType) && sizeInBits == SIZE_OF_JAVA_LONG)
            || sizeInBits > SIZE_OF_JAVA_LONG) {
          BigInteger leftBigInt = l.getNumber() instanceof BigInteger ? (BigInteger) l.getNumber()
              : BigInteger.valueOf(l.getNumber().longValue());
          BigInteger rightBigInt = r.getNumber() instanceof BigInteger ? (BigInteger) r.getNumber()
              : BigInteger.valueOf(r.getNumber().longValue());
          cmp = leftBigInt.compareTo(rightBigInt);
          break;
        }
        cmp = Long.compare(l.getNumber().longValue(), r.getNumber().longValue());
        break;
      }
      case FLOAT: {
        float lVal = l.getNumber().floatValue();
        float rVal = r.getNumber().floatValue();

        if (Float.isNaN(lVal) || Float.isNaN(rVal)) { return new NumericValueCreator()
            .factoryMethod((op == BinaryOperator.NOT_EQUALS ? 1L : 0L)); }
        if (lVal == 0 && rVal == 0) {
          cmp = 0;
        } else {
          cmp = Float.compare(lVal, rVal);
        }
        break;
      }
      case DOUBLE: {
        double lVal = l.getNumber().doubleValue();
        double rVal = r.getNumber().doubleValue();

        if (Double.isNaN(lVal) || Double.isNaN(rVal)) { return new NumericValueCreator()
            .factoryMethod(op == BinaryOperator.NOT_EQUALS ? 1L : 0L); }

        if (lVal == 0 && rVal == 0) {
          cmp = 0;
        } else {
          cmp = Double.compare(lVal, rVal);
        }
        break;
      }
      default: {
        logger.logf(Level.FINE, "unsupported type %s for result of binary operation %s",
            type.toString(), op);
        return NumberInterface.UnknownValue.getInstance();
      }
    }

    // return 1 if expression holds, 0 otherwise
    return new NumericValueCreator().factoryMethod(matchBooleanOperation(op, cmp) ? 1L : 0L);
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
  public NumberInterface visit(CCastExpression pE) throws UnrecognizedCCodeException {
    return castCValue(pE.getOperand().accept(this), pE.getExpressionType(), machineModel,
        logger, pE.getFileLocation());
  }

  @Override
  public NumberInterface visit(CComplexCastExpression pE) throws UnrecognizedCCodeException {
    // evaluation of complex numbers is not supported by now
    return NumberInterface.UnknownValue.getInstance();
  }

  @Override
  public NumberInterface visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCCodeException {
    CExpression functionNameExp = pIastFunctionCallExpression.getFunctionNameExpression();

    // We only handle builtin functions
    if (functionNameExp instanceof CIdExpression) {
      String functionName = ((CIdExpression) functionNameExp).getName();

      if (BuiltinFunctions.isBuiltinFunction(functionName)) {
        CType functionType = BuiltinFunctions.getFunctionType(functionName);

        if (isUnspecifiedType(functionType)) {
          // unsupported formula
          return NumberInterface.UnknownValue.getInstance();
        }

        List<CExpression> parameterExpressions =
            pIastFunctionCallExpression.getParameterExpressions();
        List<NumberInterface> parameterValues = new ArrayList<>(parameterExpressions.size());

        for (CExpression currParamExp : parameterExpressions) {
          NumberInterface newValue = currParamExp.accept(this);

          parameterValues.add(newValue);
        }

        if (BuiltinFloatFunctions.matchesAbsolute(functionName)) {
          assert parameterValues.size() == 1;

          final CType parameterType = parameterExpressions.get(0).getExpressionType();
          final NumberInterface parameter = parameterValues.get(0);

          if (parameterType instanceof CSimpleType && !((CSimpleType) parameterType).isSigned()) {
            return parameter;

          } else if (parameter.isExplicitlyKnown()) {
            assert parameter.isNumericValue();
            final double absoluteValue = Math.abs(parameter.getNumber().doubleValue());

            // absolute value for INT_MIN is undefined behaviour, so we do not bother handling it
            // in any specific way
            return new NumericValueCreator().factoryMethod(absoluteValue);
          }

        } else if (BuiltinFloatFunctions.matchesHugeVal(functionName)
            || BuiltinFloatFunctions.matchesInfinity(functionName)) {

          assert parameterValues.isEmpty();
          if (BuiltinFloatFunctions.matchesHugeValFloat(functionName)
              || BuiltinFloatFunctions.matchesInfinityFloat(functionName)) {

            return new NumericValueCreator().factoryMethod(Float.POSITIVE_INFINITY);

          } else {
            assert BuiltinFloatFunctions.matchesInfinityDouble(functionName)
                || BuiltinFloatFunctions.matchesInfinityLongDouble(functionName)
                || BuiltinFloatFunctions.matchesHugeValDouble(functionName)
                || BuiltinFloatFunctions.matchesHugeValLongDouble(
                    functionName) : " Unhandled builtin function for infinity: " + functionName;

            return new NumericValueCreator().factoryMethod(Double.POSITIVE_INFINITY);
          }

        } else if (BuiltinFloatFunctions.matchesNaN(functionName)) {
          assert parameterValues.isEmpty() || parameterValues.size() == 1;

          if (BuiltinFloatFunctions.matchesNaNFloat(functionName)) {
            return new NumericValueCreator().factoryMethod(Float.NaN);
          } else {
            assert BuiltinFloatFunctions.matchesNaNDouble(functionName)
                || BuiltinFloatFunctions.matchesNaNLongDouble(
                    functionName) : "Unhandled builtin function for NaN: " + functionName;

            return new NumericValueCreator().factoryMethod(Double.NaN);
          }
        } else if (BuiltinFloatFunctions.matchesIsNaN(functionName)) {
          if (parameterValues.size() == 1) {
            NumberInterface value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isNaN(value.getNumber().floatValue())
                      ? new NumericValueCreator().factoryMethod(1)
                      : new NumericValueCreator().factoryMethod(0);
                case DOUBLE:
                  return Double.isNaN(value.getNumber().doubleValue())
                      ? new NumericValueCreator().factoryMethod(1)
                      : new NumericValueCreator().factoryMethod(0);
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesIsInfinity(functionName)) {
          if (parameterValues.size() == 1) {
            NumberInterface value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isInfinite(value.getNumber().floatValue())
                      ? new NumericValueCreator().factoryMethod(1)
                      : new NumericValueCreator().factoryMethod(0);
                case DOUBLE:
                  return Double.isInfinite(value.getNumber().doubleValue())
                      ? new NumericValueCreator().factoryMethod(1)
                      : new NumericValueCreator().factoryMethod(0);
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFinite(functionName)) {
          if (parameterValues.size() == 1) {
            NumberInterface value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isInfinite(value.getNumber().floatValue())
                      ? new NumericValueCreator().factoryMethod(0)
                      : new NumericValueCreator().factoryMethod(1);
                case DOUBLE:
                  return Double.isInfinite(value.getNumber().doubleValue())
                      ? new NumericValueCreator().factoryMethod(0)
                      : new NumericValueCreator().factoryMethod(1);
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFloor(functionName)) {
          if (parameterValues.size() == 1) {
            NumberInterface parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValueCreator()
                    .factoryMethod(((BigDecimal) number).setScale(0, BigDecimal.ROUND_FLOOR));
              } else if (number instanceof Float) {
                return new NumericValueCreator().factoryMethod(Math.floor(number.floatValue()));
              } else if (number instanceof Double) {
                return new NumericValueCreator().factoryMethod(Math.floor(number.doubleValue()));
              } else if (number instanceof NegativeNaN) { return parameter; }
            }
          }
        } else if (BuiltinFloatFunctions.matchesCeil(functionName)) {
          if (parameterValues.size() == 1) {
            NumberInterface parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValueCreator()
                    .factoryMethod(((BigDecimal) number).setScale(0, BigDecimal.ROUND_CEILING));
              } else if (number instanceof Float) {
                return new NumericValueCreator().factoryMethod(Math.ceil(number.floatValue()));
              } else if (number instanceof Double) {
                return new NumericValueCreator().factoryMethod(Math.ceil(number.doubleValue()));
              } else if (number instanceof NegativeNaN) { return parameter; }
            }
          }
        } else if (BuiltinFloatFunctions.matchesRound(functionName)
            || BuiltinFloatFunctions.matchesLround(functionName)
            || BuiltinFloatFunctions.matchesLlround(functionName)) {
          if (parameterValues.size() == 1) {
            NumberInterface parameter = parameterValues.get(0);
            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValueCreator().factoryMethod(
                    ((BigDecimal) number).setScale(0, BigDecimal.ROUND_HALF_UP));
              } else if (number instanceof Float) {
                float f = number.floatValue();
                if (0 == f || Float.isInfinite(f)) { return parameter; }
                return new NumericValueCreator().factoryMethod(Math.round(f));
              } else if (number instanceof Double) {
                double d = number.doubleValue();
                if (0 == d || Double.isInfinite(d)) { return parameter; }
                return new NumericValueCreator().factoryMethod(Math.round(d));
              } else if (number instanceof NegativeNaN) { return parameter; }
            }
          }
        } else if (BuiltinFloatFunctions.matchesTrunc(functionName)) {
          if (parameterValues.size() == 1) {
            NumberInterface parameter = parameterValues.get(0);
            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return new NumericValueCreator()
                    .factoryMethod(((BigDecimal) number).setScale(0, BigDecimal.ROUND_DOWN));
              } else if (number instanceof Float) {
                float f = number.floatValue();
                if (0 == f || Float.isInfinite(f) || Float.isNaN(f)) {
                  // +/-0.0 and +/-INF and +/-NaN are returned unchanged
                  return parameter;
                }
                return new NumericValueCreator().factoryMethod(
                    BigDecimal.valueOf(number.floatValue())
                        .setScale(0, BigDecimal.ROUND_DOWN)
                        .floatValue());
              } else if (number instanceof Double) {
                double d = number.doubleValue();
                if (0 == d || Double.isInfinite(d) || Double.isNaN(d)) {
                  // +/-0.0 and +/-INF and +/-NaN are returned unchanged
                  return parameter;
                }
                return new NumericValueCreator().factoryMethod(
                    BigDecimal.valueOf(number.doubleValue())
                        .setScale(0, BigDecimal.ROUND_DOWN)
                        .doubleValue());
              } else if (number instanceof NegativeNaN) { return parameter; }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFdim(functionName)) {
          if (parameterValues.size() == 2) {
            NumberInterface operand1 = parameterValues.get(0);
            NumberInterface operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              NumberInterface result = fdim(op1, op2, functionName);
              if (!UnknownValue.getInstance().equals(result)) { return result; }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmax(functionName)) {
          if (parameterValues.size() == 2) {
            NumberInterface operand1 = parameterValues.get(0);
            NumberInterface operand2 = parameterValues.get(1);
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
            NumberInterface operand1 = parameterValues.get(0);
            NumberInterface operand2 = parameterValues.get(1);
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
            NumberInterface parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              Optional<Boolean> isNegative = isNegative(number);
              if (isNegative.isPresent()) { return new NumericValueCreator()
                  .factoryMethod(isNegative.get() ? 1 : 0); }
            }
          }
        } else if (BuiltinFloatFunctions.matchesCopysign(functionName)) {
          if (parameterValues.size() == 2) {
            NumberInterface target = parameterValues.get(0);
            NumberInterface source = parameterValues.get(1);
            if (target.isExplicitlyKnown() && source.isExplicitlyKnown()) {
              assert target.isNumericValue();
              assert source.isNumericValue();
              Number targetNumber = target.asNumericValue().getNumber();
              Number sourceNumber = source.asNumericValue().getNumber();
              Optional<Boolean> sourceNegative = isNegative(sourceNumber);
              Optional<Boolean> targetNegative = isNegative(targetNumber);
              if (sourceNegative.isPresent() && targetNegative.isPresent()) {
                if (sourceNegative.get() == targetNegative
                    .get()) { return new NumericValueCreator().factoryMethod(targetNumber); }
                return target.asNumericValue().negate();
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFloatClassify(functionName)) {

          if (parameterValues.size() == 1) {
            NumberInterface value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
                case FLOAT: {
                  float v = value.getNumber().floatValue();
                  if (Float.isNaN(v)) { return new NumericValueCreator().factoryMethod(0); }
                  if (Float.isInfinite(v)) { return new NumericValueCreator().factoryMethod(1); }
                  if (v == 0.0) { return new NumericValueCreator().factoryMethod(2); }
                  if (Float.toHexString(v)
                      .startsWith("0x0.")) { return new NumericValueCreator().factoryMethod(3); }
                  return new NumericValueCreator().factoryMethod(4);
                }
                case DOUBLE: {
                  double v = value.getNumber().doubleValue();
                  if (Double.isNaN(v)) { return new NumericValueCreator().factoryMethod(0); }
                  if (Double.isInfinite(v)) { return new NumericValueCreator().factoryMethod(1); }
                  if (v == 0.0) { return new NumericValueCreator().factoryMethod(2); }
                  if (Double.toHexString(v)
                      .startsWith("0x0.")) { return new NumericValueCreator().factoryMethod(3); }
                  return new NumericValueCreator().factoryMethod(4);
                }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesModf(functionName)) {
          if (parameterValues.size() == 2) {
            NumberInterface value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName);
              switch (paramType.getType()) {
                case FLOAT: {
                  long integralPart = (long) value.getNumber().floatValue();
                  float fractionalPart = value.getNumber().floatValue() - integralPart;
                  return new NumericValueCreator().factoryMethod(fractionalPart);
                }
                case DOUBLE: {
                  long integralPart = (long) value.getNumber().doubleValue();
                  double fractionalPart = value.getNumber().doubleValue() - integralPart;
                  return new NumericValueCreator().factoryMethod(fractionalPart);
                }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFremainder(functionName)) {
          if (parameterValues.size() == 2) {
            NumberInterface numer = parameterValues.get(0);
            NumberInterface denom = parameterValues.get(1);
            if (numer.isExplicitlyKnown() && denom.isExplicitlyKnown()) {
              switch (BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName).getType()) {
                case FLOAT: {
                  float num = numer.getNumber().floatValue();
                  float den = denom.getNumber().floatValue();
                  if (Float.isNaN(num) || Float.isNaN(den) || Float.isInfinite(num)
                      || den == 0) { return new NumericValueCreator().factoryMethod(Float.NaN); }
                  return new NumericValueCreator()
                      .factoryMethod((float) Math.IEEEremainder(num, den));
                }
                case DOUBLE: {
                  double num = numer.getNumber().doubleValue();
                  double den = denom.getNumber().doubleValue();
                  if (Double.isNaN(num)
                      || Double.isNaN(den)
                      || Double.isInfinite(num)
                      || den == 0) { return new NumericValueCreator().factoryMethod(Double.NaN); }
                  return new NumericValueCreator().factoryMethod(Math.IEEEremainder(num, den));
                }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmod(functionName)) {
          if (parameterValues.size() == 2) {
            NumberInterface numer = parameterValues.get(0);
            NumberInterface denom = parameterValues.get(1);
            if (numer.isExplicitlyKnown() && denom.isExplicitlyKnown()) {
              //              NumericValue numerValue = numer.asNumericValue();
              //              NumericValue denomValue = denom.asNumericValue();
              switch (BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(functionName).getType()) {
                case FLOAT: {
                  float num = numer.getNumber().floatValue();
                  float den = denom.getNumber().floatValue();
                  if (Float.isNaN(num) || Float.isNaN(den) || Float.isInfinite(num)
                      || den == 0) { return new NumericValueCreator().factoryMethod(Float.NaN); }
                  if (num == 0 && den != 0) {
                    // keep the sign on +0 and -0
                    return numer;
                  }
                  // TODO computations on float/double are imprecise! Use epsilon environment?
                  return new NumericValueCreator().factoryMethod(num % den);
                }
                case DOUBLE: {
                  double num = numer.getNumber().doubleValue();
                  double den = denom.getNumber().doubleValue();
                  if (Double.isNaN(num)
                      || Double.isNaN(den)
                      || Double.isInfinite(num)
                      || den == 0) { return new NumericValueCreator().factoryMethod(Double.NaN); }
                  if (num == 0 && den != 0) {
                    // keep the sign on +0 and -0
                    return numer;
                  }
                  // TODO computations on float/double are imprecise! Use epsilon environment?
                  return new NumericValueCreator().factoryMethod(num % den);
                }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesIsgreater(functionName)) {
          NumberInterface op1 = parameterValues.get(0);
          NumberInterface op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValueCreator().factoryMethod(num1 > num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIsgreaterequal(functionName)) {
          NumberInterface op1 = parameterValues.get(0);
          NumberInterface op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValueCreator().factoryMethod(num1 >= num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIsless(functionName)) {
          NumberInterface op1 = parameterValues.get(0);
          NumberInterface op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValueCreator().factoryMethod(num1 < num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIslessequal(functionName)) {
          NumberInterface op1 = parameterValues.get(0);
          NumberInterface op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValueCreator().factoryMethod(num1 <= num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIslessgreater(functionName)) {
          NumberInterface op1 = parameterValues.get(0);
          NumberInterface op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValueCreator().factoryMethod(num1 > num2 || num1 < num2 ? 1 : 0);
          }
        } else if (BuiltinFloatFunctions.matchesIsunordered(functionName)) {
          NumberInterface op1 = parameterValues.get(0);
          NumberInterface op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return new NumericValueCreator()
                .factoryMethod(Double.isNaN(num1) || Double.isNaN(num2) ? 1 : 0);
          }
        }
      }
    }

    return UnknownValue.getInstance();
  }

  private NumberInterface fmax(Number pOp1, Number pOp2) {
    if (Double.isNaN(pOp1.doubleValue())
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() < 0)
        || (Double.isInfinite(pOp2.doubleValue())
            && pOp2.doubleValue() > 0)) { return new NumericValueCreator().factoryMethod(pOp2); }
    if (Double.isNaN(pOp2.doubleValue())
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() < 0)
        || (Double.isInfinite(pOp1.doubleValue())
            && pOp1.doubleValue() > 0)) { return new NumericValueCreator().factoryMethod(pOp1); }

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

    if (op1bd.compareTo(op2bd) > 0) { return new NumericValueCreator().factoryMethod(op1bd); }
    return new NumericValueCreator().factoryMethod(op2bd);
  }

  private NumberInterface fmin(Number pOp1, Number pOp2) {
    if (Double.isNaN(pOp1.doubleValue())
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() > 0)
        || (Double.isInfinite(pOp2.doubleValue())
            && pOp2.doubleValue() < 0)) { return new NumericValueCreator().factoryMethod(pOp2); }
    if (Double.isNaN(pOp2.doubleValue())
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() > 0)
        || (Double.isInfinite(pOp1.doubleValue())
            && pOp1.doubleValue() < 0)) { return new NumericValueCreator().factoryMethod(pOp1); }

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

    if (op1bd.compareTo(op2bd) < 0) { return new NumericValueCreator().factoryMethod(op1bd); }
    return new NumericValueCreator().factoryMethod(op2bd);
  }

  private NumberInterface fdim(Number pOp1, Number pOp2, String pFunctionName) {
    if (Double.isNaN(pOp1.doubleValue()) || Double
        .isNaN(pOp2.doubleValue())) { return new NumericValueCreator().factoryMethod(Double.NaN); }

    if (Double.isInfinite(pOp1.doubleValue())) {
      if (Double.isInfinite(pOp2.doubleValue())) {
        if (pOp1.doubleValue() > pOp2.doubleValue()) { return new NumericValueCreator()
            .factoryMethod(pOp1.doubleValue() - pOp2.doubleValue()); }
        return new NumericValueCreator().factoryMethod(0.0);
      }
      if (pOp1.doubleValue() < 0) { return new NumericValueCreator().factoryMethod(0.0); }
      return new NumericValueCreator().factoryMethod(pOp1);
    }
    if (Double.isInfinite(pOp2.doubleValue())) {
      if (pOp2.doubleValue() < 0) { return new NumericValueCreator().factoryMethod(Double.NaN); }
      return new NumericValueCreator().factoryMethod(0.0);
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
          return UnknownValue.getInstance();
      }
      if (difference.compareTo(maxValue) > 0) { return new NumericValueCreator()
          .factoryMethod(Double.POSITIVE_INFINITY); }
      return new NumericValueCreator().factoryMethod(difference);
    }
    return new NumericValueCreator().factoryMethod(0.0);
  }

  private Optional<Boolean> isNegative(Number pNumber) {
    if (pNumber instanceof BigDecimal) {
      return Optional.of(((BigDecimal) pNumber).signum() < 0);
    } else if (pNumber instanceof Float) {
      float number = pNumber.floatValue();
      if (Float.isNaN(number)) { return Optional.of(false); }
      return Optional.of(number < 0 || 1 / number < 0);
    } else if (pNumber instanceof Double) {
      double number = pNumber.doubleValue();
      if (Double.isNaN(number)) { return Optional.of(false); }
      return Optional.of(number < 0 || 1 / number < 0);
    } else if (pNumber instanceof NegativeNaN) { return Optional.of(true); }
    return Optional.empty();
  }

  private boolean isUnspecifiedType(CType pType) {
    return pType instanceof CSimpleType
        && ((CSimpleType) pType).getType() == CBasicType.UNSPECIFIED;
  }

  @Override
  public NumberInterface visit(CCharLiteralExpression pE) throws UnrecognizedCCodeException {
    return new NumericValueCreator().factoryMethod((long) pE.getCharacter());
  }

  @Override
  public NumberInterface visit(CFloatLiteralExpression pE) throws UnrecognizedCCodeException {
    return new NumericValueCreator().factoryMethod(pE.getValue());
  }

  @Override
  public NumberInterface visit(CIntegerLiteralExpression pE) throws UnrecognizedCCodeException {
    return new NumericValueCreator().factoryMethod(pE.getValue());
  }

  @Override
  public NumberInterface visit(CImaginaryLiteralExpression pE) throws UnrecognizedCCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public NumberInterface visit(CStringLiteralExpression pE) throws UnrecognizedCCodeException {
    return UnknownValue.getInstance();
  }

  @Override
  public NumberInterface visit(final CTypeIdExpression pE) {
    final TypeIdOperator idOperator = pE.getOperator();
    final CType innerType = pE.getType();

    switch (idOperator) {
      case SIZEOF:
        int size = machineModel.getSizeof(innerType);
        return new NumericValueCreator().factoryMethod(size);

      default: // TODO support more operators
        return UnknownValue.getInstance();
    }
  }

  @Override
  public NumberInterface visit(CIdExpression idExp) throws UnrecognizedCCodeException {
    if (idExp.getDeclaration() instanceof CEnumerator) {
      CEnumerator enumerator = (CEnumerator) idExp.getDeclaration();
      if (enumerator.hasValue()) {
        // TODO rewrite CEnumerator to handle abstract type Value and not just Long
        return new NumericValueCreator().factoryMethod(enumerator.getValue());
      } else {
        return UnknownValue.getInstance();
      }
    }

    return evaluateCIdExpression(idExp);
  }

  @Override
  public NumberInterface visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
    final UnaryOperator unaryOperator = unaryExpression.getOperator();
    final CExpression unaryOperand = unaryExpression.getOperand();

    if (unaryOperator == UnaryOperator.SIZEOF) { return new NumericValueCreator()
        .factoryMethod(machineModel.getSizeof(unaryOperand.getExpressionType())); }
    if (unaryOperator == UnaryOperator.ALIGNOF) { return new NumericValueCreator()
        .factoryMethod(machineModel.getAlignof(unaryOperand.getExpressionType())); }
    if (unaryOperator == UnaryOperator.AMPER) { return UnknownValue.getInstance(); }

    final NumberInterface value = unaryOperand.accept(this);

    if (value.isUnknown()) { return UnknownValue.getInstance(); }

    if (value instanceof SymbolicValue) {
      final CType expressionType = unaryExpression.getExpressionType();
      final CType operandType = unaryOperand.getExpressionType();

      return createSymbolicExpression(value, operandType, unaryOperator, expressionType);

    } else if (!value.isNumericValue()) {
      logger.logf(Level.FINE, "Invalid argument %s for unary operator %s.", value, unaryOperator);
      return UnknownValue.getInstance();
    }
    switch (unaryOperator) {
      case MINUS:
        return value.negate();

      case TILDE:
        return new NumericValueCreator().factoryMethod(~(value).getNumber().longValue());

      default:
        throw new AssertionError("unknown operator: " + unaryOperator);
    }
  }

  private NumberInterface createSymbolicExpression(NumberInterface pValue, CType pOperandType,
      UnaryOperator pUnaryOperator,
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
  public NumberInterface visit(CPointerExpression pointerExpression)
      throws UnrecognizedCCodeException {
    return evaluateCPointerExpression(pointerExpression);
  }

  @Override
  public NumberInterface visit(CFieldReference fieldReferenceExpression)
      throws UnrecognizedCCodeException {
    return evaluateCFieldReference(fieldReferenceExpression);
  }

  @Override
  public NumberInterface visit(CArraySubscriptExpression pE)
      throws UnrecognizedCCodeException {
    return evaluateCArraySubscriptExpression(pE);
  }

  @Override
  public NumberInterface visit(JCharLiteralExpression pE) {
    return new NumericValueCreator().factoryMethod((long) pE.getCharacter());
  }

  @Override
  public NumberInterface visit(JBinaryExpression pE) {
    JBinaryExpression.BinaryOperator binaryOperator = pE.getOperator();
    JExpression lVarInBinaryExp = pE.getOperand1();
    JExpression rVarInBinaryExp = pE.getOperand2();
    JType lValType = lVarInBinaryExp.getExpressionType();
    JType rValType = rVarInBinaryExp.getExpressionType();
    JType expressionType = pE.getExpressionType();

    // Get the concrete values of the lefthandside and righthandside
    final NumberInterface lValue = lVarInBinaryExp.accept(this);
    if (lValue.isUnknown()) { return UnknownValue.getInstance(); }

    final NumberInterface rValue = rVarInBinaryExp.accept(this);
    if (rValue.isUnknown()) { return UnknownValue.getInstance(); }

    try {
      return calculateBinaryOperation(binaryOperator, lValue, lValType, rValue, rValType,
          expressionType, pE);

    } catch (IllegalOperationException e) {
      logger.logUserException(Level.SEVERE, e, pE.getFileLocation().toString());
      return UnknownValue.getInstance();
    }
  }

  private NumberInterface calculateBinaryOperation(JBinaryExpression.BinaryOperator pOperator,
      NumberInterface pLValue, JType pLType, NumberInterface pRValue, JType pRType, JType pExpType,
      JBinaryExpression pExpression)
      throws IllegalOperationException {

    assert !pLValue.isUnknown() && !pRValue.isUnknown();

    if (pLValue instanceof SymbolicValue || pRValue instanceof SymbolicValue) {
      final JType expressionType = pExpression.getExpressionType();

      return createSymbolicExpression(pLValue, pLType, pRValue, pRType, pOperator, expressionType,
          expressionType);

      //    } else if (pLValue instanceof NumericValue) {
    } else if (pLValue.isNumericValue()) {

      //      assert pRValue instanceof NumericValue;
      assert pRValue.isNumericValue();
      assert pLType instanceof JSimpleType && pRType instanceof JSimpleType;
      assert pExpType instanceof JSimpleType;

      if (isFloatType(pLType) || isFloatType(pRType)) {
        return calculateFloatOperation(pLValue, pRValue,
            pOperator, ((JSimpleType) pLType).getType(), ((JSimpleType) pRType).getType());

      } else {
        return calculateIntegerOperation(pLValue, pRValue,
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

  private NumberInterface createSymbolicExpression(NumberInterface pLeftValue, JType pLeftType,
      NumberInterface pRightValue,
      JType pRightType, JBinaryExpression.BinaryOperator pOperator, JType pExpressionType,
      JType pCalculationType) {
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
        return factory.shiftRightSigned(leftOperand, rightOperand, pExpressionType,
            pCalculationType);
      case SHIFT_RIGHT_UNSIGNED:
        return factory.shiftRightUnsigned(leftOperand, rightOperand, pExpressionType,
            pCalculationType);
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
        return factory.lessThanOrEqual(leftOperand, rightOperand, pExpressionType,
            pCalculationType);
      case GREATER_THAN:
        return factory.greaterThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(leftOperand, rightOperand, pExpressionType,
            pCalculationType);
      default:
        throw new AssertionError("Unhandled binary operation " + pOperator);
    }
  }

  /*
   * Calculates the result of the given operation for the given integer values.
   * The given values have to be of a Java integer type, that is long, int, short, or byte.
   */
  private NumberInterface calculateIntegerOperation(NumberInterface pLeftValue,
      NumberInterface pRightValue,
      JBinaryExpression.BinaryOperator pBinaryOperator, JBasicType pLeftType,
      JBasicType pRightType) throws IllegalOperationException {

    checkNotNull(pLeftType);
    checkNotNull(pRightType);

    final long lVal = pLeftValue.getNumber().longValue();
    final long rVal = pRightValue.getNumber().longValue();
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
            if (rVal == 0) { throw new IllegalOperationException(
                "Division by zero: " + lVal + " / " + rVal); }

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

        return new NumericValueCreator().factoryMethod(numResult);
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
  private NumberInterface calculateFloatOperation(NumberInterface pLeftValue,
      NumberInterface pRightValue,
      JBinaryExpression.BinaryOperator pBinaryOperator,
      JBasicType pLeftOperand, JBasicType pRightOperand) throws IllegalOperationException {

    final double lVal;
    final double rVal;

    if (pLeftOperand != JBasicType.DOUBLE && pRightOperand != JBasicType.DOUBLE) {
      lVal = pLeftValue.getNumber().floatValue();
      rVal = pRightValue.getNumber().floatValue();
    } else {
      lVal = pLeftValue.getNumber().doubleValue();
      rVal = pRightValue.getNumber().doubleValue();
    }

    switch (pBinaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MULTIPLY:
      case MODULO: {

        switch (pBinaryOperator) {
          case PLUS:
            return new NumericValueCreator().factoryMethod(lVal + rVal);

          case MINUS:
            return new NumericValueCreator().factoryMethod(lVal - rVal);

          case DIVIDE:
            if (rVal == 0) { throw new IllegalOperationException(
                "Division by zero: " + lVal + " / " + rVal); }
            return new NumericValueCreator().factoryMethod(lVal / rVal);

          case MULTIPLY:
            return new NumericValueCreator().factoryMethod(lVal * rVal);

          case MODULO:
            return new NumericValueCreator().factoryMethod(lVal % rVal);

          default:
            throw new AssertionError(
                "Unsupported binary operation " + pBinaryOperator.toString() + " on double values");
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

  private NumberInterface calculateBooleanOperation(boolean lVal, boolean rVal,
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

  private NumberInterface calculateComparison(NumberInterface pLeftValue,
      NumberInterface pRightValue,
      JBinaryExpression.BinaryOperator pOperator) {
    assert pOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS
        || pOperator == JBinaryExpression.BinaryOperator.EQUALS;

    return BooleanValue.valueOf(pOperator != JBinaryExpression.BinaryOperator.EQUALS
        ^ pLeftValue.equals(pRightValue));
  }

  @Override
  public NumberInterface visit(JIdExpression idExp) {


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
  public NumberInterface visit(JUnaryExpression unaryExpression) {

    JUnaryExpression.UnaryOperator unaryOperator = unaryExpression.getOperator();
    JExpression unaryOperand = unaryExpression.getOperand();
    final NumberInterface valueObject = unaryOperand.accept(this);

    // possible error msg if no case fits
    final String errorMsg =
        "Invalid argument [" + valueObject + "] for unary operator [" + unaryOperator + "].";

    if (valueObject.isUnknown()) {
      return UnknownValue.getInstance();

    } else if (valueObject.isNumericValue()) {
      switch (unaryOperator) {
        case MINUS:
          return valueObject.negate();

        case COMPLEMENT:
          return evaluateComplement(unaryOperand, valueObject);

        case PLUS:
          return valueObject;

        default:
          logger.log(Level.FINE, errorMsg);
          return UnknownValue.getInstance();
      }

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

  private NumberInterface createSymbolicExpression(NumberInterface pValue, JType pOperandType,
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

  private NumberInterface evaluateComplement(JExpression pExpression, NumberInterface value) {
    JType type = pExpression.getExpressionType();

    if (isIntegerType(type)) {
      return new NumericValueCreator().factoryMethod(~value.getNumber().longValue());

    } else {
      logger.logf(Level.FINE, "Invalid argument %s for unary operator ~.", value);
      return UnknownValue.getInstance();
    }
  }

  private static boolean isIntegerType(JType type) {
    return type instanceof JSimpleType && ((JSimpleType) type).getType().isIntegerType();

  }

  private static boolean isFloatType(JType type) {
    return type instanceof JSimpleType && ((JSimpleType) type).getType().isFloatingPointType();
  }

  @Override
  public NumberInterface visit(JIntegerLiteralExpression pE) {
    return new NumericValueCreator().factoryMethod(pE.asLong());
  }

  @Override
  public NumberInterface visit(JBooleanLiteralExpression pE) {
    return BooleanValue.valueOf(pE.getValue());
  }

  @Override
  public NumberInterface visit(JArraySubscriptExpression pJArraySubscriptExpression) {
    NumberInterface subscriptValue =
        pJArraySubscriptExpression.getSubscriptExpression().accept(this);
    JExpression arrayExpression = pJArraySubscriptExpression.getArrayExpression();
    NumberInterface idValue = arrayExpression.accept(this);

    if (!idValue.isUnknown()) {
      ArrayValue innerMostArray = (ArrayValue) arrayExpression.accept(this);

      assert subscriptValue.getNumber().longValue() >= 0
          && subscriptValue.getNumber().longValue() <= Integer.MAX_VALUE;
      return innerMostArray.getValueAt((int) subscriptValue.getNumber().longValue());

    } else {
      return UnknownValue.getInstance();
    }
  }

  @Override
  public NumberInterface visit(JArrayLengthExpression pJArrayLengthExpression) {
    final JExpression arrayId = pJArrayLengthExpression.getQualifier();

    NumberInterface array = arrayId.accept(this);

    if (!array.isExplicitlyKnown()) {
      return UnknownValue.getInstance();

    } else {
      assert array instanceof ArrayValue;
      return new NumericValueCreator().factoryMethod(((ArrayValue) array).getArraySize());
    }
  }

  @Override
  public NumberInterface visit(JEnumConstantExpression pJEnumConstantExpression) {
    String fullName = pJEnumConstantExpression.getConstantName();

    return new EnumConstantValue(fullName);
  }

  @Override
  public NumberInterface visit(JCastExpression pJCastExpression) {
    JExpression operand = pJCastExpression.getOperand();
    JType castType = pJCastExpression.getCastType();

    return castJValue(operand.accept(this), operand.getExpressionType(), castType, logger,
        pJCastExpression.getFileLocation());
  }

  @Override
  public NumberInterface visit(JMethodInvocationExpression pAFunctionCallExpression) {
    return UnknownValue.getInstance();
  }

  @Override
  public NumberInterface visit(JClassInstanceCreation pJClassInstanceCreation) {
    return UnknownValue.getInstance();
  }

  @Override
  public NumberInterface visit(JStringLiteralExpression pPaStringLiteralExpression) {
    return UnknownValue.getInstance();
  }

  @Override
  public NumberInterface visit(JFloatLiteralExpression pJBooleanLiteralExpression) {
    return new NumericValueCreator().factoryMethod(pJBooleanLiteralExpression.getValue());
  }

  @Override
  public NumberInterface visit(JArrayCreationExpression pJArrayCreationExpression) {
    List<JExpression> arraySizeExpressions = new ArrayList<>(pJArrayCreationExpression.getLength());
    NumberInterface lastArrayValue;
    NumberInterface currentArrayValue = null;
    int currentDimension = 0;
    long concreteArraySize;
    final JType elementType = pJArrayCreationExpression.getExpressionType().getElementType();

    Collections.reverse(arraySizeExpressions);
    for (JExpression sizeExpression : arraySizeExpressions) {
      currentDimension++;
      lastArrayValue = currentArrayValue;
      NumberInterface sizeValue = sizeExpression.accept(this);

      if (sizeValue.isUnknown()) {
        currentArrayValue = UnknownValue.getInstance();

      } else {
        concreteArraySize = sizeValue.getNumber().longValue();
        currentArrayValue =
            createArrayValue(new JArrayType(elementType, currentDimension), concreteArraySize);

        if (lastArrayValue != null) {
          NumberInterface newValue = lastArrayValue;

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

    if (pArraySize < 0 || pArraySize > Integer.MAX_VALUE) { throw new AssertionError(
        "Trying to create array of size " + pArraySize
            + ". Java arrays can't be smaller than 0 or bigger than the max int value."); }

    return new ArrayValue(pType, (int) pArraySize);
  }

  @Override
  public NumberInterface visit(JArrayInitializer pJArrayInitializer) {
    final JArrayType arrayType = pJArrayInitializer.getExpressionType();
    final List<JExpression> initializerExpressions = pJArrayInitializer.getInitializerExpressions();

    // this list stores the values in the array's slots, in occurring order
    List<NumberInterface> slotValues = new LinkedList<>();

    for (JExpression currentExpression : initializerExpressions) {
      slotValues.add(currentExpression.accept(this));
    }

    return new ArrayValue(arrayType, slotValues);
  }

  @Override
  public NumberInterface visit(JVariableRunTimeType pJThisRunTimeType) {
    return UnknownValue.getInstance();
  }

  @Override
  public NumberInterface visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) {
    return UnknownValue.getInstance();
  }

  @Override
  public NumberInterface visit(JNullLiteralExpression pJNullLiteralExpression) {
    return NullValue.getInstance();
  }

  @Override
  public NumberInterface visit(JThisExpression pThisExpression) {
    return UnknownValue.getInstance();
  }

  /* abstract methods */

  protected abstract NumberInterface evaluateCPointerExpression(
      CPointerExpression pCPointerExpression)
      throws UnrecognizedCCodeException;

  protected abstract NumberInterface evaluateCIdExpression(CIdExpression pCIdExpression)
      throws UnrecognizedCCodeException;

  protected abstract NumberInterface evaluateJIdExpression(JIdExpression varName);

  protected abstract NumberInterface evaluateCFieldReference(CFieldReference pLValue)
      throws UnrecognizedCCodeException;

  protected abstract NumberInterface evaluateCArraySubscriptExpression(
      CArraySubscriptExpression pLValue)
      throws UnrecognizedCCodeException;

  /* additional methods */

  public String getFunctionName() {
    return functionName;
  }

  public long getBitSizeof(CType pType) {
    return machineModel.getBitSizeof(pType);
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
  public NumberInterface evaluate(final CExpression pExp, final CType pTargetType)
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
  public NumberInterface evaluate(final CRightHandSide pExp, final CType pTargetType)
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
  public NumberInterface evaluate(final JRightHandSide pExp, final JType pTargetType) {
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
  public static NumberInterface castCValue(@Nonnull final NumberInterface value,
      final CType targetType,
      final MachineModel machineModel, final LogManagerWithoutDuplicates logger,
      final FileLocation fileLocation) {

    if (!value.isExplicitlyKnown()) { return castIfSymbolic(value, targetType,
        Optional.of(machineModel)); }

    // For now can only cast numeric value's
    if (!value.isNumericValue()) {
      logger.logf(Level.FINE, "Can not cast C value %s to %s", value.toString(),
          targetType.toString());
      return value;
    }

    final CType type = targetType.getCanonicalType();
    if (type instanceof CSimpleType) {
      final CSimpleType st = (CSimpleType) type;

      switch (st.getType()) {

        case INT:
        case CHAR: {
          final int size = machineModel.getSizeofInBits(st);
          final long longValue = value.getNumber().longValue();
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

            return new NumericValueCreator().factoryMethod(result);

          } else if (size == SIZE_OF_JAVA_LONG) {
            // we can handle this with java-type "long", because the bitwise representation is correct.
            // but for unsigned long we need BigInteger
            if (!targetIsSigned && longValue < 0) { return new NumericValueCreator().factoryMethod(
                BigInteger.valueOf(longValue).andNot(BigInteger.valueOf(-1).shiftLeft(size))); }
            return new NumericValueCreator().factoryMethod(longValue);

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
          float floatValue = value.getNumber().floatValue();
          NumberInterface result;

          final int bitPerByte = machineModel.getSizeofCharInBits();
          final int numBytes = machineModel.getSizeof(st);
          final int size = bitPerByte * numBytes;

          if (NegativeNaN.VALUE.equals(value.getNumber())) {
            result = value;
          } else if (size == SIZE_OF_JAVA_FLOAT) {
            // 32 bit means Java float
            result = new NumericValueCreator().factoryMethod(floatValue);
          } else if (size == SIZE_OF_JAVA_DOUBLE) {
            // 64 bit means Java double
            result = new NumericValueCreator().factoryMethod(floatValue);
          } else {
            throw new AssertionError("Trying to cast to unsupported floating point type: " + st);
          }

          return result;
        }
        case DOUBLE: {
          // TODO: look more closely at the INT/CHAR cases, especially at the loggedEdges stuff
          // TODO: check for overflow(source larger than the highest number we can store in target etc.)

          // casting to DOUBLE, if value is INT or FLOAT. This is sound, if we would also do this cast in C.
          double doubleValue = value.getNumber().doubleValue();
          NumberInterface result;

          final int bitPerByte = machineModel.getSizeofCharInBits();
          final int numBytes = machineModel.getSizeof(st);
          final int size = bitPerByte * numBytes;

          if (NegativeNaN.VALUE.equals(value.getNumber())) {
            result = value;
          } else if (size == SIZE_OF_JAVA_FLOAT) {
            // 32 bit means Java float
            result = new NumericValueCreator().factoryMethod((float) doubleValue);
          } else if (size == SIZE_OF_JAVA_DOUBLE) {
            // 64 bit means Java double
            result = new NumericValueCreator().factoryMethod(doubleValue);
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

  private static NumberInterface castIfSymbolic(NumberInterface pValue, Type pTargetType,
      Optional<MachineModel> pMachineModel) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    if (pValue instanceof SymbolicValue
        && (pTargetType instanceof JSimpleType || pTargetType instanceof CSimpleType)) {

    return factory.cast((SymbolicValue) pValue, pTargetType, pMachineModel); }

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
  public static NumberInterface castJValue(@Nonnull final NumberInterface value, JType sourceType,
      JType targetType, final LogManagerWithoutDuplicates logger, final FileLocation fileLocation) {

    if (!value.isExplicitlyKnown()) { return castIfSymbolic(value, targetType, Optional.empty()); }

    // Other than symbolic values, we can only cast numeric values, for now.
    if (!value.isNumericValue()) {
      logger.logf(Level.FINE, "Can not cast Java value %s to %s", value.toString(),
          targetType.toString());
      return value;
    }


    if (targetType instanceof JSimpleType) {
      final JSimpleType st = (JSimpleType) targetType;

      if (isIntegerType(sourceType)) {
        long longValue = value.getNumber().longValue();

        return createValue(longValue, st.getType());

      } else if (isFloatType(sourceType)) {
        double doubleValue = value.getNumber().doubleValue();

        return createValue(doubleValue, st.getType());

      } else {
        throw new AssertionError("Cast from " + sourceType.toString() + " to "
            + targetType.toString() + " not possible.");
      }
    } else {
      return value; // TODO handle casts between object types
    }
  }

  private static NumberInterface createValue(long value, JBasicType targetType) {
    switch (targetType) {
      case BYTE:
        return new NumericValueCreator().factoryMethod((byte) value);

      case CHAR:
        char castedValue = (char) value;
        return new NumericValueCreator().factoryMethod((int) castedValue);
      case SHORT:
        return new NumericValueCreator().factoryMethod((short) value);

      case INT:
        return new NumericValueCreator().factoryMethod((int) value);

      case LONG:
        return new NumericValueCreator().factoryMethod(value);

      case FLOAT:
        return new NumericValueCreator().factoryMethod((float) value);

      case DOUBLE:
        return new NumericValueCreator().factoryMethod((double) value);

      default:
        throw new AssertionError("Trying to cast to unsupported type " + targetType);
    }
  }

  private static NumberInterface createValue(double value, JBasicType targetType) {
    switch (targetType) {
      case BYTE:
        return new NumericValueCreator().factoryMethod((byte) value);

      case CHAR:
      case SHORT:
        return new NumericValueCreator().factoryMethod((short) value);

      case INT:
        return new NumericValueCreator().factoryMethod((int) value);

      case LONG:
        return new NumericValueCreator().factoryMethod(value);

      case FLOAT:
        return new NumericValueCreator().factoryMethod((float) value);

      case DOUBLE:
        return new NumericValueCreator().factoryMethod(value);

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
