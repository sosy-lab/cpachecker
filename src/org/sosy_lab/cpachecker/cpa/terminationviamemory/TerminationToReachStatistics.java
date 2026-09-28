// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkState;
import static java.util.logging.Level.WARNING;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.PrintStream;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.NonterminationCounterexampleToWitness;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.TerminationArgumentsToWitnessUtils;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.TerminationYAMLWitnessExporter;

public class TerminationToReachStatistics extends ARGStatistics implements Statistics {
  private ImmutableSet<Loop> nonterminatingLoops = null;
  private final TerminationYAMLWitnessExporter terminationWitnessExporter;
  private FormulaManagerView fmgr;
  private BooleanFormulaManagerView bfmgr;
  private NonterminationCounterexampleToWitness nonterminationWitnessExporter;
  private Scope scope;

  public TerminationToReachStatistics(
      Configuration pConfig, LogManager pLogger, CFA pCFA, ConfigurableProgramAnalysis pCPA)
      throws InvalidConfigurationException {
    super(
        pConfig,
        pLogger,
        pCPA,
        Specification.alwaysSatisfied()
            .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION)),
        pCFA);

    scope = new CProgramScope(pCFA, pLogger);
    terminationWitnessExporter =
        new TerminationYAMLWitnessExporter(
            pConfig,
            pCFA,
            Specification.alwaysSatisfied()
                .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION)),
            pLogger);
    nonterminationWitnessExporter =
        new NonterminationCounterexampleToWitness(
            pConfig,
            pCFA,
            Specification.alwaysSatisfied()
                .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION)),
            pLogger);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    if (nonterminationWitnessExporter.isExportEnabled() && pResult == Result.FALSE) {
      int uniqueId = 0;
      for (CounterexampleInfo info : getAllCounterexamples(pReached).values()) {
        try {
          nonterminationWitnessExporter.export(
              info, nonterminationWitnessExporter.getOutputFileTemplate(), uniqueId);
        } catch (IOException e) {
          logger.logUserException(
              WARNING, e, "There is a problem when writing the witness into a file.");
        }
        uniqueId++;
      }
    }

    if (terminationWitnessExporter.isExportEnabled() && pResult == Result.TRUE) {
      try {
        terminationWitnessExporter.export(
            TerminationArgumentsToWitnessUtils.getTransitionInvariantsFromARG(
                pReached, cfa, fmgr, bfmgr, scope));
      } catch (IOException e) {
        logger.logUserException(
            WARNING, e, "There is a problem when writing the witness into a file.");
      }
    }
  }

  @Override
  public String getName() {
    return null;
  }

  void setNonterminatingLoop(ImmutableSet<Loop> pLoop) {
    checkState(nonterminatingLoops == null);
    checkState(pLoop != null);
    nonterminatingLoops = pLoop;
  }

  public void setFormulaManager(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  public void setBooleanFormulaManager(BooleanFormulaManagerView pBfmgr) {
    bfmgr = pBfmgr;
  }
}
