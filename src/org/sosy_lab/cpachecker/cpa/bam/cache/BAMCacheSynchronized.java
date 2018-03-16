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
package org.sosy_lab.cpachecker.cpa.bam.cache;

import java.io.PrintStream;
import java.util.Collection;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

/** A wrapper for a fully synchronized cache access. */
// TODO we should use more fine-grained locking implementation
public class BAMCacheSynchronized implements BAMCache {

  private final BAMCache cache;
  private final StatTimer timer = new StatTimer("Time for cache-access");

  public BAMCacheSynchronized(BAMCache pCache) {
    cache = pCache;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    synchronized (this) {
      cache.printStatistics(pOut, pResult, pReached);
      pOut.println(timer.getTitle() + ":                           " + timer + " (count=" + timer.getUpdateCount() + ")");
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
  public void put(AbstractState pStateKey, Precision pPrecisionKey, Block pContext,
      ReachedSet pItem) {
    synchronized (this) {
      timer.start();
      cache.put(pStateKey, pPrecisionKey, pContext, pItem);
      timer.stop();
    }
  }

  @Override
  public void put(AbstractState pStateKey, Precision pPrecisionKey, Block pContext,
      Collection<AbstractState> pItem, @Nullable ARGState pRootOfBlock) {
    synchronized (this) {
      timer.start();
      cache.put(pStateKey, pPrecisionKey, pContext, pItem, pRootOfBlock);
      timer.stop();
    }
  }

  @Override
  public void remove(AbstractState pStateKey, Precision pPrecisionKey, Block pContext) {
    synchronized (this) {
      timer.start();
      cache.remove(pStateKey, pPrecisionKey, pContext);
      timer.stop();
    }
  }

  @Override
  public Pair<ReachedSet, Collection<AbstractState>> get(AbstractState pStateKey,
      Precision pPrecisionKey, Block pContext) {
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
  public boolean containsPreciseKey(AbstractState pStateKey, Precision pPrecisionKey,
      Block pContext) {
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
