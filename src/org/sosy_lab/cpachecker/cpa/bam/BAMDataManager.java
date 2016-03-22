/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/** This class contains all additional data-structures needed to run BAM.
 * If possible, we should clear some data sometimes to avoid memory-leaks. */
public class BAMDataManager {

  final LogManager logger;

  /** The bamCache is the main-data-structure of BAM.
   * It contains every reached-set of every sub-analysis. */
  final BAMCache bamCache;

  private final ReachedSetFactory reachedSetFactory;

  /** abstractStateToReachedSet contains the mapping of non-reduced initial states
   *  to the reached-sets, where the root-state is the corresponding reduced state. */
  final Map<AbstractState, ReachedSet> initialStateToReachedSet = new HashMap<>();

  /** expandedToReducedCache contains the mapping of an expanded state at a block-end towards
   * the corresponding reduced state, from which it was expanded. */
  final Map<AbstractState, AbstractState> expandedStateToReducedState = new HashMap<>();

  /** expandedToBlockCache contains the mapping of an expanded state at a block-end towards
   * the inner block of the corresponding reduced state, from which it was expanded. */
  private final Map<AbstractState, Block> expandedStateToBlock = new HashMap<>();

  /** expandedStateToExpandedPrecision contains the mapping an expanded state at a block-end towards
   * the corresponding expanded precision. */
  final Map<AbstractState, Precision> expandedStateToExpandedPrecision = new HashMap<>();

  public BAMDataManager(BAMCache pArgCache, ReachedSetFactory pReachedSetFactory, LogManager pLogger) {
    bamCache = pArgCache;
    reachedSetFactory = pReachedSetFactory;
    logger = pLogger;
  }

  void replaceStateInCaches(AbstractState oldState, AbstractState newState, boolean oldStateMustExist) {
    if (oldStateMustExist || expandedStateToReducedState.containsKey(oldState)) {
      final AbstractState reducedState = expandedStateToReducedState.remove(oldState);
      expandedStateToReducedState.put(newState, reducedState);
    }

    if (oldStateMustExist || expandedStateToBlock.containsKey(oldState)) {
      final Block innerBlock = expandedStateToBlock.remove(oldState);
      expandedStateToBlock.put(newState, innerBlock);
    }

    if (oldStateMustExist || expandedStateToExpandedPrecision.containsKey(oldState)) {
      final Precision expandedPrecision = expandedStateToExpandedPrecision.remove(oldState);
      expandedStateToExpandedPrecision.put(newState, expandedPrecision);
    }
  }

  /** unused? */
  void clearCaches() {
    bamCache.clear();
    initialStateToReachedSet.clear();
  }

  ReachedSet createInitialReachedSet(AbstractState initialState, Precision initialPredicatePrecision) {
    ReachedSet reached = reachedSetFactory.create();
    reached.add(initialState, initialPredicatePrecision);
    return reached;
  }

  /** Register an expanded state in our data-manager,
   * such that we know later, which state in which block was expanded to the state. */
  void registerExpandedState(AbstractState expandedState, Precision expandedPrecision,
      AbstractState reducedState, Block innerBlock) {
    expandedStateToReducedState.put(expandedState, reducedState);
    expandedStateToBlock.put(expandedState, innerBlock);
    expandedStateToExpandedPrecision.put(expandedState, expandedPrecision);
  }

  /** This method checks, if the current state is at a node,
   * where several block-exits are available and one of them was already left.
   * The state has to be an block-end-state.
   * It can be a expanded or reduced (or even reduced expanded) state,
   * because this depends on the nesting of blocks,
   * i.e. if there are several overlapping block-end-nodes
   * (e.g. nested loops or program calls 'exit()' inside a function). */
  boolean alreadyReturnedFromSameBlock(AbstractState state, Block block) {
    while (expandedStateToReducedState.containsKey(state)) {
      if (expandedStateToBlock.containsKey(state) && block == expandedStateToBlock.get(state)) {
        return true;
      }
      state = expandedStateToReducedState.get(state);
    }
    return false;
  }

  ARGState getMostInnerState(ARGState state) {
    while (expandedStateToReducedState.containsKey(state)) {
      state = (ARGState) expandedStateToReducedState.get(state);
    }
    return state;
  }
}
