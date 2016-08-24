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
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.HashSet;
import java.util.Set;


public class ValueAnalysisReducer implements Reducer {

  /** returns a collection of variables used in the block */
  private Set<String> getBlockVariables(Block pBlock) {
    Set<String> vars = new HashSet<>();
    for (ReferencedVariable referencedVar : pBlock.getReferencedVariables()) {
      vars.add(referencedVar.getName());
    }
    return vars;
  }

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedState, Block pContext, CFANode pCallNode) {
    ValueAnalysisState expandedState = (ValueAnalysisState)pExpandedState;

    ValueAnalysisState clonedElement = ValueAnalysisState.copyOf(expandedState);
    Set<String> blockVariables = getBlockVariables(pContext);
    for (MemoryLocation trackedVar : expandedState.getTrackedMemoryLocations()) {
      // ignore offset (like "3" from "array[3]") to match assignments in loops ("array[i]=12;")
      final String simpleName = trackedVar.getAsSimpleString();
      if (!blockVariables.contains(simpleName)) {
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
    ValueAnalysisState diffElement = ValueAnalysisState.copyOf(reducedState);

    Set<String> blockVariables = getBlockVariables(pReducedContext);

    for (MemoryLocation trackedVar : rootState.getTrackedMemoryLocations()) {
      // ignore offset ("3" from "array[3]") to match assignments in loops ("array[i]=12;")
      final String simpleName = trackedVar.getAsSimpleString();
      if (!blockVariables.contains(simpleName)) {
        diffElement.assignConstant(trackedVar, rootState.getValueFor(trackedVar), rootState.getTypeForMemoryLocation(trackedVar));

      //} else {
        // ignore this case, the variables are part of the reduced state
        // (or might even be deleted, then they must stay unknown)
      }
    }

    return diffElement;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    VariableTrackingPrecision precision = (VariableTrackingPrecision)pPrecision;

    // TODO: anything meaningful we can do here?

    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {
    VariableTrackingPrecision rootPrecision = (VariableTrackingPrecision)pRootPrecision;
    VariableTrackingPrecision reducedPrecision = (VariableTrackingPrecision)pReducedPrecision;
    // After a refinement, rootPrecision can contain more variables than reducedPrecision.
    // This happens for recursive files or imprecise caching.
    // In this case we just merge the two precisions.
    return reducedPrecision.join(rootPrecision);
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    ValueAnalysisState elementKey = (ValueAnalysisState)pElementKey;
    VariableTrackingPrecision precisionKey = (VariableTrackingPrecision)pPrecisionKey;
    return Pair.of(elementKey, precisionKey);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState pRootState, AbstractState entryState,
      AbstractState pExpandedState, FunctionExitNode exitLocation) {

    ValueAnalysisState rootState = (ValueAnalysisState)pRootState;
    ValueAnalysisState expandedState = (ValueAnalysisState)pExpandedState;

    return expandedState.rebuildStateAfterFunctionCall(rootState, exitLocation);
  }
}
