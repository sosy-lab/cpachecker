// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam.cache;

import java.io.PrintStream;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

/** A wrapper for a fully synchronized cache access. */
// TODO we should use more fine-grained locking implementation
public class BAMCacheSynchronized implements BAMCache {

  private final BAMCache cache;
  private final StatTimer timer = new StatTimer("Time for cache-access");

  public BAMCacheSynchronized(Configuration pConfig, Reducer pReducer, LogManager pLogger)
      throws InvalidConfigurationException {
    cache = new BAMCacheImpl(pConfig, pReducer, pLogger);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    synchronized (this) {
      cache.printStatistics(pOut, pResult, pReached);
      pOut.println(
          timer.getTitle()
              + ":                           "
              + timer
              + " (count="
              + timer.getUpdateCount()
              + ")");
    }
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    synchronized (this) {
      cache.writeOutputFiles(pResult, pReached);
    }
  }

  @Override
  public @Nullable String getName() {
    return cache.getName();
  }

  @Override
  public BAMCacheEntry put(
      AbstractState pStateKey, Precision pPrecisionKey, Block pContext, ReachedSet pItem) {
    synchronized (this) {
      timer.start();
      try {
        return cache.put(pStateKey, pPrecisionKey, pContext, pItem);
      } finally {
        timer.stop();
      }
    }
  }

  @Override
  public BAMCacheEntry get(AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
    synchronized (this) {
      try {
        timer.start();
        return cache.get(pStateKey, pPrecisionKey, pContext);
      } finally {
        timer.stop();
      }
    }
  }

  @Override
  @Deprecated
  public ARGState getLastAnalyzedBlock() {
    synchronized (this) {
      try {
        timer.start();
        return cache.getLastAnalyzedBlock();
      } finally {
        timer.stop();
      }
    }
  }

  @Override
  public boolean containsPreciseKey(
      AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
    synchronized (this) {
      try {
        timer.start();
        return cache.containsPreciseKey(pStateKey, pPrecisionKey, pContext);
      } finally {
        timer.stop();
      }
    }
  }

  @Override
  public Collection<ReachedSet> getAllCachedReachedStates() {
    synchronized (this) {
      return cache.getAllCachedReachedStates();
    }
  }

  @Override
  public void clear() {
    synchronized (this) {
      cache.clear();
    }
  }
}
