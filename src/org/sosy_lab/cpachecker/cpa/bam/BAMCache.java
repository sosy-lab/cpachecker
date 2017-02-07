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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix = "cpa.bam")
public class BAMCache implements Statistics {

  @Option(secure=true, description = "If enabled, cache queries also consider blocks with "
      + "non-matching precision for reuse.")
  private boolean aggressiveCaching = true;

  @Option(secure=true, description = "If enabled, the reached set cache is analysed "
      + "for each cache miss to find the cause of the miss.")
  private boolean gatherCacheMissStatistics = false;

  private final Timer hashingTimer = new Timer();
  private final Timer equalsTimer = new Timer();
  private final Timer searchingTimer = new Timer();

  private int cacheMisses = 0;
  private int partialCacheHits = 0;
  private int fullCacheHits = 0;

  private int abstractionCausedMisses = 0;
  private int precisionCausedMisses = 0;
  private int noSimilarCausedMisses = 0;

  // we use LinkedHashMaps to avoid non-determinism
  private final Map<AbstractStateHash, ReachedSet> preciseReachedCache = new LinkedHashMap<>();
  private final Map<AbstractStateHash, ReachedSet> impreciseReachedCache = new HashMap<>();
  private final Map<AbstractStateHash, Collection<AbstractState>> returnCache = new HashMap<>();
  private final Map<AbstractStateHash, ARGState> blockARGCache = new HashMap<>();

  private AbstractStateHash lastAnalyzedBlockCache = null;
  private final Reducer reducer;

  private final LogManager logger;

  public BAMCache(
      Configuration config,
      Reducer reducer,
      LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    this.reducer = reducer;
    this.logger = logger;
  }

  public boolean doesAggressiveCaching() {
    return aggressiveCaching;
  }

  private AbstractStateHash getHashCode(AbstractState stateKey, Precision precisionKey, Block context) {
    return new AbstractStateHash(stateKey, precisionKey, context);
  }

  public void put(AbstractState stateKey, Precision precisionKey, Block context, ReachedSet item) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    assert !preciseReachedCache.containsKey(hash);
    preciseReachedCache.put(hash, item);
  }

  public void put(AbstractState stateKey, Precision precisionKey, Block context, Collection<AbstractState> item,
                   ARGState rootOfBlock) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    assert preciseReachedCache.get(hash) != null : "key not found in cache";
    assert allStatesContainedInReachedSet(item, preciseReachedCache.get(hash)) : "output-states must be in reached-set";
    returnCache.put(hash, item);
    blockARGCache.put(hash, rootOfBlock);
    lastAnalyzedBlockCache = hash;
  }

  private static boolean allStatesContainedInReachedSet(Collection<AbstractState> pElements, ReachedSet reached) {
    return reached.asCollection().containsAll(pElements);
  }

  public void removeReturnEntry(AbstractState stateKey, Precision precisionKey, Block context) {
    returnCache.remove(getHashCode(stateKey, precisionKey, context));
  }

  public void remove(AbstractState stateKey, Precision precisionKey, Block context) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    blockARGCache.remove(hash);
    returnCache.remove(hash);
  }

  /**
   * This function returns a Pair of the reached-set and the returnStates for the given keys.
   * Both members of the returned Pair are NULL, if there is a cache miss.
   * For a partial cache hit we return the partly computed reached-set and NULL as returnStates. */
  public Pair<ReachedSet, Collection<AbstractState>> get(
      final AbstractState stateKey,
      final Precision precisionKey,
      final Block context) {

    final Pair<ReachedSet, Collection<AbstractState>> pair = get0(stateKey, precisionKey, context);
    Preconditions.checkNotNull(pair);

    // get some statistics
    final ReachedSet reached = pair.getFirst();
    final Collection<AbstractState> returnStates = pair.getSecond();

    if (reached != null && returnStates != null) { // we have reached-set and elements
      assert Iterables.all(returnStates, s -> !((ARGState) s).isDestroyed())
          : "do not use destroyed states: " + returnStates;
      assert allStatesContainedInReachedSet(returnStates, reached)
          : "output-states must be in reached-set: "
              + returnStates
              + " not available in reachedset with root "
              + reached.getFirstState()
              + " and last state "
              + reached.getLastState();
      fullCacheHits++;
    } else if (reached != null) { // we have cached a partly computed reached-set
      partialCacheHits++;
    } else if (returnStates == null) {
      cacheMisses++;
      if (gatherCacheMissStatistics) {
        findCacheMissCause(stateKey, precisionKey, context);
      }
    } else {
      throw new AssertionError("invalid return-value for BAMCache.get(): " + pair);
    }

    return pair;
  }

  private Pair<ReachedSet, Collection<AbstractState>> get0(
      final AbstractState stateKey,
      final Precision precisionKey,
      final Block context) {

    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    ReachedSet result = preciseReachedCache.get(hash);
    if (result != null) {
      lastAnalyzedBlockCache = hash;
      logger.log(Level.FINEST, "CACHE_ACCESS: precise entry");
      return Pair.of(result, returnCache.get(hash));
    }

    if (aggressiveCaching) {
      result = impreciseReachedCache.get(hash);
      if (result != null) {
        AbstractStateHash impreciseHash = getHashCode(
            stateKey,
            result.getPrecision(result.getFirstState()),
            context);

        lastAnalyzedBlockCache = impreciseHash;
        logger.log(Level.FINEST, "CACHE_ACCESS: imprecise entry, directly from cache");
        return Pair.of(result, returnCache.get(impreciseHash));
      }

      // Search for similar entry.
      Pair<ReachedSet, Collection<AbstractState>> pair = lookForSimilarState(
          stateKey, precisionKey, context);

      if (pair != null) {
        //found similar element, use this
        impreciseReachedCache.put(hash, pair.getFirst());
        lastAnalyzedBlockCache = getHashCode(
                stateKey,
                pair.getFirst().getPrecision(pair.getFirst().getFirstState()),
                context);
        logger.log(Level.FINEST, "CACHE_ACCESS: imprecise entry, searched in cache");
        return pair;
      }
    }

    lastAnalyzedBlockCache = null;
    logger.log(Level.FINEST, "CACHE_ACCESS: entry not available");
    return Pair.of(null, null);
  }

  public ARGState getLastAnalyzedBlock() {
    return blockARGCache.get(lastAnalyzedBlockCache);
  }

  /**
   * Return the cache hit with the closest precision (used for aggressive
   * caching).
   */
  private Pair<ReachedSet, Collection<AbstractState>> lookForSimilarState(
      AbstractState pStateKey,
      Precision pPrecisionKey,
      Block pContext) {
    searchingTimer.start();
    try {
      int min = Integer.MAX_VALUE;
      Pair<ReachedSet, Collection<AbstractState>> result = null;

      for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
        //searchKey != cacheKey, check whether it is the same if we ignore the precision
        AbstractStateHash ignorePrecisionSearchKey = getHashCode(pStateKey, cacheKey.precisionKey, pContext);
        if (ignorePrecisionSearchKey.equals(cacheKey)) {
          int distance = reducer.measurePrecisionDifference(pPrecisionKey, cacheKey.precisionKey);
          if (distance < min) { //prefer similar precisions
            min = distance;
            result = Pair.of(
                    preciseReachedCache.get(ignorePrecisionSearchKey),
                    returnCache.get(ignorePrecisionSearchKey));
          }
        }
      }

      return result;
    } finally {
      searchingTimer.stop();
    }
  }

  private void findCacheMissCause(AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
    AbstractStateHash searchKey = getHashCode(pStateKey, pPrecisionKey, pContext);
    for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
      assert !searchKey.equals(cacheKey);

      // searchKey != cacheKey, check whether it is the same if we ignore the
      // precision
      AbstractStateHash ignorePrecisionSearchKey = getHashCode(pStateKey, cacheKey.precisionKey, pContext);
      if (ignorePrecisionSearchKey.equals(cacheKey)) {
        precisionCausedMisses++;
        return;
      }

      // Precision was not the cause. Check abstraction.
      AbstractStateHash ignoreAbsSearchKey = getHashCode(cacheKey.stateKey, pPrecisionKey, pContext);
      if (ignoreAbsSearchKey.equals(cacheKey)) {
        abstractionCausedMisses++;
        return;
      }
    }
    noSimilarCausedMisses++;
  }

  @Deprecated /* unused */
  public void clear() {
    preciseReachedCache.clear();
    impreciseReachedCache.clear();
    returnCache.clear();
  }

  public boolean containsPreciseKey(AbstractState stateKey, Precision precisionKey, Block context) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    return preciseReachedCache.containsKey(hash);
  }

  public Collection<ReachedSet> getAllCachedReachedStates() {
    return preciseReachedCache.values();
  }

  private class AbstractStateHash {

    private final Object wrappedHash;
    private final Block context;
    private final AbstractState stateKey;
    private final Precision precisionKey;

    public AbstractStateHash(AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
      wrappedHash = reducer.getHashCodeForState(pStateKey, pPrecisionKey);
      context = checkNotNull(pContext);
      stateKey = pStateKey;
      precisionKey = pPrecisionKey;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof AbstractStateHash)) {
        return false;
      }
      if (pObj == this) {
        return true;
      }
      AbstractStateHash other = (AbstractStateHash) pObj;
      equalsTimer.start();
      try {
        return context.equals(other.context)
                && wrappedHash.equals(other.wrappedHash);
      } finally {
        equalsTimer.stop();
      }
    }

    @Override
    public int hashCode() {
      hashingTimer.start();
      try {
        return wrappedHash.hashCode() * 17 + context.hashCode();
      } finally {
        hashingTimer.stop();
      }
    }

    @Override
    public String toString() {
      return "AbstractStateHash [hash=" + hashCode() + ", wrappedHash=" + wrappedHash + ", context="
              + context + ", predicateKey=" + stateKey + ", precisionKey=" + precisionKey + "]";
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

    int sumCalls = cacheMisses + partialCacheHits + fullCacheHits;

    int sumARTElements = 0;
    for (UnmodifiableReachedSet subreached : getAllCachedReachedStates()) {
      sumARTElements += subreached.size();
    }

    out.println("Total size of all ARGs:                              " + sumARTElements);
    out.println("Total number of recursive CPA calls:                 " + sumCalls);
    out.println("  Number of cache misses:                            " + cacheMisses + " (" + toPercent(cacheMisses, sumCalls) + " of all calls)");
    out.println("  Number of partial cache hits:                      " + partialCacheHits + " (" + toPercent(partialCacheHits, sumCalls) + " of all calls)");
    out.println("  Number of full cache hits:                         " + fullCacheHits + " (" + toPercent(fullCacheHits, sumCalls) + " of all calls)");
    if (gatherCacheMissStatistics) {
      out.println("Cause for cache misses:                              ");
      out.println("  Number of abstraction caused misses:               " + abstractionCausedMisses + " (" + toPercent(abstractionCausedMisses, cacheMisses) + " of all misses)");
      out.println("  Number of precision caused misses:                 " + precisionCausedMisses + " (" + toPercent(precisionCausedMisses, cacheMisses) + " of all misses)");
      out.println("  Number of misses with no similar elements:         " + noSimilarCausedMisses + " (" + toPercent(noSimilarCausedMisses, cacheMisses) + " of all misses)");
    }
    out.println("Time for checking equality of abstract states:       " + equalsTimer + " (Calls: " + equalsTimer.getNumberOfIntervals() + ")");
    out.println("Time for computing the hashCode of abstract states:  " + hashingTimer + " (Calls: " + hashingTimer.getNumberOfIntervals() + ")");
  }

  @Override
  public String getName() {
    return "BAMCache";
  }
}
