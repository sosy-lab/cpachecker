// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.witnesses.LocationWitnessExporter;

/**
 * Uses the standard YAML witness exporters with the location summary prepared by the trivial rules.
 *
 * <p>A witness is written only if a rule has decided the task, i.e. only if the reached set was
 * prepared by one of the {@code prepare...} methods; the answer UNKNOWN leaves the reached set
 * empty and there is nothing to export.
 */
class TrivialRulesWitnessExporter extends LocationWitnessExporter {

  private boolean decided = false;

  TrivialRulesWitnessExporter(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa, pSpecification, pCfa);
  }

  @Override
  public void prepareCorrectnessWitness(
      ReachedSet pReached, Multimap<CFANode, ImmutableList<ExpressionTreeReportingState>> pInvars) {
    super.prepareCorrectnessWitness(pReached, pInvars);
    decided = true;
  }

  @Override
  public void prepareViolationWitness(
      ReachedSet pReached, CFAEdge pEdge, DummyTargetState pTarget) {
    super.prepareViolationWitness(pReached, pEdge, pTarget);
    decided = true;
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (isYamlWitnessExportEnabled() && decided) {
      writeYamlWitnesses(pResult, pReached);
    }
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
