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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

class CompositeReducer extends GenericReducer<CompositeState, CompositePrecision> {

  private final List<Reducer> wrappedReducers;

  CompositeReducer(List<Reducer> pWrappedReducers) {
    wrappedReducers = pWrappedReducers;
  }

  @Override
  protected CompositeState getVariableReducedState0(
      CompositeState pExpandedState, Block pContext, CFANode pLocation)
      throws InterruptedException {

    List<AbstractState> states = pExpandedState.getWrappedStates();

    List<AbstractState> result = new ArrayList<>(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers.get(i).getVariableReducedState(states.get(i), pContext, pLocation));
    }
    return new CompositeState(result);
  }

  @Override
  protected CompositeState getVariableExpandedState0(
      CompositeState pRootState, Block pReducedContext, CompositeState pReducedState)
      throws InterruptedException {

    List<AbstractState> rootStates = pRootState.getWrappedStates();
    List<AbstractState> reducedStates = pReducedState.getWrappedStates();

    List<AbstractState> result = new ArrayList<>();
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers
              .get(i)
              .getVariableExpandedState(rootStates.get(i), pReducedContext, reducedStates.get(i)));
    }
    return new CompositeState(result);
  }

  @Override
  protected Object getHashCodeForState0(
      CompositeState pElementKey, CompositePrecision pPrecisionKey) {

    List<AbstractState> elements = pElementKey.getWrappedStates();
    List<Precision> precisions = pPrecisionKey.getWrappedPrecisions();

    List<Object> result = new ArrayList<>(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(wrappedReducers.get(i).getHashCodeForState(elements.get(i), precisions.get(i)));
    }
    return result;
  }

  @Override
  protected Precision getVariableReducedPrecision0(CompositePrecision pPrecision, Block pContext) {
    List<Precision> precisions = pPrecision.getWrappedPrecisions();

    List<Precision> result = new ArrayList<>(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(wrappedReducers.get(i).getVariableReducedPrecision(precisions.get(i), pContext));
    }

    return new CompositePrecision(result);
  }

  @Override
  protected CompositePrecision getVariableExpandedPrecision0(
      CompositePrecision pRootPrecision, Block pRootContext, CompositePrecision pReducedPrecision) {
    List<Precision> rootPrecisions = pRootPrecision.getWrappedPrecisions();
    List<Precision> reducedPrecisions = pReducedPrecision.getWrappedPrecisions();

    List<Precision> result = new ArrayList<>(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers
              .get(i)
              .getVariableExpandedPrecision(
                  rootPrecisions.get(i), pRootContext, reducedPrecisions.get(i)));
    }
    return new CompositePrecision(result);
  }

  @Override
  protected int measurePrecisionDifference0(
      CompositePrecision pPrecision, CompositePrecision pOtherPrecision) {
    List<Precision> precisions = pPrecision.getWrappedPrecisions();
    List<Precision> otherPrecisions = pOtherPrecision.getWrappedPrecisions();

    int sum = 0;
    for (int i = 0; i < wrappedReducers.size(); i++) {
      sum +=
          wrappedReducers
              .get(i)
              .measurePrecisionDifference(precisions.get(i), otherPrecisions.get(i));
    }

    return sum;
  }

  @Override
  protected CompositeState getVariableReducedStateForProofChecking0(
      CompositeState pExpandedState, Block pContext, CFANode pCallNode)
      throws InterruptedException {

    List<AbstractState> expandedStates = pExpandedState.getWrappedStates();

    List<AbstractState> result = new ArrayList<>(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers
              .get(i)
              .getVariableReducedStateForProofChecking(expandedStates.get(i), pContext, pCallNode));
    }
    return new CompositeState(result);
  }

  @Override
  protected CompositeState getVariableExpandedStateForProofChecking0(
      CompositeState pRootState, Block pReducedContext, CompositeState pReducedState)
      throws InterruptedException {
    List<AbstractState> rootStates = pRootState.getWrappedStates();
    List<AbstractState> reducedStates = pReducedState.getWrappedStates();

    List<AbstractState> result = new ArrayList<>(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers
              .get(i)
              .getVariableExpandedStateForProofChecking(
                  rootStates.get(i), pReducedContext, reducedStates.get(i)));
    }
    return new CompositeState(result);
  }

  @Override
  protected CompositeState rebuildStateAfterFunctionCall0(
      CompositeState pRootState,
      CompositeState pEntryState,
      CompositeState pExpandedState,
      FunctionExitNode exitLocation) {
    List<AbstractState> rootStates = pRootState.getWrappedStates();
    List<AbstractState> entryStates = pEntryState.getWrappedStates();
    List<AbstractState> expandedStates = pExpandedState.getWrappedStates();

    List<AbstractState> result = new ArrayList<>(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers
              .get(i)
              .rebuildStateAfterFunctionCall(
                  rootStates.get(i), entryStates.get(i), expandedStates.get(i), exitLocation));
    }
    return new CompositeState(result);
  }

  @Override
  protected boolean canBeUsedInCache0(CompositeState pState) {
    for (int i = 0; i < wrappedReducers.size(); i++) {
      if (!wrappedReducers.get(i).canBeUsedInCache(pState.get(i))) { return false; }
    }
    return true;
  }
}
