// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

class CSizeOfVisitor extends BaseSizeofVisitor {
  private final CFAEdge edge;
  private final SMGState state;
  private final Optional<CExpression> expression;
  private final SMGExpressionEvaluator eval;

  public CSizeOfVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator,
      CFAEdge pEdge,
      SMGState pState,
      Optional<CExpression> pExpression) {
    super(pSmgExpressionEvaluator.machineModel);
    edge = pEdge;
    state = pState;
    expression = pExpression;
    eval = pSmgExpressionEvaluator;
  }

  @Override
  public BigInteger visit(CArrayType pArrayType) throws IllegalArgumentException {

    CExpression arrayLength = pArrayType.getLength();

    BigInteger sizeOfType = pArrayType.getType().accept(this);

    /* If the array type has a constant size, we can simply
     * get the length of the array, but if the size
     * of the array type is variable, we have to try and calculate
     * the current size.
     */
    BigInteger length;

    if (arrayLength == null) {
      // treat size of unknown array length type as ptr
      return super.visit(pArrayType);
    } else if (arrayLength instanceof CIntegerLiteralExpression) {
      length = ((CIntegerLiteralExpression) arrayLength).getValue();
    } else if (edge instanceof CDeclarationEdge) {

      /* If we currently declare the array of this type,
       * we simply need to calculate the current length of the array
       * from the given expression in the type.
       */
      SMGExplicitValue lengthAsExplicitValue;

      try {
        lengthAsExplicitValue = eval.evaluateExplicitValueV2(state, edge, arrayLength);
      } catch (CPATransferException e) {
        throw new IllegalArgumentException(
            "Exception when calculating array length of " + pArrayType.toASTString("") + ".", e);
      }

      if (lengthAsExplicitValue.isUnknown()) {
        length = handleUnkownArrayLengthValue(pArrayType);
      } else {
        length = lengthAsExplicitValue.getValue();
      }

    } else {

      /*
       * If we are not at the declaration of the variable array type, we try to get the
       * smg object that represents the array, and calculate the current array size that way.
       */

      if (expression.filter(CLeftHandSide.class::isInstance).isPresent()) {

        LValueAssignmentVisitor visitor = eval.getLValueAssignmentVisitor(edge, state);

        List<SMGAddressAndState> addressOfFieldAndState;
        try {
          addressOfFieldAndState = expression.orElseThrow().accept(visitor);
        } catch (CPATransferException e) {
          return handleUnkownArrayLengthValue(pArrayType);
        }

        assert !addressOfFieldAndState.isEmpty();

        SMGAddress addressOfField = addressOfFieldAndState.get(0).getObject();

        if (addressOfField.isUnknown()) {
          return handleUnkownArrayLengthValue(pArrayType);
        }

        SMGObject arrayObject = addressOfField.getObject();
        BigInteger offset = addressOfField.getOffset().getValue();
        return BigInteger.valueOf(arrayObject.getSize()).subtract(offset);
      } else {
        throw new IllegalArgumentException(
            "Unable to calculate the size of the array type " + pArrayType.toASTString("") + ".");
      }
    }

    return length.multiply(sizeOfType);
  }

  BigInteger handleUnkownArrayLengthValue(CArrayType pArrayType) {
    throw new IllegalArgumentException(
        "Can't calculate array length of type " + pArrayType.toASTString("") + ".");
  }
}
