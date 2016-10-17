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
package org.sosy_lab.cpachecker.core.algorithm.tgar;

import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.div;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class TGARStatistics extends AbstractStatistics {

  private final LogManager logger;
  final Timer totalTimer = new Timer();
  final Timer refinementTimer = new Timer();

  public interface TGARMXBean {
    int getNumberOfRefinements();
    int getSizeOfReachedSetBeforeLastRefinement();
    boolean isRefinementActive();
  }

  private class TGARMBean extends AbstractMBean implements TGARMXBean {
    public TGARMBean() {
      super("org.sosy_lab.cpachecker:type=TGAR", logger);
      register();
    }

    @Override
    public int getNumberOfRefinements() {
      return countRefinements;
    }

    @Override
    public int getSizeOfReachedSetBeforeLastRefinement() {
      return sizeOfReachedSetBeforeRefinement;
    }

    @Override
    public boolean isRefinementActive() {
      return refinementTimer.isRunning();
    }
  }

  @SuppressFBWarnings(value = "VO_VOLATILE_INCREMENT",
      justification = "only one thread writes, others read")
  volatile int countRefinements = 0;
  int countSuccessfulRefinements = 0;
  int countFailedRefinements = 0;

  int maxReachedSizeBeforeRefinement = 0;
  int maxReachedSizeAfterRefinement = 0;
  long totalReachedSizeBeforeRefinement = 0;
  long totalReachedSizeAfterRefinement = 0;

  int maxRemovedTargets = 0;
  int totalRemovedTargets = 0;
  int timesAllTargetCandidatesRemoved = 0;
  int timesNotAllTargetCandidatesRemoved = 0;

  volatile int sizeOfReachedSetBeforeRefinement = 0;

  private final Multiset<Property> timesInfeasible;
  private final Multiset<Property> timesFeasible;

  public TGARStatistics(LogManager pLogManager) {
    logger = pLogManager;
    timesFeasible = HashMultiset.create();
    timesInfeasible = HashMultiset.create();

    new TGARMBean(); // don't store it because we wouldn't know when to unregister anyway
  }

  @Override
  public String getName() {
    return "TGAR Algorithm";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {

    put(out, "Number of refinements", countRefinements);

    if (countRefinements > 0) {
      put(out, "Number of successful refinements", countSuccessfulRefinements);
      put(out, "Number of failed refinements", countFailedRefinements);
      out.println("");

      put(out, "Max. size of reached set before ref.", maxReachedSizeBeforeRefinement);
      put(out, "Max. size of reached set after ref.", maxReachedSizeAfterRefinement);
      put(out, "Avg. size of reached set before ref.",div(totalReachedSizeBeforeRefinement, countRefinements));
      put(out, "Avg. size of reached set after ref.", div(totalReachedSizeAfterRefinement, countSuccessfulRefinements));
      out.println("");

      put(out, "Total time for TGAR algorithm", totalTimer);
      put(out, "Total time for refinements", refinementTimer);
      put(out, "Avg. time for refinement", refinementTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
      put(out, "Max. time for refinement", refinementTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
      out.println("");

      int maxInfeasibleUntilFeasible = 0;
      int maxFeasible = 0;
      int maxInfeasible = 0;
      for (Property p : timesFeasible) {
        int feasibleCount = timesFeasible.count(p);
        int infeasibleCount = timesInfeasible.count(p);
        if (feasibleCount > 0) {
          maxInfeasibleUntilFeasible = Math.max(maxInfeasibleUntilFeasible, infeasibleCount);
        }
        maxFeasible = Math.max(maxFeasible, feasibleCount);
      }

      int totalTimesInfeasible = 0;
      for (Property p : timesInfeasible) {
        final int infeasibleCount = timesInfeasible.count(p);
        maxInfeasible = Math.max(maxInfeasible, infeasibleCount);
        totalTimesInfeasible = totalTimesInfeasible + infeasibleCount;
      }
      final int avgInfeasible;
      if (timesInfeasible.size() > 0) {
        avgInfeasible = totalTimesInfeasible / timesInfeasible.size();
      } else {
        avgInfeasible = 0;
      }

      put(out, "Max. infeasible counterexamples", maxInfeasible);
      put(out, "Avg. infeasible counterexamples", avgInfeasible);
      put(out, "Max. infeasible counterexamples until feasible", maxInfeasibleUntilFeasible);
      put(out, "Max. feasible counterexamples", maxFeasible);
      out.println("");

      put(out, "Max. removed targets", maxRemovedTargets);
      put(out, "Avg. removed targets", totalRemovedTargets / countRefinements);
      put(out, "Times all target candidates removed", timesAllTargetCandidatesRemoved);
      put(out, "Times not all target candidates removed", timesNotAllTargetCandidatesRemoved);
    }
  }

  void beginRefinement(ReachedSet pReachedSetBeforeRefine, ARGState pTargetState) {
    countRefinements++;
    totalReachedSizeBeforeRefinement += pReachedSetBeforeRefine.size();
    maxReachedSizeBeforeRefinement = Math.max(maxReachedSizeBeforeRefinement, pReachedSetBeforeRefine.size());
    sizeOfReachedSetBeforeRefinement = pReachedSetBeforeRefine.size();
    refinementTimer.start();
  }

  void endWithError(ReachedSet pReachedSetAfterFailure) {
    countFailedRefinements++;
    refinementTimer.stop();
  }

  void endWithFeasible(ReachedSet pReachedSetBeforeRefine, ARGState pTargetState) {
    refinementTimer.stop();
    timesFeasible.addAll(pTargetState.getViolatedProperties());
  }

  void endWithInfeasible(
      ReachedSet pReachedSetAfterRefine,
      ARGState pTargetState,
      int pRemovedTargets,
      boolean pAllCandidatesEliminated,
      Collection<? extends Property> pViolatedProperties) {
    countSuccessfulRefinements++;
    totalReachedSizeAfterRefinement += pReachedSetAfterRefine.size();
    maxReachedSizeAfterRefinement = Math.max(maxReachedSizeAfterRefinement, pReachedSetAfterRefine.size());
    refinementTimer.stop();
    timesInfeasible.addAll(pViolatedProperties);
    maxRemovedTargets = Math.max(maxRemovedTargets, pRemovedTargets);
    totalRemovedTargets = totalRemovedTargets + maxRemovedTargets;
    if (pAllCandidatesEliminated) {
      timesAllTargetCandidatesRemoved++;
    } else {
      timesNotAllTargetCandidatesRemoved++;
    }
  }

}
