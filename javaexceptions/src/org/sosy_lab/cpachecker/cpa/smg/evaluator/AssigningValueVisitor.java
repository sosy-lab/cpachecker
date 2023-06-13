// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/** Visitor that derives further information from an assume edge */
class AssigningValueVisitor extends DefaultCExpressionVisitor<Void, CPATransferException> {

  private final SMGRightHandSideEvaluator smgRightHandSideEvaluator;
  private SMGState assignableState;
  private final boolean truthValue;

  /**
   * The edge should never be used to retrieve any information. It should only be used for logging
   * and debugging, because we do not know the context of the caller.
   */
  private final CFAEdge edge;

  public AssigningValueVisitor(
      SMGRightHandSideEvaluator pSmgRightHandSideEvaluator,
      SMGState pSMGState,
      boolean pTruthvalue,
      CFAEdge pEdge) {
    smgRightHandSideEvaluator = pSmgRightHandSideEvaluator;
    assignableState = pSMGState;
    truthValue = pTruthvalue;
    edge = pEdge;
  }

  public SMGState getAssignedState() {
    return assignableState;
  }

  @Override
  protected Void visitDefault(CExpression pExp) {
    return null;
  }

  @Override
  public Void visit(CPointerExpression pointerExpression) throws CPATransferException {
    deriveFurtherInformation(pointerExpression);
    return null;
  }

  @Override
  public Void visit(CIdExpression pExp) throws CPATransferException {
    deriveFurtherInformation(pExp);
    return null;
  }

  @Override
  public Void visit(CArraySubscriptExpression pExp) throws CPATransferException {
    deriveFurtherInformation(pExp);
    return null;
  }

  @Override
  public Void visit(CFieldReference pExp) throws CPATransferException {
    deriveFurtherInformation(pExp);
    return null;
  }

  @Override
  public Void visit(CCastExpression pE) throws CPATransferException {
    // TODO cast reinterpretations
    return pE.getOperand().accept(this);
  }

  @Override
  public Void visit(CCharLiteralExpression pE) throws CPATransferException {
    throw new AssertionError();
  }

  @Override
  public Void visit(CFloatLiteralExpression pE) throws CPATransferException {
    throw new AssertionError();
  }

  @Override
  public Void visit(CIntegerLiteralExpression pE) throws CPATransferException {
    throw new AssertionError();
  }

  @Override
  public Void visit(CBinaryExpression binExp) throws CPATransferException {
    // TODO More precise

    CExpression operand1 = unwrap(binExp.getOperand1());
    CExpression operand2 = unwrap(binExp.getOperand2());
    BinaryOperator op = binExp.getOperator();

    if (operand1 instanceof CLeftHandSide) {
      deriveFurtherInformation((CLeftHandSide) operand1, operand2, op);
    }

    if (operand2 instanceof CLeftHandSide) {
      deriveFurtherInformation(
          (CLeftHandSide) operand2, operand1, op.getSwitchOperandsSidesLogicalOperator());
    }

    return null;
  }

  private void deriveFurtherInformation(CLeftHandSide lValue, CExpression exp, BinaryOperator op)
      throws CPATransferException {

    SMGExplicitValue rValue =
        smgRightHandSideEvaluator.evaluateExplicitValueV2(assignableState, edge, exp);

    if (rValue.isUnknown()) {
      // no further information can be inferred
      return;
    }

    SMGValue rSymValue =
        smgRightHandSideEvaluator.evaluateExpressionValueV2(assignableState, edge, lValue);

    CType lValueType = TypeUtils.getRealExpressionType(lValue);
    if (rSymValue.isUnknown()) {

      rSymValue = SMGKnownSymValue.of();

      LValueAssignmentVisitor visitor =
          smgRightHandSideEvaluator.getLValueAssignmentVisitor(edge, assignableState);

      List<SMGAddressAndState> addressOfFields = lValue.accept(visitor);

      if (addressOfFields.size() != 1) {
        return;
      }

      SMGAddress addressOfField = addressOfFields.get(0).getObject();

      if (addressOfField.isUnknown()) {
        return;
      }

      assignableState =
          smgRightHandSideEvaluator.writeValue(
              assignableState,
              addressOfField.getObject(),
              addressOfField.getOffset().getAsLong(),
              lValueType,
              rSymValue,
              edge);
    }
    SMGType symValueType =
        SMGType.constructSMGType(lValueType, assignableState, edge, smgRightHandSideEvaluator);
    assignableState.addPredicateRelation(rSymValue, symValueType, rValue, op, edge);
    if (truthValue) {
      if (op == BinaryOperator.EQUALS) {
        assignableState.putExplicit((SMGKnownSymbolicValue) rSymValue, (SMGKnownExpValue) rValue);
      }
    } else {
      if (op == BinaryOperator.NOT_EQUALS) {
        assignableState.putExplicit((SMGKnownSymbolicValue) rSymValue, (SMGKnownExpValue) rValue);
        // TODO more precise
      }
    }
  }

  @Override
  public Void visit(CUnaryExpression pE) throws CPATransferException {

    UnaryOperator op = pE.getOperator();

    CExpression operand = pE.getOperand();

    switch (op) {
      case AMPER:
        throw new AssertionError("In this case, the assume should be able to be calculated");
      case MINUS:
      case TILDE:
        // don't change the truth value
        return operand.accept(this);
      case SIZEOF:
        throw new AssertionError("At the moment, this case should be able to be calculated");
      default:
        // TODO alignof is not handled
    }

    return null;
  }

  private void deriveFurtherInformation(CLeftHandSide lValue) throws CPATransferException {

    if (truthValue) {
      return; // no further explicit Information can be derived
    }

    LValueAssignmentVisitor visitor =
        smgRightHandSideEvaluator.getLValueAssignmentVisitor(edge, assignableState);

    List<SMGAddressAndState> addressOfFields = lValue.accept(visitor);

    if (addressOfFields.size() != 1) {
      return;
    }

    SMGAddress addressOfField = addressOfFields.get(0).getObject();

    if (addressOfField.isUnknown()) {
      return;
    }

    // If this value is known, the assumption can be evaluated, therefore it should be unknown
    assert smgRightHandSideEvaluator
        .evaluateExplicitValueV2(assignableState, edge, lValue)
        .isUnknown();

    SMGValue value =
        smgRightHandSideEvaluator.evaluateExpressionValueV2(assignableState, edge, lValue);

    // This symbolic value should have been added when evaluating the assume
    assert !value.isUnknown();

    assignableState.putExplicit((SMGKnownSymbolicValue) value, SMGZeroValue.INSTANCE);
  }

  private CExpression unwrap(CExpression expression) {
    // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

    if (expression instanceof CCastExpression) {
      expression = unwrap(((CCastExpression) expression).getOperand());
    }

    return expression;
  }
}
