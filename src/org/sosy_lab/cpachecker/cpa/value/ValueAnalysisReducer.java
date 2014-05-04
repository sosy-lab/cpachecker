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

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class ValueAnalysisReducer implements Reducer {

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
    ValueAnalysisState expandedState = (ValueAnalysisState)pExpandedState;

    ValueAnalysisState clonedElement = expandedState.clone();
    for (String trackedVar : expandedState.getTrackedVariableNames()) {
      if (!occursInBlock(pContext, trackedVar)) {
        clonedElement.forget(trackedVar);
      }
    }

    return clonedElement;
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    ValueAnalysisState rootState = (ValueAnalysisState)pRootState;
    ValueAnalysisState reducedState = (ValueAnalysisState)pReducedState;

    // the expanded state will contain:
    // - all variables of the reduced state -> copy the state
    // - all non-block variables of the rootState -> copy those values
    // - not the variables of rootState used in the block -> just ignore those values
    ValueAnalysisState diffElement = reducedState.clone();

    for (String trackedVar : rootState.getTrackedVariableNames()) {
      if (!occursInBlock(pReducedContext, trackedVar)) {
        diffElement.assignConstant(trackedVar, rootState.getValueFor(trackedVar));

      //} else {
        // ignore this case, the variables are part of the reduced state
        // (or might even be deleted, then they must stay unknown)
      }
    }

    // set difference to avoid null pointer exception due to precision adaption of omniscient composite precision adjustment
    // to avoid that due to precision adaption in BAM ART which is not yet propagated tracked variable information is deleted
    diffElement.addToDelta(diffElement);

    return diffElement;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    ValueAnalysisPrecision precision = (ValueAnalysisPrecision)pPrecision;

    // TODO: anything meaningful we can do here?

    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {
    //ValueAnalysisPrecision rootPrecision = (ValueAnalysisPrecision)pRootPrecision;
    ValueAnalysisPrecision reducedPrecision = (ValueAnalysisPrecision)pReducedPrecision;

    // TODO: anything meaningful we can do here?

    return reducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    ValueAnalysisState elementKey = (ValueAnalysisState)pElementKey;
    ValueAnalysisPrecision precisionKey = (ValueAnalysisPrecision)pPrecisionKey;

    return Pair.of(elementKey.getConstantsMap(), precisionKey);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    return getVariableReducedState(pExpandedState, pContext, pCallNode);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    return getVariableExpandedState(pRootState, pReducedContext, pReducedState);
  }

}
