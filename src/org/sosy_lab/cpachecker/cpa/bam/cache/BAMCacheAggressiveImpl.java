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
package org.sosy_lab.cpachecker.cpa.bam.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

/**
 * This implementation of BAMCache uses an heuristic to improve the cache-hit-rate. Whenever
 * accessing the cache without a direct hit, we search for an abstract state with a similar
 * precision. This heuristic can lead to repeated counterexamples in the analysis. Thus it must be
 * considered, e.g., when performing a refinement.
 */
public class BAMCacheAggressiveImpl extends BAMCacheImpl {

  private final Map<AbstractStateHash, BAMCacheEntry> impreciseReachedCache = new LinkedHashMap<>();

  public BAMCacheAggressiveImpl(Configuration config, Reducer reducer, LogManager logger)
      throws InvalidConfigurationException {
    super(config, reducer, logger);
  }

  @Override
  protected @Nullable BAMCacheEntry getIfNotExistant(
      final AbstractState stateKey,
      final Precision precisionKey,
      final Block context,
      AbstractStateHash hash) {
    BAMCacheEntry result = impreciseReachedCache.get(hash);
    if (result != null) {
      lastAnalyzedEntry = result;
      logger.log(Level.FINEST, "CACHE_ACCESS: imprecise entry, directly from cache");
      return result;
    }

    // Search for similar entry.
    result = lookForSimilarState(stateKey, precisionKey, context);

    if (result != null) {
      // found similar element, use this
      impreciseReachedCache.put(hash, result);
      lastAnalyzedEntry = result;
      logger.log(Level.FINEST, "CACHE_ACCESS: imprecise entry, searched in cache");
      return result;
    }

    return super.getIfNotExistant(stateKey, precisionKey, context, hash);
  }

  /** Return the cache hit with the closest precision (used for aggressive caching). */
  private BAMCacheEntry lookForSimilarState(
      AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
    int min = Integer.MAX_VALUE;
    BAMCacheEntry result = null;

    for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
      //searchKey != cacheKey, check whether it is the same if we ignore the precision
      AbstractStateHash ignorePrecisionSearchKey =
          getHashCode(pStateKey, cacheKey.precisionKey, pContext);
      if (ignorePrecisionSearchKey.equals(cacheKey)) {
        int distance = reducer.measurePrecisionDifference(pPrecisionKey, cacheKey.precisionKey);
        if (distance < min) { //prefer similar precisions
          min = distance;
          result = preciseReachedCache.get(ignorePrecisionSearchKey);
        }
      }
    }

    return result;
  }
}
