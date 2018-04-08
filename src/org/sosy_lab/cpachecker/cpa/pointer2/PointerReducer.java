/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.pointer2;

import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PointerReducer implements Reducer {
  final Timer reduceTime = new Timer();
  final Timer expandTime = new Timer();

  @Override
  public AbstractState getVariableReducedState(
      AbstractState expandedState, Block context, CFANode callNode) throws InterruptedException {
    reduceTime.start();
    PointerState clonedState = PointerState.copyOf((PointerState) expandedState);
    for (MemoryLocation ptr : clonedState.getTrackedMemoryLocations()) {
      if (!(PointerState.isFictionalPointer(ptr) || ptr.isGlobal()) &&
          !context.getMemoryLocations().contains(ptr)) {
        clonedState.forget(ptr);
      }
    }
    // ("RRR: " + callNode.describeFileLocation());
    reduceTime.stop();
    return clonedState;
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState rootState, Block reducedContext, AbstractState reducedState)
      throws InterruptedException {
    expandTime.start();
    PointerState clonedState = PointerState.copyOf((PointerState) reducedState);
    for (MemoryLocation ptr : ((PointerState) rootState).getTrackedMemoryLocations()) {
      if (!reducedContext.getMemoryLocations().contains(ptr)) {
        clonedState = clonedState.addPointsToInformation(ptr, ((PointerState) rootState)
            .getPointsToMap().get(ptr));
      }
    }
    // ("EEE: " + reducedContext.getCallNode().describeFileLocation());
    expandTime.stop();
    return clonedState;
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
