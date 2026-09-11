// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import java.io.PrintStream;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.witnesses.LocationWitnessExporter;

/**
 * Uses the standard YAML witness exporters with the location summary prepared by the trivial rules.
 */
class TrivialRulesWitnessExporter extends LocationWitnessExporter {

  private final TrivialRulesStatistics stats;

  TrivialRulesWitnessExporter(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa,
      TrivialRulesStatistics pStats)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa, pSpecification, pCfa);
    stats = pStats;
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (!isYamlWitnessExportEnabled() || stats.verdict() == null) {
      return;
    }
    if (pResult == Result.FALSE) {
      if (stats.verdict().violatingEdge() == null) {
        logger.log(
            Level.WARNING,
            "Cannot export a violation witness because the violated location is unknown.");
        return;
      }
    }
    writeYamlWitnesses(pResult, pReached);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    // This class only writes output files.
  }

  @Override
  public @Nullable String getName() {
    return null;
  }
}
