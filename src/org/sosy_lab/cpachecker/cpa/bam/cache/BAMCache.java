// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam.cache;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

/**
 * The BAMCache is the central storage for BAM. It is a map of a triple (state, precision, context)
 * towards a rached-set (or its result-states).
 *
 * <p>In most cases the following is satisfied: The state is equal to first state of the
 * reached-set. The precision is equal to the initial precision of the reached-set. The context is
 * the block where a block-entry equals the initial location of the reached-set.
 */
public interface BAMCache extends Statistics {

  /**
   * Store a reached-set in the cache. Returns an entry where the result-states for the reached-set
   * can be registered.
   *
   * @param item reached-set to be inserted into the cache
   */
  BAMCacheEntry put(AbstractState stateKey, Precision precisionKey, Block context, ReachedSet item);

  /**
   * Return the entry for the given key. The entry is NULL, if there is a cache miss. For a partial
   * cache hit we return an entry with the partly computed reached-set and NULL as exitStates.
   */
  BAMCacheEntry get(AbstractState stateKey, Precision precisionKey, Block context);

  /** Return the root-state of the last analyzed block, based on the last cache-access. */
  @Deprecated // reason: last block is not deterministic in parallel context
  ARGState getLastAnalyzedBlock();

  /** Check whether a cache entry exists for a given key. */
  boolean containsPreciseKey(AbstractState stateKey, Precision precisionKey, Block context);

  /** Return all cached reached-sets. Useful for statistics. */
  Collection<ReachedSet> getAllCachedReachedStates();

  /**
   * Some benchmarks are complicated and all intermediate cache entries can not be stored due to
   * large memory consumption, then there is a way to clear all caches and to restore ARG
   * completely.
   */
  void clear();

  class BAMCacheEntry {
    private final ReachedSet rs;
    private Set<AbstractState> exitStates;
    private ARGState rootOfBlock;

    protected BAMCacheEntry(ReachedSet pRs) {
      rs = Preconditions.checkNotNull(pRs);
    }

    public ReachedSet getReachedSet() {
      return rs;
    }

    public void setExitStates(Set<AbstractState> pExitStates) {
      exitStates = Preconditions.checkNotNull(pExitStates);
      check();
    }

    @Nullable
    public Set<AbstractState> getExitStates() {
      return exitStates;
    }

    private void check() {
      Preconditions.checkArgument(
          Iterables.all(exitStates, s -> !((ARGState) s).isDestroyed()),
          "do not use destroyed states: %s.",
          Collections2.transform(exitStates, this::id));
      Preconditions.checkArgument(
          rs.asCollection().containsAll(exitStates),
          "exit-states %s not available in reached-set with root %s and last state %s.",
          Collections2.transform(exitStates, this::id),
          id(rs.getFirstState()),
          id(rs.getLastState()));
    }

    public void setRootOfBlock(ARGState pRootOfBlock) {
      rootOfBlock = pRootOfBlock;
    }

    @Nullable
    public ARGState getRootOfBlock() {
      return rootOfBlock;
    }

    public void deleteInfo() {
      exitStates = null;
      rootOfBlock = null;
    }

    private String id(AbstractState s) {
      return "" + (s instanceof ARGState ? ((ARGState) s).getStateId() : s);
    }

    @Override
    public String toString() {
      return String.format(
          "CacheEntry {root=%s, exits=%s}",
          id(rs.getFirstState()),
          (exitStates == null ? null : Collections2.transform(exitStates, this::id)));
    }
  }
}
