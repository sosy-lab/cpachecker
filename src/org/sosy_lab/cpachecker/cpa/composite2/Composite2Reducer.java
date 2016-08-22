/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite2;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.ArrayList;
import java.util.List;

class Composite2Reducer implements Reducer {

  private final List<Reducer> wrappedReducers;

  Composite2Reducer(List<Reducer> pWrappedReducers) {
    wrappedReducers = pWrappedReducers;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext,
      CFANode pLocation) {

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (AbstractState expandedState : ((Composite2State)pExpandedState).getWrappedStates()) {
      result.add(wrappedReducers.get(i++).getVariableReducedState(expandedState, pContext, pLocation));
    }
    return new Composite2State(result);
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {

    List<? extends AbstractState> rootStates = ((Composite2State)pRootState).getWrappedStates();
    List<? extends AbstractState> reducedStates = ((Composite2State)pReducedState).getWrappedStates();

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (Pair<? extends AbstractState, ? extends AbstractState> p : Pair.zipList(rootStates, reducedStates)) {
      result.add(wrappedReducers.get(i++).getVariableExpandedState(p.getFirst(), pReducedContext, p.getSecond()));
    }
    return new Composite2State(result);
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {

    List<? extends AbstractState> elements = ((Composite2State)pElementKey).getWrappedStates();
    List<Precision> precisions = ((Composite2Precision) pPrecisionKey).getWrappedPrecisions();

    List<Object> result = new ArrayList<>(elements.size());
    int i = 0;
    for (Pair<? extends AbstractState, ? extends Precision> p : Pair.zipList(elements, precisions)) {
      result.add(wrappedReducers.get(i++).getHashCodeForState(p.getFirst(), p.getSecond()));
    }
    return result;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision,
      Block pContext) {
    List<Precision> precisions = ((Composite2Precision) pPrecision).getWrappedPrecisions();
    List<Precision> result = new ArrayList<>(precisions.size());

    int i = 0;
    for (Precision precision : precisions) {
      result.add(wrappedReducers.get(i++).getVariableReducedPrecision(precision, pContext));
    }

    return new Composite2Precision(result);
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext, Precision pReducedPrecision) {
    List<Precision> rootPrecisions = ((Composite2Precision) pRootPrecision).getWrappedPrecisions();
    List<Precision> reducedPrecisions =
        ((Composite2Precision) pReducedPrecision).getWrappedPrecisions();
    List<Precision> result = new ArrayList<>(rootPrecisions.size());

    int i = 0;
    for (Precision rootPrecision : rootPrecisions) {
      result.add(wrappedReducers.get(i).getVariableExpandedPrecision(rootPrecision, pRootContext, reducedPrecisions.get(i)));
      i++;
    }

    return new Composite2Precision(result);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    List<Precision> precisions = ((Composite2Precision) pPrecision).getWrappedPrecisions();
    List<Precision> otherPrecisions = ((Composite2Precision) pOtherPrecision).getWrappedPrecisions();

    int i = 0;
    int sum = 0;
    for (Precision rootPrecision : precisions) {
      sum += wrappedReducers.get(i).measurePrecisionDifference(rootPrecision, otherPrecisions.get(i));
      i++;
    }

    return sum;
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (AbstractState expandedState : ((Composite2State)pExpandedState).getWrappedStates()) {
      result.add(wrappedReducers.get(i++).getVariableReducedStateForProofChecking(expandedState, pContext, pCallNode));
    }
    return new Composite2State(result);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    List<? extends AbstractState> rootStates = ((Composite2State)pRootState).getWrappedStates();
    List<? extends AbstractState> reducedStates = ((Composite2State)pReducedState).getWrappedStates();

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (Pair<? extends AbstractState, ? extends AbstractState> p : Pair.zipList(rootStates, reducedStates)) {
      result.add(wrappedReducers.get(i++).getVariableExpandedStateForProofChecking(p.getFirst(), pReducedContext, p.getSecond()));
    }
    return new Composite2State(result);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState pRootState, AbstractState pEntryState,
      AbstractState pExpandedState, FunctionExitNode exitLocation) {
    List<? extends AbstractState> rootStates = ((Composite2State)pRootState).getWrappedStates();
    List<? extends AbstractState> entryStates = ((Composite2State)pEntryState).getWrappedStates();
    List<? extends AbstractState> expandedStates = ((Composite2State)pExpandedState).getWrappedStates();

    List<AbstractState> results = new ArrayList<>();
    for (int i = 0; i < rootStates.size(); i++) {
      results.add(wrappedReducers.get(i).rebuildStateAfterFunctionCall(
              rootStates.get(i), entryStates.get(i), expandedStates.get(i), exitLocation));
    }
    return new Composite2State(results);
  }
}
