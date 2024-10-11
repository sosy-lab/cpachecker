// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.value")
public class ValueAnalysisCPAStatistics implements Statistics {

  @Option(secure = true, description = "target file to hold the exported precision")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path precisionFile = null;

  @Option(secure = true, description = "target file to hold the exported loop invariants")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path loopInvariantsFile = null;

  private enum LoopInvExport {
    ALWAYS,
    IF_NOT_FALSE,
    IF_TRUE
  }

  @Option(secure = true, description = "configure when to export loop invariants")
  private LoopInvExport exportLoopInvariants = LoopInvExport.IF_TRUE;

  private LongAdder iterations = new LongAdder();
  private StatCounter assumptions = new StatCounter("Number of assumptions");
  private StatCounter deterministicAssumptions =
      new StatCounter("Number of deterministic assumptions");
  private final ValueAnalysisCPA cpa;
  private final LogManager logger;
  private final ValueAnalysisResultToLoopInvariants loopInvGenExporter;

  public ValueAnalysisCPAStatistics(
      ValueAnalysisCPA cpa,
      final CFA cfa,
      Configuration config,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    this.cpa = cpa;
    logger = pLogger;
    if (loopInvariantsFile != null) {
      loopInvGenExporter =
          new ValueAnalysisResultToLoopInvariants(
              cfa.getAllLoopHeads().orElse(null), config, logger, pShutdownNotifier, cfa);
    } else {
      loopInvGenExporter = null;
    }

    config.inject(this, ValueAnalysisCPAStatistics.class);
  }

  @Override
  public String getName() {
    return "ValueAnalysisCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatInt numberOfVariables = new StatInt(StatKind.AVG, "Number of variables per state");
    StatInt numberOfGlobalVariables =
        new StatInt(StatKind.AVG, "Number of global variables per state");

    for (AbstractState currentAbstractState : reached) {
      ValueAnalysisState currentState =
          AbstractStates.extractStateByType(currentAbstractState, ValueAnalysisState.class);

      numberOfVariables.setNextValue(currentState.getSize());
      numberOfGlobalVariables.setNextValue(currentState.getNumberOfGlobalVariables());
    }

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put(numberOfVariables);
    writer.put(numberOfGlobalVariables);

    if (precisionFile != null) {
      exportPrecision(reached);
    }

    if (loopInvariantsFile != null && shouldExportLoopInvariants(result)) {
      try (Writer w = IO.openOutputFile(loopInvariantsFile, Charset.defaultCharset())) {
        loopInvGenExporter.generateAndExportLoopInvariantsAsPredicatePrecision(reached, w);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write loop invariants to file");
      }
    }

    writer
        .put(assumptions)
        .put(deterministicAssumptions)
        .put("Level of Determinism", getCurrentLevelOfDeterminism() + "%");
  }

  private boolean shouldExportLoopInvariants(final Result result) {
    return switch (exportLoopInvariants) {
      case ALWAYS -> true;
      case IF_NOT_FALSE -> result != Result.FALSE;
      case IF_TRUE -> result == Result.TRUE;
    };
  }

  /**
   * This method exports the precision to file.
   *
   * @param reached the set of reached states.
   */
  private void exportPrecision(UnmodifiableReachedSet reached) {
    VariableTrackingPrecision consolidatedPrecision =
        VariableTrackingPrecision.joinVariableTrackingPrecisionsInReachedSet(reached);
    try (Writer writer = IO.openOutputFile(precisionFile, Charset.defaultCharset())) {
      consolidatedPrecision.serialize(writer);
    } catch (IOException e) {
      cpa.getLogger()
          .logUserException(Level.WARNING, e, "Could not write value-analysis precision to file");
    }
  }

  void incrementIterations() {
    iterations.increment();
  }

  void incrementAssumptions() {
    assumptions.inc();
  }

  void incrementDeterministicAssumptions() {
    assumptions.inc();
  }

  int getCurrentNumberOfIterations() {
    return iterations.intValue();
  }

  int getCurrentLevelOfDeterminism() {
    if (assumptions.getValue() == 0) {
      return 100;
    } else {
      return (int)
          Math.round((deterministicAssumptions.getValue() * 100) / (double) assumptions.getValue());
    }
  }
}
