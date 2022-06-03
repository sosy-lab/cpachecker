// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class ExplicitValueVisitor extends AbstractExpressionValueVisitor {

  private final SMGExpressionEvaluator smgExpressionEvaluator;

  private final CFAEdge edge;

  /* Will be updated while evaluating left hand side expressions.
   * Represents the current state of the value state pair
   */
  private SMGState smgState;

  /*
   * If there is more than one result based on the current
   * smg State due to abstraction, store the additional smgStates
   * that have to be usd to calculate a different result for the current
   * value in this list.
   *
   */
  private final List<SMGState> smgStatesToBeProccessed = new ArrayList<>();

  public ExplicitValueVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator,
      SMGState pSmgState,
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      CFAEdge pEdge) {
    super(pFunctionName, pMachineModel, pLogger);
    smgExpressionEvaluator = pSmgExpressionEvaluator;
    smgState = pSmgState;
    edge = pEdge;
  }

  public SMGState getState() {
    return smgState;
  }

  void setState(SMGState pSmgState) {
    smgState = pSmgState;
  }

  CFAEdge getEdge() {
    return edge;
  }

  public List<SMGState> getSmgStatesToBeProccessed() {
    return smgStatesToBeProccessed;
  }

  private SMGExplicitValue getExplicitValue(SMGValue pValue) {
    if (pValue.isUnknown()) {
      return SMGUnknownValue.INSTANCE;
    }
    if (pValue instanceof SMGKnownExpValue) {
      return (SMGExplicitValue) pValue;
    }
    if (!getState().isExplicit(pValue)) {
      return SMGUnknownValue.INSTANCE;
    }
    return Preconditions.checkNotNull(
        getState().getExplicit(pValue), "known and existing value cannot be read from state");
  }

  @Override
  public Value visit(CBinaryExpression binaryExp) throws UnrecognizedCodeException {

    Value value = super.visit(binaryExp);

    if (value.isUnknown()) {
      if (binaryExp.getOperator().isLogicalOperator()) {
        /* We may be able to get an explicit Value from pointer comparisons. */

        List<? extends SMGValueAndState> symValueAndStates;

        try {
          symValueAndStates =
              smgExpressionEvaluator.evaluateAssumptionValue(getState(), edge, binaryExp);
        } catch (CPATransferException e) {
          UnrecognizedCodeException e2 =
              new UnrecognizedCodeException("SMG cannot be evaluated", binaryExp);
          e2.initCause(e);
          throw e2;
        }

        SMGValueAndState symValueAndState = getStateAndAddRestForLater(symValueAndStates);
        SMGValue symValue = symValueAndState.getObject();
        setState(symValueAndState.getSmgState());

        if (symValue.equals(SMGKnownSymValue.TRUE)) {
          return new NumericValue(1);
        } else if (symValue.equals(SMGZeroValue.INSTANCE)) {
          return new NumericValue(0);
        }
      } else if (BinaryOperator.MINUS == binaryExp.getOperator()) {
        /* We may be able to get an explicit Value from pointer comparisons. */
        // TODO without the redirection to the explicit value visitor above,
        // we could also directly solve this and avoid those special cases.

        List<? extends SMGValueAndState> symValueAndStates;

        try {
          symValueAndStates =
              smgExpressionEvaluator.evaluateAssumptionValue(getState(), edge, binaryExp);
        } catch (CPATransferException e) {
          UnrecognizedCodeException e2 =
              new UnrecognizedCodeException("SMG cannot be evaluated", binaryExp);
          e2.initCause(e);
          throw e2;
        }

        // TODO the next line sets a backtracking point within a visitor.
        // This is a really bad idea and makes the control flow really ugly.
        // I have no idea whether and how this worked at any time. We should avoid this.
        SMGValueAndState symValueAndState = getStateAndAddRestForLater(symValueAndStates);
        SMGValue symValue = symValueAndState.getObject();
        setState(symValueAndState.getSmgState());

        CType type1 = binaryExp.getOperand1().getExpressionType().getCanonicalType();
        if (symValue instanceof SMGKnownExpValue && type1 instanceof CPointerType) {
          return new NumericValue(((SMGKnownExpValue) symValue).getValue());
        }
      }
    }

    return value;
  }

  @Override
  protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
      throws UnrecognizedCodeException {
    return evaluateLeftHandSideExpression(pCPointerExpression);
  }

  private Value evaluateLeftHandSideExpression(CLeftHandSide leftHandSide)
      throws UnrecognizedCodeException {

    List<? extends SMGValueAndState> valueAndStates;
    try {
      valueAndStates =
          smgExpressionEvaluator.evaluateExpressionValue(getState(), edge, leftHandSide);
    } catch (CPATransferException e) {
      UnrecognizedCodeException e2 =
          new UnrecognizedCodeException("SMG cannot be evaluated", leftHandSide);
      e2.initCause(e);
      throw e2;
    }

    SMGValueAndState valueAndState = getStateAndAddRestForLater(valueAndStates);
    SMGValue value = valueAndState.getObject();
    setState(valueAndState.getSmgState());

    SMGExplicitValue expValue = getExplicitValue(value);
    if (expValue.isUnknown()) {
      return UnknownValue.getInstance();
    } else {
      return new NumericValue(expValue.getAsLong());
    }
  }

  /**
   * Returns the first state (or a new state if list is empty) and stores the rest of the list for
   * later analysis.
   */
  private SMGValueAndState getStateAndAddRestForLater(
      final List<? extends SMGValueAndState> valueAndStates) {
    final SMGValueAndState valueAndState;
    if (!valueAndStates.isEmpty()) {
      valueAndState = valueAndStates.get(0);
    } else {
      valueAndState = SMGValueAndState.withUnknownValue(getState());
    }

    for (int c = 1; c < valueAndStates.size(); c++) {
      smgStatesToBeProccessed.add(valueAndStates.get(c).getSmgState());
    }
    return valueAndState;
  }

  @Override
  protected Value evaluateCIdExpression(CIdExpression pCIdExpression)
      throws UnrecognizedCodeException {
    return evaluateLeftHandSideExpression(pCIdExpression);
  }

  @Override
  protected Value evaluateJIdExpression(JIdExpression pVarName) {
    return null;
  }

  @Override
  protected Value evaluateCFieldReference(CFieldReference pLValue)
      throws UnrecognizedCodeException {
    return evaluateLeftHandSideExpression(pLValue);
  }

  @Override
  protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
      throws UnrecognizedCodeException {
    return evaluateLeftHandSideExpression(pLValue);
  }

  @Override
  public Value visit(JClassLiteralExpression pJClassLiteralExpression) {
    return UnknownValue.getInstance();
  }
}
