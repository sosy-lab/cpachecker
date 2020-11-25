// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.bam;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;

public class BAMSMGReducer implements Reducer {

  @Override
  public AbstractState getVariableReducedState(
      AbstractState expandedState, Block context, CFANode callNode) throws InterruptedException {
    SMGState expandedBAMSMGState = (SMGState) expandedState;
    return expandedBAMSMGState.prepareForBam();
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState rootState, Block reducedContext, AbstractState reducedState)
      throws InterruptedException {
    SMGState reducedBAMSMGState = (SMGState) reducedState;
    var baseState = ((SMGState) rootState);
    return (SMGState.expandBAM(reducedBAMSMGState, baseState));
  }

  @Override
  public Precision getVariableReducedPrecision(Precision precision, Block context) {
    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return reducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState stateKey, Precision precisionKey) {
    return null;
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState,
      AbstractState entryState,
      AbstractState expandedState,
      FunctionExitNode exitLocation) {
    return null;
  }
}
