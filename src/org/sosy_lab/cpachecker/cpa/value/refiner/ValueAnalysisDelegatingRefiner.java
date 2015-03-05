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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.util.Collection;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateStaticRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

import com.google.common.base.Optional;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator},
 * and if this fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
@Options(prefix="cpa.value.refiner")
public class ValueAnalysisDelegatingRefiner extends AbstractARGBasedRefiner implements StatisticsProvider {

  private ShutdownNotifier shutDownNotifier;

  /**
   * refiner used for (optional) initial static refinement, based on information extracted solely from the CFA
   */
  private ValueAnalysisStaticRefiner staticRefiner;

  /**
   * refiner used for value-analysis interpolation refinement
   */
  private ValueAnalysisRefiner valueRefiner;

  /**
   * backup-refiner used for predicate refinement, when value-analysis refinement fails (due to lack of expressiveness)
   */
  private PredicateCPARefiner predicatingRefiner;

  /**
   * the flag to determine whether or not to check for repeated refinements
   */
  @Option(secure=true, description="whether or not to check for repeated refinements, to then reset the refinement root")
  private boolean checkForRepeatedRefinements = true;

  private final CFA cfa;

  private final LogManager logger;
  private final Configuration config;

  public static ValueAnalysisDelegatingRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " could not find the ValueAnalysisCPA");
    }

    WrapperCPA wrapperCpa = ((WrapperCPA)cpa);

    ValueAnalysisCPA valueAnalysisCpa = wrapperCpa.retrieveWrappedCpa(ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    ValueAnalysisDelegatingRefiner refiner = initialiseValueAnalysisRefiner(cpa, valueAnalysisCpa);
    valueAnalysisCpa.getStats().addRefiner(refiner);

    return refiner;
  }

  private static PredicateCPARefiner createBackupRefiner(final Configuration config,
        final LogManager logger, final ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {

    PredicateCPA predicateCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);

    if (predicateCpa == null) {
      return null;

    } else {
        Solver solver                               = predicateCpa.getSolver();
        PathFormulaManager pathFormulaManager       = predicateCpa.getPathFormulaManager();
        PredicateStaticRefiner extractor            = predicateCpa.getStaticRefiner();
        MachineModel machineModel                   = predicateCpa.getMachineModel();
        Optional<LoopStructure> loopStructure       = predicateCpa.getCfa().getLoopStructure();

        InterpolationManager manager = new InterpolationManager(
            pathFormulaManager,
            solver,
            loopStructure,
            config,
            predicateCpa.getShutdownNotifier(),
            logger);

        PathChecker pathChecker = new PathChecker(logger, predicateCpa.getShutdownNotifier(), pathFormulaManager, solver, machineModel);

        RefinementStrategy backupRefinementStrategy = new PredicateAbstractionRefinementStrategy(
            config,
            logger,
            predicateCpa.getShutdownNotifier(),
            predicateCpa.getPredicateManager(),
            extractor,
            solver);

        return new PredicateCPARefiner(
            config,
            logger,
            cpa,
            manager,
            pathChecker,
            pathFormulaManager,
            backupRefinementStrategy,
            solver,
            predicateCpa.getAssumesStore(),
            predicateCpa.getCfa());
      }
  }

  private static ValueAnalysisDelegatingRefiner initialiseValueAnalysisRefiner(
      ConfigurableProgramAnalysis cpa, ValueAnalysisCPA pValueAnalysisCpa)
          throws CPAException, InvalidConfigurationException {
    Configuration config              = pValueAnalysisCpa.getConfiguration();
    LogManager logger                 = pValueAnalysisCpa.getLogger();
    PredicateCPARefiner backupRefiner = createBackupRefiner(config, logger, cpa);

    pValueAnalysisCpa.injectRefinablePrecision();

    return new ValueAnalysisDelegatingRefiner(
        config,
        logger,
        pValueAnalysisCpa.getShutdownNotifier(),
        cpa,
        backupRefiner,
        pValueAnalysisCpa.getCFA());
  }

  protected ValueAnalysisDelegatingRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa,
      @Nullable final PredicateCPARefiner pBackupRefiner,
      final CFA pCfa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;

    shutDownNotifier = pShutdownNotifier;

    cfa = pCfa;

    valueRefiner = new ValueAnalysisRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    staticRefiner = new ValueAnalysisStaticRefiner(pConfig, pLogger);

    predicatingRefiner = pBackupRefiner;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    boolean isRefined = staticRefiner.performRefinement(reached, pErrorPath)
        || valueRefiner.performRefinement(reached);

    if(isRefined) {
      return CounterexampleInfo.spurious();
    }

    else if(predicatingRefiner != null) {
      return predicatingRefiner.performRefinement(reached, pErrorPath);
    }

    try {
      return CounterexampleInfo.feasible(pErrorPath, createModel(pErrorPath));
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Failed to configure feasbility checker", e);
    }
  }

  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   * @throws InvalidConfigurationException
   * @throws InterruptedException
   * @throws CPAException
   */
  private Model createModel(ARGPath errorPath) throws InvalidConfigurationException, InterruptedException,
      CPAException {
    ValueAnalysisFeasibilityChecker evaluator = new ValueAnalysisFeasibilityChecker(logger, cfa, config);
    ValueAnalysisConcreteErrorPathAllocator va = new ValueAnalysisConcreteErrorPathAllocator(logger, shutDownNotifier);

    return va.allocateAssignmentsToPath(evaluator.evaluate(errorPath), cfa.getMachineModel());
  }

  /**
   * This method checks if the given path is feasible, when doing a full-precision check.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException if the path check gets interrupted
   */
  boolean isPathFeasable(ARGPath path) throws CPAException {
    try {
      // create a new ValueAnalysisPathChecker, which does check the given path at full precision
      ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa, config);

      return checker.isFeasible(path);
    }
    catch (InterruptedException | InvalidConfigurationException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {

    valueRefiner.collectStatistics(pStatsCollection);

    if (predicatingRefiner != null) {
      predicatingRefiner.collectStatistics(pStatsCollection);
    }
  }
}
