/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.Builder;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;

class FunctionPointerReducer extends GenericReducer<FunctionPointerState, Precision> {

  @Override
  protected FunctionPointerState getVariableReducedState0(
      FunctionPointerState pExpandedState, Block pContext, CFANode pCallNode) {
    Builder builder = pExpandedState.createBuilder();
    for (String trackedVar : pExpandedState.getTrackedVariables()) {
      if (!pContext.getVariables().contains(trackedVar)) {
        builder.setTarget(trackedVar, UnknownTarget.getInstance());
      }
    }
    return builder.build();
  }

  @Override
  protected FunctionPointerState getVariableExpandedState0(
      FunctionPointerState pRootState, Block pReducedContext, FunctionPointerState pReducedState) {
    // the expanded state will contain:
    // - all variables of the reduced state -> copy the state
    // - all non-block variables of the rootState -> copy those values
    // - not the variables of rootState used in the block -> just ignore those values
    Builder diffElement = pReducedState.createBuilder();
    for (String trackedVar : pRootState.getTrackedVariables()) {
      if (!pReducedContext.getVariables().contains(trackedVar)) {
        diffElement.setTarget(trackedVar, pRootState.getTarget(trackedVar));
        // } else {
        // ignore this case, the variables are part of the reduced state
        // (or might even be deleted, then they must stay unknown)
      }
    }

    return diffElement.build();
  }

  @Override
  protected Precision getVariableReducedPrecision0(Precision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  protected Precision getVariableExpandedPrecision0(
      Precision pRootPrecision, Block pRootContext, Precision pReducedPrecision) {
    return pRootPrecision;
  }

  @Override
  protected Object getHashCodeForState0(FunctionPointerState pState, Precision pPrecision) {
    return pState;
  }

  @Override
  protected FunctionPointerState rebuildStateAfterFunctionCall0(
      FunctionPointerState pCallState,
      FunctionPointerState entryState,
      FunctionPointerState pExpandedState,
      FunctionExitNode exitLocation) {

    // we build a new state from:
    // - local variables from callState,
    // - global variables from expandedState,
    // - the local return variable from expandedState.
    // we copy callState and override all global values and the return variable.

    Builder rebuildState = pCallState.createBuilder();

    // first forget all global information
    for (String trackedVar : pCallState.getTrackedVariables()) {
      if (!isOnFunctionStack(trackedVar)) { // global -> delete
        rebuildState.setTarget(trackedVar, UnknownTarget.getInstance());
      }
    }

    // second: learn new information
    Optional<? extends AVariableDeclaration> retval =
        exitLocation.getEntryNode().getReturnVariable();
    for (String trackedVar : pExpandedState.getTrackedVariables()) {
      if (!isOnFunctionStack(trackedVar)) { // global -> override deleted value
        rebuildState.setTarget(trackedVar, pExpandedState.getTarget(trackedVar));
      } else if (retval.isPresent() && retval.get().getQualifiedName().equals(trackedVar)) {
        if (pExpandedState.getTarget(trackedVar) != UnknownTarget.getInstance()) {
          rebuildState.setTarget(trackedVar, pExpandedState.getTarget(trackedVar));
        }
      }
    }

    return rebuildState.build();
  }

  private static boolean isOnFunctionStack(String var) {
    return var.contains("::");
  }
}
