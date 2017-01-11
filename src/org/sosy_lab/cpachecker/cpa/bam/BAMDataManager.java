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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
/**
 * Data structures required for BAM.
 *
 * <p>TODO: clear cache to avoid memory-leaks.
 * */
public class BAMDataManager {

  final LogManager logger;

  /**
   * Main data structure.
   * Contains every {@link ReachedSet} of every recursive
   * {@link org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm}
   * invocation.
   * */
  final BAMCache bamCache;

  final ReachedSetFactory reachedSetFactory;

  /**
   * Mapping of non-reduced initial states
   * to {@link ReachedSet}.
   **/
  private final Table<AbstractState, AbstractState, ReachedSet> initialStateToReachedSet = HashBasedTable.create();

  /**
   * Mapping from expanded states at the end of the block to corresponding
   * reduced states, from which the key state was originally expanded.
   * */
  private final Map<AbstractState, AbstractState> expandedStateToReducedState = new LinkedHashMap<>();

  /**
   * Mapping from expanded states at a block-end to
   * inner blocks of the corresponding reduced state,
   * from which the key was originally expanded.
   **/
  private final Map<AbstractState, Block> expandedStateToBlock = new LinkedHashMap<>();

  /**
   * Mapping from expanded states at a block-end to
   * corresponding expanded precisions.
   **/
  final Map<AbstractState, Precision> expandedStateToExpandedPrecision = new LinkedHashMap<>();

  public BAMDataManager(
      BAMCache pArgCache,
      ReachedSetFactory pReachedSetFactory,
      LogManager pLogger) {
    bamCache = pArgCache;
    reachedSetFactory = pReachedSetFactory;
    logger = pLogger;
  }

  /**
   * Associate the value previously associated with {@code oldState} with
   * {@code newState}.
   *
   * @param oldStateMustExist If set, assumes that {@code oldState} is in the
   *                          cache, otherwise, fails silently if it isn't.
   */
  void replaceStateInCaches(
      AbstractState oldState,
      AbstractState newState,
      boolean oldStateMustExist) {

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

  @Deprecated /* unused */
  void clearCaches() {
    bamCache.clear();
    initialStateToReachedSet.clear();
  }

  /**
   * Create a new reached-set with the given state as root and register it in the cache.
   **/
  ReachedSet createAndRegisterNewReachedSet(
      AbstractState initialState, Precision initialPrecision, Block context) {
    final ReachedSet reached = reachedSetFactory.create();
    reached.add(initialState, initialPrecision);
    bamCache.put(initialState, initialPrecision, context, reached);
    return reached;
  }

  /**
   * Register an expanded state in our data-manager,
   * such that we know later, which state in which block was expanded to the state.
   * */
  void registerExpandedState(AbstractState expandedState, Precision expandedPrecision,
      AbstractState reducedState, Block innerBlock) {
    expandedStateToReducedState.put(expandedState, reducedState);
    expandedStateToBlock.put(expandedState, innerBlock);
    expandedStateToExpandedPrecision.put(expandedState, expandedPrecision);
  }

  /**
   * @param state Has to be a block-end state.
   * It can be expanded or reduced (or even reduced expanded),
   * because this depends on the nesting of blocks,
   * i.e. if there are several overlapping block-end-nodes
   * (e.g. nested loops or program calls 'exit()' inside a function).
   *
   * @return Whether the current state is at a node,
   * where several block-exits are available and one of them was already left.
   **/
  boolean alreadyReturnedFromSameBlock(AbstractState state, Block block) {
    while (expandedStateToReducedState.containsKey(state)) {
      if (expandedStateToBlock.containsKey(state) && block == expandedStateToBlock.get(state)) {
        return true;
      }
      state = expandedStateToReducedState.get(state);
    }
    return false;
  }

  ARGState getInnermostState(ARGState state) {
    while (expandedStateToReducedState.containsKey(state)) {
      state = (ARGState) expandedStateToReducedState.get(state);
    }
    return state;
  }

  /**
   * Get a list of states {@code [s1,s2,s3...]},
   * such that {@code expand(s1)=s2}, {@code expand(s2)=s3},...
   * The state {@code s1} is the most inner state.
   * Returns an empty list, if the state is not an exit state.
   */
  @Deprecated // works correct, maybe unused
  List<AbstractState> getExpandedStatesList(AbstractState state) {
    List<AbstractState> lst = new ArrayList<>();
    AbstractState tmp = state;
    while (expandedStateToReducedState.containsKey(tmp)) {
      tmp = expandedStateToReducedState.get(tmp);
      lst.add(tmp);
    }
    return Lists.reverse(lst);
  }

  /**
   * Get a list of states {@code [s1,s2,s3...]},
   * such that {@code expand(s0)=s1}, {@code expand(s1)=s2},...
   * The state {@code s0} is the most inner non-expanded state and is not included in the list.
   * This method returns an empty list, if the state is not an exit state.
   */
  List<AbstractState> getExpandedStateList(AbstractState state) {
    List<AbstractState> lst = new ArrayList<>();
    AbstractState tmp = state;
    while (expandedStateToReducedState.containsKey(tmp)) {
      lst.add(tmp);
      tmp = expandedStateToReducedState.get(tmp);
    }
    return Lists.reverse(lst);
  }

  /** Register a mapping from the non-reduced initial state and the non-expanded block-exit states
   * to a reached-set that was used between them. */
  void registerInitialState(AbstractState initialState, AbstractState exitState, ReachedSet reachedSet) {
//    Collection<ReachedSet> oldReachedSets = initialStateToReachedSet.get(initialState);
//    if (!oldReachedSets.isEmpty() && oldReachedSets.contains(reachedSet)) {
//      // TODO This might be a hint for a memory leak, i.e., the old reachedset
//      // is no longer accessible through BAMDataManager, but registered in BAM-cache.
//      // This happens, when the reducer changes, e.g., BAMPredicateRefiner.refineRelevantPredicates.
//      logger.logf(
//          Level.ALL,
//          "New abstract state %s overrides old reachedset %s with new reachedset %s.",
//          initialState,
//          Collections2.transform(oldReachedSets, ReachedSet::getFirstState),
//          reachedSet.getFirstState());
//    }
    assert !initialStateToReachedSet.contains(initialState, exitState) :
      "mapping already exists: " + initialState + " -> " + exitState;
    initialStateToReachedSet.put(initialState, exitState, reachedSet);
  }

  void registerInitialState(AbstractState initialState, Collection<AbstractState> exitStates, ReachedSet reachedSet) {
    for (AbstractState exitState : exitStates) {
      registerInitialState(initialState, exitState, reachedSet);
    }
  }

  @Deprecated
  ReachedSet getReachedSetForInitialState(AbstractState initialState) {
    assert initialStateToReachedSet.containsRow(initialState) : "no initial state for a block: " + initialState;
    return checkNotNull(initialStateToReachedSet.row(initialState).values().iterator().next());
  }

  ReachedSet getReachedSetForInitialState(AbstractState initialState, AbstractState exitState) {
    assert initialStateToReachedSet.contains(initialState, exitState) : "no block matching states: " + initialState + " -> " + exitState;
    ReachedSet reached = checkNotNull(initialStateToReachedSet.get(initialState, exitState));
    assert reached.contains(exitState) : "reachedset should contain exit state for block: " + exitState;
    return reached;
  }

  boolean hasInitialState(AbstractState state) {
    return initialStateToReachedSet.containsRow(state);
  }

  AbstractState getReducedStateForExpandedState(AbstractState state) {
    assert expandedStateToReducedState.containsKey(state) : "no match for state: " + state;
    return expandedStateToReducedState.get(state);
  }

  Block getInnerBlockForExpandedState(AbstractState state) {
    assert expandedStateToReducedState.containsKey(state);
    return expandedStateToBlock.get(state);
  }

  Precision getExpandedPrecisionForExpandedState(AbstractState state) {
    assert expandedStateToReducedState.containsKey(state);
    return expandedStateToExpandedPrecision.get(state);
  }

  boolean hasExpandedState(AbstractState state) {
    return expandedStateToReducedState.containsKey(state);
  }

  static int getId(AbstractState state) {
    return ((ARGState) state).getStateId();
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder("BAM DATA MANAGER\n");

    str.append("initial state to (first state of) reached set:\n");
    for (Cell<AbstractState, AbstractState, ReachedSet> entry : initialStateToReachedSet.cellSet()) {
      str.append(
          String.format(
              "    (%s, %s) -> %s%n", getId(entry.getRowKey()), getId(entry.getColumnKey()), getId((entry.getValue()).getFirstState())));
    }

    str.append("expanded state to reduced state:\n");
    for (Entry<AbstractState, AbstractState> entry : expandedStateToReducedState.entrySet()) {
      str.append(String.format("    %s -> %s%n", getId(entry.getKey()), getId(entry.getValue())));
    }

    return str.toString();
  }
}
