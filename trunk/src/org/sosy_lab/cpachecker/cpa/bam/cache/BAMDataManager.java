// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam.cache;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache.BAMCacheEntry;

public interface BAMDataManager {

  /**
   * Associate the value previously associated with {@code oldState} with {@code newState}.
   *
   * @param oldStateMustExist If set, assumes that {@code oldState} is in the cache, otherwise,
   *     fails silently if it isn't.
   */
  void replaceStateInCaches(
      AbstractState oldState, AbstractState newState, boolean oldStateMustExist);

  /** Create a new reached-set with the given state as root and register it in the cache. */
  BAMCacheEntry createAndRegisterNewReachedSet(
      AbstractState initialState, Precision initialPrecision, Block context);

  ReachedSetFactory getReachedSetFactory();

  /**
   * Register an expanded state in our data-manager, such that we know later, which state in which
   * block was expanded to the state.
   */
  void registerExpandedState(
      AbstractState expandedState,
      Precision expandedPrecision,
      AbstractState reducedState,
      Block innerBlock);

  /**
   * Returns whether the current state is at a node, where several block-exits are available and one
   * of them was already left.
   *
   * @param state Has to be a block-end state. It can be expanded or reduced (or even reduced
   *     expanded), because this depends on the nesting of blocks, i.e. if there are several
   *     overlapping block-end-nodes (e.g. nested loops or program calls 'exit()' inside a
   *     function).
   */
  boolean alreadyReturnedFromSameBlock(AbstractState state, Block block);

  /**
   * Returns the non-expanded abstract state for a expanded abstract state, recursively if needed,
   * or the given state itself, if the state was never an expanded result.
   */
  AbstractState getInnermostState(AbstractState state);

  /**
   * Get a list of states {@code [s2,s3...]}, such that {@code expand(s1)=s2}, {@code
   * expand(s2)=s3},... The state {@code s1} is the state given as argument and is not contained in
   * the list.
   */
  List<AbstractState> getExpandedStatesList(AbstractState state);

  /**
   * Add a mapping of a non-reduced abstract state and non-expanded exit state to a reached-set
   * whose first state is the matching reduced abstract state. The exit state should be contained in
   * the reached-set.
   */
  void registerInitialState(AbstractState state, AbstractState exitState, ReachedSet reachedSet);

  /**
   * Receive the reached-set for a non-reduced initial state with a non-expanded exit-state. We
   * expect that the given abstract state has a matching reached-set.
   */
  ReachedSet getReachedSetForInitialState(AbstractState state, AbstractState exitState);

  /** Check whether the given abstract state is the non-reduced initial state of a reached-set. */
  boolean hasInitialState(AbstractState state);

  /** Returns all non-reduced intiial states for a reduced initial state. */
  ImmutableSet<AbstractState> getNonReducedInitialStates(AbstractState reducedState);

  /**
   * Returns the non-expanded abstract state for an expanded abstract state. We expect that the
   * given abstract state was registered as expanded state.
   */
  AbstractState getReducedStateForExpandedState(AbstractState state);

  /** Returns the block from where the expanded state is an exit-state. */
  Block getInnerBlockForExpandedState(AbstractState state);

  /** Check whether any abstract state was expanded to the given abstract state. */
  boolean hasExpandedState(AbstractState state);

  BAMCache getCache();

  /**
   * Some benchmarks are complicated and all intermediate cache entries can not be stored due to
   * large memory consumption, then there is a way to clear all caches and to restore ARG
   * completely.
   */
  void clear();

  /** return a matching precision for the given expanded state, or Null if state is not found. */
  @Nullable Precision getExpandedPrecisionForState(AbstractState pState);

  /**
   * The results from cache will never be used for corresponding block entry
   *
   * @param node The block entry
   * @return success
   */
  boolean addUncachedBlockEntry(CFANode node);

  /**
   * If the corresponding block is 'uncached' the recursive analysis will not start
   *
   * @param node Block entry to check
   * @return true if the block entry was added as 'uncached'
   */
  boolean isUncachedBlockEntry(CFANode node);
}
