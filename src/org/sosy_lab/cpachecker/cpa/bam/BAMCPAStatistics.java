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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

/**
 * Prints some BAM related statistics
 */
class BAMCPAStatistics extends AbstractStatistics {

  private final AbstractBAMCPA cpa;
  private List<BAMBasedRefiner> refiners = new ArrayList<>();

  public BAMCPAStatistics(AbstractBAMCPA cpa) {
    this.cpa = cpa;
  }

  @Override
  public String getName() {
    return "BAMCPA";
  }

  public void addRefiner(BAMBasedRefiner pRefiner) {
    refiners.add(pRefiner);
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

    put(out, "Number of blocks", cpa.getBlockPartitioning().getBlocks().size());
    put(out, "Time for building block partitioning", cpa.blockPartitioningTimer);
    put(out, 0, cpa.reducerStatistics.reduceTime);
    put(out, 0, cpa.reducerStatistics.expandTime);
    put(out, 0, cpa.reducerStatistics.reducePrecisionTime);
    put(out, 0, cpa.reducerStatistics.expandPrecisionTime);

    for (BAMBasedRefiner refiner : refiners) {
      // TODO We print these statistics also for use-cases of BAM-refiners, that never use timers. Can we ignore them?
      out.println("\n" + refiner.getClass().getSimpleName() + ":");
      put(out, 1, refiner.computePathTimer);
      put(out, 1, refiner.computeSubtreeTimer);
      put(out, 1, refiner.computeCounterexampleTimer);
      put(out, 1, refiner.removeCachedSubtreeTimer);
    }

    //Add to reached set all states from BAM cache
    // These lines collect all states for 'Coverage Reporting'
//    Collection<ReachedSet> cachedStates = data.bamCache.getAllCachedReachedStates();
//    for (ReachedSet set : cachedStates) {
//      set.forEach(
//          (state, precision) -> {
//            // Method 'add' adds state not only in list of reached states, but also in waitlist,
//            // so we should delete it.
//            reached.add(state, precision);
//            reached.removeOnlyFromWaitlist(state);
//          });
//    }
  }
}
