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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.termination.LassoAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.toolchain.CPAcheckerToolchainStorage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.api.SolverContext;
import org.sosy_lab.solver.basicimpl.AbstractFormulaManager;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;

import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoRankerPreferences;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgumentSynthesizer;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgumentSynthesizer;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.AffineTemplate;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Term;

public class LassoAnalysisImpl implements LassoAnalysis {

  private final LogManager logger;

  private final Solver solver;
  private final AbstractFormulaManager<Term, ?, ?, ?> formulaManager;
  private final FormulaManagerView formulaManagerView;
  private final PathFormulaManager pathFormulaManager;

  private final LassoBuilder lassoBuilder;

  private final LassoRankerPreferences lassoRankerPreferences;
  private final NonTerminationAnalysisSettings nonTerminationAnalysisSettings;
  private final TerminationAnalysisSettings terminationAnalysisSettings;

  private final CPAcheckerToolchainStorage toolchainStorage;

  @SuppressWarnings("unchecked")
  public LassoAnalysisImpl(
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      SolverContext pSolverContext,
      CFA pCfa)
      throws InvalidConfigurationException {
    logger = Preconditions.checkNotNull(pLogger);
    Configuration solverConfig = Configuration.defaultConfiguration();
    solver = Solver.create(solverConfig, pLogger, pShutdownNotifier);
    formulaManager = (AbstractFormulaManager<Term, ?, ?, ?>) pSolverContext.getFormulaManager();
    formulaManagerView = solver.getFormulaManager();
    pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManagerView,
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);

    lassoBuilder =
        new LassoBuilder(pLogger, formulaManager, formulaManagerView, pathFormulaManager);

    lassoRankerPreferences = new LassoRankerPreferences();
    lassoRankerPreferences.externalSolver = false; // use SMTInterpol
    nonTerminationAnalysisSettings = new NonTerminationAnalysisSettings();
    terminationAnalysisSettings = new TerminationAnalysisSettings();
    toolchainStorage = new CPAcheckerToolchainStorage(pLogger, pShutdownNotifier);
  }

  @Override
  public LassoAnalysisResult checkTermination(AbstractState targetState)
      throws CPATransferException, InterruptedException {
    Preconditions.checkArgument(AbstractStates.isTargetState(targetState));

    Lasso lasso;
    try {
      lasso = lassoBuilder.buildLasso(targetState);
    } catch (TermException e) {
      logger.logUserException(Level.WARNING, e, "Could not extract lasso.");
      return new LassoAnalysisResult(Optional.empty(), Optional.empty());
    }

    logger.logf(Level.INFO, "Extracted lasso: %s", lasso);

    try {
      return checkTermination(lasso);
    } catch (IOException | SMTLIBException | TermException e) {
      logger.logUserException(Level.WARNING, e, "Could not check (non)-termination of lasso.");
      return new LassoAnalysisResult(Optional.empty(), Optional.empty());
    }
  }

  private LassoAnalysisResult checkTermination(Lasso lasso)
      throws IOException, SMTLIBException, TermException {
    NonTerminationArgument nonTerminationArgument = null;
    try (NonTerminationArgumentSynthesizer nonTerminationArgumentSynthesizer =
        new NonTerminationArgumentSynthesizer(
            lasso,
            lassoRankerPreferences,
            nonTerminationAnalysisSettings,
            toolchainStorage,
            toolchainStorage)) {
      nonTerminationArgumentSynthesizer.synthesize();
      if (nonTerminationArgumentSynthesizer.synthesisSuccessful()) {
        nonTerminationArgument = nonTerminationArgumentSynthesizer.getArgument();
        logger.logf(Level.FINE, "Proved non-termintion: %s", nonTerminationArgument);
      }
    }

    TerminationArgument terminationArgument = null;
    try (TerminationArgumentSynthesizer terminationArgumentSynthesizer =
        new TerminationArgumentSynthesizer(
            lasso,
            new AffineTemplate(),
            lassoRankerPreferences,
            terminationAnalysisSettings,
            Collections.emptySet(),
            toolchainStorage,
            toolchainStorage)) {
      terminationArgumentSynthesizer.synthesize();
      if (terminationArgumentSynthesizer.synthesisSuccessful()) {
        terminationArgument = terminationArgumentSynthesizer.getArgument();
        logger.logf(Level.FINEST, "Found termination argument: %s", terminationArgument);
      }
    }

    return new LassoAnalysisResult(
        Optional.ofNullable(nonTerminationArgument), Optional.ofNullable(terminationArgument));
  }
}
