/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


/**
 * This Visitor returns the value from an expression.
 * The result may be null, i.e., the value is unknown.
 */
public class ExplicitExpressionValueVisitor
    extends DefaultCExpressionVisitor<Long, UnrecognizedCCodeException>
    implements CRightHandSideVisitor<Long, UnrecognizedCCodeException>,
    JRightHandSideVisitor<Long, UnrecognizedCCodeException>,
    JExpressionVisitor<Long, UnrecognizedCCodeException> {


  private final ExplicitState state;
  private final String functionName;
  private final MachineModel machineModel;

  private final Set<String> globalVariables; // TODO do we really need this?


  // for logging
  private final LogManager logger;
  private final CFAEdge edge;


  private boolean missingPointer = false;
  private boolean missingFieldAccessInformation = false;
  private boolean missingEnumComparisonInformation = false;


  /** This Visitor returns the numeral value for an expression.
   * @param pState where to get the values for variables (identifiers)
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   * @param pEdge only for logging, not needed */
  public ExplicitExpressionValueVisitor(ExplicitState pState, String pFunctionName,
      MachineModel pMachineModel, Set<String> pGlobalVariables,
      LogManager pLogger, @Nullable CFAEdge pEdge) {

    this.state = pState;
    this.functionName = pFunctionName;
    this.machineModel = pMachineModel;
    this.globalVariables = pGlobalVariables;
    this.logger = pLogger;
    this.edge = pEdge;
  }

  public boolean hasMissingPointer() {
    return missingPointer;
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

  @Override
  public Long visit(final CBinaryExpression pE) throws UnrecognizedCCodeException {
    final BinaryOperator binaryOperator = pE.getOperator();
    final CExpression op1 = pE.getOperand1();
    final CExpression op2 = pE.getOperand2();

    // commonType is the converted eclipse-type, that is not always correct.
    // the eclipse-type is the _simplest_ type of a read-access to the expression.
    // this leads to (almost) non-deterministic types in a binExpr.
    // the type may not be correct for the evaluation.
    // example: INT op LONG_LONG_INT
    // -> commonType is one of {INT, LONG_LONG_INT}, but should be LONG_LONG_INT.
    // TODO fix this, either manually here or directly in C-TypeConverter?
    final CType commonType = pE.getExpressionType();

    //    final CType t1 = op1.getExpressionType();
    //    final CType t2 = op2.getExpressionType();

    final Long lVal = evaluate(op1, commonType);
    if (lVal == null) { return null; }
    final Long rVal = evaluate(op2, commonType);
    if (rVal == null) { return null; }

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

      result = arithmeticOperation(lVal, rVal, binaryOperator);
      result = castCValue(result, commonType);

      break;
    }

    case EQUALS:
    case NOT_EQUALS:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL: {

      final boolean tmp = booleanOperation(lVal, rVal, binaryOperator);
      // return 1 if expression holds, 0 otherwise
      result = tmp ? 1L : 0L;

      break;
    }

    default:
      // TODO check which cases can be handled (I think all)
      result = null;
    }

    return result;
  }

  private long arithmeticOperation(long l, long r, BinaryOperator op) {
    // TODO machinemodel
    switch (op) {
    case PLUS:
      return l + r;
    case MINUS:
      return l - r;
    case DIVIDE:
      // TODO signal a division by zero error?
      if (r == 0) { return 0; }
      return l / r;
    case MODULO:
      return l % r;
    case MULTIPLY:
      return l * r;
    case SHIFT_LEFT:
      return l << r;
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

  private boolean booleanOperation(long l, long r, BinaryOperator op) {
    // TODO machinemodel
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
    return castCValue(pE.getOperand().accept(this), pE.getType());
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

    return getValue(idExp.getName());
  }

  @Override
  public Long visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
    final UnaryOperator unaryOperator = unaryExpression.getOperator();
    final CExpression unaryOperand = unaryExpression.getOperand();

    final Long value = unaryOperand.accept(this);

    if (value == null) { return null; }

    switch (unaryOperator) {
    case PLUS:
      return value;

    case MINUS:
      return -value;

    case NOT:
      return (value == 0L) ? 1L : 0L;

    case SIZEOF:
      return (long) machineModel.getSizeof(unaryOperand.getExpressionType());

    case AMPER: // valid expression, but it's a pointer value
    case TILDE:
    default:
      // TODO handle unimplemented operators
      return null;
    }
  }

  @Override
  public Long visit(CPointerExpression pointerExpression) throws UnrecognizedCCodeException {
    missingPointer = true;
    return null;
  }

  @Override
  public Long visit(CFieldReference fieldReferenceExpression) throws UnrecognizedCCodeException {
    return getValue(fieldReferenceExpression.toASTString());
  }

  @Override
  public Long visit(JCharLiteralExpression pE) throws UnrecognizedCCodeException {
    return (long) pE.getCharacter();
  }

  @Override
  public Long visit(JThisExpression thisExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JStringLiteralExpression pPaStringLiteralExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JBinaryExpression pE) throws UnrecognizedCCodeException {

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
  public Long visit(JIdExpression idExp) throws UnrecognizedCCodeException {


    IASimpleDeclaration decl = idExp.getDeclaration();

    // Java IdExpression could not be resolved
    if (decl == null) { return null; }

    if (decl instanceof JFieldDeclaration
        && !((JFieldDeclaration) decl).isStatic()) {
      missingFieldAccessInformation = true;
    }

    return getValue(idExp.getName());
  }

  @Override
  public Long visit(JUnaryExpression unaryExpression) throws UnrecognizedCCodeException {

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
  public Long visit(JIntegerLiteralExpression pE) throws UnrecognizedCCodeException {
    return pE.asLong();
  }

  @Override
  public Long visit(JBooleanLiteralExpression pE) throws UnrecognizedCCodeException {
    return ((pE.getValue()) ? 1l : 0l);
  }

  @Override
  public Long visit(JFloatLiteralExpression pJBooleanLiteralExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JMethodInvocationExpression pAFunctionCallExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JArrayCreationExpression aCE) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JArrayInitializer pJArrayInitializer) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JArraySubscriptExpression pAArraySubscriptExpression) throws UnrecognizedCCodeException {
    return pAArraySubscriptExpression.getSubscriptExpression().accept(this);
  }

  @Override
  public Long visit(JClassInstanceCreation pJClassInstanzeCreation) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JVariableRunTimeType pJThisRunTimeType) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JNullLiteralExpression pJNullLiteralExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Long visit(JEnumConstantExpression pJEnumConstantExpression) throws UnrecognizedCCodeException {
    missingEnumComparisonInformation = true;
    return null;
  }

  @Override
  public Long visit(JCastExpression pJCastExpression) throws UnrecognizedCCodeException {
    return pJCastExpression.getOperand().accept(this);
  }


  /* additional methods */


  /** This method returns the value of a variable from the current state. */
  private Long getValue(String varName) {
    // TODO remove globalVars-collection and replace it with isGlobal() ?

    if (!globalVariables.contains(varName)) {
      varName = functionName + "::" + varName;
    }

    if (state.contains(varName)) {
      return state.getValueFor(varName);
    } else {
      return null;
    }
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
    return castCValue(pExp.accept(this), pTargetType);
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
    return castCValue(pExp.accept(this), pTargetType);
  }


  /**
   * This method returns the input-value, casted to match the type.
   * If the value matches the type, it is returned unchanged.
   * This method handles overflows and print warnings for the user.
   * Example:
   * This method is called, when an value of type 'integer'
   * is assigned to a variable of type 'char'.
   */
  private Long castCValue(@Nullable final Long value, final CType targetType) {
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

        if ((size < 64) || (size == 64 && st.isSigned())
            || (value < Long.MAX_VALUE / 2 && value > Long.MIN_VALUE / 2)) {
          // we can handle this with java-type "long"
          // TODO otherwise switch to BigInteger

          final long maxValue = 1L << size; // 2^size

          long result = value;

          if (size < 64) { // otherwise modulo is useless, because result would be 1
            result = value % maxValue; // shrink to number of bits

            if (st.isSigned()) {
              if (result > (maxValue / 2) - 1) {
                result -= maxValue;
              } else if (result < -(maxValue / 2)) {
                result += maxValue;
              }
            }
          }

          if (result != value) {
            // TODO perhaps we should log this only once?
            logger.logf(Level.WARNING,
                "overflow in line %d: value %d is to big for type '%s', casting to %d.",
                edge == null ? null : edge.getLineNumber(),
                value, targetType, result);
          }

          if (st.isUnsigned() && value < 0) {

            if (size < 64) {
              result = maxValue + result; // value is negative!

              logger.logf(Level.WARNING,
                  "overflow in line %d: target-type is '%s', value %d is changed to %d.",
                  edge == null ? null : edge.getLineNumber(),
                  targetType, value, result);

            } else {
              // java-type "long" is too small
              logger.logf(Level.SEVERE,
                  "overflow in line %d: value %s of c-type '%s' is too big "
                      + "for java-type 'long', analysis may produce wrong results.",
                  edge == null ? null : edge.getLineNumber(),
                  value, targetType);
            }
          }

          return result;

        } else { // java-type "long" is too small
          logger.logf(Level.SEVERE,
              "overflow in line %d: value %s of c-type '%s' is too big "
                  + "for java-type 'long'\", analysis may produce wrong results.",
              edge == null ? null : edge.getLineNumber(),
              value, targetType);

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
}
