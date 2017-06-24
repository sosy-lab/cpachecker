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
import org.sosy_lab.cpachecker.util.Pair;

class CompositeReducer extends GenericReducer<CompositeState, CompositePrecision> {

  private final List<Reducer> wrappedReducers;

  CompositeReducer(List<Reducer> pWrappedReducers) {
    wrappedReducers = pWrappedReducers;
  }

  @Override
  protected CompositeState getVariableReducedState0(
      CompositeState pExpandedState, Block pContext, CFANode pLocation)
      throws InterruptedException {

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (AbstractState expandedState : pExpandedState.getWrappedStates()) {
      result.add(wrappedReducers.get(i++).getVariableReducedState(expandedState, pContext, pLocation));
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
    int i = 0;
    for (Pair<AbstractState, AbstractState> p : Pair.zipList(rootStates, reducedStates)) {
      result.add(wrappedReducers.get(i++).getVariableExpandedState(p.getFirst(), pReducedContext, p.getSecond()));
    }
    return new CompositeState(result);
  }

  @Override
  protected Object getHashCodeForState0(
      CompositeState pElementKey, CompositePrecision pPrecisionKey) {

    List<AbstractState> elements = pElementKey.getWrappedStates();
    List<Precision> precisions = pPrecisionKey.getWrappedPrecisions();

    List<Object> result = new ArrayList<>(elements.size());
    int i = 0;
    for (Pair<AbstractState, Precision> p : Pair.zipList(elements, precisions)) {
      result.add(wrappedReducers.get(i++).getHashCodeForState(p.getFirst(), p.getSecond()));
    }
    return result;
  }

  @Override
  protected Precision getVariableReducedPrecision0(CompositePrecision pPrecision, Block pContext) {
    List<Precision> precisions = pPrecision.getWrappedPrecisions();
    List<Precision> result = new ArrayList<>(precisions.size());

    int i = 0;
    for (Precision precision : precisions) {
      result.add(wrappedReducers.get(i++).getVariableReducedPrecision(precision, pContext));
    }

    return new CompositePrecision(result);
  }

  @Override
  protected CompositePrecision getVariableExpandedPrecision0(
      CompositePrecision pRootPrecision, Block pRootContext, CompositePrecision pReducedPrecision) {
    List<Precision> rootPrecisions = pRootPrecision.getWrappedPrecisions();
    List<Precision> reducedPrecisions = pReducedPrecision.getWrappedPrecisions();
    List<Precision> result = new ArrayList<>(rootPrecisions.size());

    int i = 0;
    for (Precision rootPrecision : rootPrecisions) {
      result.add(wrappedReducers.get(i).getVariableExpandedPrecision(rootPrecision, pRootContext, reducedPrecisions.get(i)));
      i++;
    }

    return new CompositePrecision(result);
  }

  @Override
  protected int measurePrecisionDifference0(
      CompositePrecision pPrecision, CompositePrecision pOtherPrecision) {
    List<Precision> precisions = pPrecision.getWrappedPrecisions();
    List<Precision> otherPrecisions = pOtherPrecision.getWrappedPrecisions();

    int i = 0;
    int sum = 0;
    for (Precision rootPrecision : precisions) {
      sum += wrappedReducers.get(i).measurePrecisionDifference(rootPrecision, otherPrecisions.get(i));
      i++;
    }

    return sum;
  }

  @Override
  protected CompositeState getVariableReducedStateForProofChecking0(
      CompositeState pExpandedState, Block pContext, CFANode pCallNode)
      throws InterruptedException {
    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (AbstractState expandedState : pExpandedState.getWrappedStates()) {
      result.add(wrappedReducers.get(i++).getVariableReducedStateForProofChecking(expandedState, pContext, pCallNode));
    }
    return new CompositeState(result);
  }

  @Override
  protected CompositeState getVariableExpandedStateForProofChecking0(
      CompositeState pRootState, Block pReducedContext, CompositeState pReducedState)
      throws InterruptedException {
    List<AbstractState> rootStates = pRootState.getWrappedStates();
    List<AbstractState> reducedStates = pReducedState.getWrappedStates();

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (Pair<AbstractState, AbstractState> p : Pair.zipList(rootStates, reducedStates)) {
      result.add(wrappedReducers.get(i++).getVariableExpandedStateForProofChecking(p.getFirst(), pReducedContext, p.getSecond()));
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

    List<AbstractState> results = new ArrayList<>();
    for (int i = 0; i < rootStates.size(); i++) {
      results.add(wrappedReducers.get(i).rebuildStateAfterFunctionCall(
              rootStates.get(i), entryStates.get(i), expandedStates.get(i), exitLocation));
    }
    return new CompositeState(results);
  }
}
