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
package org.sosy_lab.cpachecker.cpa.bam;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * This implementation of BAMCache uses an heuristic to improve the cache-hit-rate. Whenever
 * accessing the cache without a direct hit, we search for an abstract state with a similar
 * precision. This heuristic can lead to repeated counterexamples in the analysis. Thus it must be
 * considered, e.g., when performing a refinement.
 */
public class BAMCacheAggressiveImpl extends BAMCacheImpl {

  private final Map<AbstractStateHash, ReachedSet> impreciseReachedCache = new HashMap<>();

  public BAMCacheAggressiveImpl(Configuration config, Reducer reducer, LogManager logger)
      throws InvalidConfigurationException {
    super(config, reducer, logger);
  }

  @Override
  protected @Nullable Pair<ReachedSet, Collection<AbstractState>> getIfNotExistant(
      final AbstractState stateKey,
      final Precision precisionKey,
      final Block context,
      AbstractStateHash hash) {
    ReachedSet result;
    result = impreciseReachedCache.get(hash);
    if (result != null) {
      AbstractStateHash impreciseHash =
          getHashCode(stateKey, result.getPrecision(result.getFirstState()), context);

      lastAnalyzedBlockCache = impreciseHash;
      logger.log(Level.FINEST, "CACHE_ACCESS: imprecise entry, directly from cache");
      return Pair.of(result, returnCache.get(impreciseHash));
    }

    // Search for similar entry.
    Pair<ReachedSet, Collection<AbstractState>> pair =
        lookForSimilarState(stateKey, precisionKey, context);

    if (pair != null) {
      //found similar element, use this
      impreciseReachedCache.put(hash, pair.getFirst());
      lastAnalyzedBlockCache =
          getHashCode(
              stateKey, pair.getFirst().getPrecision(pair.getFirst().getFirstState()), context);
      logger.log(Level.FINEST, "CACHE_ACCESS: imprecise entry, searched in cache");
      return pair;
    }

    return super.getIfNotExistant(stateKey, precisionKey, context, hash);
  }

  /** Return the cache hit with the closest precision (used for aggressive caching). */
  private Pair<ReachedSet, Collection<AbstractState>> lookForSimilarState(
      AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
    int min = Integer.MAX_VALUE;
    Pair<ReachedSet, Collection<AbstractState>> result = null;

    for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
      //searchKey != cacheKey, check whether it is the same if we ignore the precision
      AbstractStateHash ignorePrecisionSearchKey =
          getHashCode(pStateKey, cacheKey.precisionKey, pContext);
      if (ignorePrecisionSearchKey.equals(cacheKey)) {
        int distance = reducer.measurePrecisionDifference(pPrecisionKey, cacheKey.precisionKey);
        if (distance < min) { //prefer similar precisions
          min = distance;
          result =
              Pair.of(
                  preciseReachedCache.get(ignorePrecisionSearchKey),
                  returnCache.get(ignorePrecisionSearchKey));
        }
      }
    }

    return result;
  }

  @Override
  @Deprecated /* unused */
  public void clear() {
    impreciseReachedCache.clear();
    super.clear();
  }
}
