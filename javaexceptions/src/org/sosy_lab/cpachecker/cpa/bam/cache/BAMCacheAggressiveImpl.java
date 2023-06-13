// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
      // searchKey != cacheKey, check whether it is the same if we ignore the precision
      AbstractStateHash ignorePrecisionSearchKey =
          getHashCode(pStateKey, cacheKey.precisionKey, pContext);
      if (ignorePrecisionSearchKey.equals(cacheKey)) {
        int distance = reducer.measurePrecisionDifference(pPrecisionKey, cacheKey.precisionKey);
        if (distance < min) { // prefer similar precisions
          min = distance;
          result = preciseReachedCache.get(ignorePrecisionSearchKey);
        }
      }
    }

    return result;
  }
}
