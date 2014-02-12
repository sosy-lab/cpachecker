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

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class CompositeReducer implements Reducer {

  private final List<Reducer> wrappedReducers;

  public CompositeReducer(List<Reducer> pWrappedReducers) {
    wrappedReducers = pWrappedReducers;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext,
      CFANode pLocation) {

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (AbstractState expandedState : ((CompositeState)pExpandedState).getWrappedStates()) {
      result.add(wrappedReducers.get(i++).getVariableReducedState(expandedState, pContext, pLocation));
    }
    return new CompositeState(result);
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {

    List<AbstractState> rootStates = ((CompositeState)pRootState).getWrappedStates();
    List<AbstractState> reducedStates = ((CompositeState)pReducedState).getWrappedStates();

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (Pair<AbstractState, AbstractState> p : Pair.zipList(rootStates, reducedStates)) {
      result.add(wrappedReducers.get(i++).getVariableExpandedState(p.getFirst(), pReducedContext, p.getSecond()));
    }
    return new CompositeState(result);
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {

    List<AbstractState> elements = ((CompositeState)pElementKey).getWrappedStates();
    List<Precision> precisions = ((CompositePrecision)pPrecisionKey).getPrecisions();

    List<Object> result = new ArrayList<>(elements.size());
    int i = 0;
    for (Pair<AbstractState, Precision> p : Pair.zipList(elements, precisions)) {
      result.add(wrappedReducers.get(i++).getHashCodeForState(p.getFirst(), p.getSecond()));
    }
    return result;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision,
      Block pContext) {
    List<Precision> precisions = ((CompositePrecision)pPrecision).getPrecisions();
    List<Precision> result = new ArrayList<>(precisions.size());

    int i = 0;
    for (Precision precision : precisions) {
      result.add(wrappedReducers.get(i++).getVariableReducedPrecision(precision, pContext));
    }

    return new CompositePrecision(result);
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext, Precision pReducedPrecision) {
    List<Precision> rootPrecisions = ((CompositePrecision)pRootPrecision).getPrecisions();
    List<Precision> reducedPrecisions = ((CompositePrecision)pReducedPrecision).getPrecisions();
    List<Precision> result = new ArrayList<>(rootPrecisions.size());

    int i = 0;
    for (Precision rootPrecision : rootPrecisions) {
      result.add(wrappedReducers.get(i).getVariableExpandedPrecision(rootPrecision, pRootContext, reducedPrecisions.get(i)));
      i++;
    }

    return new CompositePrecision(result);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    List<Precision> precisions = ((CompositePrecision)pPrecision).getPrecisions();
    List<Precision> otherPrecisions = ((CompositePrecision)pOtherPrecision).getPrecisions();

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
    for (AbstractState expandedState : ((CompositeState)pExpandedState).getWrappedStates()) {
      result.add(wrappedReducers.get(i++).getVariableReducedStateForProofChecking(expandedState, pContext, pCallNode));
    }
    return new CompositeState(result);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    List<AbstractState> rootStates = ((CompositeState)pRootState).getWrappedStates();
    List<AbstractState> reducedStates = ((CompositeState)pReducedState).getWrappedStates();

    List<AbstractState> result = new ArrayList<>();
    int i = 0;
    for (Pair<AbstractState, AbstractState> p : Pair.zipList(rootStates, reducedStates)) {
      result.add(wrappedReducers.get(i++).getVariableExpandedStateForProofChecking(p.getFirst(), pReducedContext, p.getSecond()));
    }
    return new CompositeState(result);
  }
}
