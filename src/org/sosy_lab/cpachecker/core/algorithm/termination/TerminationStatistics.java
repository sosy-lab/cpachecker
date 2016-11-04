/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.valueWithPercentage;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;

@Options(prefix = "termination")
public class TerminationStatistics implements Statistics {

  @Option(
      secure = true,
      description =
          "A human readable representation of the synthesized (non-)termination arguments is "
              + "exported to this file."
    )
  @FileOption(Type.OUTPUT_FILE)
  private Path resultFile = Paths.get("terminationAnalysisResult.txt");

  private final int totalLoops;

  private final Set<Loop> analysedLoops = Sets.newConcurrentHashSet();

  private final Timer totalTime = new Timer();

  private final Timer loopTime = new Timer();

  private final Timer recursionTime = new Timer();

  private final Timer safetyAnalysisTime = new Timer();

  private final Timer lassoTime = new Timer();

  private final Timer lassoConstructionTime = new Timer();

  private final Timer lassoNonTerminationTime = new Timer();

  private final Timer lassoTerminationTime = new Timer();

  private final Map<Loop, AtomicInteger> safetyAnalysisRunsPerLoop = Maps.newConcurrentMap();

  private final Map<Loop, AtomicInteger> lassosPerLoop = Maps.newConcurrentMap();

  private final AtomicInteger maxLassosPerIteration = new AtomicInteger();

  private final AtomicInteger lassosCurrentIteration = new AtomicInteger();

  private final Multimap<Loop, TerminationArgument> terminationArguments =
      MultimapBuilder.linkedHashKeys().arrayListValues().build();

  private final Map<Loop, NonTerminationArgument> nonTerminationArguments = Maps.newConcurrentMap();

  private final LogManager logger;

  public TerminationStatistics(
      Configuration pConfig, LogManager pLogger, int pTotalNumberOfLoops)
          throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = checkNotNull(pLogger);
    totalLoops = pTotalNumberOfLoops;
  }

  void algorithmStarted() {
    totalTime.start();
  }

  void algorithmFinished() {
    totalTime.stop();
    safetyAnalysisTime.stopIfRunning();
    lassoTime.stopIfRunning();
    loopTime.stopIfRunning();
  }

  void analysisOfLoopStarted(Loop pLoop) {
    boolean newLoop = analysedLoops.add(pLoop);
    checkState(newLoop);
    loopTime.start();
  }

  void analysisOfLoopFinished(Loop pLoop) {
    checkState(analysedLoops.contains(pLoop));
    loopTime.stop();
    recursionTime.stopIfRunning();
    safetyAnalysisTime.stopIfRunning();
    lassoTime.stopIfRunning();
    lassoConstructionTime.stopIfRunning();
    lassoNonTerminationTime.stopIfRunning();
    lassoTerminationTime.stopIfRunning();
  }

  void analysisOfRecursionStarted() {
    recursionTime.start();
  }

  void analysisOfRecursionFinished() {
    recursionTime.stop();
  }

  void safetyAnalysisStarted(Loop pLoop) {
    checkState(analysedLoops.contains(pLoop));
    safetyAnalysisRunsPerLoop.computeIfAbsent(pLoop, l -> new AtomicInteger()).incrementAndGet();
    safetyAnalysisTime.start();
  }

  void safetyAnalysisFinished(Loop pLoop) {
    checkState(analysedLoops.contains(pLoop));
    checkState(safetyAnalysisRunsPerLoop.containsKey(pLoop));
    safetyAnalysisTime.stop();
  }

  public void analysisOfLassosStarted() {
    lassoTime.start();
  }

  public void analysisOfLassosFinished() {
    lassoTime.stop();
    lassoConstructionTime.stopIfRunning();
    lassoNonTerminationTime.stopIfRunning();
    lassoTerminationTime.stopIfRunning();
    maxLassosPerIteration.accumulateAndGet(lassosCurrentIteration.getAndSet(0), Math::max);
  }

  public void lassoConstructionStarted() {
    lassoConstructionTime.start();
  }

  public void lassoConstructionFinished() {
    lassoConstructionTime.stop();
  }

  public void nonTerminationAnalysisOfLassoStarted() {
    lassoNonTerminationTime.start();
  }

  public void nonTerminationAnalysisOfLassoFinished() {
    lassoNonTerminationTime.stop();
  }

  public void terminationAnalysisOfLassoStarted() {
    lassoTerminationTime.start();
  }

  public void terminationAnalysisOfLassoFinished() {
    lassoTerminationTime.stop();
  }

  public void lassosConstructed(Loop pLoop, int numberOfLassos) {
    lassosPerLoop.computeIfAbsent(pLoop, l -> new AtomicInteger()).addAndGet(numberOfLassos);
    lassosCurrentIteration.addAndGet(numberOfLassos);
  }

  public void synthesizedTerminationArgument(Loop pLoop, TerminationArgument pTerminationArgument) {
    checkState(analysedLoops.contains(pLoop));
    terminationArguments.put(pLoop, pTerminationArgument);
  }

  public void synthesizedNonTerminationArgument(
      Loop pLoop, NonTerminationArgument pNonTerminationArgument) {
    nonTerminationArguments.put(pLoop, pNonTerminationArgument);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("Total time :                                        " + totalTime);
    pOut.println("Time for recursion analysis:                        " + recursionTime);
    pOut.println();

    int loops = analysedLoops.size();
    pOut.println("Number of analysed loops:                               " + valueWithPercentage(loops, totalLoops));
    pOut.println("Total time for loop analysis:                       " + loopTime);
    pOut.println("  Avg time per loop analysis:                       " + format(loopTime.getAvgTime()));
    pOut.println("  Max time per loop analysis:                       " + format(loopTime.getMaxTime()));
    pOut.println();

    int safetyAnalysisRuns =
        safetyAnalysisRunsPerLoop.values().stream().mapToInt(AtomicInteger::get).sum();
    assert safetyAnalysisRuns == safetyAnalysisTime.getNumberOfIntervals();
    int maxSafetyAnalysisRuns =
        safetyAnalysisRunsPerLoop.values().stream().mapToInt(AtomicInteger::get).max().orElse(0);
    String loopsWithMaxSafetyAnalysisRuns =
        safetyAnalysisRunsPerLoop
        .entrySet()
        .stream()
        .filter(e -> e.getValue().get() == maxSafetyAnalysisRuns)
        .map(Entry::getKey)
        .map(l -> l.getLoopHeads().toString())
        .collect(Collectors.joining(", "));
    pOut.println("Number of safety analysis runs:                     " + format(safetyAnalysisRuns));
    if (loops > 0) {
      pOut.println("  Avg safety analysis run per loop:                 " + div(safetyAnalysisRuns, loops));
    }
    pOut.println("  Max safety analysis run per loop:                 " + format(maxSafetyAnalysisRuns) + " \t for loops " + loopsWithMaxSafetyAnalysisRuns);

    pOut.println("Total time for safety analysis:                     " + safetyAnalysisTime);
    pOut.println("  Avg time per safety analysis run:                 " + format(safetyAnalysisTime.getAvgTime()));
    pOut.println("  Max time per safety analysis run:                 " + format(safetyAnalysisTime.getMaxTime()));
    pOut.println();

    int iterations = lassoTime.getNumberOfIntervals();
    int lassos = lassosPerLoop.values().stream().mapToInt(AtomicInteger::get).sum();
    int maxLassosPerLoop = lassosPerLoop.values().stream().mapToInt(AtomicInteger::get).max().orElse(0);
    String loopsWithMaxLassos =
        lassosPerLoop
        .entrySet()
        .stream()
        .filter(e -> e.getValue().get() == maxLassosPerLoop)
        .map(Entry::getKey)
        .map(l -> l.getLoopHeads().toString())
        .collect(Collectors.joining(", "));
    pOut.println("Number of analysed lassos:                          " + format(lassos));
    if (loops > 0) {
      pOut.println("  Avg number of lassos per loop:                    " + div(lassos, loops));
    }
    pOut.println("  Max number of lassos per loop:                    " + format(maxLassosPerLoop) + " \t for loops " + loopsWithMaxLassos);
    if (loops > 0) {
      pOut.println("  Avg number of lassos per iteration:               " + div(lassos, iterations));
    }
    pOut.println("  Max number of lassos per iteration:               " + format(maxLassosPerIteration.get()));
    pOut.println();

    pOut.println("Total time for lassos analysis:                     " + lassoTime);
    pOut.println("  Avg time per iteration:                           " + format(lassoTime.getAvgTime()));
    pOut.println("  Max time per iteration:                           " + format(lassoTime.getMaxTime()));
    pOut.println("  Time for lassos construction:                     " + lassoConstructionTime);
    pOut.println("    Avg time for lasso construction per iteration:  " + format(lassoConstructionTime.getAvgTime()));
    pOut.println("    Max time for lasso construction per iteration:  " + format(lassoConstructionTime.getMaxTime()));
    pOut.println("  Total time for non-termination analysis:          " + lassoNonTerminationTime);
    pOut.println("    Avg time for non-termination analysis per lasso:" + format(lassoNonTerminationTime.getAvgTime()));
    pOut.println("    Max time for non-termination analysis per lasso:" + format(lassoNonTerminationTime.getMaxTime()));
    pOut.println("  Total time for termination analysis:              " + lassoTerminationTime);
    pOut.println("    Avg time for termination analysis per lasso:    " + format(lassoTerminationTime.getAvgTime()));
    pOut.println("    Max time for termination analysis per lasso:    " + format(lassoTerminationTime.getMaxTime()));
    pOut.println();

    int totoalTerminationArguments = terminationArguments.size();
    int maxTerminationArgumentsPerLoop =
        terminationArguments.asMap().values().stream().mapToInt(Collection::size).max().orElse(0);
    String loopsWithMaxTerminationArguments =
        terminationArguments
        .asMap()
        .entrySet()
        .stream()
        .filter(e -> e.getValue().size() == maxTerminationArgumentsPerLoop)
        .map(Entry::getKey)
        .map(l -> l.getLoopHeads().toString())
        .collect(Collectors.joining(", "));
    pOut.println("Total number of termination arguments:              " + format(totoalTerminationArguments));
    if (loops > 0) {
      pOut.println("  Avg termination arguments per loop:               " + div(totoalTerminationArguments, loops));
    }
    pOut.println("  Max termination arguments per loop:               " + format(maxTerminationArgumentsPerLoop) + " \t for loops " + loopsWithMaxTerminationArguments);


    pOut.println();
    Map<String, Integer> terminationArguementTypes = Maps.newHashMap();
    for (TerminationArgument terminationArgument : terminationArguments.values()) {
      String name = terminationArgument.getRankingFunction().getName();
      terminationArguementTypes.merge(name, 1, Integer::sum);
    }

    for (Entry<String, Integer> terminationArgument : terminationArguementTypes.entrySet()) {
      String name = terminationArgument.getKey();
      String whiteSpaces = Strings.repeat(" ", 49 - name.length());
      pOut.println("  " + name + ":" + whiteSpaces + format(terminationArgument.getValue()));
    }

    exportSynthesizedArguments();
  }

  private void exportSynthesizedArguments() {
    if (resultFile != null) {
      logger.logf(FINER, "Writing result of termination analysis into %s.", resultFile);

      try (Writer writer = MoreFiles.openOutputFile(resultFile, UTF_8)) {
        writer.append("Non-termination arguments:\n");
        for (Entry<Loop, NonTerminationArgument> nonTerminationArgument :
            nonTerminationArguments.entrySet()) {
          writer.append(nonTerminationArgument.getKey().toString());
          writer.append(":\n");
          writer.append(nonTerminationArgument.getValue().toString());
          writer.append('\n');
        }

        writer.append("\n\nTermination arguments:\n");
        for (Loop loop : terminationArguments.keySet()) {
          for (TerminationArgument terminationArgument : terminationArguments.get(loop)) {
            writer.append(loop.toString());
            writer.append(":\n");
            writer.append(terminationArgument.toString());
            writer.append('\n');
          }
          writer.append('\n');
        }

      } catch (IOException e) {
        logger.logException(WARNING, e, "Could not export (non-)termination arguments.");
      }
    }
  }

  @Override
  public @Nullable String getName() {
    return "Termination Algorithm";
  }

  private static String format(TimeSpan pTimeSpan) {
    return pTimeSpan.formatAs(SECONDS);
  }

  private static String format(int value) {
    return String.format("%5d", value);
  }

  private static String div(double val, double full) {
    return String.format("%8.2f", val / full);
  }
}
