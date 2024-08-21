// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.splitter.SplitInfoState;
import org.sosy_lab.cpachecker.cpa.splitter.SplitterCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "program.splitter")
public class ProgramSplitAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  @Option(
      secure = true,
      name = "exportAsCondition",
      description = "export program splitting as conditions (assumption automata)")
  private boolean exportCondition = true;

  @Option(secure = true, name = "conditionFile", description = "where to export conditions")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate conditionPathFile = PathTemplate.ofFormatString("Condition.%d.txt");

  private final LogManager logger;
  private final Algorithm innerAlgorithm;
  private final int numSplits;
  private final ShutdownNotifier shutdownNotifier;

  private final Timer determineSplitTime = new Timer();
  private final Timer extractSplitTime = new Timer();

  private final List<Set<ARGState>> splitConditions;

  private boolean extractionComplete = false;

  public ProgramSplitAlgorithm(
      final Algorithm pAlgorithm,
      final ConfigurableProgramAnalysis pCpa,
      final Configuration pConfig,
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
    innerAlgorithm = pAlgorithm;
    shutdownNotifier = pShutdownNotifier;

    SplitterCPA splitterCPA = CPAs.retrieveCPAOrFail(pCpa, SplitterCPA.class, getClass());
    numSplits = splitterCPA.getMaximalSplitNumber();
    splitConditions = new ArrayList<>(numSplits);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(
        pReachedSet.getFirstState() instanceof ARGState,
        "ProgramSplitAlgorithm requires ARG state as top level state.");

    logger.log(Level.INFO, "Determining program splitting.");

    determineSplitTime.start();
    try {
      innerAlgorithm.run(pReachedSet);
    } finally {
      determineSplitTime.stop();
    }

    if (pReachedSet.hasWaitingState()) {
      logger.log(
          Level.WARNING,
          "The computation of the program splitting is not complete. The result remains a proper"
              + " splitting, but may not be the expected one.");
    }

    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.INFO, "Extract description of each part of the program split.");
    extractSplitTime.start();
    try {
      extractSplitConditions(pReachedSet);
    } finally {
      extractSplitTime.stop();
    }

    // Algorithm is never meant to prove or falsify a property, only does splitting for later
    // (parallel) conditional model checking
    return AlgorithmStatus.SOUND_AND_IMPRECISE;
  }

  private void extractSplitConditions(final ReachedSet pReached) throws InterruptedException {
    extractionComplete = false;
    for (int i = 0; i < numSplits; i++) {
      int index = i;
      shutdownNotifier.shutdownIfNecessary();

      splitConditions.add(
          new HashSet<>(
              FluentIterable.from(pReached)
                  .filter(
                      state ->
                          AbstractStates.extractStateByType(state, SplitInfoState.class)
                              .isInSplit(index))
                  .filter(ARGState.class)
                  .toSet()));
    }
    extractionComplete = true;
  }

  @Override
  public void printStatistics(
      final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
    StatisticsWriter statsWriter = StatisticsWriter.writingStatisticsTo(pOut);

    statsWriter.put("Number of splits", numSplits);
    statsWriter.put("Time to determine split", determineSplitTime);
    statsWriter.put("Time to extract split", extractSplitTime);
  }

  @Override
  public void writeOutputFiles(final Result pResult, final UnmodifiableReachedSet pReached) {
    if (extractionComplete) {
      if (exportCondition) {
        exportSplitConditionsToFiles((ARGState) pReached.getFirstState());
      }
    } else {
      logger.log(
          Level.INFO, "Extraction of program splitting incomplete. Do not extract the splitting.");
    }
  }

  private void exportSplitConditionsToFiles(final ARGState pInitialState) {
    Path condFile;

    try {
      int i = 0;
      for (Set<ARGState> splitConditionStates : splitConditions) {
        condFile = conditionPathFile.getPath(++i);
        condFile = condFile.resolveSibling(condFile.getFileName() + ".gz");

        Set<AbstractState> falseAssumptionStates =
            new HashSet<>(
                FluentIterable.from(splitConditionStates)
                    .filter(state -> state.getChildren().isEmpty())
                    .toSet());

        IO.writeGZIPFile(
            condFile,
            Charset.defaultCharset(),
            (Appender)
                appendable ->
                    AssumptionCollectorAlgorithm.writeAutomaton(
                        appendable,
                        pInitialState,
                        splitConditionStates,
                        falseAssumptionStates,
                        0,
                        true,
                        false));
      }
    } catch (IOException e) {
      logger.log(
          Level.SEVERE,
          "Failed to write a condition reflecting a part of program split. Abort writting"
              + " conditions describing program splitting.",
          e);
    }
  }

  @Override
  public @Nullable String getName() {
    return "Program Splitter";
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
    if (innerAlgorithm instanceof Statistics) {
      pStatsCollection.add((Statistics) innerAlgorithm);
    }
  }
}
