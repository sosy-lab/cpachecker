/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.rcucpa.rcusearch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerReducer;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class RCUSearchReducer implements Reducer {
  private final PointerReducer pointerReducer;

  RCUSearchReducer(PointerReducer pPointerReducer) {
    pointerReducer = pPointerReducer;
  }


  @Override
  public AbstractState getVariableReducedState(
      AbstractState expandedState, Block context, CFANode callNode) throws InterruptedException {
    PointerState pointerState = (PointerState) pointerReducer.getVariableReducedState(expandedState,
                                                                                  context, callNode);
    return new RCUSearchState(new HashSet<>(), pointerState);
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState rootState, Block reducedContext, AbstractState reducedState)
      throws InterruptedException {
    Set<MemoryLocation> expandedSet = new HashSet<>(((RCUSearchState) rootState).getRcuPointers());
    expandedSet.addAll(((RCUSearchState) reducedState).getRcuPointers());
    PointerState rootPointerState = (PointerState) ((List<AbstractState>)((RCUSearchState) rootState)
        .getWrappedStates()).get(0);
    PointerState reducedPointerState = (PointerState) ((List<AbstractState>)((RCUSearchState) reducedState)
        .getWrappedStates()).get(0);
    PointerState expandedPointerState = (PointerState) pointerReducer.getVariableExpandedState
        (rootPointerState, reducedContext, reducedPointerState);
    return new RCUSearchState(expandedSet, expandedPointerState);
  }

  @Override
  public Precision getVariableReducedPrecision(
      Precision precision, Block context) {
    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return rootPrecision;
  }

  @Override
  public Object getHashCodeForState(
      AbstractState stateKey, Precision precisionKey) {
    return Pair.of(stateKey, precisionKey);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState,
      AbstractState entryState,
      AbstractState expandedState,
      FunctionExitNode exitLocation) {
    return rootState;
  }
}
