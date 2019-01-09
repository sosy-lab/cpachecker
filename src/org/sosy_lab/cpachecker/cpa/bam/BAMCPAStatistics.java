/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.sosy_lab.cpachecker.util.statistics.StatHist;

/** Prints some BAM related statistics */
@Options(prefix = "cpa.bam")
class BAMCPAStatistics implements Statistics {

  @Option(secure = true, description = "file for exporting detailed statistics about blocks")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path blockStatisticsFile = Paths.get("block_statistics.txt");

  private final LogManager logger;
  private final AbstractBAMCPA cpa;
  private List<BAMBasedRefiner> refiners = new ArrayList<>();

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

  public void addRefiner(BAMBasedRefiner pRefiner) {
    refiners.add(pRefiner);
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

    for (BAMBasedRefiner refiner : refiners) {
      // TODO We print these statistics also for use-cases of BAM-refiners, that never use timers.
      // Can we ignore them?
      out.println("\n" + refiner.getClass().getSimpleName() + ":");
      put(out, 1, refiner.computePathTimer);
      put(out, 1, refiner.computeSubtreeTimer);
      put(out, 1, refiner.computeCounterexampleTimer);
      put(out, 1, refiner.removeCachedSubtreeTimer);
    }

    writeBlockStatistics(out);
  }

  private void writeBlockStatistics(PrintStream out) {

    if (timeForBlock.isEmpty()) {
      return;
    }

    out.println("\nBlock statistics:");

    // collect data
    StatHist allTimers = new StatHist("time for block");
    for (Entry<Block, Timer> entry : timeForBlock.entrySet()) {
      Timer timer = entry.getValue();
      timer.stopIfRunning();
      allTimers.insertValue(timer.getSumTime().asMillis());
    }

    // write data
    put(out, "Analyzed blocks", timeForBlock.size());
    put(out, "Avg. time for block analysis", ofMillis((long) allTimers.getAvg()));
    put(out, "Mean time for block analysis", ofMillis(allTimers.getMean()));
    put(out, "Min time for block analysis", ofMillis(allTimers.getMin()));
    put(out, "Max time for block analysis", ofMillis(allTimers.getMax()));
    put(out, "StdDev time for block analysis", ofMillis((long) allTimers.getStdDeviation()));
    put(out, "Total time for block analysis", ofMillis((long) allTimers.getSum()));

    if (timeForBlock.containsKey(cpa.getBlockPartitioning().getMainBlock())) {
      put(
          out,
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
        w.write("start; end; #locations; #variables; sumtime; maxtime; avgtime; #intervals");
        w.write("\n");
        for (Entry<Block, Timer> entry : timeForBlock.entrySet()) {
          Block block = entry.getKey();
          Timer timer = entry.getValue();
          w.write(
              String.format(
                  "%s; %s; %s; %s; %s; %s; %s; %s",
                  block.getCallNodes(),
                  block.getReturnNodes(),
                  block.getNodes().size(),
                  block.getVariables().size(),
                  timer.getSumTime(),
                  timer.getMaxTime(),
                  timer.getAvgTime(),
                  timer.getNumberOfIntervals()));
          w.write("\n");
        }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not export block statistics");
        // ignore exception and continue analysis
      }
    }
  }
}
