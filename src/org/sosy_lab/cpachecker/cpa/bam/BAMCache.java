/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.Collection;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;

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
   * Store a reached-set in the cache. Does not yet register the result-states for the reached-set.
   *
   * @param item reached-set to be inserted into the cache
   */
  void put(AbstractState stateKey, Precision precisionKey, Block context, ReachedSet item);

  /**
   * Store the result-states for the reached-set in the cache. Does not yet register the
   * result-states for the reached-set.
   *
   * @param item result-states to be inserted into the cache
   * @param rootOfBlock optional, can be {@code null}, only used for PCC
   */
  void put(
      AbstractState stateKey,
      Precision precisionKey,
      Block context,
      Collection<AbstractState> item,
      @Nullable ARGState rootOfBlock);

  /**
   * Invalidate the result-states of the given key. Does not remove the reached-set, thus it can be
   * used for re-exploration.
   */
  void remove(AbstractState stateKey, Precision precisionKey, Block context);

  /**
   * Return a Pair of the reached-set and the result-states for the given key. Both members of the
   * returned Pair are NULL, if there is a cache miss. For a partial cache hit we return the partly
   * computed reached-set and NULL as returnStates.
   */
  Pair<ReachedSet, Collection<AbstractState>> get(
      AbstractState stateKey, Precision precisionKey, Block context);

  /** Return the root-state of the last analyzed block, based on the last cache-access. */
  ARGState getLastAnalyzedBlock();

  /** Check whether a cache entry exists for a given key. */
  boolean containsPreciseKey(AbstractState stateKey, Precision precisionKey, Block context);

  /** Return all cached reached-sets. Useful for statistics. */
  Collection<ReachedSet> getAllCachedReachedStates();
}