// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Splitter;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class ValueAnalysisReducer extends GenericReducer<ValueAnalysisState, VariableTrackingPrecision> {

  @Override
  protected ValueAnalysisState getVariableReducedState0(
      ValueAnalysisState pExpandedState, Block pContext, CFANode pCallNode) {
    ValueAnalysisState clonedElement = ValueAnalysisState.copyOf(pExpandedState);
    for (MemoryLocation trackedVar : pExpandedState.getTrackedMemoryLocations()) {
      // ignore offset (like "3" from "array/3") to match assignments in loops ("array[i]=12;")
      final String simpleName =
          Splitter.on("/").splitToList(trackedVar.getExtendedQualifiedName()).get(0);
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

    for (Entry<MemoryLocation, ValueAndType> e : pRootState.getConstants()) {
      // ignore offset ("3" from "array[3]") to match assignments in loops ("array[i]=12;")
      final MemoryLocation memloc = e.getKey();
      final ValueAndType valueAndType = e.getValue();
      final String simpleName = memloc.getExtendedQualifiedName();
      if (!pReducedContext.getVariables().contains(simpleName)) {
        diffElement.assignConstant(memloc, valueAndType.getValue(), valueAndType.getType());

        // } else {
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
