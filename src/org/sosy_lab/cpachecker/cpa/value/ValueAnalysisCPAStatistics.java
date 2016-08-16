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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Level;

@Options(prefix="cpa.value")
public class ValueAnalysisCPAStatistics implements Statistics {

  @Option(secure=true, description="target file to hold the exported precision")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path precisionFile = null;

  private final ValueAnalysisCPA cpa;

  public ValueAnalysisCPAStatistics(ValueAnalysisCPA cpa, Configuration config) throws InvalidConfigurationException {
    this.cpa = cpa;

    config.inject(this, ValueAnalysisCPAStatistics.class);
  }

  @Override
  public String getName() {
    return "ValueAnalysisCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    StatInt numberOfVariables       = new StatInt(StatKind.COUNT, "Number of variables");
    StatInt numberOfGlobalVariables = new StatInt(StatKind.COUNT, "Number of global variables");

    for (AbstractState currentAbstractState : reached) {
      ValueAnalysisState currentState = AbstractStates.extractStateByType(currentAbstractState, ValueAnalysisState.class);

      numberOfVariables.setNextValue(currentState.getSize());
      numberOfGlobalVariables.setNextValue(currentState.getNumberOfGlobalVariables());
    }

    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put(numberOfVariables);
    writer.put(numberOfGlobalVariables);

    if (precisionFile != null) {
      exportPrecision(reached);
    }
  }

  /**
   * This method exports the precision to file.
   *
   * @param reached the set of reached states.
   */
  private void exportPrecision(ReachedSet reached) {
    VariableTrackingPrecision consolidatedPrecision =
        VariableTrackingPrecision.joinVariableTrackingPrecisionsInReachedSet(reached);
    try (Writer writer = MoreFiles.openOutputFile(precisionFile, Charset.defaultCharset())) {
      consolidatedPrecision.serialize(writer);
    } catch (IOException e) {
      cpa.getLogger().logUserException(Level.WARNING, e, "Could not write value-analysis precision to file");
    }
  }
}
