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
package org.sosy_lab.cpachecker.cpa.explicit;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class ExplicitReducer implements Reducer {

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
    ExplicitState expandedState = (ExplicitState)pExpandedState;

    ExplicitState clonedElement = expandedState.clone();
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
    ExplicitState rootState = (ExplicitState)pRootState;
    ExplicitState reducedState = (ExplicitState)pReducedState;

    ExplicitState diffElement = rootState.clone();
    for (String trackedVar : reducedState.getTrackedVariableNames()) {
      diffElement.forget(trackedVar);
    }
    //TODO: following is needed with threshold != inf
  /*  for (String trackedVar : diffElement.getTrackedVariableNames()) {
      if (occursInBlock(pReducedContext, trackedVar)) {
        diffElement.deleteValue(trackedVar);
      }
    }*/
    for (String trackedVar : reducedState.getTrackedVariableNames()) {
      Long value = reducedState.getValueFor(trackedVar);
      if (value != null) {
        diffElement.assignConstant(trackedVar, reducedState.getValueFor(trackedVar));
      } else {
        diffElement.forget(trackedVar);
      }
    }
    // set difference to avoid null pointer exception due to precision adaption of omniscient composite precision adjustment
    // to avoid that due to precision adaption in ABM ART which is not yet propagated tracked variable information is deleted
    diffElement.addToDelta(diffElement);

    return diffElement;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    ExplicitPrecision precision = (ExplicitPrecision)pPrecision;

    // TODO: anything meaningful we can do here?

    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {
    //ExplicitPrecision rootPrecision = (ExplicitPrecision)pRootPrecision;
    ExplicitPrecision reducedPrecision = (ExplicitPrecision)pReducedPrecision;

    // TODO: anything meaningful we can do here?

    return reducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    ExplicitState elementKey = (ExplicitState)pElementKey;
    ExplicitPrecision precisionKey = (ExplicitPrecision)pPrecisionKey;

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
