// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatHist;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;

/** Prints some BAM related statistics */
@Options(prefix = "cpa.bam")
@SuppressWarnings("deprecation")
class BAMCPAStatistics implements Statistics {

  // stats about refinement
  final ThreadSafeTimerContainer computePathTimer =
      new ThreadSafeTimerContainer("Compute path for refinement");
  final ThreadSafeTimerContainer computeSubtreeTimer =
      new ThreadSafeTimerContainer("Constructing flat ARG");
  final ThreadSafeTimerContainer computeCounterexampleTimer =
      new ThreadSafeTimerContainer("Searching path to error location");
  final ThreadSafeTimerContainer removeCachedSubtreeTimer =
      new ThreadSafeTimerContainer("Removing cached subtrees");

  final StatCounter refinementWithMissingBlocks =
      new StatCounter("Number of refinements with a missing block");
  final StatCounter startedRefinements = new StatCounter("Number of started refinements");
  final StatCounter spuriousCex = new StatCounter("Number of spurious counterexamples");
  final StatCounter preciseCex = new StatCounter("Number of precise counterexamples");

  final StatCounter algorithmInstances = new StatCounter("Number of created nested algortihms");
  final StatHist depthsOfTargetStates =
      new StatHist("Nesting level of target states with caching") {
        @Override
        public String toString() {
          return String.format(
              "%.2f (#=%d, min=%d, max=%d, hist=%s)",
              getAvg(), getUpdateCount(), getMin(), getMax(), hist);
        }
      };
  final StatHist depthsOfFoundTargetStates =
      new StatHist("Nesting level of target states without caching") {
        @Override
        public String toString() {
          return String.format(
              "%.2f (#=%d, min=%d, max=%d, hist=%s)",
              getAvg(), getUpdateCount(), getMin(), getMax(), hist);
        }
      };

  @Option(secure = true, description = "file for exporting detailed statistics about blocks")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path blockStatisticsFile = Path.of("block_statistics.txt");

  private final LogManager logger;
  private final AbstractBAMCPA cpa;

  private int maxRecursiveDepth = 0;
  private final Map<Block, Timer> timeForBlock = new LinkedHashMap<>();

  public BAMCPAStatistics(Configuration pConfig, LogManager pLogger, AbstractBAMCPA pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    cpa = pCpa;
  }

  @Override
  public String getName() {
    return "BAMCPA";
  }

  void updateBlockNestingLevel(int newLevel) {
    maxRecursiveDepth = Math.max(newLevel, maxRecursiveDepth);
  }

  /** to be called before and after executing a transfer relation for a block in (sequential) BAM */
  void switchBlock(Block oldBlock, Block newBlock) {
    timeForBlock.computeIfAbsent(oldBlock, b -> new Timer()).stopIfRunning();
    timeForBlock.computeIfAbsent(newBlock, b -> new Timer()).start();
    // TODO how and when can we start and stop the timer of the main-block?
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

    put(out, "Number of blocks", cpa.getBlockPartitioning().getBlocks().size());
    put(out, "Max reached nesting level", maxRecursiveDepth);
    put(out, "Time for building block partitioning", cpa.blockPartitioningTimer);
    put(out, 0, cpa.reducerStatistics.reduceTime);
    put(out, 0, cpa.reducerStatistics.expandTime);
    put(out, 0, cpa.reducerStatistics.reducePrecisionTime);
    put(out, 0, cpa.reducerStatistics.expandPrecisionTime);
    put(out, 0, algorithmInstances);
    if (depthsOfTargetStates.getUpdateCount() > 0) {
      put(out, 0, depthsOfTargetStates);
      put(out, 0, depthsOfFoundTargetStates);
    }

    out.println("\nBAM-based Refinement:");
    put(out, 1, computePathTimer);
    put(out, 1, computeSubtreeTimer);
    put(out, 1, computeCounterexampleTimer);
    put(out, 1, removeCachedSubtreeTimer);
    put(out, 1, refinementWithMissingBlocks);
    put(out, 1, startedRefinements);
    put(out, 1, spuriousCex);
    put(out, 1, preciseCex);

    writeBlockStatistics(out);
  }

  private void writeBlockStatistics(PrintStream out) {

    if (timeForBlock.isEmpty()) {
      return;
    }

    out.println("\nBlock statistics:");

    // collect data
    StatHist allTimers = new StatHist("time for block");
    for (Timer timer : timeForBlock.values()) {
      timer.stopIfRunning();
      allTimers.insertValue(timer.getSumTime().asMillis());
    }

    // write data
    put(out, 1, "Analyzed blocks", timeForBlock.size());
    put(out, 1, "Avg. time for block analysis", ofMillis((long) allTimers.getAvg()));
    put(out, 1, "Mean time for block analysis", ofMillis(allTimers.getMean()));
    put(out, 1, "Min time for block analysis", ofMillis(allTimers.getMin()));
    put(out, 1, "Max time for block analysis", ofMillis(allTimers.getMax()));
    put(out, 1, "StdDev time for block analysis", ofMillis((long) allTimers.getStdDeviation()));
    put(out, 1, "Total time for block analysis", ofMillis((long) allTimers.getSum()));

    if (timeForBlock.containsKey(cpa.getBlockPartitioning().getMainBlock())) {
      put(
          out,
          1,
          "Time for main block",
          String.format(
              "%s (%s)",
              timeForBlock.get(cpa.getBlockPartitioning().getMainBlock()),
              cpa.getBlockPartitioning().getMainBlock().getCallNodes()));
    }
  }

  private String ofMillis(long millis) {
    return TimeSpan.ofMillis(millis).formatAs(TimeUnit.SECONDS);
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {

    // write block data as CSV
    if (blockStatisticsFile != null) {
      try (Writer w = IO.openOutputFile(blockStatisticsFile, Charset.defaultCharset())) {
        w.write(
            "start; end; #locations; #variables; sumtime; maxtime; avgtime; #intervals;"
                + " variables;");
        w.write("\n");
        for (Entry<Block, Timer> entry : timeForBlock.entrySet()) {
          Block block = entry.getKey();
          Timer timer = entry.getValue();
          w.write(
              String.format(
                  "%s; %s; %s; %s; %s; %s; %s; %s; %s;",
                  block.getCallNodes(),
                  block.getReturnNodes(),
                  block.getNodes().size(),
                  block.getVariables().size(),
                  timer.getSumTime(),
                  timer.getMaxTime(),
                  timer.getAvgTime(),
                  timer.getNumberOfIntervals(),
                  block.getVariables()));
          w.write("\n");
        }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not export block statistics");
        // ignore exception and continue analysis
      }
    }
  }
}
