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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * This class evaluates expressions that evaluate to a array type. The type of every expression
 * visited by this visitor has to be a {@link CArrayType }. The result of the evaluation is an
 * {@link SMGAddress}. The object represents the memory this array is placed in, the offset
 * represents the start of the array in the object.
 */
class ArrayVisitor extends AddressVisitor
    implements CRightHandSideVisitor<List<SMGAddressAndState>, CPATransferException> {

  public ArrayVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgExpressionEvaluator, pEdge, pSmgState);
  }

  @Override
  public List<SMGAddressAndState> visit(CUnaryExpression unaryExpression)
      throws CPATransferException {
    throw new AssertionError("The result of any unary expression " + "cannot be an array type.");
  }

  @Override
  public List<SMGAddressAndState> visit(CBinaryExpression binaryExp) throws CPATransferException {

    CExpression lVarInBinaryExp = binaryExp.getOperand1();
    CExpression rVarInBinaryExp = binaryExp.getOperand2();
    CType lVarInBinaryExpType = TypeUtils.getRealExpressionType(lVarInBinaryExp);
    CType rVarInBinaryExpType = TypeUtils.getRealExpressionType(rVarInBinaryExp);

    boolean lVarIsAddress = lVarInBinaryExpType instanceof CArrayType;
    boolean rVarIsAddress = rVarInBinaryExpType instanceof CArrayType;

    CExpression address;
    CExpression arrayOffset;
    CType addressType;

    if (lVarIsAddress == rVarIsAddress) {
      return Collections.singletonList(
          SMGAddressAndState.withUnknownAddress(
              getInitialSmgState())); // If both or neither are Addresses,
      //  we can't evaluate the address this pointer stores.
    } else if (lVarIsAddress) {
      address = lVarInBinaryExp;
      arrayOffset = rVarInBinaryExp;
      addressType = lVarInBinaryExpType;
    } else if (rVarIsAddress) {
      address = rVarInBinaryExp;
      arrayOffset = lVarInBinaryExp;
      addressType = rVarInBinaryExpType;
    } else {
      throw new UnrecognizedCodeException(
          "Expected either "
              + lVarInBinaryExp.toASTString()
              + " or "
              + rVarInBinaryExp.toASTString()
              + "to be a pointer to an array.",
          binaryExp);
    }

    // a = &a[0]
    return asAddressAndStateList(
        smgExpressionEvaluator.handlePointerArithmetic(
            getInitialSmgState(),
            getCfaEdge(),
            address,
            arrayOffset,
            addressType,
            lVarIsAddress,
            binaryExp));
  }

  @Override
  public List<SMGAddressAndState> visit(CIdExpression pVariableName) throws CPATransferException {

    List<SMGAddressAndState> addressAndStates = super.visit(pVariableName);

    // TODO correct?
    // parameter declaration array types are converted to pointer
    if (pVariableName.getDeclaration() instanceof CParameterDeclaration) {

      CType type = TypeUtils.getRealExpressionType(pVariableName);
      if (type instanceof CArrayType) {
        // if function declaration is in form 'int foo(char b[32])' then omit array length
        // TODO support C11 6.7.6.3 7:
        // actual argument shall provide access to the first element of
        // an array with at least as many elements as specified by the size expression
        type = new CPointerType(type.isConst(), type.isVolatile(), ((CArrayType) type).getType());
      }

      List<SMGAddressAndState> result = new ArrayList<>();
      for (SMGAddressAndState addressAndState : addressAndStates) {

        SMGAddress address = addressAndState.getObject();
        SMGState newState = addressAndState.getSmgState();

        SMGValueAndState pointerAndState =
            smgExpressionEvaluator.readValue(
                newState, address.getObject(), address.getOffset(), type, getCfaEdge());

        result.addAll(
            asAddressAndStateList(
                smgExpressionEvaluator.getAddressFromSymbolicValue(pointerAndState)));
      }

      return result;
    } else {
      return addressAndStates;
    }
  }

  @Override
  public List<SMGAddressAndState> visit(CCastExpression cast) throws CPATransferException {

    CExpression op = cast.getOperand();

    if (op.getExpressionType() instanceof CArrayType) {
      return cast.getOperand().accept(this);
    } else {
      // TODO cast reinterpretation
      return Collections.singletonList(SMGAddressAndState.withUnknownAddress(getInitialSmgState()));
    }
  }

  @Override
  public List<SMGAddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression) {
    return visitDefault(pIastFunctionCallExpression);
  }
}
