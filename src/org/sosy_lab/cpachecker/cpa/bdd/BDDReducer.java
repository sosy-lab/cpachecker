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

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;

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

  BDDReducer() {}

  @Override
  protected BDDState getVariableReducedState0(
      BDDState pExpandedState, Block pContext, CFANode pCallNode) {
    return pExpandedState;
  }

  @Override
  protected BDDState getVariableExpandedState0(
      BDDState pRootState, Block pReducedContext, BDDState pReducedState) {
    return pReducedState;
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
