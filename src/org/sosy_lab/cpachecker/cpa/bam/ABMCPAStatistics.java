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
package org.sosy_lab.cpachecker.cpa.abm;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import java.io.PrintStream;
import java.util.Collection;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

/**
 * Prints some ABM related statistics
 */
class ABMCPAStatistics implements Statistics {

  private final ABMCPA cpa;
  private AbstractABMBasedRefiner refiner = null;

  public ABMCPAStatistics(ABMCPA cpa) {
    this.cpa = cpa;
  }

  @Override
  public String getName() {
    return "ABMCPA";
  }

  public void addRefiner(AbstractABMBasedRefiner pRefiner) {
    checkState(refiner == null);
    refiner = pRefiner;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {

    ABMTransferRelation transferRelation = cpa.getTransferRelation();
    TimedReducer reducer = cpa.getReducer();

    int sumCalls = transferRelation.cacheMisses + transferRelation.partialCacheHits + transferRelation.fullCacheHits;

    int sumARTElemets = 0;
    for (ReachedSet subreached : ABMARGUtils.gatherReachedSets(cpa, reached).values()) {
      sumARTElemets += subreached.size();
    }

    out.println("Total size of all ARGs:                                         " + sumARTElemets);
    out.println("Maximum block depth:                                            " + transferRelation.maxRecursiveDepth);
    out.println("Total number of recursive CPA calls:                            " + sumCalls);
    out.println("  Number of cache misses:                                       " + transferRelation.cacheMisses + " (" + toPercent(transferRelation.cacheMisses, sumCalls) + " of all calls)");
    out.println("  Number of partial cache hits:                                 " + transferRelation.partialCacheHits + " (" + toPercent(transferRelation.partialCacheHits, sumCalls) + " of all calls)");
    out.println("  Number of full cache hits:                                    " + transferRelation.fullCacheHits + " (" + toPercent(transferRelation.fullCacheHits, sumCalls) + " of all calls)");
    if (transferRelation.gatherCacheMissStatistics) {
      out.println("Cause for cache misses:                                         ");
      out.println("  Number of abstraction caused misses:                          " + transferRelation.abstractionCausedMisses + " (" + toPercent(transferRelation.abstractionCausedMisses, transferRelation.cacheMisses) + " of all misses)");
      out.println("  Number of precision caused misses:                            " + transferRelation.precisionCausedMisses + " (" + toPercent(transferRelation.precisionCausedMisses, transferRelation.cacheMisses) + " of all misses)");
      out.println("  Number of misses with no similar elements:                    " + transferRelation.noSimilarCausedMisses + " (" + toPercent(transferRelation.noSimilarCausedMisses, transferRelation.cacheMisses) + " of all misses)");
    }
    out.println("Time for reducing abstract states:                            " + reducer.reduceTime + " (Calls: " + reducer.reduceTime.getNumberOfIntervals() + ")");
    out.println("Time for expanding abstract states:                           " + reducer.expandTime + " (Calls: " + reducer.expandTime.getNumberOfIntervals() + ")");
    out.println("Time for checking equality of abstract states:                " + transferRelation.equalsTimer + " (Calls: " + transferRelation.equalsTimer.getNumberOfIntervals() + ")");
    out.println("Time for computing the hashCode of abstract states:           " + transferRelation.hashingTimer + " (Calls: " + transferRelation.hashingTimer.getNumberOfIntervals() + ")");
    out.println("Time for searching for similar cache entries:                   " + transferRelation.searchingTimer + " (Calls: " + transferRelation.searchingTimer.getNumberOfIntervals() + ")");
    out.println("Time for reducing precisions:                                   " + reducer.reducePrecisionTime + " (Calls: " + reducer.reducePrecisionTime.getNumberOfIntervals() + ")");
    out.println("Time for expanding precisions:                                  " + reducer.expandPrecisionTime + " (Calls: " + reducer.expandPrecisionTime.getNumberOfIntervals() + ")");

    out.println("Time for removing cached subtrees for refinement:               " + transferRelation.removeCachedSubtreeTimer);
    out.println("Time for recomputing ARGs during counterexample analysis:       " + transferRelation.recomputeARTTimer);
    if (refiner != null) {
      out.println("Compute path for refinement:                                    " + refiner.computePathTimer);
      out.println("  Constructing flat ARG:                                        " + refiner.computeSubtreeTimer);
      out.println("  Searching path to error location:                             " + refiner.computeCounterexampleTimer);
    }

    //Add to reached set all states from abm cache
    Collection<ReachedSet> cachedStates = transferRelation.getCachedReachedSet();
    for (ReachedSet set : cachedStates) {
      for (AbstractState state : set.asCollection()) {
        /* Method 'add' add state not only in list of reached states, but also in waitlist,
         * so we should delete it.
         */
        reached.add(state, set.getPrecision(state));
        reached.removeOnlyFromWaitlist(state);
      }
    }
  }

}