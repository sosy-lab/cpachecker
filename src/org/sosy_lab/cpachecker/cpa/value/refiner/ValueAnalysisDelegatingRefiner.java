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

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.bdd.BDDPrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateStaticRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.cpa.value.ComponentAwarePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator},
 * and if this fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
@Options(prefix="cpa.value.refiner")
public class ValueAnalysisDelegatingRefiner extends AbstractARGBasedRefiner implements StatisticsProvider {

  private ShutdownNotifier shutDownNotifier;

  /**
   * the flag to determine if initial refinement was done already
   */
  private boolean initialStaticRefinementDone = false;

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

    if (!(wrapperCpa.retrieveWrappedCpa(CompositeCPA.class).getPrecisionAdjustment() instanceof ComponentAwarePrecisionAdjustment)) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a ComponentAwarePrecisionAdjustment operator");
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
        FormulaManagerFactory factory               = predicateCpa.getFormulaManagerFactory();
        FormulaManagerView formulaManager           = predicateCpa.getFormulaManager();
        Solver solver                               = predicateCpa.getSolver();
        PathFormulaManager pathFormulaManager       = predicateCpa.getPathFormulaManager();
        PredicateStaticRefiner extractor            = predicateCpa.getStaticRefiner();
        MachineModel machineModel                   = predicateCpa.getMachineModel();

        InterpolationManager manager = new InterpolationManager(
            formulaManager,
            pathFormulaManager,
            solver,
            factory,
            config,
            predicateCpa.getShutdownNotifier(),
            logger);

        PathChecker pathChecker = new PathChecker(logger, predicateCpa.getShutdownNotifier(), pathFormulaManager, solver, machineModel);

        RefinementStrategy backupRefinementStrategy = new PredicateAbstractionRefinementStrategy(
            config,
            logger,
            predicateCpa.getShutdownNotifier(),
            formulaManager,
            predicateCpa.getPredicateManager(),
            extractor,
            solver);

        return new PredicateCPARefiner(
            config,
            logger,
            cpa,
            manager,
            pathChecker,
            formulaManager,
            pathFormulaManager,
            backupRefinementStrategy,
            solver,
            predicateCpa.getAssumesStore());
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
        pValueAnalysisCpa.getStaticRefiner(),
        pValueAnalysisCpa.getCFA());
  }

  protected ValueAnalysisDelegatingRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa,
      @Nullable final PredicateCPARefiner pBackupRefiner,
      ValueAnalysisStaticRefiner pValueAnalysisStaticRefiner,
      final CFA pCfa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;

    shutDownNotifier = pShutdownNotifier;

    cfa = pCfa;

    valueRefiner = new ValueAnalysisRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    predicatingRefiner = pBackupRefiner;
    staticRefiner = pValueAnalysisStaticRefiner;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    MutableARGPath errorPath = pErrorPath.mutableCopy();

    // if path is infeasible, try to refine the precision
    if (!isPathFeasable(errorPath)) {
      if (performValueAnalysisRefinement(reached, errorPath)) {
        return CounterexampleInfo.spurious();
      }
    }

    if (predicatingRefiner != null) {
      return predicatingRefiner.performRefinement(reached, pErrorPath);

    } else {

      try {
        return CounterexampleInfo.feasible(pErrorPath, createModel(errorPath));
      } catch (InvalidConfigurationException e) {
        throw new CPAException("Failed to configure feasbility checker", e);
      }
    }
  }

  /**
   * This method performs an value-analysis refinement.
   *
   * @param reached the current reached set
   * @param errorPath the current error path
   * @returns true, if the value-analysis refinement was successful, else false
   * @throws CPAException when value-analysis interpolation fails
   */
  private boolean performValueAnalysisRefinement(final ARGReachedSet reached, final MutableARGPath errorPath) throws CPAException, InterruptedException {

    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision = reachedSet.getPrecision(reachedSet.getLastState());

    FluentIterable<Precision> precisions = Precisions.asIterable(precision);
    VariableTrackingPrecision valueAnalysisPrecision = (VariableTrackingPrecision) precisions.filter(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class)).get(0);
    BDDPrecision bddPrecision = Precisions.extractPrecisionByType(precision, BDDPrecision.class);

    ArrayList<Precision> refinedPrecisions = new ArrayList<>(2);
    ArrayList<Predicate<? super Precision>> newPrecisionTypes = new ArrayList<>(2);

    if (!initialStaticRefinementDone && staticRefiner != null) {
      initialStaticRefinementDone = true;

      Multimap<CFANode, MemoryLocation> increment = staticRefiner.extractPrecisionIncrementFromCfa(errorPath);

      refinedPrecisions.add(valueAnalysisPrecision.withIncrement(increment));
      newPrecisionTypes.add(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));

      if (bddPrecision != null) {
        BDDPrecision refinedBDDPrecision = new BDDPrecision(bddPrecision, increment);
        refinedPrecisions.add(refinedBDDPrecision);
        newPrecisionTypes.add(Predicates.instanceOf(BDDPrecision.class));
      }

      return true;

    } else {
      return valueRefiner.performRefinement(reached);
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
  private Model createModel(MutableARGPath errorPath) throws InvalidConfigurationException, InterruptedException,
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
  boolean isPathFeasable(MutableARGPath path) throws CPAException {
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
