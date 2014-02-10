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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.eclipse.cdt.internal.core.dom.parser.c.CArrayType;
import org.eclipse.cdt.internal.core.dom.parser.c.CPointerType;
import org.sosy_lab.common.log.LogManager;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedLongs;


/**
 * This Visitor implements explicit evaluation
 * of simply typed expressions. An expression is
 * defined as simply typed iff it is not an
 * array type (vgl {@link CArrayType}), a struct or
 * union type (vgl {@link CComplexType}),
 * a imaginary type (vgl {@link CImaginaryLiteralExpression}),
 * or a pointer type (vgl {@link CPointerType}).
 * The key distinction between these types and simply typed types is,
 * that a value of simply typed types can be represented as a numerical
 * value without losing information.
 *
 * Furthermore, the visitor abstracts from using abstract states
 * to get values stored in the memory of a program.
 */
public abstract class AbstractExplicitExpressionValueVisitor
    extends DefaultCExpressionVisitor<Long, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Long, UnrecognizedCCodeException>,
    JRightHandSideVisitor<Long, RuntimeException>,
    JExpressionVisitor<Long, RuntimeException> {

  /** length of type LONG in Java. */
  private final static int SIZE_OF_JAVA_LONG = 64;

  //private final ExplicitState state;
  private final String functionName;
  private final MachineModel machineModel;


  // for logging
  private final LogManager logger;
  private final CFAEdge edge;
  // we log for each edge only once, that avoids much output. the user knows all critical lines.
  private final static Set<CFAEdge> loggedEdges = Sets.newHashSet();

  private boolean missingFieldAccessInformation = false;
  private boolean missingEnumComparisonInformation = false;

  /** This Visitor returns the numeral value for an expression.
   * @param pState where to get the values for variables (identifiers)
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   * @param pEdge only for logging, not needed */
  public AbstractExplicitExpressionValueVisitor(String pFunctionName,
      MachineModel pMachineModel, LogManager pLogger, @Nullable CFAEdge pEdge) {

    //this.state = pState;
    this.functionName = pFunctionName;
    this.machineModel = pMachineModel;
    this.logger = pLogger;
    this.edge = pEdge;
  }

  public boolean hasMissingFieldAccessInformation() {
    return missingFieldAccessInformation;
  }

  public boolean hasMissingEnumComparisonInformation() {
    return missingEnumComparisonInformation;
  }

  @Override
  protected Long visitDefault(CExpression pExp) {
    return null;
  }

  public void reset() {
    missingFieldAccessInformation = false;
    missingEnumComparisonInformation = false;
  }

  @Override
  public Long visit(final CBinaryExpression pE) throws UnrecognizedCCodeException {

    final Long lVal = pE.getOperand1().accept(this);
    if (lVal == null) { return null; }
    final Long rVal = pE.getOperand2().accept(this);
    if (rVal == null) { return null; }
    Long result = calculateBinaryOperation(lVal, rVal, pE, machineModel, logger, edge);

    return result;
  }

  /**
   * This method calculates the exact result for a binary operation.
   *
   * @param lVal first operand
   * @param rVal second operand
   * @param binaryOperator this operation willbe performed
   * @param resultType the result will be casted to this type
   * @param machineModel information about types
   * @param logger for logging
   * @param edge only for logging
   */
  public static Long calculateBinaryOperation(Long lVal, Long rVal,
      final CBinaryExpression binaryExpr,
      final MachineModel machineModel, final LogManager logger, @Nullable CFAEdge edge) {

    final BinaryOperator binaryOperator = binaryExpr.getOperator();
    final CType calculationType = binaryExpr.getCalculationType();

    lVal = castCValue(lVal, calculationType, machineModel, logger, edge);
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
      rVal = castCValue(rVal, calculationType, machineModel, logger, edge);
    }

    Long result;
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

      result = arithmeticOperation(lVal, rVal, binaryOperator, calculationType, machineModel, logger);
      result = castCValue(result, binaryExpr.getExpressionType(), machineModel, logger, edge);

      break;
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {

      final boolean tmp = booleanOperation(lVal, rVal, binaryOperator, calculationType, machineModel);
      // return 1 if expression holds, 0 otherwise
      result = tmp ? 1L : 0L;
      // we do not cast here, because 0 and 1 should be small enough for every type.

      break;
    }

    default:
      throw new AssertionError("unhandled binary operator");
    }

    return result;
  }

  private static long arithmeticOperation(final long l, final long r,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel, final LogManager logger) {

    // special handling for UNSIGNED_LONGLONG (32 and 64bit), UNSIGNED_LONG (64bit)
    // because Java only has SIGNED_LONGLONG
    if (calculationType instanceof CSimpleType) {
      final CSimpleType st = (CSimpleType) calculationType;
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
      return l % r; // TODO in C always sign of first operand?
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

  private static boolean booleanOperation(final long l, final long r,
      final BinaryOperator op, final CType calculationType,
      final MachineModel machineModel) {

    // special handling for UNSIGNED_LONGLONG (32 and 64bit), UNSIGNED_LONG (64bit)
    // because Java only has SIGNED_LONGLONG
    if (calculationType instanceof CSimpleType) {
      final CSimpleType st = (CSimpleType) calculationType;
      if (machineModel.getSizeof(st) * machineModel.getSizeofCharInBits() >= SIZE_OF_JAVA_LONG
          && st.isUnsigned()) {
        final int cmp = UnsignedLongs.compare(l, r);
        switch (op) {
        case GREATER_THAN:
          return cmp > 0;
        case GREATER_EQUAL:
          return cmp >= 0;
        case LESS_THAN:
          return cmp < 0;
        case LESS_EQUAL:
          return cmp <= 0;
        }
      }
    }

    switch (op) {
    case EQUALS:
      return (l == r);
    case NOT_EQUALS:
      return (l != r);
    case GREATER_THAN:
      return (l > r);
    case GREATER_EQUAL:
      return (l >= r);
    case LESS_THAN:
      return (l < r);
    case LESS_EQUAL:
      return (l <= r);

    default:
      throw new AssertionError("unknown binary operation: " + op);
    }
  }

  @Override
  public Long visit(CCastExpression pE) throws UnrecognizedCCodeException {
    return castCValue(pE.getOperand().accept(this), pE.getExpressionType(), machineModel, logger, edge);
  }

  @Override
  public Long visit(CComplexCastExpression pE) throws UnrecognizedCCodeException {
    // evaluation of complex numbers is not supported by now
    return null;
  }

  @Override
  public Long visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(CCharLiteralExpression pE) throws UnrecognizedCCodeException {
    return (long) pE.getCharacter();
  }

  @Override
  public Long visit(CFloatLiteralExpression pE) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(CIntegerLiteralExpression pE) throws UnrecognizedCCodeException {
    return pE.asLong();
  }

  @Override
  public Long visit(CImaginaryLiteralExpression pE) throws UnrecognizedCCodeException {
    return pE.getValue().accept(this);
  }

  @Override
  public Long visit(CStringLiteralExpression pE) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(final CTypeIdExpression pE) {
    final TypeIdOperator idOperator = pE.getOperator();
    final CType innerType = pE.getType();

    switch (idOperator) {
    case SIZEOF:
      int size = machineModel.getSizeof(innerType);
      return (long) size;

    default: // TODO support more operators
      return null;
    }
  }

  @Override
  public Long visit(CIdExpression idExp) throws UnrecognizedCCodeException {
    if (idExp.getDeclaration() instanceof CEnumerator) {
      CEnumerator enumerator = (CEnumerator) idExp.getDeclaration();
      if (enumerator.hasValue()) {
        return enumerator.getValue();
      } else {
        return null;
      }
    }

    return evaluateCIdExpression(idExp);
  }

  @Override
  public Long visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
    final UnaryOperator unaryOperator = unaryExpression.getOperator();
    final CExpression unaryOperand = unaryExpression.getOperand();

    if (unaryOperator == UnaryOperator.SIZEOF) { return (long) machineModel.getSizeof(unaryOperand.getExpressionType()); }

    final Long value = unaryOperand.accept(this);

    if (value == null && unaryOperator != UnaryOperator.SIZEOF) {
      return null;
    }

    switch (unaryOperator) {
    case PLUS:
      return value;

    case MINUS:
      return -value;

    case NOT:
      return (value == 0L) ? 1L : 0L;

    case SIZEOF:
      throw new AssertionError("SIZEOF should be handled before!");

    case AMPER: // valid expression, but it's a pointer value
      // TODO Not precise enough
      return getSizeof(unaryOperand.getExpressionType());
    case TILDE:
    default:
      // TODO handle unimplemented operators
      return null;
    }
  }

  @Override
  public Long visit(CPointerExpression pointerExpression) throws UnrecognizedCCodeException {
    return evaluateCPointerExpression(pointerExpression);
  }

  @Override
  public Long visit(CFieldReference fieldReferenceExpression) throws UnrecognizedCCodeException {
    return evaluateCFieldReference(fieldReferenceExpression);
  }

  @Override
  public Long visit(CArraySubscriptExpression pE)
      throws UnrecognizedCCodeException {
    return evaluateCArraySubscriptExpression(pE);
  }

  @Override
  public Long visit(JCharLiteralExpression pE) {
    return (long) pE.getCharacter();
  }

  @Override
  public Long visit(JThisExpression thisExpression) {
    return null;
  }

  @Override
  public Long visit(JStringLiteralExpression pPaStringLiteralExpression) {
    return null;
  }

  @Override
  public Long visit(JBinaryExpression pE) {

    org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator binaryOperator = pE.getOperator();
    JExpression lVarInBinaryExp = pE.getOperand1();
    JExpression rVarInBinaryExp = pE.getOperand2();

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
      Long lVal = lVarInBinaryExp.accept(this);
      if (lVal == null) { return null; }

      Long rVal = rVarInBinaryExp.accept(this);
      if (rVal == null) { return null; }

      switch (binaryOperator) {
      case PLUS:
        return lVal + rVal;

      case MINUS:
        return lVal - rVal;

      case DIVIDE:
        // TODO maybe we should signal a division by zero error?
        if (rVal == 0) { return null; }

        return lVal / rVal;

      case MULTIPLY:
        return lVal * rVal;

      case SHIFT_LEFT:
        return lVal << rVal;

      case BINARY_AND:
        return lVal & rVal;

      case BINARY_OR:
        return lVal | rVal;

      case BINARY_XOR:
        return lVal ^ rVal;

      case MODULO:
        return lVal % rVal;

      case SHIFT_RIGHT_SIGNED:
        return lVal >> rVal;
      case SHIFT_RIGHT_UNSIGNED:
        return lVal >>> rVal;

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

      Long lVal = lVarInBinaryExp.accept(this);
      Long rVal = rVarInBinaryExp.accept(this);
      if (lVal == null || rVal == null) { return null; }

      long l = lVal;
      long r = rVal;

      boolean result;
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
      return (result ? 1L : 0L);
    }
    default:
      // TODO check which cases can be handled
      return null;
    }
  }

  @Override
  public Long visit(JIdExpression idExp) {


    IASimpleDeclaration decl = idExp.getDeclaration();

    // Java IdExpression could not be resolved
    if (decl == null) { return null; }

    if (decl instanceof JFieldDeclaration
        && !((JFieldDeclaration) decl).isStatic()) {
      missingFieldAccessInformation = true;
    }

    return evaluateJIdExpression(idExp);
  }

  @Override
  public Long visit(JUnaryExpression unaryExpression) {

    JUnaryExpression.UnaryOperator unaryOperator = unaryExpression.getOperator();
    JExpression unaryOperand = unaryExpression.getOperand();

    Long value = null;

    switch (unaryOperator) {
    case MINUS:
      value = unaryOperand.accept(this);
      return (value != null) ? -value : null;

    case NOT:
      value = unaryOperand.accept(this);

      if (value == null) {
        return null;
      } else {
        // if the value is 0, return 1, if it is anything other than 0, return 0
        return (value == 0L) ? 1L : 0L;
      }

    case COMPLEMENT:
      value = unaryOperand.accept(this);
      return (value != null) ? ~value : null;

    case PLUS:
      value = unaryOperand.accept(this);
      return value;
    default:
      return null;
    }
  }

  @Override
  public Long visit(JIntegerLiteralExpression pE) {
    return pE.asLong();
  }

  @Override
  public Long visit(JBooleanLiteralExpression pE) {
    return ((pE.getValue()) ? 1l : 0l);
  }

  @Override
  public Long visit(JFloatLiteralExpression pJBooleanLiteralExpression) {
    return null;
  }

  @Override
  public Long visit(JMethodInvocationExpression pAFunctionCallExpression) {
    return null;
  }

  @Override
  public Long visit(JArrayCreationExpression aCE) {
    return null;
  }

  @Override
  public Long visit(JArrayInitializer pJArrayInitializer) {
    return null;
  }

  @Override
  public Long visit(JArraySubscriptExpression pAArraySubscriptExpression) {
    return pAArraySubscriptExpression.getSubscriptExpression().accept(this);
  }

  @Override
  public Long visit(JClassInstanceCreation pJClassInstanzeCreation) {
    return null;
  }

  @Override
  public Long visit(JVariableRunTimeType pJThisRunTimeType) {
    return null;
  }

  @Override
  public Long visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) {
    return null;
  }

  @Override
  public Long visit(JNullLiteralExpression pJNullLiteralExpression) {
    return null;
  }

  @Override
  public Long visit(JEnumConstantExpression pJEnumConstantExpression) {
    missingEnumComparisonInformation = true;
    return null;
  }

  @Override
  public Long visit(JCastExpression pJCastExpression) {
    return pJCastExpression.getOperand().accept(this);
  }

  /* abstract methods */

  protected abstract Long evaluateCPointerExpression(CPointerExpression pCPointerExpression) throws UnrecognizedCCodeException;

  protected abstract Long evaluateCIdExpression(CIdExpression pCIdExpression) throws UnrecognizedCCodeException;

  protected abstract Long evaluateJIdExpression(JIdExpression varName);

  protected abstract Long evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException;

  protected abstract Long evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue) throws UnrecognizedCCodeException;

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
  public Long evaluate(final CExpression pExp, final CType pTargetType)
      throws UnrecognizedCCodeException {
    return castCValue(pExp.accept(this), pTargetType, machineModel, logger, edge);
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
  public Long evaluate(final CRightHandSide pExp, final CType pTargetType)
      throws UnrecognizedCCodeException {
    return castCValue(pExp.accept(this), pTargetType, machineModel, logger, edge);
  }


  /**
   * This method returns the input-value, casted to match the type.
   * If the value matches the type, it is returned unchanged.
   * This method handles overflows and print warnings for the user.
   * Example:
   * This method is called, when an value of type 'integer'
   * is assigned to a variable of type 'char'.
   *
   * @param value will be casted. If value is null, null is returned.
   * @param targetType value will be casted to targetType.
   * @param machineModel contains information about types
   * @param logger for logging
   * @param edge only for logging
   */
  public static Long castCValue(@Nullable final Long value, final CType targetType,
      final MachineModel machineModel, final LogManager logger, @Nullable final CFAEdge edge) {
    if (value == null) { return null; }

    final CType type = targetType.getCanonicalType();
    if (type instanceof CSimpleType) {
      final CSimpleType st = (CSimpleType) type;

      switch (st.getType()) {

      case INT:
      case CHAR: {
        final int bitPerByte = machineModel.getSizeofCharInBits();
        final int numBytes = machineModel.getSizeof(st);
        final int size = bitPerByte * numBytes;

        if ((size < SIZE_OF_JAVA_LONG) || (size == SIZE_OF_JAVA_LONG && st.isSigned())
            || (value < Long.MAX_VALUE / 2 && value > Long.MIN_VALUE / 2)) {
          // we can handle this with java-type "long"

          final long maxValue = 1L << size; // 2^size

          long result = value;

          if (size < SIZE_OF_JAVA_LONG) { // otherwise modulo is useless, because result would be 1
            result = value % maxValue; // shrink to number of bits

            if (st.isSigned() ||
                (st.getType() == CBasicType.CHAR && !st.isUnsigned() && machineModel.isDefaultCharSigned())) {
              if (result > (maxValue / 2) - 1) {
                result -= maxValue;
              } else if (result < -(maxValue / 2)) {
                result += maxValue;
              }
            }
          }

          if (result != value && loggedEdges.add(edge)) {
            logger.logf(Level.INFO,
                "overflow in line %d: value %d is to big for type '%s', casting to %d.",
                edge == null ? null : edge.getLineNumber(),
                value, targetType, result);
          }

          if (st.isUnsigned() && value < 0) {

            if (size < SIZE_OF_JAVA_LONG) {
              // value is negative, so adding maxValue makes it positive
              result = maxValue + result;

              if (loggedEdges.add(edge)) {
                logger.logf(Level.INFO,
                    "overflow in line %d: target-type is '%s', value %d is changed to %d.",
                    edge == null ? null : edge.getLineNumber(),
                    targetType, value, result);
              }

            } else {
              // java-type "long" is too small for big types like UNSIGNED_LONGLONG,
              // so we do nothing here and trust the analysis, that handles it later
              if (loggedEdges.add(edge)) {
                logger.logf(Level.INFO,
                    "overflow in line %d: value %s of c-type '%s' may be too big for java-type 'long'.",
                    edge == null ? null : edge.getLineNumber(),
                    value, targetType);
              }
            }
          }

          return result;

        } else {
          // java-type "long" is too small for big types like UNSIGNED_LONGLONG,
          // so we do nothing here and trust the analysis, that handles it later
          if (loggedEdges.add(edge)) {
            logger.logf(Level.INFO,
                "overflow in line %d: value %s of c-type '%s' may be too big for java-type 'long'.",
                edge == null ? null : edge.getLineNumber(),
                value, targetType);
          }

          return value;
        }
      }

      default:
        return value; // currently we do not handle floats, doubles or voids
      }

    } else {
      return value; // pointer like (void)*, (struct s)*, ...
    }
  }

  public CFAEdge getEdge() {
    return edge;
  }
}