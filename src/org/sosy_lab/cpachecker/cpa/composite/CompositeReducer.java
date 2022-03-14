// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;
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
    ImmutableList.Builder<AbstractState> result =
        ImmutableList.builderWithExpectedSize(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers.get(i).getVariableReducedState(states.get(i), pContext, pLocation));
    }
    return new CompositeState(result.build());
  }

  @Override
  protected CompositeState getVariableExpandedState0(
      CompositeState pRootState, Block pReducedContext, CompositeState pReducedState)
      throws InterruptedException {

    List<AbstractState> rootStates = pRootState.getWrappedStates();
    List<AbstractState> reducedStates = pReducedState.getWrappedStates();

    ImmutableList.Builder<AbstractState> result =
        ImmutableList.builderWithExpectedSize(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      AbstractState nestedState =
          wrappedReducers
              .get(i)
              .getVariableExpandedState(rootStates.get(i), pReducedContext, reducedStates.get(i));
      if (nestedState == null) {
        return null;
      }
      result.add(nestedState);
    }
    return new CompositeState(result.build());
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

    ImmutableList.Builder<Precision> result =
        ImmutableList.builderWithExpectedSize(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(wrappedReducers.get(i).getVariableReducedPrecision(precisions.get(i), pContext));
    }

    return new CompositePrecision(result.build());
  }

  @Override
  protected CompositePrecision getVariableExpandedPrecision0(
      CompositePrecision pRootPrecision, Block pRootContext, CompositePrecision pReducedPrecision) {
    List<Precision> rootPrecisions = pRootPrecision.getWrappedPrecisions();
    List<Precision> reducedPrecisions = pReducedPrecision.getWrappedPrecisions();

    ImmutableList.Builder<Precision> result =
        ImmutableList.builderWithExpectedSize(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers
              .get(i)
              .getVariableExpandedPrecision(
                  rootPrecisions.get(i), pRootContext, reducedPrecisions.get(i)));
    }
    return new CompositePrecision(result.build());
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

    ImmutableList.Builder<AbstractState> result =
        ImmutableList.builderWithExpectedSize(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers
              .get(i)
              .getVariableReducedStateForProofChecking(expandedStates.get(i), pContext, pCallNode));
    }
    return new CompositeState(result.build());
  }

  @Override
  protected CompositeState getVariableExpandedStateForProofChecking0(
      CompositeState pRootState, Block pReducedContext, CompositeState pReducedState)
      throws InterruptedException {
    List<AbstractState> rootStates = pRootState.getWrappedStates();
    List<AbstractState> reducedStates = pReducedState.getWrappedStates();

    ImmutableList.Builder<AbstractState> result =
        ImmutableList.builderWithExpectedSize(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      AbstractState nestedState =
          wrappedReducers
              .get(i)
              .getVariableExpandedStateForProofChecking(
                  rootStates.get(i), pReducedContext, reducedStates.get(i));
      if (nestedState == null) {
        return null;
      }
      result.add(nestedState);
    }
    return new CompositeState(result.build());
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

    ImmutableList.Builder<AbstractState> result =
        ImmutableList.builderWithExpectedSize(wrappedReducers.size());
    for (int i = 0; i < wrappedReducers.size(); i++) {
      result.add(
          wrappedReducers
              .get(i)
              .rebuildStateAfterFunctionCall(
                  rootStates.get(i), entryStates.get(i), expandedStates.get(i), exitLocation));
    }
    return new CompositeState(result.build());
  }

  @Override
  protected boolean canBeUsedInCache0(CompositeState pState) {
    for (int i = 0; i < wrappedReducers.size(); i++) {
      if (!wrappedReducers.get(i).canBeUsedInCache(pState.get(i))) {
        return false;
      }
    }
    return true;
  }
}
