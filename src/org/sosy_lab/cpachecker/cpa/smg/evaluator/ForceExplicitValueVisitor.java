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
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

class ForceExplicitValueVisitor extends ExplicitValueVisitor {

  private final SMGRightHandSideEvaluator smgRightHandSideEvaluator;
  private final SMGKnownExpValue guessSize;

  public ForceExplicitValueVisitor(
      SMGRightHandSideEvaluator pSmgRightHandSideEvaluator,
      SMGExpressionEvaluator pSmgExpressionEvaluator,
      SMGState pSmgState,
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      CFAEdge pEdge,
      SMGOptions pOptions) {
    super(pSmgExpressionEvaluator, pSmgState, pFunctionName, pMachineModel, pLogger, pEdge);
    smgRightHandSideEvaluator = pSmgRightHandSideEvaluator;
    guessSize = SMGKnownExpValue.valueOf(pOptions.getGuessSize());
  }

  @Override
  protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
      throws UnrecognizedCCodeException {
    Value result = super.evaluateCArraySubscriptExpression(pLValue);
    return returnValueOrGuess(result, pLValue);
  }

  @Override
  protected Value evaluateCIdExpression(CIdExpression pCIdExpression)
      throws UnrecognizedCCodeException {
    Value result = super.evaluateCIdExpression(pCIdExpression);
    return returnValueOrGuess(result, pCIdExpression);
  }

  @Override
  protected Value evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
    Value result = super.evaluateCFieldReference(pLValue);
    return returnValueOrGuess(result, pLValue);
  }

  @Override
  protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
      throws UnrecognizedCCodeException {
    Value result = super.evaluateCPointerExpression(pCPointerExpression);
    return returnValueOrGuess(result, pCPointerExpression);
  }

  private Value returnValueOrGuess(Value value, CLeftHandSide exp) throws UnrecognizedCCodeException {
    return value.isUnknown() ? guessLHS(exp) : value;
  }

  private Value guessLHS(CLeftHandSide exp)
      throws UnrecognizedCCodeException {

    SMGValueAndState symbolicValueAndState;

    try {
      SMGValueAndStateList symbolicValueAndStates =
          smgRightHandSideEvaluator.evaluateExpressionValue(getNewState(), getEdge(), exp);

      if (symbolicValueAndStates.size() != 1) {
        throw new SMGInconsistentException(
            "Found abstraction where non should exist, due to the expression " + exp.toASTString()
                + " already being evaluated once in this transfer-relation step.");
      }

      symbolicValueAndState = symbolicValueAndStates.getValueAndStateList().get(0);

    } catch (CPATransferException e) {
      UnrecognizedCCodeException e2 = new UnrecognizedCCodeException(
          "SMG cannot get symbolic value of : " + exp.toASTString(), exp);
      e2.initCause(e);
      throw e2;
    }

    SMGSymbolicValue value = symbolicValueAndState.getObject();
    setSmgState(symbolicValueAndState.getSmgState());

    if (value.isUnknown()) {
      return UnknownValue.getInstance();
    }

    getNewState().putExplicit((SMGKnownSymValue) value, guessSize);

    return new NumericValue(guessSize.getValue());
  }
}
