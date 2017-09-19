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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

class BDDReducer extends GenericReducer<BDDState, Precision> {

  private final PredicateManager predmgr;

  BDDReducer(PredicateManager pPredmgr) {
    predmgr = pPredmgr;
  }

  @Override
  protected BDDState getVariableReducedState0(
      BDDState pExpandedState, Block pBlock, CFANode pCallNode) {
    BDDState state = pExpandedState;

    final Set<String> trackedVars = predmgr.getTrackedVars().keySet();
    for (final String var : trackedVars) {
      if (!pBlock.getVariables().contains(var)) {
        int size = predmgr.getTrackedVars().get(var);
        Region[] toRemove = predmgr.createPredicateWithoutPrecisionCheck(var, size);
        state = state.forget(toRemove);
      }
    }

    return state;
  }

  @Override
  protected BDDState getVariableExpandedState0(
      BDDState pRootState, Block reducedContext, BDDState pReducedState) {
    BDDState state = pRootState;
    BDDState reducedState = pReducedState;

    // remove all vars, that are used in the block
    final Set<String> trackedVars = predmgr.getTrackedVars().keySet();
    for (final String var : trackedVars) {
      if (reducedContext.getVariables().contains(var)) {
        int size = predmgr.getTrackedVars().get(var);
        Region[] toRemove = predmgr.createPredicateWithoutPrecisionCheck(var, size);
        state = state.forget(toRemove);
      }
    }

    // TODO maybe we have to add some heuristics like in BAMPredicateReducer,
    // because we existentially quantify "block-inner" variables from formulas like "outer==inner".
    // This is sound, but leads to weaker formulas, maybe to weak for a useful analysis.
    // Or simpler solution: We could replace this Reducer with a NoOpReducer.

    // add information from block to state
    state = state.addConstraint(reducedState.getRegion());

    return state;
  }

  @Override
  protected Precision getVariableReducedPrecision0(Precision precision, Block context) {
    // TODO what to do?
    return precision;
  }

  @Override
  protected Precision getVariableExpandedPrecision0(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    // TODO what to do?
    return reducedPrecision;
  }

  @Override
  protected Object getHashCodeForState0(BDDState stateKey, Precision precisionKey) {
    return Pair.of(stateKey.getRegion(), precisionKey);
  }

  @Override
  protected BDDState rebuildStateAfterFunctionCall0(
      BDDState rootState,
      BDDState entryState,
      BDDState expandedState,
      FunctionExitNode exitLocation) {
    throw new UnsupportedOperationException("not implemented");
  }
}
