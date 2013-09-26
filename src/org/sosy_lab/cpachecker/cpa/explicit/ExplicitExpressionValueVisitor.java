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

import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.forwarding.ForwardingTransferRelation;
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

    // for logging
  @SuppressWarnings("unused")
  private final LogManager logger;
  @SuppressWarnings("unused")
  private final CFAEdge edge;

  public boolean missingPointer = false;
  public boolean missingFieldAccessInformation = false;
  public boolean missingEnumComparisonInformation = false;


  /** This Visitor returns the numeral value for an expression.
   * @param pState where to get the values for variables (identifiers)
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   * @param pEdge only for logging, not needed */
  public ExplicitExpressionValueVisitor(ExplicitState pState, String pFunctionName,
      MachineModel pMachineModel, LogManager pLogger, @Nullable CFAEdge pEdge) {

    this.state = pState;
    this.functionName = pFunctionName;
    this.machineModel = pMachineModel;
    this.logger = pLogger;
    this.edge = pEdge;
  }

  public void reset() {
    missingPointer = false;
    missingFieldAccessInformation = false;
    missingEnumComparisonInformation = false;
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
    final CExpression lVarInBinaryExp = pE.getOperand1();
    final CExpression rVarInBinaryExp = pE.getOperand2();

    Long lVal = lVarInBinaryExp.accept(this);
    if (lVal == null) { return null; }
    Long rVal = rVarInBinaryExp.accept(this);
    if (rVal == null) { return null; }

    final Long result;
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
    return pE.getOperand().accept(this);
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
  public Long visit(CIdExpression idExp) throws UnrecognizedCCodeException {
    if (idExp.getDeclaration() instanceof CEnumerator) {
      CEnumerator enumerator = (CEnumerator) idExp.getDeclaration();
      if (enumerator.hasValue()) {
        return enumerator.getValue();
      } else {
        return null;
      }
    }

    return getValue(idExp.getName(), ForwardingTransferRelation.isGlobal(idExp));
  }

  @Override
  public Long visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
    UnaryOperator unaryOperator = unaryExpression.getOperator();
    CExpression unaryOperand = unaryExpression.getOperand();

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
        return (value == 0L) ? 1L : 0L;
      }

    case AMPER:
      return null; // valid expression, but it's a pointer value

    case SIZEOF:
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
    missingPointer = true;
    return null;
  }

  @Override
  public Long visit(CFieldReference fieldReferenceExpression) throws UnrecognizedCCodeException {
    return evaluateLValue(fieldReferenceExpression);
  }

  @Override
  public Long visit(CArraySubscriptExpression pE)
      throws UnrecognizedCCodeException {
    return evaluateLValue(pE);
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

	return getValue(idExp.getName(), ForwardingTransferRelation.isGlobal(idExp));
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
  private Long getValue(String varName, boolean isGlobal) {

    MemoryLocation memLoc;

    if (!isGlobal) {
      memLoc = MemoryLocation.valueOf(functionName, varName, 0);
    } else {
      memLoc = MemoryLocation.valueOf(varName, 0);
    }

    if (state.contains(memLoc)) {
      return state.getValueFor(memLoc);
    } else {
      return null;
    }
  }

  private Long evaluateLValue(CLeftHandSide pLValue) throws UnrecognizedCCodeException {

    MemoryLocation varLoc = evaluateMemoryLocation(pLValue);

    if (varLoc == null) { return null; }

    if (getState().contains(varLoc)) {
      return getState().getValueFor(varLoc);
    } else {
      return null;
    }
  }

  public boolean canBeEvaluated(CLeftHandSide lValue) throws UnrecognizedCCodeException {
    return lValue.accept(new MemoryLocationEvaluator(this)) != null;
  }

  public MemoryLocation evaluateMemoryLocation(CLeftHandSide lValue) throws UnrecognizedCCodeException {
    return lValue.accept(new MemoryLocationEvaluator(this));
  }

  private static class MemoryLocationEvaluator implements CLeftHandSideVisitor <MemoryLocation, UnrecognizedCCodeException> {

    private final ExplicitExpressionValueVisitor evv;


    public MemoryLocationEvaluator(ExplicitExpressionValueVisitor pEvv) {
      evv = pEvv;
    }

    @Override
    public MemoryLocation visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws UnrecognizedCCodeException {

      CLeftHandSide arrayExpression = (CLeftHandSide) pIastArraySubscriptExpression.getArrayExpression();

      CType arrayExpressionType = arrayExpression.getExpressionType().getCanonicalType();

      /* A subscript Expression can also include an Array Expression.
      In that case, it is a dereference*/
      if (arrayExpressionType instanceof CPointerType) {
        evv.missingPointer = true;
        return null;
      }

      CExpression subscript = pIastArraySubscriptExpression.getSubscriptExpression();

      CType elementType = pIastArraySubscriptExpression.getExpressionType();

      MemoryLocation arrayLoc = arrayExpression.accept(this);

      if (arrayLoc == null) {
        return null;
      }

      Long subscriptValue = subscript.accept(evv);

      if(subscriptValue == null) {
        return null;
      }

      long typeSize = evv.machineModel.getSizeof(elementType);

      long subscriptOffset = subscriptValue * typeSize;

      if (arrayLoc.isOnFunctionStack()) {

        return MemoryLocation.valueOf(arrayLoc.getFunctionName(),
            arrayLoc.getIdentifier(),
            subscriptOffset);
      } else {

        return MemoryLocation.valueOf(arrayLoc.getIdentifier(),
            subscriptOffset);
      }
    }

    @Override
    public MemoryLocation visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {

      if (pIastFieldReference.isPointerDereference()) {
        evv.missingPointer = true;
        return null;
      }

      CLeftHandSide fieldOwner = (CLeftHandSide) pIastFieldReference.getFieldOwner();

      MemoryLocation memLocOfFieldOwner = fieldOwner.accept(this);

      if (memLocOfFieldOwner == null) {
        return null;
      }

      CType ownerType = fieldOwner.getExpressionType().getCanonicalType();
      String fieldName = pIastFieldReference.getFieldName();

      Integer offset = getFieldOffset(ownerType, fieldName);

      if(offset == null) {
        return null;
      }

      if (memLocOfFieldOwner.isOnFunctionStack()) {

        return MemoryLocation.valueOf(memLocOfFieldOwner.getFunctionName(),
            memLocOfFieldOwner.getIdentifier(),
            memLocOfFieldOwner.getOffset() + offset);
      } else {

        return MemoryLocation.valueOf(memLocOfFieldOwner.getIdentifier(),
            offset + memLocOfFieldOwner.getOffset());
      }
    }

    private Integer getFieldOffset(CType ownerType, String fieldName) throws UnrecognizedCCodeException {

      if (ownerType instanceof CElaboratedType) {
        return getFieldOffset(((CElaboratedType) ownerType).getRealType(), fieldName);
      } else if (ownerType instanceof CCompositeType) {
        return getFieldOffset((CCompositeType) ownerType, fieldName);
      } else if (ownerType instanceof CPointerType) {
        evv.missingPointer = true;
        return null;
      }

      throw new AssertionError();
    }

    private Integer getFieldOffset(CCompositeType ownerType, String fieldName) throws UnrecognizedCCodeException {

      List<CCompositeTypeMemberDeclaration> membersOfType = ownerType.getMembers();

      int offset = 0;

      for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
        String memberName = typeMember.getName();

        if (memberName.equals(fieldName)) {
          return offset;
        }

        if (!(ownerType.getKind() == ComplexTypeKind.UNION)) {

          CType fieldType = typeMember.getType().getCanonicalType();

          offset = offset + evv.machineModel.getSizeof(fieldType);
        }
      }

      return null;
    }

    @Override
    public MemoryLocation visit(CIdExpression idExp) throws UnrecognizedCCodeException {

      boolean isGlobal = ForwardingTransferRelation.isGlobal(idExp);

      if(isGlobal) {
        return MemoryLocation.valueOf(idExp.getName(), 0);
      } else {
        return MemoryLocation.valueOf(evv.functionName, idExp.getName(), 0);
      }
    }

    @Override
    public MemoryLocation visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
      evv.missingPointer = true;
      return null;
    }

    @Override
    public MemoryLocation visit(CComplexCastExpression pComplexCastExpression) throws UnrecognizedCCodeException {
      //TODO Investigate
      return null;
    }

  }

  public ExplicitState getState() {
    return state;
  }

  public String getFunctionName() {
    return functionName;
  }

  public long getSizeof(CType pType) throws UnrecognizedCCodeException {
    return machineModel.getSizeof(pType);
  }
}