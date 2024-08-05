// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
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

@Options(prefix = "cpa.smg2")
public class SMGCPAStatistics implements Statistics {

  @Option(secure = true, description = "target file to hold the exported precision")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path precisionFile = null;

  private LongAdder iterations = new LongAdder();
  private StatCounter assumptions = new StatCounter("Number of assumptions");
  private StatCounter deterministicAssumptions =
      new StatCounter("Number of deterministic assumptions");
  private final SMGCPA cpa;

  public SMGCPAStatistics(SMGCPA cpa, Configuration config) throws InvalidConfigurationException {
    this.cpa = cpa;

    config.inject(this, SMGCPAStatistics.class);
  }

  @Override
  public String getName() {
    return "SMGCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatInt numberOfVariables = new StatInt(StatKind.AVG, "Number of variables per state");
    StatInt numberOfGlobalVariables =
        new StatInt(StatKind.AVG, "Number of global variables per state");

    for (AbstractState currentAbstractState : reached) {
      SMGState currentState =
          AbstractStates.extractStateByType(currentAbstractState, SMGState.class);

      numberOfVariables.setNextValue(currentState.getSize());
      numberOfGlobalVariables.setNextValue(currentState.getNumberOfGlobalVariables());
    }

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put(numberOfVariables);
    writer.put(numberOfGlobalVariables);

    if (precisionFile != null) {
      exportPrecision(reached);
    }

    writer
        .put(assumptions)
        .put(deterministicAssumptions)
        .put("Level of Determinism", getCurrentLevelOfDeterminism() + "%");
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
      cpa.getLogger().logUserException(Level.WARNING, e, "Could not write SMG precision to file");
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
