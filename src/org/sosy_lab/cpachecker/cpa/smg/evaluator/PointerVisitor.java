// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.ArrayList;
import java.util.Collections;
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
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * This class evaluates expressions that evaluate to a pointer type. The type of every expression
 * visited by this visitor has to be a {@link CPointerType }. The result of this evaluation is a
 * {@link SMGAddressValue}. The object and the offset of the result represent the address this
 * pointer points to. The value represents the value of the address itself. Note that the offset of
 * pointer addresses that point to the null object represent also the explicit value of the pointer.
 */
class PointerVisitor extends ExpressionValueVisitor {

  public PointerVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgExpressionEvaluator, pEdge, pSmgState);
  }

  @Override
  public List<SMGAddressValueAndState> visit(CIntegerLiteralExpression exp)
      throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public List<SMGAddressValueAndState> visit(CCharLiteralExpression exp)
      throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public List<SMGAddressValueAndState> visit(CFloatLiteralExpression pExp)
      throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(pExp));
  }

  @Override
  public List<SMGAddressValueAndState> visit(CIdExpression exp) throws CPATransferException {

    CType c = TypeUtils.getRealExpressionType(exp);

    if (c instanceof CArrayType) {
      // a == &a[0];
      return createAddressOfVariable(exp);
    }

    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public List<SMGAddressValueAndState> visit(CUnaryExpression unaryExpression)
      throws CPATransferException {

    UnaryOperator unaryOperator = unaryExpression.getOperator();
    CExpression unaryOperand = unaryExpression.getOperand();

    switch (unaryOperator) {
      case AMPER:
        return handleAmper(unaryOperand);

      case SIZEOF:
        throw new UnrecognizedCodeException(
            "Misinterpreted the expression type of "
                + unaryOperand.toASTString()
                + " as pointer type",
            cfaEdge,
            unaryExpression);

      case MINUS:
      case TILDE:
      default:
        // Can't evaluate these Addresses
        return Collections.singletonList(SMGAddressValueAndState.of(getInitialSmgState()));
    }
  }

  private List<SMGAddressValueAndState> handleAmper(CRightHandSide amperOperand)
      throws CPATransferException {

    if (TypeUtils.getRealExpressionType(amperOperand) instanceof CFunctionType
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
      return Collections.singletonList(SMGAddressValueAndState.of(getInitialSmgState()));
    }
  }

  List<SMGAddressValueAndState> createAddressOfFunction(CIdExpression idFunctionExpression)
      throws SMGInconsistentException {

    SMGState state = getInitialSmgState();

    SMGObject functionObject =
        state.getObjectForFunction((CFunctionDeclaration) idFunctionExpression.getDeclaration());

    if (functionObject == null) {
      return Collections.singletonList(SMGAddressValueAndState.of(state));
    }

    return smgExpressionEvaluator.createAddress(state, functionObject, SMGZeroValue.INSTANCE);
  }

  private List<SMGAddressValueAndState> createAddressOfArraySubscript(
      CArraySubscriptExpression lValue) throws CPATransferException {

    CExpression arrayExpression = lValue.getArrayExpression();

    List<SMGAddressValueAndState> result = new ArrayList<>(4);
    for (SMGAddressValueAndState arrayAddressAndState :
        smgExpressionEvaluator.evaluateAddress(
            getInitialSmgState(), getCfaEdge(), arrayExpression)) {

      SMGAddressValue arrayAddress = arrayAddressAndState.getObject();
      SMGState newState = arrayAddressAndState.getSmgState();

      if (arrayAddress.isUnknown()) {
        result.add(SMGAddressValueAndState.of(newState));
        continue;
      }

      CExpression subscriptExpr = lValue.getSubscriptExpression();
      for (SMGExplicitValueAndState subscriptValueAndState :
          smgExpressionEvaluator.evaluateExplicitValue(newState, getCfaEdge(), subscriptExpr)) {

        SMGExplicitValue subscriptValue = subscriptValueAndState.getObject();
        newState = subscriptValueAndState.getSmgState();

        if (subscriptValue.isUnknown()) {
          result.add(SMGAddressValueAndState.of(newState));
        } else {
          SMGExplicitValue arrayOffset = arrayAddress.getOffset();
          long typeSize =
              smgExpressionEvaluator.getBitSizeof(
                  getCfaEdge(), TypeUtils.getRealExpressionType(lValue), newState, lValue);
          SMGExplicitValue sizeOfType = SMGKnownExpValue.valueOf(typeSize);
          SMGExplicitValue offset = arrayOffset.add(subscriptValue.multiply(sizeOfType));
          List<SMGAddressValueAndState> resultAddressAndState =
              smgExpressionEvaluator.createAddress(newState, arrayAddress.getObject(), offset);
          result.addAll(resultAddressAndState);
        }
      }
    }

    return result;
  }

  private List<SMGAddressValueAndState> createAddressOfField(CFieldReference lValue)
      throws CPATransferException {

    List<SMGAddressValueAndState> result = new ArrayList<>(2);

    for (SMGAddressAndState addressOfFieldAndState :
        smgExpressionEvaluator.getAddressOfField(getInitialSmgState(), getCfaEdge(), lValue)) {

      SMGAddress addressOfField = addressOfFieldAndState.getObject();
      SMGState newState = addressOfFieldAndState.getSmgState();

      if (addressOfField.isUnknown()) {
        result.add(SMGAddressValueAndState.of(newState));
      } else {
        List<SMGAddressValueAndState> resultAddressValueAndState =
            smgExpressionEvaluator.createAddress(
                addressOfFieldAndState.getSmgState(),
                addressOfField.getObject(),
                addressOfField.getOffset());
        result.addAll(resultAddressValueAndState);
      }
    }

    return result;
  }

  private List<SMGAddressValueAndState> createAddressOfVariable(CIdExpression idExpression)
      throws SMGInconsistentException {

    SMGState state = getInitialSmgState();

    SMGObject variableObject = state.getHeap().getObjectForVisibleVariable(idExpression.getName());

    if (variableObject == null) {
      return Collections.singletonList(SMGAddressValueAndState.of(state));
    } else {
      state.addElementToCurrentChain(variableObject);
      return smgExpressionEvaluator.createAddress(state, variableObject, SMGZeroValue.INSTANCE);
    }
  }

  @Override
  public List<SMGAddressValueAndState> visit(CPointerExpression pointerExpression)
      throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(pointerExpression));
  }

  @Override
  public List<SMGAddressValueAndState> visit(CBinaryExpression binaryExp)
      throws CPATransferException {

    CExpression lVarInBinaryExp = binaryExp.getOperand1();
    CExpression rVarInBinaryExp = binaryExp.getOperand2();
    CType lVarInBinaryExpType =
        CTypes.adjustFunctionOrArrayType(TypeUtils.getRealExpressionType(lVarInBinaryExp));
    CType rVarInBinaryExpType =
        CTypes.adjustFunctionOrArrayType(TypeUtils.getRealExpressionType(rVarInBinaryExp));

    boolean lVarIsAddress = lVarInBinaryExpType instanceof CPointerType;
    boolean rVarIsAddress = rVarInBinaryExpType instanceof CPointerType;

    CExpression address;
    CExpression pointerOffset;
    CPointerType addressType;

    if (lVarIsAddress == rVarIsAddress) {
      return Collections.singletonList(
          SMGAddressValueAndState.of(getInitialSmgState())); // If both or neither are Addresses,
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
      throw new UnrecognizedCodeException(
          "Expected either "
              + lVarInBinaryExp.toASTString()
              + " or "
              + rVarInBinaryExp.toASTString()
              + "to be a pointer.",
          binaryExp);
    }

    CType typeOfPointer = TypeUtils.getRealExpressionType(addressType.getType());

    return smgExpressionEvaluator.handlePointerArithmetic(
        getInitialSmgState(),
        getCfaEdge(),
        address,
        pointerOffset,
        typeOfPointer,
        lVarIsAddress,
        binaryExp);
  }

  @Override
  public List<SMGAddressValueAndState> visit(CArraySubscriptExpression exp)
      throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public List<SMGAddressValueAndState> visit(CFieldReference exp) throws CPATransferException {
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(exp));
  }

  @Override
  public List<SMGAddressValueAndState> visit(CCastExpression pCast) throws CPATransferException {
    // TODO Maybe cast values to pointer to null Object with offset as explicit value
    // for pointer arithmetic substraction ((void *) 4) - ((void *) 3)?
    return smgExpressionEvaluator.getAddressFromSymbolicValues(super.visit(pCast));
  }
}
