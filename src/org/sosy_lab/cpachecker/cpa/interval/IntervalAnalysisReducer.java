/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Pair;


public class IntervalAnalysisReducer implements Reducer {

  private boolean occursInBlock(Block pBlock, String pVar) {
    // TODO could be more efficient (avoid linear runtime)
    for (ReferencedVariable referencedVar : pBlock.getReferencedVariables()) {
      if (referencedVar.getName().equals(pVar)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedState, Block pContext, CFANode pCallNode) {
    IntervalAnalysisState expandedState = (IntervalAnalysisState)pExpandedState;

    IntervalAnalysisState clonedElement = IntervalAnalysisState.copyOf(expandedState);
    for (String trackedVar : expandedState.getIntervalMap().keySet()) {
      // ignore offset (like "3" from "array[3]") to match assignments in loops ("array[i]=12;")
      if (!occursInBlock(pContext, trackedVar)) {
        clonedElement.removeInterval(trackedVar);
      }
    }

    return clonedElement;
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    IntervalAnalysisState rootState = (IntervalAnalysisState)pRootState;
    IntervalAnalysisState reducedState = (IntervalAnalysisState)pReducedState;

    // the expanded state will contain:
    // - all variables of the reduced state -> copy the state
    // - all non-block variables of the rootState -> copy those values
    // - not the variables of rootState used in the block -> just ignore those values
    IntervalAnalysisState diffElement = IntervalAnalysisState.copyOf(reducedState);

    for (String trackedVar : rootState.getIntervalMap().keySet()) {
      // ignore offset ("3" from "array[3]") to match assignments in loops ("array[i]=12;")
      if (!occursInBlock(pReducedContext, trackedVar)) {
        diffElement.addInterval(trackedVar, rootState.getInterval(trackedVar), -1);

      //} else {
        // ignore this case, the variables are part of the reduced state
        // (or might even be deleted, then they must stay unknown)
      }
    }

    return diffElement;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {

    // TODO: anything meaningful we can do here?

    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {

    // TODO: anything meaningful we can do here?

    return pReducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    IntervalAnalysisState elementKey = (IntervalAnalysisState)pElementKey;
    return Pair.of(elementKey.getIntervalMap(), pPrecisionKey);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState pRootState, AbstractState entryState,
      AbstractState pExpandedState, FunctionExitNode exitLocation) {

    IntervalAnalysisState rootState = (IntervalAnalysisState)pRootState;
    IntervalAnalysisState expandedState = (IntervalAnalysisState)pExpandedState;

    return expandedState.rebuildStateAfterFunctionCall(rootState, exitLocation);
  }
}
