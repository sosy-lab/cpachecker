/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LiveVariables;


public class ValueAnalysisPrecisionAdjustment implements PrecisionAdjustment {

  private final ValueAnalysisTransferRelation transfer;
  private final LiveVariables liveVariables;

  public ValueAnalysisPrecisionAdjustment(ValueAnalysisTransferRelation pTransfer, LiveVariables pLiveVariables) {
    transfer = pTransfer;
    liveVariables = pLiveVariables;
  }

  @Override
  public PrecisionAdjustmentResult prec(AbstractState pState, Precision pPrecision, UnmodifiableReachedSet pStates, AbstractState fullState)
      throws CPAException, InterruptedException {

    return prec((ValueAnalysisState)pState, (VariableTrackingPrecision)pPrecision);
  }

  private PrecisionAdjustmentResult prec(ValueAnalysisState pState, VariableTrackingPrecision pPrecision) {
    ValueAnalysisState resultState = ValueAnalysisState.copyOf(pState);

    for (MemoryLocation loc : pState.getTrackedMemoryLocations()) {
      if (!liveVariables.isVariableLive(loc.getAsSimpleString(), transfer.getSuccessorNodeFromLastEdge())) {
        resultState.forget(loc);
      }
    }

    return PrecisionAdjustmentResult.create(resultState, pPrecision, Action.CONTINUE);
  }
}
