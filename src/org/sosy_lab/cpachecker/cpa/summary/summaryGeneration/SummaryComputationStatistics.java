/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.summary.summaryGeneration;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * Statistics on summary generation.
 */
public class SummaryComputationStatistics implements Statistics {

  // todo: we need statistics on cache coherence, to see whether summary was
  // used.
  // #cache hits / #cache misses? What are the good numbers to record?

  // or does a TopLevelCPA decide on this?
  // btw the latter is a misleading name, since it's not actually top-level in the
  // configuration.

  int noTimesSummaryRequested = 0;
  private final AtomicInteger summaryCacheHit = new AtomicInteger(0);

  public void recordSummaryCacheHit() {
    summaryCacheHit.incrementAndGet();
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {



  }

  @Override
  public String getName() {
    return "SummaryGenerationCPA";
  }
}
