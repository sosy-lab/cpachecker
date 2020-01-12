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
package org.sosy_lab.cpachecker.cpa.bdd;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

/**
 * This reducer is nearly a {@link NoOpReducer}.
 *
 * <p>The key is the unwrapped BDD and analyzing recursive procedures (rebuild) is not supported.
 */
class BDDReducer extends GenericReducer<BDDState, Precision> {

  // TODO This implementation is simple, sound and guarantees progress.
  // Maybe we could add some heuristics like in BAMPredicateReducer and
  // existentially quantify "block-inner" variables from formulas like "outer==inner".
  // This is sound, but leads to weaker formulas, maybe to weak for a useful analysis.
  // This would require a refinement step after repeated CEX, similar to
  // BAMPredicateAbstractionRefinementStrategy.

  private final NamedRegionManager manager;
  private final BitvectorManager bvmgr;
  private final PredicateManager predmgr;
  private final int maxBitsize;
  private final boolean useBlockAbstraction;

  BDDReducer(
      NamedRegionManager pManager,
      BitvectorManager pBvmgr,
      PredicateManager pPredmgr,
      MachineModel pMachineModel,
      boolean pUseBlockAbstraction) {
    manager = pManager;
    bvmgr = pBvmgr;
    predmgr = pPredmgr;
    maxBitsize = PredicateManager.getMaxBitsize(pMachineModel);
    useBlockAbstraction = pUseBlockAbstraction;
  }

  @Override
  protected BDDState getVariableReducedState0(
      BDDState pExpandedState, Block pContext, CFANode pCallNode) {
    if (!useBlockAbstraction) {
      return pExpandedState;
    }

    // lets start the block abstraction with "x==input_x && global==input_global"
    // for all variables of the block.
    // This lets us compute a block abstraction relative to the input variables.
    Region inputEquality = getEqualityOfInputVariables(pContext);
    return new BDDState(manager, bvmgr, inputEquality);
    // TODO use methods from BDDState instead of our own code.
  }

  private Region getEqualityOfInputVariables(Block pContext) {
    Region state = manager.makeTrue();
    for (String variable : pContext.getVariables()) {
      // make sure to have at least one additional variable declared in the BDD,
      // before calling this method, otherwise the overhead is quite large!
      String inputVariable = predmgr.getAdditionalVariableWithIndex(variable, 1);
      Region inputVariableAssignment =
          bvmgr.makeLogicalEqual(
              predmgr.createPredicateWithoutPrecisionCheck(variable, maxBitsize),
              predmgr.createPredicateWithoutPrecisionCheck(inputVariable, maxBitsize));
      state = manager.makeAnd(state, inputVariableAssignment);
    }
    return state;
  }

  private Region[] getVariables(Block pContext) {
    List<Region> result = new ArrayList<>();
    for (String variable : pContext.getVariables()) {
      for (Region r : predmgr.createPredicateWithoutPrecisionCheck(variable, maxBitsize)) {
        result.add(r);
      }
    }
    return result.toArray(new Region[] {});
  }

  private Region[] getInputVariables(Block pContext) {
    List<Region> result = new ArrayList<>();
    for (String variable : pContext.getVariables()) {
      String inputVariable = predmgr.getAdditionalVariableWithIndex(variable, 1);
      for (Region r : predmgr.createPredicateWithoutPrecisionCheck(inputVariable, maxBitsize)) {
        result.add(r);
      }
    }
    return result.toArray(new Region[] {});
  }

  @Override
  protected BDDState getVariableExpandedState0(
      BDDState pRootState, Block pReducedContext, BDDState pReducedState) {
    if (!useBlockAbstraction) {
      return pReducedState;
    }

    Region inputEquality = getEqualityOfInputVariables(pReducedContext);
    Region[] variables = getVariables(pReducedContext);
    Region[] inputVariables = getInputVariables(pReducedContext);

    // prepare root state for block abstraction application
    Region state = pRootState.getRegion();
    state = manager.makeAnd(state, inputEquality);
    state = manager.makeExists(state, variables);

    // apply block abstraction
    Region blockAbstraction = pReducedState.getRegion();
    state = manager.makeAnd(state, blockAbstraction);

    // cleanup
    state = manager.makeExists(state, inputVariables);

    return new BDDState(manager, bvmgr, state);
  }

  @Override
  protected Precision getVariableReducedPrecision0(Precision precision, Block context) {
    return precision;
  }

  @Override
  protected Precision getVariableExpandedPrecision0(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
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
