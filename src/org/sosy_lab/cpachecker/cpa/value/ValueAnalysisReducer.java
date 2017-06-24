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

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


class ValueAnalysisReducer extends GenericReducer<ValueAnalysisState, VariableTrackingPrecision> {

  @Override
  protected ValueAnalysisState getVariableReducedState0(
      ValueAnalysisState pExpandedState, Block pContext, CFANode pCallNode) {
    ValueAnalysisState clonedElement = ValueAnalysisState.copyOf(pExpandedState);
    for (MemoryLocation trackedVar : pExpandedState.getTrackedMemoryLocations()) {
      // ignore offset (like "3" from "array[3]") to match assignments in loops ("array[i]=12;")
      final String simpleName = trackedVar.getAsSimpleString();
      if (!pContext.getVariables().contains(simpleName)) {
        clonedElement.forget(trackedVar);
      }
    }
    return clonedElement;
  }

  @Override
  protected ValueAnalysisState getVariableExpandedState0(
      ValueAnalysisState pRootState, Block pReducedContext, ValueAnalysisState pReducedState) {
    // the expanded state will contain:
    // - all variables of the reduced state -> copy the state
    // - all non-block variables of the rootState -> copy those values
    // - not the variables of rootState used in the block -> just ignore those values
    ValueAnalysisState diffElement = ValueAnalysisState.copyOf(pReducedState);

    for (MemoryLocation trackedVar : pRootState.getTrackedMemoryLocations()) {
      // ignore offset ("3" from "array[3]") to match assignments in loops ("array[i]=12;")
      final String simpleName = trackedVar.getAsSimpleString();
      if (!pReducedContext.getVariables().contains(simpleName)) {
        diffElement.assignConstant(
            trackedVar,
            pRootState.getValueFor(trackedVar),
            pRootState.getTypeForMemoryLocation(trackedVar));

      //} else {
        // ignore this case, the variables are part of the reduced state
        // (or might even be deleted, then they must stay unknown)
      }
    }

    return diffElement;
  }

  @Override
  protected VariableTrackingPrecision getVariableReducedPrecision0(
      VariableTrackingPrecision pPrecision, Block pContext) {
    VariableTrackingPrecision precision = pPrecision;

    // TODO: anything meaningful we can do here?

    return precision;
  }

  @Override
  protected VariableTrackingPrecision getVariableExpandedPrecision0(
      VariableTrackingPrecision pRootPrecision,
      Block pRootContext,
      VariableTrackingPrecision pReducedPrecision) {
    // After a refinement, rootPrecision can contain more variables than reducedPrecision.
    // This happens for recursive files or imprecise caching.
    // In this case we just merge the two precisions.
    return pReducedPrecision.join(pRootPrecision);
  }

  @Override
  protected Object getHashCodeForState0(
      ValueAnalysisState pElementKey, VariableTrackingPrecision pPrecisionKey) {
    return Pair.of(pElementKey, pPrecisionKey);
  }

  @Override
  protected ValueAnalysisState rebuildStateAfterFunctionCall0(
      ValueAnalysisState pRootState,
      ValueAnalysisState entryState,
      ValueAnalysisState pExpandedState,
      FunctionExitNode exitLocation) {
    return pExpandedState.rebuildStateAfterFunctionCall(pRootState, exitLocation);
  }
}
