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
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class ExplicitValueVisitor extends AbstractExpressionValueVisitor {

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

  public ExplicitValueVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator, SMGState pSmgState, String pFunctionName,
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger,
      CFAEdge pEdge) {
    super(pFunctionName, pMachineModel, pLogger);
    smgExpressionEvaluator = pSmgExpressionEvaluator;
    smgState = pSmgState;
    edge = pEdge;
  }

  public SMGState getNewState() {
    return smgState;
  }

  public CFAEdge getEdge() {
    return edge;
  }

  public List<SMGState> getSmgStatesToBeProccessed() {
    return smgStatesToBeProccessed;
  }

  private SMGExplicitValue getExplicitValue(SMGSymbolicValue pValue) {

    if (pValue.isUnknown()) {
      return SMGUnknownValue.getInstance();
    }

    SMGExplicitValue explicitValue = smgState.getExplicit((SMGKnownSymValue) pValue);

    return explicitValue;
  }

  protected void setSmgState(SMGState pSmgState) {
    smgState = pSmgState;
  }

  @Override
  public Value visit(CBinaryExpression binaryExp)
      throws UnrecognizedCCodeException {

    Value value = super.visit(binaryExp);

    if (value.isUnknown() && binaryExp.getOperator().isLogicalOperator()) {
      /* We may be able to get an explicit Value from pointer comaprisons. */

      SMGValueAndStateList symValueAndStates;

      try {
        symValueAndStates = smgExpressionEvaluator.evaluateAssumptionValue(smgState, edge, binaryExp);
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 = new UnrecognizedCCodeException(
            "SMG cannot be evaluated", binaryExp);
        e2.initCause(e);
        throw e2;
      }

      SMGValueAndState symValueAndState;

      if (symValueAndStates.size() > 0) {
        symValueAndState = symValueAndStates.getValueAndStateList().get(0);
      } else {
        symValueAndState = SMGValueAndState.of(getNewState());
      }

      for (int c = 1; c < symValueAndStates.size(); c++) {
        smgStatesToBeProccessed.add(symValueAndStates.getValueAndStateList().get(c).getSmgState());
      }

      SMGSymbolicValue symValue = symValueAndState.getObject();
      smgState = symValueAndState.getSmgState();

      if (symValue.equals(SMGKnownSymValue.TRUE)) {
        return new NumericValue(1);
      } else if (symValue.equals(SMGKnownSymValue.FALSE)) {
        return new NumericValue(0);
      }
    }

    return value;
  }

  @Override
  protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
      throws UnrecognizedCCodeException {
    return evaluateLeftHandSideExpression(pCPointerExpression);
  }

  private Value evaluateLeftHandSideExpression(CLeftHandSide leftHandSide)
      throws UnrecognizedCCodeException {

    SMGValueAndStateList valueAndStates;
    try {
      valueAndStates = smgExpressionEvaluator.evaluateExpressionValue(smgState, edge, leftHandSide);
    } catch (CPATransferException e) {
      UnrecognizedCCodeException e2 =
          new UnrecognizedCCodeException("SMG cannot be evaluated", leftHandSide);
      e2.initCause(e);
      throw e2;
    }

    SMGValueAndState valueAndState;
    if (valueAndStates.size() > 0) {
      valueAndState = valueAndStates.getValueAndStateList().get(0);
    } else {
      valueAndState = SMGValueAndState.of(getNewState());
    }

    for (int c = 1; c < valueAndStates.size(); c++) {
      smgStatesToBeProccessed.add(valueAndStates.getValueAndStateList().get(c).getSmgState());
    }

    SMGSymbolicValue value = valueAndState.getObject();
    smgState = valueAndState.getSmgState();

    SMGExplicitValue expValue = getExplicitValue(value);
    if (expValue.isUnknown()) {
      return UnknownValue.getInstance();
    } else {
      return new NumericValue(expValue.getAsLong());
    }
  }

  @Override
  protected Value evaluateCIdExpression(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {
    return evaluateLeftHandSideExpression(pCIdExpression);
  }

  @Override
  protected Value evaluateJIdExpression(JIdExpression pVarName) {
    return null;
  }

  @Override
  protected Value evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
    return evaluateLeftHandSideExpression(pLValue);
  }

  @Override
  protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
      throws UnrecognizedCCodeException {
    return evaluateLeftHandSideExpression(pLValue);
  }
}