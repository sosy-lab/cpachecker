/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * This class evaluates expressions that evaluate to a
 * pointer type. The type of every expression visited by this
 * visitor has to be a {@link CPointerType }. The result
 * of this evaluation is a {@link SMGAddressValue}.
 * The object and the offset of the result represent
 * the address this pointer points to. The value represents
 * the value of the address itself. Note that the offset of
 * pointer addresses that point to the null object represent
 * also the explicit value of the pointer.
 */
public class PointerVisitor extends ExpressionValueVisitor {

  public PointerVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgExpressionEvaluator, pEdge, pSmgState);
  }

  @Override
  public SMGAddressValueAndStateList visit(CIntegerLiteralExpression exp) throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public SMGAddressValueAndStateList visit(CCharLiteralExpression exp) throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public SMGAddressValueAndStateList visit(CFloatLiteralExpression pExp) throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(pExp));
  }

  @Override
  public SMGAddressValueAndStateList visit(CIdExpression exp) throws CPATransferException {

    CType c = smgExpressionEvaluator.getRealExpressionType(exp);

    if (c instanceof CArrayType) {
      // a == &a[0];
      return createAddressOfVariable(exp);
    }

    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public SMGAddressValueAndStateList visit(CUnaryExpression unaryExpression) throws CPATransferException {

    UnaryOperator unaryOperator = unaryExpression.getOperator();
    CExpression unaryOperand = unaryExpression.getOperand();

    switch (unaryOperator) {

    case AMPER:
      return handleAmper(unaryOperand);

    case SIZEOF:
      throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
          + unaryOperand.toASTString()
          + " as pointer type", cfaEdge, unaryExpression);

    case MINUS:
    case TILDE:
    default:
      // Can't evaluate these Addresses
      return SMGAddressValueAndStateList.of(getInitialSmgState());
    }
  }

  private SMGAddressValueAndStateList handleAmper(CRightHandSide amperOperand) throws CPATransferException {

    if (smgExpressionEvaluator.getRealExpressionType(amperOperand) instanceof CFunctionType
        && amperOperand instanceof CIdExpression) {
      // function type &foo
      return createAddressOfFunction((CIdExpression) amperOperand);
    } else if (amperOperand instanceof CIdExpression) {
      // &a
      return createAddressOfVariable((CIdExpression) amperOperand);
    } else if (amperOperand instanceof CPointerExpression) {
      // &(*(a))
      CExpression rValue = ((CPointerExpression) amperOperand).getOperand();
      return smgExpressionEvaluator.evaluateAddress(getInitialSmgState(), getCfaEdge(), rValue);
    } else if (amperOperand instanceof CFieldReference) {
      // &(a.b)
      return createAddressOfField((CFieldReference) amperOperand);
    } else if (amperOperand instanceof CArraySubscriptExpression) {
      // &(a[b])
      return createAddressOfArraySubscript((CArraySubscriptExpression) amperOperand);
    } else {
      return SMGAddressValueAndStateList.of(getInitialSmgState());
    }
  }

  protected SMGAddressValueAndStateList createAddressOfFunction(CIdExpression idFunctionExpression)
      throws SMGInconsistentException {

    SMGState state = getInitialSmgState();

    SMGObject functionObject =
        state.getObjectForFunction((CFunctionDeclaration) idFunctionExpression.getDeclaration());

    if (functionObject == null) {
      return SMGAddressValueAndStateList.of(state);
    }

    return smgExpressionEvaluator.createAddress(state, functionObject, SMGKnownExpValue.ZERO);
  }

  private SMGAddressValueAndStateList createAddressOfArraySubscript(CArraySubscriptExpression lValue)
      throws CPATransferException {

    CExpression arrayExpression = lValue.getArrayExpression();

    List<SMGAddressValueAndState> result = new ArrayList<>(4);

    SMGAddressValueAndStateList arrayAddressAndStates =
        smgExpressionEvaluator.evaluateAddress(getInitialSmgState(), getCfaEdge(), arrayExpression);

    for (SMGAddressValueAndState arrayAddressAndState : arrayAddressAndStates.asAddressValueAndStateList()) {

      SMGAddressValue arrayAddress = arrayAddressAndState.getObject();
      SMGState newState = arrayAddressAndState.getSmgState();

      if (arrayAddress.isUnknown()) {
        result.add(SMGAddressValueAndState.of(newState));
        continue;
      }

      CExpression subscriptExpr = lValue.getSubscriptExpression();

      List<SMGExplicitValueAndState> subscriptValueAndStates =
          smgExpressionEvaluator.evaluateExplicitValue(newState, getCfaEdge(), subscriptExpr);

      for (SMGExplicitValueAndState subscriptValueAndState : subscriptValueAndStates) {

        SMGExplicitValue subscriptValue = subscriptValueAndState.getObject();
        newState = subscriptValueAndState.getSmgState();

        if (subscriptValue.isUnknown()) {
          result.add(SMGAddressValueAndState.of(newState));
        } else {
          SMGExplicitValue arrayOffset = arrayAddress.getOffset();
          int typeSize = smgExpressionEvaluator.getBitSizeof(getCfaEdge(), smgExpressionEvaluator.getRealExpressionType(lValue), newState, lValue);
          SMGExplicitValue sizeOfType = SMGKnownExpValue.valueOf(typeSize);
          SMGExplicitValue offset = arrayOffset.add(subscriptValue.multiply(sizeOfType));
          SMGAddressValueAndStateList resultAddressAndState = smgExpressionEvaluator.createAddress(newState, arrayAddress.getObject(), offset);
          result.addAll(resultAddressAndState.asAddressValueAndStateList());
        }
      }
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  private SMGAddressValueAndStateList createAddressOfField(CFieldReference lValue)
      throws CPATransferException {

    List<SMGAddressValueAndState> result = new ArrayList<>(2);

    List<SMGAddressAndState> addressOfFieldAndStates = smgExpressionEvaluator.getAddressOfField(
        getInitialSmgState(), getCfaEdge(), lValue);

    for (SMGAddressAndState addressOfFieldAndState : addressOfFieldAndStates) {

      SMGAddress addressOfField = addressOfFieldAndState.getObject();
      SMGState newState = addressOfFieldAndState.getSmgState();

      if (addressOfField.isUnknown()) {
        result.add(SMGAddressValueAndState.of(newState));
      } else {
        SMGAddressValueAndStateList resultAddressValueAndState = smgExpressionEvaluator.createAddress(addressOfFieldAndState.getSmgState(),
            addressOfField.getObject(), addressOfField.getOffset());
        result.addAll(resultAddressValueAndState.asAddressValueAndStateList());
      }
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  private SMGAddressValueAndStateList createAddressOfVariable(CIdExpression idExpression) throws SMGInconsistentException {

    SMGState state = getInitialSmgState();

    SMGObject variableObject = state.getObjectForVisibleVariable(idExpression.getName());

    if (variableObject == null) {
      return SMGAddressValueAndStateList.of(state);
    } else {
      state.addElementToCurrentChain(variableObject);
      return smgExpressionEvaluator.createAddress(state, variableObject, SMGKnownExpValue.ZERO);
    }
  }

  @Override
  public SMGAddressValueAndStateList visit(CPointerExpression pointerExpression) throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(pointerExpression));
  }

  @Override
  public SMGAddressValueAndStateList visit(CBinaryExpression binaryExp) throws CPATransferException {

    CExpression lVarInBinaryExp = binaryExp.getOperand1();
    CExpression rVarInBinaryExp = binaryExp.getOperand2();
    CType lVarInBinaryExpType = smgExpressionEvaluator.getRealExpressionType(lVarInBinaryExp);
    CType rVarInBinaryExpType = smgExpressionEvaluator.getRealExpressionType(rVarInBinaryExp);

    boolean lVarIsAddress = lVarInBinaryExpType instanceof CPointerType;
    boolean rVarIsAddress = rVarInBinaryExpType instanceof CPointerType;

    CExpression address = null;
    CExpression pointerOffset = null;
    CPointerType addressType = null;

    if (lVarIsAddress == rVarIsAddress) {
      return SMGAddressValueAndStateList.of(getInitialSmgState()); // If both or neither are Addresses,
      //  we can't evaluate the address this pointer stores.
    } else if (lVarIsAddress) {
      address = lVarInBinaryExp;
      pointerOffset = rVarInBinaryExp;
      addressType = (CPointerType) lVarInBinaryExpType;
    } else if (rVarIsAddress) {
      address = rVarInBinaryExp;
      pointerOffset = lVarInBinaryExp;
      addressType = (CPointerType) rVarInBinaryExpType;
    } else {
      throw new UnrecognizedCCodeException("Expected either "
    + lVarInBinaryExp.toASTString() + " or "
    + rVarInBinaryExp.toASTString() +
    "to be a pointer.", binaryExp);
    }

    CType typeOfPointer = smgExpressionEvaluator.getRealExpressionType(addressType.getType());

    return smgExpressionEvaluator.handlePointerArithmetic(getInitialSmgState(), getCfaEdge(),
        address, pointerOffset, typeOfPointer, lVarIsAddress,
        binaryExp);
  }

  @Override
  public SMGAddressValueAndStateList visit(CArraySubscriptExpression exp) throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public SMGAddressValueAndStateList visit(CFieldReference exp) throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public SMGAddressValueAndStateList visit(CCastExpression pCast) throws CPATransferException {
    // TODO Maybe cast values to pointer to null Object with offset as explicit value
    // for pointer arithmetic substraction ((void *) 4) - ((void *) 3)?
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(pCast));
  }
}