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

import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.cpa.value.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.primitives.UnsignedLongs;


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

  /** length of type LONG in Java. */
  private final static int SIZE_OF_JAVA_LONG = 64;

  //private final ValueAnalysisState state;
  private final String functionName;
  private final MachineModel machineModel;


  // for logging
  private final LogManagerWithoutDuplicates logger;

  private boolean missingFieldAccessInformation = false;
  private boolean missingEnumComparisonInformation = false;

  /** This Visitor returns the numeral value for an expression.
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   */
  public AbstractExpressionValueVisitor(String pFunctionName,
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {

    //this.state = pState;
    this.functionName = pFunctionName;
    this.machineModel = pMachineModel;
    this.logger = pLogger;
  }

  public boolean hasMissingFieldAccessInformation() {
    return missingFieldAccessInformation;
  }

  public boolean hasMissingEnumComparisonInformation() {
    return missingEnumComparisonInformation;
  }

  @Override
  protected Value visitDefault(CExpression pExp) {
    return Value.UnknownValue.getInstance();
  }

  public void reset() {
    missingFieldAccessInformation = false;
    missingEnumComparisonInformation = false;
  }

  @Override
  public Value visit(final CBinaryExpression pE) throws UnrecognizedCCodeException {
    final Value lVal = pE.getOperand1().accept(this);
    if (lVal.isUnknown()) { return Value.UnknownValue.getInstance(); }
    final Value rVal = pE.getOperand2().accept(this);
    if (rVal.isUnknown()) { return Value.UnknownValue.getInstance(); }
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

    lVal = castCValue(lVal, binaryExpr.getOperand1().getExpressionType(), calculationType, machineModel, logger, binaryExpr.getFileLocation());
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
          castCValue(rVal, binaryExpr.getOperand2().getExpressionType(), calculationType, machineModel, logger, binaryExpr.getFileLocation());
    }

    if (lVal instanceof SymbolicValueFormula || rVal instanceof SymbolicValueFormula) {
      return calculateSymbolicBinaryExpression(lVal, rVal, binaryExpr, logger);
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
      result = castCValue(result, calculationType, binaryExpr.getExpressionType(), machineModel, logger, binaryExpr.getFileLocation());

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
   * Join a symbolic formula with something else using a binary expression.
   *
   * e.g. joining `a` and `5` with `+` will produce `a + 5`
   *
   * @param lVal left hand side value
   * @param rVal right hand side value
   * @param binaryExpr the binary expression with the operator
   * @param logger logging
   * @return the calculated Value
   */
  public static Value calculateSymbolicBinaryExpression(Value lVal, Value rVal,
      final CBinaryExpression binaryExpr, LogManagerWithoutDuplicates logger) {

    // Convert the CBinaryOperator to an operator suitable for our symbolic value formulas
    SymbolicValueFormula.BinaryExpression.BinaryOperator op =
        SymbolicValueFormula.BinaryExpression.BinaryOperator.fromString(
            binaryExpr.getOperator().getOperator());

    // If there's no suitable operator, return UNKNOWN
    if(op == null) {
      return Value.UnknownValue.getInstance();
    }

    SymbolicValueFormula.ExpressionBase leftHand =
        SymbolicValueFormula.expressionFromExplicitValue(lVal);
    SymbolicValueFormula.ExpressionBase rightHand =
        SymbolicValueFormula.expressionFromExplicitValue(rVal);

    return new SymbolicValueFormula(new SymbolicValueFormula.BinaryExpression(leftHand, rightHand, op, binaryExpr.getExpressionType(), binaryExpr.getCalculationType())).simplify(logger);
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
      if (machineModel.getSizeof(st) * machineModel.getSizeofCharInBits() >= SIZE_OF_JAVA_LONG
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
      if (r == 0) {
        logger.logf(Level.SEVERE, "Division by Zero (%f / %f)", l, r);
        return 0;
      }
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
      if (r == 0) {
        logger.logf(Level.SEVERE, "Division by Zero (%f / %f)", l, r);
        return 0;
      }
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
        cmp = Long.compare(l.longValue(), r.longValue());
        break;
      }
      case FLOAT: {
        cmp = Float.compare(l.floatValue(), r.floatValue());
        break;
      }
      case DOUBLE: {
        cmp = Double.compare(l.doubleValue(),r.doubleValue());
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
    return castCValue(pE.getOperand().accept(this), pE.getOperand().getExpressionType(), pE.getExpressionType(),
        machineModel, logger, pE.getFileLocation());
  }

  @Override
  public Value visit(CComplexCastExpression pE) throws UnrecognizedCCodeException {
    // evaluation of complex numbers is not supported by now
    return Value.UnknownValue.getInstance();
  }

  @Override
  public Value visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
    return Value.UnknownValue.getInstance();
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

    if (!value.isNumericValue()) {
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

    // TODO: for logical expressions, work with unknown values (for example 'true || unknown' is 'true')
    // Get the concrete values of the lefthandside and righthandside
    final Value lValue = lVarInBinaryExp.accept(this);
    if (lValue.isUnknown()) {
      return UnknownValue.getInstance();
    }

    final Value rValue = rVarInBinaryExp.accept(this);
    if (rValue.isUnknown()) {
      return UnknownValue.getInstance();
    }

    // Calculate the result of the expression
    if (lValue instanceof NumericValue && rValue instanceof NumericValue) {

      // calculate the result for double values
      if (isFloatType(lValType) || isFloatType(rValType)) {
        final double lVal = ((NumericValue) lValue).doubleValue();
        final double rVal = ((NumericValue) rValue).doubleValue();

        return calculateBinaryOperation(lVal, rVal, binaryOperator);

      // calculate the result for integer values
      } else {
        final long lVal = ((NumericValue) lValue).longValue();
        final long rVal = ((NumericValue) rValue).longValue();

        return calculateBinaryOperation(lVal, rVal, binaryOperator);
      }

    // calculate the result for enum constant and null values
    } else if (isEnumType(lValue)) {

      assert isEnumType(rValue);
      assert binaryOperator.equals(JBinaryExpression.BinaryOperator.EQUALS)
        || binaryOperator.equals(JBinaryExpression.BinaryOperator.NOT_EQUALS);

      if (binaryOperator.equals(JBinaryExpression.BinaryOperator.EQUALS)) {
        if (lValue.equals(rValue)) {
          return BooleanValue.valueOf(true);

        } else {
          return BooleanValue.valueOf(false);
        }

      // binary operator has to be NOT_EQUALS since no other operators are allowed for enums
      } else if (lValue.equals(rValue)) {
          return BooleanValue.valueOf(false);

      } else {
          return BooleanValue.valueOf(true);
      }
    } else if (lValue instanceof BooleanValue) {
      assert rValue instanceof BooleanValue;

      boolean lVal = ((BooleanValue) lValue).isTrue();
      boolean rVal = ((BooleanValue) rValue).isTrue();

      return calculateBinaryOperation(lVal, rVal, binaryOperator);

    } else {
      return UnknownValue.getInstance();
    }
  }

  private Value calculateBinaryOperation(long lVal, long rVal, JBinaryExpression.BinaryOperator binaryOperator) {

    switch (binaryOperator) {
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

      switch (binaryOperator) {
      case PLUS:
        return new NumericValue(lVal + rVal);

      case MINUS:
        return new NumericValue(lVal - rVal);

      case DIVIDE:
        if (rVal == 0) {
          logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", lVal, rVal);
          return UnknownValue.getInstance();
        }
        return new NumericValue(lVal / rVal);

      case MULTIPLY:
        return new NumericValue(lVal * rVal);

      case SHIFT_LEFT:
        return new NumericValue(lVal << rVal);

      case BINARY_AND:
        return new NumericValue(lVal & rVal);

      case BINARY_OR:
        return new NumericValue(lVal | rVal);

      case BINARY_XOR:
        return new NumericValue(lVal ^ rVal);

      case MODULO:
        return new NumericValue(lVal % rVal);

      case SHIFT_RIGHT_SIGNED:
        return new NumericValue(lVal >> rVal);
      case SHIFT_RIGHT_UNSIGNED:
        return new NumericValue(lVal >>> rVal);

      default:
        throw new AssertionError();
      }
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {

      final long l = lVal;
      final long r = rVal;

      final boolean result;
      switch (binaryOperator) {
      case EQUALS:
        result = (l == r);
        break;
      case NOT_EQUALS:
        result = (l != r);
        break;
      case GREATER_THAN:
        result = (l > r);
        break;
      case GREATER_EQUAL:
        result = (l >= r);
        break;
      case LESS_THAN:
        result = (l < r);
        break;
      case LESS_EQUAL:
        result = (l <= r);
        break;

      default:
        throw new AssertionError();
      }

      // return 1 if expression holds, 0 otherwise
      return (result ? new NumericValue(1L) : new NumericValue(0L));
    }
    default:
      // TODO check which cases can be handled
      return UnknownValue.getInstance();
    }
  }

  private Value calculateBinaryOperation(double lVal, double rVal, JBinaryExpression.BinaryOperator binaryOperator) {

    switch (binaryOperator) {
    case PLUS:
    case MINUS:
    case DIVIDE:
    case MULTIPLY:
    case MODULO: {

      switch (binaryOperator) {
      case PLUS:
        return new NumericValue(lVal + rVal);

      case MINUS:
        return new NumericValue(lVal - rVal);

      case DIVIDE:
        if (rVal == 0) {
          logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", lVal, rVal);
          return UnknownValue.getInstance();
        }
        return new NumericValue(lVal / rVal);

      case MULTIPLY:
        return new NumericValue(lVal * rVal);

      case MODULO:
        return new NumericValue(lVal % rVal);

      default:
        throw new AssertionError("Unsupported binary operation " + binaryOperator.toString() + " on double values");
      }
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {

      final double l = lVal;
      final double r = rVal;

      final boolean result;
      switch (binaryOperator) {
      case EQUALS:
        result = (l == r);
        break;
      case NOT_EQUALS:
        result = (l != r);
        break;
      case GREATER_THAN:
        result = (l > r);
        break;
      case GREATER_EQUAL:
        result = (l >= r);
        break;
      case LESS_THAN:
        result = (l < r);
        break;
      case LESS_EQUAL:
        result = (l <= r);
        break;

      default:
        throw new AssertionError("Unsupported binary operation " + binaryOperator.toString() + " on double values");
      }

      // return 1 if expression holds, 0 otherwise
      return (result ? new NumericValue(1L) : new NumericValue(0L));
    }
    default:
      // TODO check which cases can be handled
      return UnknownValue.getInstance();
    }
  }

  private Value calculateBinaryOperation(boolean lVal, boolean rVal, JBinaryExpression.BinaryOperator operator) {

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

  private boolean isEnumType(Value value) {
    return value instanceof NullValue || value instanceof EnumConstantValue;
  }

  @Override
  public Value visit(JIdExpression idExp) {


    IASimpleDeclaration decl = idExp.getDeclaration();

    // Java IdExpression could not be resolved
    if (decl == null) { return UnknownValue.getInstance(); }

    if (decl instanceof JFieldDeclaration
        && !((JFieldDeclaration) decl).isStatic()) {
      missingFieldAccessInformation = true;
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
      NumericValue value = (NumericValue) valueObject;

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

    } else if (valueObject instanceof BooleanValue && unaryOperator.equals(JUnaryExpression.UnaryOperator.NOT)) {
      return ((BooleanValue) valueObject).negate();

    } else {
      logger.logf(Level.FINE, errorMsg);
      return UnknownValue.getInstance();
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
    if (!(type instanceof JSimpleType)) {
      return false;
    }

    JBasicType concreteType = ((JSimpleType) type).getType();

    return concreteType.equals(JBasicType.BYTE)
      || concreteType.equals(JBasicType.CHAR)
      || concreteType.equals(JBasicType.INT)
      || concreteType.equals(JBasicType.LONG)
      || concreteType.equals(JBasicType.SHORT);
  }

  private static boolean isFloatType(JType type) {
    if (!(type instanceof JSimpleType)) {
      return false;
    }

    JBasicType concreteType = ((JSimpleType) type).getType();

    return concreteType.equals(JBasicType.FLOAT)
        || concreteType.equals(JBasicType.DOUBLE);
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
  public Value visit(JArraySubscriptExpression pAArraySubscriptExpression) {
    return pAArraySubscriptExpression.getSubscriptExpression().accept(this);
  }

  @Override
  public Value visit(JEnumConstantExpression pJEnumConstantExpression) {
    JClassType enumType = pJEnumConstantExpression.getExpressionType();
    String fullName = pJEnumConstantExpression.getConstantName();

    return new EnumConstantValue(enumType, fullName);
  }

  @Override
  public Value visit(JCastExpression pJCastExpression) {
    JExpression operand = pJCastExpression.getOperand();
    JType castType = pJCastExpression.getCastType();

    return castJValue(operand.accept(this), operand.getExpressionType(), castType, logger, pJCastExpression.getFileLocation());
  }

  @Override
  public Value visit(JMethodInvocationExpression pAFunctionCallExpression) throws RuntimeException {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JClassInstanceCreation pJClassInstanzeCreation) throws RuntimeException {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JStringLiteralExpression pPaStringLiteralExpression) throws RuntimeException {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JFloatLiteralExpression pJBooleanLiteralExpression) throws RuntimeException {
    return new NumericValue(pJBooleanLiteralExpression.getValue());
  }

  @Override
  public Value visit(JArrayCreationExpression pJBooleanLiteralExpression) throws RuntimeException {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JArrayInitializer pJArrayInitializer) throws RuntimeException {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JVariableRunTimeType pJThisRunTimeType) throws RuntimeException {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) throws RuntimeException {
    return UnknownValue.getInstance();
  }

  @Override
  public Value visit(JNullLiteralExpression pJNullLiteralExpression) throws RuntimeException {
    return NullValue.getInstance();
  }

  @Override
  public Value visit(JThisExpression pThisExpression) throws RuntimeException {
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

  public long getSizeof(CType pType) throws UnrecognizedCCodeException {
    return machineModel.getSizeof(pType);
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
    return castCValue(pExp.accept(this), pExp.getExpressionType(), pTargetType, machineModel, logger, pExp.getFileLocation());
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
    return castCValue(pExp.accept(this), pExp.getExpressionType(), pTargetType, machineModel, logger, pExp.getFileLocation());
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
   * @param sourceType the type of the input value
   * @param targetType value will be casted to targetType.
   * @param machineModel contains information about types
   * @param logger for logging
   * @param fileLocation the location of the corresponding code in the source file
   * @return the casted Value
   */
  public static Value castCValue(@Nonnull final Value value, final CType sourceType,
      final CType targetType, final MachineModel machineModel,
      final LogManagerWithoutDuplicates logger, final FileLocation fileLocation) {

    // If we don't know the value explicitly, just return it.
    if (!value.isExplicitlyKnown()) { return value; }

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
        final int bitPerByte = machineModel.getSizeofCharInBits();
        final int numBytes = machineModel.getSizeof(st);
        final int size = bitPerByte * numBytes;
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
          // we must look for unsigned numbers in later analysis
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
        Value result = null;

        final int bitPerByte = machineModel.getSizeofCharInBits();
        final int numBytes = machineModel.getSizeof(st);
        final int size = bitPerByte * numBytes;

        if (size == 32) {
          // 32 bit means Java float
          result = new NumericValue(floatValue);
        } else if (size == 64) {
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
        Value result = null;

        final int bitPerByte = machineModel.getSizeofCharInBits();
        final int numBytes = machineModel.getSizeof(st);
        final int size = bitPerByte * numBytes;

        if (size == 32) {
          // 32 bit means Java float
          result = new NumericValue((float) doubleValue);
        } else if (size == 64) {
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

  /**
   * TODO
   * @param value
   * @param sourceType
   * @param targetType
   * @param logger
   * @param fileLocation
   * @return
   */
  public static Value castJValue(@Nonnull final Value value, JType sourceType,
      JType targetType, final LogManagerWithoutDuplicates logger, final FileLocation fileLocation) {

    // If we don't know the value explicitly, just return it.
    if (!value.isExplicitlyKnown()) { return value; }

    // For now can only cast numeric value's
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
}
