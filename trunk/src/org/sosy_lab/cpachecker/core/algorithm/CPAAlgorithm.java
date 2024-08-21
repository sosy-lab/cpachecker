// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Functions;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCovering;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoinCPAEnabledAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatValue;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatHist;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class CPAAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer = new Timer();
    private Timer chooseTimer = new Timer();
    private Timer precisionTimer = new Timer();
    private Timer transferTimer = new Timer();
    private Timer mergeTimer = new Timer();
    private Timer stopTimer = new Timer();
    private Timer addTimer = new Timer();
    private Timer forcedCoveringTimer = new Timer();

    private int countIterations = 0;
    private int maxWaitlistSize = 0;
    private long countWaitlistSize = 0;
    private int countSuccessors = 0;
    private int maxSuccessors = 0;
    private int countMerge = 0;
    private int countStop = 0;
    private int countBreak = 0;

    private Map<String, AbstractStatValue> reachedSetStatistics = new HashMap<>();

    private void stopAllTimers() {
      totalTimer.stopIfRunning();
      chooseTimer.stopIfRunning();
      precisionTimer.stopIfRunning();
      transferTimer.stopIfRunning();
      mergeTimer.stopIfRunning();
      stopTimer.stopIfRunning();
      addTimer.stopIfRunning();
      forcedCoveringTimer.stopIfRunning();
    }

    private void updateReachedSetStatistics(Map<String, AbstractStatValue> newStatistics) {
      for (Entry<String, AbstractStatValue> e : newStatistics.entrySet()) {
        String key = e.getKey();
        AbstractStatValue val = e.getValue();
        if (!reachedSetStatistics.containsKey(key)) {
          reachedSetStatistics.put(key, val);
        } else {
          AbstractStatValue newVal = reachedSetStatistics.get(key);

          if (val == newVal) {
            // ignore, otherwise counters would double
          } else if (newVal instanceof StatCounter) {
            assert val instanceof StatCounter;
            ((StatCounter) newVal).mergeWith((StatCounter) val);
          } else if (newVal instanceof StatInt) {
            assert val instanceof StatInt;
            ((StatInt) newVal).add((StatInt) val);
          } else if (newVal instanceof StatHist) {
            assert val instanceof StatHist;
            ((StatHist) newVal).mergeWith((StatHist) val);
          } else {
            throw new AssertionError("Can't handle " + val.getClass().getSimpleName());
          }
        }
      }
    }

    @Override
    public String getName() {
      return "CPA algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println("Number of iterations:            " + countIterations);
      if (countIterations == 0) {
        // Statistics not relevant, prevent division by zero
        return;
      }

      out.println("Max size of waitlist:            " + maxWaitlistSize);
      out.println("Average size of waitlist:        " + countWaitlistSize / countIterations);
      StatisticsWriter w = StatisticsWriter.writingStatisticsTo(out);
      for (AbstractStatValue c : reachedSetStatistics.values()) {
        w.put(c);
      }
      out.println("Number of computed successors:   " + countSuccessors);
      out.println("Max successors for one state:    " + maxSuccessors);
      out.println("Number of times merged:          " + countMerge);
      out.println("Number of times stopped:         " + countStop);
      out.println("Number of times breaked:         " + countBreak);
      out.println();
      out.println(
          "Total time for CPA algorithm:     "
              + totalTimer
              + " (Max: "
              + totalTimer.getMaxTime().formatAs(TimeUnit.SECONDS)
              + ")");
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

  @Options(prefix = "cpa")
  public static class CPAAlgorithmFactory implements AlgorithmFactory {

    @Option(
        secure = true,
        description = "Which strategy to use for forced coverings (empty for none)",
        name = "forcedCovering")
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
    private ForcedCovering.@Nullable Factory forcedCoveringClass = null;

    @Option(
        secure = true,
        description =
            "Do not report 'False' result, return UNKNOWN instead. "
                + " Useful for incomplete analysis with no counterexample checking.")
    private boolean reportFalseAsUnknown = false;

    private final ForcedCovering forcedCovering;

    private final ConfigurableProgramAnalysis cpa;
    private final LogManager logger;
    private final ShutdownNotifier shutdownNotifier;

    public CPAAlgorithmFactory(
        ConfigurableProgramAnalysis cpa,
        LogManager logger,
        Configuration config,
        ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException {

      config.inject(this);
      this.cpa = cpa;
      this.logger = logger;
      shutdownNotifier = pShutdownNotifier;

      if (forcedCoveringClass != null) {
        forcedCovering = forcedCoveringClass.create(config, logger, cpa);
      } else {
        forcedCovering = null;
      }
    }

    @Override
    public CPAAlgorithm newInstance() {
      return new CPAAlgorithm(cpa, logger, shutdownNotifier, forcedCovering, reportFalseAsUnknown);
    }
  }

  public static CPAAlgorithm create(
      ConfigurableProgramAnalysis cpa,
      LogManager logger,
      Configuration config,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    return new CPAAlgorithmFactory(cpa, logger, config, pShutdownNotifier).newInstance();
  }

  private final ForcedCovering forcedCovering;

  private final CPAStatistics stats = new CPAStatistics();

  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;

  private final LogManager logger;

  private final ShutdownNotifier shutdownNotifier;

  private final AlgorithmStatus status;

  private CPAAlgorithm(
      ConfigurableProgramAnalysis cpa,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      ForcedCovering pForcedCovering,
      boolean pIsImprecise) {

    transferRelation = cpa.getTransferRelation();
    mergeOperator = cpa.getMergeOperator();
    stopOperator = cpa.getStopOperator();
    precisionAdjustment = cpa.getPrecisionAdjustment();
    this.logger = logger;
    shutdownNotifier = pShutdownNotifier;
    forcedCovering = pForcedCovering;
    status = AlgorithmStatus.SOUND_AND_PRECISE.withPrecise(!pIsImprecise);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    stats.totalTimer.start();
    try {
      return run0(reachedSet);
    } finally {
      stats.stopAllTimers();
      stats.updateReachedSetStatistics(reachedSet.getStatistics());
    }
  }

  private AlgorithmStatus run0(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    while (reachedSet.hasWaitingState()) {
      shutdownNotifier.shutdownIfNecessary();

      stats.countIterations++;

      // Pick next state using strategy
      // BFS, DFS or top sort according to the configuration
      int size = reachedSet.getWaitlist().size();
      if (size >= stats.maxWaitlistSize) {
        stats.maxWaitlistSize = size;
      }
      stats.countWaitlistSize += size;

      stats.chooseTimer.start();
      final AbstractState state = reachedSet.popFromWaitlist();
      final Precision precision = reachedSet.getPrecision(state);
      stats.chooseTimer.stop();

      logger.log(Level.FINER, "Retrieved state from waitlist");
      try {
        if (handleState(state, precision, reachedSet)) {
          // Prec operator requested break
          return status;
        }
      } catch (CPAException | InterruptedException e) {
        // re-add the old state to the waitlist, there might be unhandled successors left
        // that otherwise would be forgotten (which would be unsound)
        reachedSet.reAddToWaitlist(state);
        throw e;
      }
    }

    return status;
  }

  /**
   * Handle one state from the waitlist, i.e., produce successors etc.
   *
   * @param state The abstract state that was taken out of the waitlist
   * @param precision The precision for this abstract state.
   * @param reachedSet The reached set.
   * @return true if analysis should terminate, false if analysis should continue with next state
   */
  private boolean handleState(
      final AbstractState state, final Precision precision, final ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    logger.log(Level.ALL, "Current state is", state, "with precision", precision);

    if (forcedCovering != null) {
      stats.forcedCoveringTimer.start();
      try {
        boolean stop = forcedCovering.tryForcedCovering(state, precision, reachedSet);

        if (stop) {
          // TODO: remove state from reached set?
          return false;
        }
      } finally {
        stats.forcedCoveringTimer.stop();
      }
    }

    stats.transferTimer.start();
    Collection<? extends AbstractState> successors;
    try {
      successors = transferRelation.getAbstractSuccessors(state, precision);
    } finally {
      stats.transferTimer.stop();
    }
    // TODO When we have a nice way to mark the analysis result as incomplete,
    // we could continue analysis on a CPATransferException with the next state from waitlist.

    int numSuccessors = successors.size();
    logger.log(Level.FINER, "Current state has", numSuccessors, "successors");
    stats.countSuccessors += numSuccessors;
    stats.maxSuccessors = Math.max(numSuccessors, stats.maxSuccessors);

    for (Iterator<? extends AbstractState> it = successors.iterator(); it.hasNext(); ) {
      AbstractState successor = it.next();
      shutdownNotifier.shutdownIfNecessary();
      logger.log(Level.FINER, "Considering successor of current state");
      logger.log(Level.ALL, "Successor of", state, "\nis", successor);

      stats.precisionTimer.start();
      PrecisionAdjustmentResult precAdjustmentResult;
      try {
        Optional<PrecisionAdjustmentResult> precAdjustmentOptional =
            precisionAdjustment.prec(
                successor, precision, reachedSet, Functions.identity(), successor);
        if (!precAdjustmentOptional.isPresent()) {
          continue;
        }
        precAdjustmentResult = precAdjustmentOptional.orElseThrow();
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
          logger.log(Level.FINER, "Break was signalled but ignored because the state is covered.");
          continue;

        } else {
          stats.countBreak++;
          logger.log(Level.FINER, "Break signalled, CPAAlgorithm will stop.");

          // add the new state
          reachedSet.add(successor, successorPrecision);

          if (it.hasNext()) {
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
          try {
            logger.log(
                Level.FINER, "Considering", reached.size(), "states from reached set for merge");
            for (AbstractState reachedState : reached) {
              shutdownNotifier.shutdownIfNecessary();
              AbstractState mergedState =
                  mergeOperator.merge(successor, reachedState, successorPrecision);

              if (!mergedState.equals(reachedState)) {
                logger.log(Level.FINER, "Successor was merged with state from reached set");
                logger.log(
                    Level.ALL, "Merged", successor, "\nand", reachedState, "\n-->", mergedState);
                stats.countMerge++;

                toRemove.add(reachedState);
                toAdd.add(Pair.of(mergedState, successorPrecision));
              }
            }
          } finally {
            // If we terminate, we should still update the reachedSet if necessary
            // because ARGCPA doesn't like states in toRemove to be in the reachedSet.
            reachedSet.removeAll(toRemove);
            reachedSet.addAll(toAdd);
          }

          if (mergeOperator instanceof ARGMergeJoinCPAEnabledAnalysis) {
            ((ARGMergeJoinCPAEnabledAnalysis) mergeOperator).cleanUp(reachedSet);
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
        logger.log(Level.FINER, "Successor is covered or unreachable, not adding to waitlist");
        stats.countStop++;

      } else {
        logger.log(Level.FINER, "No need to stop, adding successor to waitlist");

        stats.addTimer.start();
        reachedSet.add(successor, successorPrecision);
        stats.addTimer.stop();
      }
    }

    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (forcedCovering instanceof StatisticsProvider) {
      ((StatisticsProvider) forcedCovering).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }
}
