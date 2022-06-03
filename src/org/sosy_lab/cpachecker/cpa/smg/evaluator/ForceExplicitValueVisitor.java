// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.List;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class ForceExplicitValueVisitor extends ExplicitValueVisitor {

  private final SMGRightHandSideEvaluator smgRightHandSideEvaluator;
  private final SMGKnownExpValue guessSize;

  public ForceExplicitValueVisitor(
      SMGRightHandSideEvaluator pSmgRightHandSideEvaluator,
      SMGState pSmgState,
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      CFAEdge pEdge,
      SMGOptions pOptions) {
    super(pSmgRightHandSideEvaluator, pSmgState, pFunctionName, pMachineModel, pLogger, pEdge);
    smgRightHandSideEvaluator = pSmgRightHandSideEvaluator;
    guessSize = SMGKnownExpValue.valueOf(pOptions.getGuessSize());
  }

  @Override
  protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
      throws UnrecognizedCodeException {
    Value result = super.evaluateCArraySubscriptExpression(pLValue);
    return returnValueOrGuess(result, pLValue);
  }

  @Override
  protected Value evaluateCIdExpression(CIdExpression pCIdExpression)
      throws UnrecognizedCodeException {
    Value result = super.evaluateCIdExpression(pCIdExpression);
    return returnValueOrGuess(result, pCIdExpression);
  }

  @Override
  protected Value evaluateCFieldReference(CFieldReference pLValue)
      throws UnrecognizedCodeException {
    Value result = super.evaluateCFieldReference(pLValue);
    return returnValueOrGuess(result, pLValue);
  }

  @Override
  protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
      throws UnrecognizedCodeException {
    Value result = super.evaluateCPointerExpression(pCPointerExpression);
    return returnValueOrGuess(result, pCPointerExpression);
  }

  private Value returnValueOrGuess(Value value, CLeftHandSide exp)
      throws UnrecognizedCodeException {
    return value.isUnknown() ? guessLHS(exp) : value;
  }

  private Value guessLHS(CLeftHandSide exp) throws UnrecognizedCodeException {

    SMGValueAndState symbolicValueAndState;

    try {
      List<? extends SMGValueAndState> symbolicValueAndStates =
          smgRightHandSideEvaluator.evaluateExpressionValue(getState(), getEdge(), exp);

      if (symbolicValueAndStates.size() != 1) {
        throw new SMGInconsistentException(
            "Found abstraction where non should exist, due to the expression "
                + exp.toASTString()
                + " already being evaluated once in this transfer-relation step.");
      }

      symbolicValueAndState = symbolicValueAndStates.get(0);

    } catch (CPATransferException e) {
      UnrecognizedCodeException e2 =
          new UnrecognizedCodeException(
              "SMG cannot get symbolic value of : " + exp.toASTString(), exp);
      e2.initCause(e);
      throw e2;
    }

    SMGValue value = symbolicValueAndState.getObject();
    setState(symbolicValueAndState.getSmgState());

    if (value.isUnknown()) {
      return UnknownValue.getInstance();
    }

    getState().putExplicit((SMGKnownSymbolicValue) value, guessSize);

    return new NumericValue(guessSize.getAsInt());
  }
}
