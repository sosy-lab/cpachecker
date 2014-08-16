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
package org.sosy_lab.cpachecker.core.algorithm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCovering;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment.Action;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoinPredicatedAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.Iterables;

public class CPAAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer         = new Timer();
    private Timer chooseTimer        = new Timer();
    private Timer precisionTimer     = new Timer();
    private Timer transferTimer      = new Timer();
    private Timer mergeTimer         = new Timer();
    private Timer stopTimer          = new Timer();
    private Timer addTimer           = new Timer();
    private Timer forcedCoveringTimer = new Timer();

    private int   countIterations   = 0;
    private int   maxWaitlistSize   = 0;
    private long  countWaitlistSize = 0;
    private int   countSuccessors   = 0;
    private int   maxSuccessors     = 0;
    private int   countMerge        = 0;
    private int   countStop         = 0;
    private int   countBreak        = 0;

    @Override
    public String getName() {
      return "CPA algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {
      out.println("Number of iterations:            " + countIterations);
      if (countIterations == 0) {
        // Statistics not relevant, prevent division by zero
        return;
      }

      out.println("Max size of waitlist:            " + maxWaitlistSize);
      out.println("Average size of waitlist:        " + countWaitlistSize
          / countIterations);
      out.println("Number of computed successors:   " + countSuccessors);
      out.println("Max successors for one state:    " + maxSuccessors);
      out.println("Number of times merged:          " + countMerge);
      out.println("Number of times stopped:         " + countStop);
      out.println("Number of times breaked:         " + countBreak);
      out.println();
      out.println("Total time for CPA algorithm:     " + totalTimer + " (Max: " + totalTimer.getMaxTime().formatAs(TimeUnit.SECONDS) + ")");
      out.println("  Time for choose from waitlist:  " + chooseTimer);
      if (forcedCoveringTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for forced covering:       " + forcedCoveringTimer);
      }
      out.println("  Time for precision adjustment:  " + precisionTimer);
      out.println("  Time for transfer relation:     " + transferTimer);
      if (mergeTimer.getNumberOfIntervals() > 0) {
        out.println("  Time for merge operator:        " + mergeTimer);
      }
      out.println("  Time for stop operator:         " + stopTimer);
      out.println("  Time for adding to reached set: " + addTimer);
    }
  }

  @Options(prefix="cpa")
  public static class CPAAlgorithmFactory {

    @Option(description="Which strategy to use for forced coverings (empty for none)",
            name="forcedCovering")
    @ClassOption(packagePrefix="org.sosy_lab.cpachecker")
    private Class<? extends ForcedCovering> forcedCoveringClass = null;
    private final ForcedCovering forcedCovering;

    private final ConfigurableProgramAnalysis cpa;
    private final LogManager logger;
    private final ShutdownNotifier shutdownNotifier;

    public CPAAlgorithmFactory(ConfigurableProgramAnalysis cpa, LogManager logger,
        Configuration config, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
      config.inject(this);
      this.cpa = cpa;
      this.logger = logger;
      this.shutdownNotifier = pShutdownNotifier;

      if (forcedCoveringClass != null) {
        forcedCovering = Classes.createInstance(ForcedCovering.class, forcedCoveringClass,
            new Class<?>[] {Configuration.class, LogManager.class, ConfigurableProgramAnalysis.class},
            new Object[]   {config,              logger,           cpa});
      } else {
        forcedCovering = null;
      }

    }

    public CPAAlgorithm newInstance() {
      return new CPAAlgorithm(cpa, logger, shutdownNotifier, forcedCovering);
    }
  }

  public static CPAAlgorithm create(ConfigurableProgramAnalysis cpa, LogManager logger,
      Configuration config, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    return new CPAAlgorithmFactory(cpa, logger, config, pShutdownNotifier).newInstance();
  }

  private final ForcedCovering forcedCovering;

  private final CPAStatistics               stats = new CPAStatistics();

  private final ConfigurableProgramAnalysis cpa;

  private final LogManager                  logger;

  private final ShutdownNotifier                   shutdownNotifier;

  private CPAAlgorithm(ConfigurableProgramAnalysis cpa, LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      ForcedCovering pForcedCovering) {
    this.cpa = cpa;
    this.logger = logger;
    this.shutdownNotifier = pShutdownNotifier;
    this.forcedCovering = pForcedCovering;
  }

  @Override
  public boolean run(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    stats.totalTimer.start();
    try {
      return run0(reachedSet);
    } finally {
      stats.totalTimer.stopIfRunning();
      stats.chooseTimer.stopIfRunning();
      stats.precisionTimer.stopIfRunning();
      stats.transferTimer.stopIfRunning();
      stats.mergeTimer.stopIfRunning();
      stats.stopTimer.stopIfRunning();
      stats.addTimer.stopIfRunning();
      stats.forcedCoveringTimer.stopIfRunning();
    }
  }

  private boolean run0(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    final TransferRelation transferRelation = cpa.getTransferRelation();
    final MergeOperator mergeOperator = cpa.getMergeOperator();
    final StopOperator stopOperator = cpa.getStopOperator();
    final PrecisionAdjustment precisionAdjustment =
        cpa.getPrecisionAdjustment();

    while (reachedSet.hasWaitingState()) {
      shutdownNotifier.shutdownIfNecessary();

      stats.countIterations++;

      // Pick next state using strategy
      // BFS, DFS or top sort according to the configuration
      int size = reachedSet.getWaitlistSize();
      if (size >= stats.maxWaitlistSize) {
        stats.maxWaitlistSize = size;
      }
      stats.countWaitlistSize += size;

      stats.chooseTimer.start();
      final AbstractState state = reachedSet.popFromWaitlist();
      final Precision precision = reachedSet.getPrecision(state);
      stats.chooseTimer.stop();

      logger.log(Level.FINER, "Retrieved state from waitlist");
      logger.log(Level.ALL, "Current state is", state, "with precision",
          precision);

      if (forcedCovering != null) {
        stats.forcedCoveringTimer.start();
        try {
          boolean stop = forcedCovering.tryForcedCovering(state, precision, reachedSet);

          if (stop) {
            // TODO: remove state from reached set?
            continue;
          }
        } finally {
          stats.forcedCoveringTimer.stop();
        }
      }

      stats.transferTimer.start();
      Collection<? extends AbstractState> successors;
      try {
        successors = transferRelation.getAbstractSuccessors(state, precision, null);
      } finally {
        stats.transferTimer.stop();
      }
      // TODO When we have a nice way to mark the analysis result as incomplete,
      // we could continue analysis on a CPATransferException with the next state from waitlist.

      int numSuccessors = successors.size();
      logger.log(Level.FINER, "Current state has", numSuccessors,
          "successors");
      stats.countSuccessors += numSuccessors;
      stats.maxSuccessors = Math.max(numSuccessors, stats.maxSuccessors);

      for (AbstractState successor : Iterables.consumingIterable(successors)) {
        logger.log(Level.FINER, "Considering successor of current state");
        logger.log(Level.ALL, "Successor of", state, "\nis", successor);

        stats.precisionTimer.start();
        PrecisionAdjustmentResult precAdjustmentResult;
        try {
          precAdjustmentResult = precisionAdjustment.prec(successor, precision, reachedSet);
        } finally {
          stats.precisionTimer.stop();
        }

        successor = precAdjustmentResult.abstractState();
        Precision successorPrecision = precAdjustmentResult.precision();
        Action action = precAdjustmentResult.action();

        if (action == Action.BREAK) {
          stats.stopTimer.start();
          boolean stop;
          try {
            stop = stopOperator.stop(successor, reachedSet.getReached(successor), successorPrecision);
          } finally {
            stats.stopTimer.stop();
          }

          if (AbstractStates.isTargetState(successor) && stop) {
            // don't signal BREAK for covered states
            // no need to call merge and stop either, so just ignore this state
            // and handle next successor
            stats.countStop++;
            logger.log(Level.FINER,
                "Break was signalled but ignored because the state is covered.");
            continue;

          } else {
            stats.countBreak++;
            logger.log(Level.FINER, "Break signalled, CPAAlgorithm will stop.");

            // add the new state
            reachedSet.add(successor, successorPrecision);

            if (!successors.isEmpty()) {
              // re-add the old state to the waitlist, there are unhandled
              // successors left that otherwise would be forgotten
              reachedSet.reAddToWaitlist(state);
            }

            return true;
          }
        }
        assert action == Action.CONTINUE : "Enum Action has unhandled values!";

        Collection<AbstractState> reached = reachedSet.getReached(successor);

        // An optimization, we don't bother merging if we know that the
        // merge operator won't do anything (i.e., it is merge-sep).
        if (mergeOperator != MergeSepOperator.getInstance() && !reached.isEmpty()) {
          stats.mergeTimer.start();
          try {
            List<AbstractState> toRemove = new ArrayList<>();
            List<Pair<AbstractState, Precision>> toAdd = new ArrayList<>();

            logger.log(Level.FINER, "Considering", reached.size(),
                "states from reached set for merge");
            for (AbstractState reachedState : reached) {
              AbstractState mergedState =
                  mergeOperator.merge(successor, reachedState,
                      successorPrecision);

              if (!mergedState.equals(reachedState)) {
                logger.log(Level.FINER,
                    "Successor was merged with state from reached set");
                logger.log(Level.ALL, "Merged", successor, "\nand",
                    reachedState, "\n-->", mergedState);
                stats.countMerge++;

                toRemove.add(reachedState);
                toAdd.add(Pair.of(mergedState, successorPrecision));
              }
            }
            reachedSet.removeAll(toRemove);
            reachedSet.addAll(toAdd);

            if(mergeOperator instanceof ARGMergeJoinPredicatedAnalysis){
              ((ARGMergeJoinPredicatedAnalysis)mergeOperator).cleanUp(reachedSet);
            }

          } finally {
            stats.mergeTimer.stop();
          }
        }

        stats.stopTimer.start();
        boolean stop;
        try {
          stop = stopOperator.stop(successor, reached, successorPrecision);
        } finally {
          stats.stopTimer.stop();
        }

        if (stop) {
          logger.log(Level.FINER,
              "Successor is covered or unreachable, not adding to waitlist");
          stats.countStop++;

        } else {
          logger.log(Level.FINER,
              "No need to stop, adding successor to waitlist");

          stats.addTimer.start();
          reachedSet.add(successor, successorPrecision);
          stats.addTimer.stop();
        }
      }
    }
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (forcedCovering instanceof StatisticsProvider) {
      ((StatisticsProvider)forcedCovering).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }
}
