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

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

import java.util.HashSet;
import java.util.Set;

public class BDDReducer implements Reducer {

  private final PredicateManager predmgr;

  BDDReducer(PredicateManager pPredmgr) {
    predmgr = pPredmgr;
  }

  private Set<String> getVarsOfBlock(Block pBlock) {
    Set<String> vars = new HashSet<>();
    for (ReferencedVariable referencedVar : pBlock.getReferencedVariables()) {
      vars.add(referencedVar.getName());
    }
    return vars;
  }

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedState, Block pBlock, CFANode pCallNode) {
    BDDState state = (BDDState)pExpandedState;

    final Set<String> trackedVars = predmgr.getTrackedVars().keySet();
    final Set<String> blockVars = getVarsOfBlock(pBlock);
    for (final String var : trackedVars) {
      if (!blockVars.contains(var)) {
        int size = predmgr.getTrackedVars().get(var);
        Region[] toRemove = predmgr.createPredicateWithoutPrecisionCheck(var, size);
        state = state.forget(toRemove);
      }
    }

    return state;
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootState, Block reducedContext, AbstractState pReducedState) {
    BDDState state = (BDDState)pRootState;
    BDDState reducedState = (BDDState)pReducedState;

    // remove all vars, that are used in the block
    final Set<String> trackedVars = predmgr.getTrackedVars().keySet();
    final Set<String> blockVars = getVarsOfBlock(reducedContext);
    for (final String var : trackedVars) {
      if (blockVars.contains(var)) {
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
  public Precision getVariableReducedPrecision(Precision precision, Block context) {
    // TODO what to do?
    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    // TODO what to do?
    return reducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState stateKey, Precision precisionKey) {
    return Pair.of(((BDDState)stateKey).getRegion(), precisionKey);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState rootState, AbstractState entryState,
      AbstractState expandedState, FunctionExitNode exitLocation) {
    throw new UnsupportedOperationException("not implemented");
  }
}
