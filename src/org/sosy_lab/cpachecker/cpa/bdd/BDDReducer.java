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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
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
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;
  private final int maxBitsize;
  private final boolean useBlockAbstraction;

  private static final int INPUT_VARIABLE_INDEX = 0;
  private static final int APPLY_VARIABLE_INDEX = 1;

  BDDReducer(
      NamedRegionManager pManager,
      BitvectorManager pBvmgr,
      PredicateManager pPredmgr,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger,
      boolean pUseBlockAbstraction) {
    manager = pManager;
    bvmgr = pBvmgr;
    predmgr = pPredmgr;
    maxBitsize = PredicateManager.getMaxBitsize(pMachineModel);
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    useBlockAbstraction = pUseBlockAbstraction;

    if (useBlockAbstraction) {
      Preconditions.checkState(
          predmgr.getNumberOfAdditionalVariables() >= 2,
          "Make sure to have at least two additional variable declared in the BDD, "
              + "before calling this method, otherwise the overhead is quite large! "
              + "The option for this is 'cpa.bdd.initAdditionalVariables=2'.");
    }
  }

  @Override
  protected BDDState getVariableReducedState0(
      BDDState pExpandedState, Block pContext, CFANode pCallNode) throws InterruptedException {
    if (!useBlockAbstraction) {
      return pExpandedState;
    }

    // lets start the block abstraction with "x==input_x && global==input_global"
    // for all variables of the block.
    // This lets us compute a block abstraction relative to the input variables.
    BDDState state = new BDDState(manager, bvmgr, manager.makeTrue());
    for (String variable : filterBlockVariables(pContext.getVariables())) {
      shutdownNotifier.shutdownIfNecessary();
      String inputVar = predmgr.getAdditionalVariableWithIndex(variable, INPUT_VARIABLE_INDEX);
      Region[] oldVariable = predmgr.createPredicateWithoutPrecisionCheck(variable, maxBitsize);
      Region[] newVariable = predmgr.createPredicateWithoutPrecisionCheck(inputVar, maxBitsize);
      state = state.addAssignment(oldVariable, newVariable);
    }
    return state;
  }

  @Override
  protected BDDState getVariableExpandedState0(
      BDDState pRootState, Block pReducedContext, BDDState pReducedState)
      throws InterruptedException {
    if (!useBlockAbstraction) {
      return pReducedState;
    }

    Collection<String> blockVariables = filterBlockVariables(pReducedContext.getVariables());

    // replace block variables in initial state with apply variables
    BDDState initState = pRootState;
    for (String variable : blockVariables) {
      shutdownNotifier.shutdownIfNecessary();
      String applyVariable = predmgr.getAdditionalVariableWithIndex(variable, APPLY_VARIABLE_INDEX);
      initState = replace(initState, variable, applyVariable);
    }

    // remove variables that appear in the initial state and are out of scope in the exit location.
    BDDState cleanedInitState = initState;
    for (String outOfScope : Iterables
        .filter(pReducedContext.getOutOfScopeVariables(), v -> !blockVariables.contains(v))) {
      // this case should only rarely appear, i.e.,
      // for function parameters that are unused within the block.
      shutdownNotifier.shutdownIfNecessary();
      Region[] toRemove = predmgr.createPredicateWithoutPrecisionCheck(outOfScope, maxBitsize);
      cleanedInitState = cleanedInitState.forget(toRemove);
    }

    // replace input variables in exit state with apply variables
    BDDState applyState = pReducedState;
    for (String variable : blockVariables) {
      shutdownNotifier.shutdownIfNecessary();
      String inputVariable = predmgr.getAdditionalVariableWithIndex(variable, INPUT_VARIABLE_INDEX);
      String applyVariable = predmgr.getAdditionalVariableWithIndex(variable, APPLY_VARIABLE_INDEX);
      applyState = replace(applyState, inputVariable, applyVariable);
    }

    // apply block abstraction
    BDDState appliedBlock = cleanedInitState.addConstraint(applyState.getRegion());

    // cleanup apply variables
    BDDState expandedState = appliedBlock;
    for (String variable : blockVariables) {
      shutdownNotifier.shutdownIfNecessary();
      String applyVariable = predmgr.getAdditionalVariableWithIndex(variable, APPLY_VARIABLE_INDEX);
      Region[] toForget = predmgr.createPredicateWithoutPrecisionCheck(applyVariable, maxBitsize);
      expandedState = expandedState.forget(toForget);
    }

    if (expandedState.getRegion().isFalse()) {
      // combination of initial state and block abstraction is not satisfied.
      return null;
    }

    return expandedState;
  }

  private Collection<String> filterBlockVariables(Iterable<String> variables) {
    List<String> result = new ArrayList<>();
    for (String variable : variables) {
      if (manager.getPredicates().contains(variable + "@0")) {
        result.add(variable);
      } else {
        // if variable is unknown here, it should never be used in the transfer relation.
        // TODO declare as interleaved variables?
        logger.logf(Level.ALL, "variable '%s' was not declared in the BDD, ignoring it", variable);
      }
    }
    return result;
  }

  private BDDState replace(BDDState state, String oldName, String newName) {
    Region[] oldVariable = predmgr.createPredicateWithoutPrecisionCheck(oldName, maxBitsize);
    Region[] newVariable = predmgr.createPredicateWithoutPrecisionCheck(newName, maxBitsize);
    return state.addAssignment(oldVariable, newVariable).forget(oldVariable);
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
