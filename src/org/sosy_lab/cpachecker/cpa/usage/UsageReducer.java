// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

public class UsageReducer implements Reducer {
  private final Reducer wrappedReducer;

  public UsageReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedElement, Block pContext, CFANode pLocation)
      throws InterruptedException {

    UsageState funElement = (UsageState) pExpandedElement;
    AbstractState red =
        wrappedReducer.getVariableReducedState(funElement.getWrappedState(), pContext, pLocation);
    return funElement.reduce(red);
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootElement, Block pReducedContext, AbstractState pReducedElement)
      throws InterruptedException {
    UsageState funRootState = (UsageState) pRootElement;
    UsageState funReducedState = (UsageState) pReducedElement;
    AbstractState exp;
    // if (!funReducedState.isExitState()) {
    exp =
        wrappedReducer.getVariableExpandedState(
            funRootState.getWrappedState(), pReducedContext, funReducedState.getWrappedState());
    /*} else {
      //Predicate analysis can not expand a random state - only abstract ones,
      // and Exitable one can occur at any moment
      exp = funReducedState.getWrappedState();
    }*/
    UsageState result = funRootState.copy(exp);
    result.joinContainerFrom(funReducedState);
    // if (funReducedState.isExitState()) {
    result.asExitable();
    // }
    return result;
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    UsageState funElement = (UsageState) pElementKey;
    UsagePrecision precision = (UsagePrecision) pPrecisionKey;
    return wrappedReducer.getHashCodeForState(
        funElement.getWrappedState(), precision.getWrappedPrecision());
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    UsagePrecision newPrecision =
        ((UsagePrecision) pPrecision)
            .copy(
                wrappedReducer.getVariableReducedPrecision(
                    ((UsagePrecision) pPrecision).getWrappedPrecision(), pContext));
    return newPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    UsagePrecision redPrecision = (UsagePrecision) reducedPrecision;
    UsagePrecision newPrecision =
        ((UsagePrecision) rootPrecision)
            .copy(
                wrappedReducer.getVariableExpandedPrecision(
                    ((UsagePrecision) rootPrecision).getWrappedPrecision(),
                    rootContext,
                    redPrecision.getWrappedPrecision()));
    return newPrecision;
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    UsagePrecision first = (UsagePrecision) pPrecision;
    UsagePrecision second = (UsagePrecision) pOtherPrecision;
    int wrapperDifference =
        wrappedReducer.measurePrecisionDifference(
            first.getWrappedPrecision(), second.getWrappedPrecision());
    return wrapperDifference + Math.abs(first.getTotalRecords() - second.getTotalRecords());
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState pRootState,
      AbstractState pEntryState,
      AbstractState pExpandedState,
      FunctionExitNode pExitLocation) {
    return pExpandedState;
  }
}
