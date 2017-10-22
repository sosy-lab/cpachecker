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
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Visitor that derives further information from an assume edge
 */
class AssigningValueVisitor extends DefaultCExpressionVisitor<Void, CPATransferException> {

  private final SMGRightHandSideEvaluator smgRightHandSideEvaluator;
  private SMGState assignableState;
  private boolean truthValue = false;
  private CFAEdge edge;

  public AssigningValueVisitor(SMGRightHandSideEvaluator pSmgRightHandSideEvaluator, SMGState pSMGState, boolean pTruthvalue, CFAEdge pEdge) {
    smgRightHandSideEvaluator = pSmgRightHandSideEvaluator;
    assignableState = pSMGState;
    truthValue = pTruthvalue;
    edge = pEdge;
  }

  public SMGState getAssignedState() {
    return assignableState;
  }

  @Override
  protected Void visitDefault(CExpression pExp) throws CPATransferException {
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
    //TODO More precise

    CExpression operand1 = unwrap(binExp.getOperand1());
    CExpression operand2 = unwrap(binExp.getOperand2());
    BinaryOperator op = binExp.getOperator();

    if (operand1 instanceof CLeftHandSide) {
      deriveFurtherInformation((CLeftHandSide) operand1, operand2, op);
    }

    if (operand2 instanceof CLeftHandSide) {
      BinaryOperator resultOp = op;

      switch (resultOp) {
        case EQUALS:
        case NOT_EQUALS:
          break;
        default:
          resultOp = resultOp.getOppositLogicalOperator();
      }

      deriveFurtherInformation((CLeftHandSide) operand2, operand1, resultOp);
    }

    return null;
  }

  private void deriveFurtherInformation(CLeftHandSide lValue, CExpression exp, BinaryOperator op) throws CPATransferException {

    SMGExplicitValue rValue = smgRightHandSideEvaluator.evaluateExplicitValueV2(assignableState, edge, exp);

    if (rValue.isUnknown()) {
      // no further information can be inferred
      return;
    }

    SMGSymbolicValue rSymValue = smgRightHandSideEvaluator.evaluateExpressionValueV2(assignableState, edge, lValue);

    if(rSymValue.isUnknown()) {

      rSymValue = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());

      LValueAssignmentVisitor visitor = smgRightHandSideEvaluator.getLValueAssignmentVisitor(edge, assignableState);

      List<SMGAddressAndState> addressOfFields = lValue.accept(visitor);

      if (addressOfFields.size() != 1) {
        return;
      }

      SMGAddress addressOfField = addressOfFields.get(0).getObject();

      if (addressOfField.isUnknown()) {
        return;
      }

      assignableState = smgRightHandSideEvaluator.smgTransferRelation.writeValue(assignableState, addressOfField.getObject(),
          addressOfField.getOffset().getAsInt(), smgRightHandSideEvaluator.getRealExpressionType(lValue), rSymValue, edge);
    }
    int size = smgRightHandSideEvaluator.getBitSizeof(edge, smgRightHandSideEvaluator.getRealExpressionType(lValue), assignableState);
    assignableState.addPredicateRelation(rSymValue, size, rValue, size, op, edge);
    if (truthValue) {
      if (op == BinaryOperator.EQUALS) {
        assignableState.putExplicit((SMGKnownSymValue) rSymValue, (SMGKnownExpValue) rValue);
      }
    } else {
      if (op == BinaryOperator.NOT_EQUALS) {
        assignableState.putExplicit((SMGKnownSymValue) rSymValue, (SMGKnownExpValue) rValue);
        //TODO more precise
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

    LValueAssignmentVisitor visitor = smgRightHandSideEvaluator.getLValueAssignmentVisitor(edge, assignableState);

    List<SMGAddressAndState> addressOfFields = lValue.accept(visitor);

    if(addressOfFields.size() != 1) {
      return;
    }

    SMGAddress addressOfField = addressOfFields.get(0).getObject();

    if (addressOfField.isUnknown()) {
      return;
    }

    // If this value is known, the assumption can be evaluated, therefore it should be unknown
    assert smgRightHandSideEvaluator.evaluateExplicitValueV2(assignableState, edge, lValue).isUnknown();

    SMGSymbolicValue value = smgRightHandSideEvaluator.evaluateExpressionValueV2(assignableState, edge, lValue);

    // This symbolic value should have been added when evaluating the assume
    assert !value.isUnknown();

    assignableState.putExplicit((SMGKnownSymValue)value, SMGKnownExpValue.ZERO);

  }

  private CExpression unwrap(CExpression expression) {
    // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

    if (expression instanceof CCastExpression) {
      expression = unwrap(((CCastExpression) expression).getOperand());
    }

    return expression;
  }
}
