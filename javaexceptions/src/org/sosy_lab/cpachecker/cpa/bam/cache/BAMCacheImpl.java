// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam.cache;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import com.google.common.collect.Collections2;
import java.io.PrintStream;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.statistics.StatHist;

@Options(prefix = "cpa.bam")
public class BAMCacheImpl implements BAMCache {

  @Option(
      secure = true,
      description =
          "If enabled, the reached set cache is analysed "
              + "for each cache miss to find the cause of the miss.")
  private boolean gatherCacheMissStatistics = false;

  private final Timer hashingTimer = new Timer();
  private final Timer equalsTimer = new Timer();

  private int cacheMisses = 0;
  private int partialCacheHits = 0;
  private int fullCacheHits = 0;

  private int abstractionCausedMisses = 0;
  private int precisionCausedMisses = 0;
  private int noSimilarCausedMisses = 0;

  // we use LinkedHashMaps to avoid non-determinism
  protected final Map<AbstractStateHash, BAMCacheEntry> preciseReachedCache = new LinkedHashMap<>();

  protected BAMCacheEntry lastAnalyzedEntry = null;
  protected final Reducer reducer;
  protected final LogManager logger;

  public BAMCacheImpl(Configuration config, Reducer reducer, LogManager logger)
      throws InvalidConfigurationException {
    config.inject(this, BAMCacheImpl.class);
    this.reducer = reducer;
    this.logger = logger;
  }

  protected AbstractStateHash getHashCode(
      AbstractState stateKey, Precision precisionKey, Block context) {
    return new AbstractStateHash(stateKey, precisionKey, context);
  }

  @Override
  public BAMCacheEntry put(
      AbstractState stateKey, Precision precisionKey, Block context, ReachedSet rs) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    BAMCacheEntry entry = new BAMCacheEntry(rs);
    // assert !preciseReachedCache.containsKey(hash);
    preciseReachedCache.put(hash, entry);
    return entry;
  }

  protected static boolean allStatesContainedInReachedSet(
      Collection<AbstractState> pElements, ReachedSet reached) {
    return reached.asCollection().containsAll(pElements);
  }

  @Override
  public BAMCacheEntry get(
      final AbstractState stateKey, final Precision precisionKey, final Block context) {

    final BAMCacheEntry entry = get0(stateKey, precisionKey, context);

    // get some statistics
    if (entry == null) {
      cacheMisses++;
      if (gatherCacheMissStatistics) {
        findCacheMissCause(stateKey, precisionKey, context);
      }
    } else {
      if (entry.getExitStates() == null) {
        // we have cached a partly computed reached-set
        partialCacheHits++;
      } else {
        // we have a full cache hit
        fullCacheHits++;
      }
    }

    return entry;
  }

  private BAMCacheEntry get0(
      final AbstractState stateKey, final Precision precisionKey, final Block context) {

    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    BAMCacheEntry result = preciseReachedCache.get(hash);
    if (result != null) {
      lastAnalyzedEntry = result;
      logger.log(Level.FINEST, "CACHE_ACCESS: precise entry");
      return result;
    }

    return getIfNotExistant(stateKey, precisionKey, context, hash);
  }

  @SuppressWarnings("unused") /* parameters used in subclass */
  protected BAMCacheEntry getIfNotExistant(
      final AbstractState stateKey,
      final Precision precisionKey,
      final Block context,
      AbstractStateHash hash) {
    lastAnalyzedEntry = null;
    logger.log(Level.FINEST, "CACHE_ACCESS: entry not available");
    return null;
  }

  @Override
  @Deprecated
  public ARGState getLastAnalyzedBlock() {
    return lastAnalyzedEntry.getRootOfBlock();
  }

  private void findCacheMissCause(
      AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
    AbstractStateHash searchKey = getHashCode(pStateKey, pPrecisionKey, pContext);
    for (AbstractStateHash cacheKey : preciseReachedCache.keySet()) {
      assert !searchKey.equals(cacheKey);

      // searchKey != cacheKey, check whether it is the same if we ignore the
      // precision
      AbstractStateHash ignorePrecisionSearchKey =
          getHashCode(pStateKey, cacheKey.precisionKey, pContext);
      if (ignorePrecisionSearchKey.equals(cacheKey)) {
        precisionCausedMisses++;
        return;
      }

      // Precision was not the cause. Check abstraction.
      AbstractStateHash ignoreAbsSearchKey =
          getHashCode(cacheKey.stateKey, pPrecisionKey, pContext);
      if (ignoreAbsSearchKey.equals(cacheKey)) {
        abstractionCausedMisses++;
        return;
      }
    }
    noSimilarCausedMisses++;
  }

  @Override
  public boolean containsPreciseKey(AbstractState stateKey, Precision precisionKey, Block context) {
    AbstractStateHash hash = getHashCode(stateKey, precisionKey, context);
    return preciseReachedCache.containsKey(hash);
  }

  @Override
  public Collection<ReachedSet> getAllCachedReachedStates() {
    return Collections2.transform(preciseReachedCache.values(), BAMCacheEntry::getReachedSet);
  }

  class AbstractStateHash {

    private final Object wrappedHash;
    private final Block context;
    final AbstractState stateKey;
    final Precision precisionKey;

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
        return context.equals(other.context) && wrappedHash.equals(other.wrappedHash);
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
      return "AbstractStateHash [hash="
          + hashCode()
          + ", wrappedHash="
          + wrappedHash
          + ", context="
          + context
          + ", predicateKey="
          + stateKey
          + ", precisionKey="
          + precisionKey
          + "]";
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

    int sumCalls = cacheMisses + partialCacheHits + fullCacheHits;

    StatHist argStats =
        new StatHist("") {
          @Override
          public String toString() {
            // overriding, because printing all sizes is not that interesting
            return String.format(
                "%.0f (#=%d, avg=%.2f, dev=%.2f, min=%d, max=%d)",
                getSum(), getUpdateCount(), getAvg(), getStdDeviation(), getMin(), getMax());
          }
        };
    for (UnmodifiableReachedSet subreached : getAllCachedReachedStates()) {
      argStats.insertValue(subreached.size());
    }

    out.println("Total size of all ARGs:                              " + argStats);
    out.println("Total number of recursive CPA calls:                 " + sumCalls);
    out.println(
        "  Number of cache misses:                            "
            + cacheMisses
            + " ("
            + toPercent(cacheMisses, sumCalls)
            + " of all calls)");
    out.println(
        "  Number of partial cache hits:                      "
            + partialCacheHits
            + " ("
            + toPercent(partialCacheHits, sumCalls)
            + " of all calls)");
    out.println(
        "  Number of full cache hits:                         "
            + fullCacheHits
            + " ("
            + toPercent(fullCacheHits, sumCalls)
            + " of all calls)");
    if (gatherCacheMissStatistics) {
      out.println("Cause for cache misses:                              ");
      out.println(
          "  Number of abstraction caused misses:               "
              + abstractionCausedMisses
              + " ("
              + toPercent(abstractionCausedMisses, cacheMisses)
              + " of all misses)");
      out.println(
          "  Number of precision caused misses:                 "
              + precisionCausedMisses
              + " ("
              + toPercent(precisionCausedMisses, cacheMisses)
              + " of all misses)");
      out.println(
          "  Number of misses with no similar elements:         "
              + noSimilarCausedMisses
              + " ("
              + toPercent(noSimilarCausedMisses, cacheMisses)
              + " of all misses)");
    }
    out.println(
        "Time for checking equality of abstract states:       "
            + equalsTimer
            + " (Calls: "
            + equalsTimer.getNumberOfIntervals()
            + ")");
    out.println(
        "Time for computing the hashCode of abstract states:  "
            + hashingTimer
            + " (Calls: "
            + hashingTimer.getNumberOfIntervals()
            + ")");
  }

  @Override
  public String getName() {
    return "BAMCache";
  }

  @Override
  public void clear() {
    preciseReachedCache.clear();
    lastAnalyzedEntry = null;
  }
}
