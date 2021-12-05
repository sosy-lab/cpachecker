// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisCoreStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisFullStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysisCoreStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysisFullStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

public class ConcurrentStatisticsCollector implements StatisticsProvider, Statistics, Runnable {
  private final LogManager logManager;

  private final ShutdownNotifier shutdownNotifier;
  
  private Optional<Thread> thread = Optional.empty();

  private final Map<Block, Integer> forwardAnalysisCount = new HashMap<>();

  private final Map<Block, Integer> backwardAnalysisCount = new HashMap<>();

  private final BlockingQueue<TaskStatistics> pendingStatistics = new LinkedBlockingQueue<>();

  @SuppressWarnings("FieldMayBeFinal")
  @Option(secure = true, name = "sequence.file",
      description = "File into which to output task sequence information")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path sequenceFile = Path.of("output/sequence.txt");

  @SuppressWarnings("FieldMayBeFinal")
  @Option(secure = true, name = "forwardAnalysisCount.file",
      description = "File into which to output the number of forward analysis tasks per block.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path faCountFile = Path.of("output/count-forwardAnalysis.txt");

  @SuppressWarnings("FieldMayBeFinal")
  @Option(secure = true, name = "backwardAnalysisCount.file",
      description = "File into which to output the number of backward analysis tasks per block.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path baCountFile = Path.of("output/count-backwardAnalysis.txt");

  public ConcurrentStatisticsCollector(
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier) {
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public void run() {
    while(!shutdownNotifier.shouldShutdown()) {
      try {
        final TaskStatistics statistics = pendingStatistics.take();
        statistics.accept(this);
      } catch(final InterruptedException ignored) {
        /*
         * Ignored because surrounding loop already checks for shutdown request. 
         */
      }
    }
  }

  public void submitNewStatistics(final TaskStatistics pStatistics) {
    try {
      pendingStatistics.put(pStatistics);  
    } catch(final InterruptedException ignored) {
      logManager.log(WARNING, 
          "Interrupted while waiting to submit statistics; pending ones discarded.", 
          "Statistics will be incomplete.");
    }
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    final StatInt forwardAnalysisCountValues = new StatInt(StatKind.AVG, "FA Count");

    out.format("Forward Analysis Average Count: %f%n", forwardAnalysisCountValues.getAverage());
    out.format("Forward Analysis Max Count: %d%n", forwardAnalysisCountValues.getMaxValue());
    out.format("Forward Analysis Min Count: %d%n", forwardAnalysisCountValues.getMinValue());
  }

  @Override
  public @Nullable String getName() {
    return "Concurrent Analysis";
  }

  public void visit(final BackwardAnalysisFullStatistics pStatistics) {
    final Block target = pStatistics.getTarget();
    final int oldValue = backwardAnalysisCount.getOrDefault(target, 0);
    backwardAnalysisCount.put(target, oldValue + 1);
  }

  public void visit(@SuppressWarnings("unused") final BackwardAnalysisCoreStatistics pStatistics) {
  }

  public void visit(final ForwardAnalysisCoreStatistics pStatistics) {
  }

  public void visit(final ForwardAnalysisFullStatistics pStatistics) {
    final Block target = pStatistics.getTarget();
    final int oldValue = forwardAnalysisCount.getOrDefault(target, 0);
    forwardAnalysisCount.put(target, oldValue + 1);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    writeForwardAnalysisCount();
    writeBackwardAnalysisCount();
    writeSequenceInformation();
  }
  
  private void writeFile(final Path path, final String content, final String identifier) {
    try {
      try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {
        writer.write(content);
      }
    } catch (final IOException ignored) {
      logManager.log(WARNING, "Could not write", identifier, " (" + path + ").");
    }
  }
  
  private void writeForwardAnalysisCount() {
    String content = buildCountFileContent(forwardAnalysisCount);
    writeFile(faCountFile, content, "forward analysis count");
  }

  private void writeBackwardAnalysisCount() {
    String content = buildCountFileContent(backwardAnalysisCount);
    writeFile(baCountFile, content, "backward analysis count");
  }

  private String buildCountFileContent(final Map<Block, Integer> counts) {
    StringBuilder content = new StringBuilder();

    for (final Map.Entry<Block, Integer> entry : counts.entrySet()) {
      content.append(entry.getKey().getPrintableNodeList());
      content.append(": ");
      content.append(entry.getValue());
      content.append("\n");
    }
    
    return content.toString();
  }
  
  private void writeSequenceInformation() {

  }
  
  public void start() {
    thread = Optional.of(new Thread(this));
    thread.get().start();
  }

  public void stop() {
    assert shutdownNotifier.shouldShutdown();
    
    if(thread.isPresent()) {
      thread.get().interrupt();
    }
  }
  
  public interface TaskStatistics {
    void accept(final ConcurrentStatisticsCollector collector);
  }
}
