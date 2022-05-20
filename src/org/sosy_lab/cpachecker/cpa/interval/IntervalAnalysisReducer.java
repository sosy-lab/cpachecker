// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;


class IntervalAnalysisReducer extends GenericReducer<IntervalAnalysisState, Precision> {

  @Override
  protected IntervalAnalysisState getVariableReducedState0(
      IntervalAnalysisState pExpandedState, Block pContext, CFANode pCallNode) {
    IntervalAnalysisState clonedElement = pExpandedState;
    for (String trackedVar : pExpandedState.getIntervalMap().keySet()) {
      // ignore offset (like "3" from "array[3]") to match assignments in loops ("array[i]=12;")
      if (!pContext.getVariables().contains(trackedVar)) {
        clonedElement = clonedElement.removeInterval(trackedVar);
      }
    }
    return clonedElement;
  }

  @Override
  protected IntervalAnalysisState getVariableExpandedState0(
      IntervalAnalysisState pRootState,
      Block pReducedContext,
      IntervalAnalysisState pReducedState) {
    // the expanded state will contain:
    // - all variables of the reduced state -> copy the state
    // - all non-block variables of the rootState -> copy those values
    // - not the variables of rootState used in the block -> just ignore those values
    IntervalAnalysisState diffElement = pReducedState;

    for (String trackedVar : pRootState.getIntervalMap().keySet()) {
      // ignore offset ("3" from "array[3]") to match assignments in loops ("array[i]=12;")
      if (!pReducedContext.getVariables().contains(trackedVar)) {
        diffElement = diffElement.addInterval(trackedVar, pRootState.getInterval(trackedVar), -1);

      //} else {
        // ignore this case, the variables are part of the reduced state
        // (or might even be deleted, then they must stay unknown)
      }
    }

    return diffElement;
  }

  @Override
  protected Precision getVariableReducedPrecision0(Precision pPrecision, Block pContext) {

    // TODO: anything meaningful we can do here?

    return pPrecision;
  }

  @Override
  protected Precision getVariableExpandedPrecision0(
      Precision pRootPrecision, Block pRootContext, Precision pReducedPrecision) {

    // TODO: anything meaningful we can do here?

    return pReducedPrecision;
  }

  @Override
  protected Object getHashCodeForState0(
      IntervalAnalysisState pElementKey, Precision pPrecisionKey) {
    return Pair.of(pElementKey.getIntervalMap(), pPrecisionKey);
  }

  @Override
  protected IntervalAnalysisState rebuildStateAfterFunctionCall0(
      IntervalAnalysisState pRootState,
      IntervalAnalysisState entryState,
      IntervalAnalysisState pExpandedState,
      FunctionExitNode exitLocation) {

    IntervalAnalysisState rootState = pRootState;
    IntervalAnalysisState expandedState = pExpandedState;

    return expandedState.rebuildStateAfterFunctionCall(rootState, exitLocation);
  }
}
