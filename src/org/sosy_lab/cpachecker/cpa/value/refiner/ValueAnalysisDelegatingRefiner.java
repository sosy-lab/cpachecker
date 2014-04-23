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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.bdd.BDDPrecision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateStaticRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.cpa.value.ComponentAwarePrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
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

import com.google.common.collect.Multimap;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisInterpolationBasedRefiner},
 * and if this fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
@Options(prefix="cpa.value.refiner")
public class ValueAnalysisDelegatingRefiner extends AbstractARGBasedRefiner implements Statistics, StatisticsProvider {
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
  private ValueAnalysisInterpolationBasedRefiner interpolatingRefiner;

  /**
   * backup-refiner used for predicate refinement, when value-analysis refinement fails (due to lack of expressiveness)
   */
  private PredicateCPARefiner predicatingRefiner;

  /**
   * the hash code of the previous error path
   */
  private int previousErrorPathID = -1;

  /**
   * the flag to determine whether or not to check for repeated refinements
   */
  @Option(description="whether or not to check for repeated refinements, to then reset the refinement root")
  private boolean checkForRepeatedRefinements = true;

  // statistics
  private int numberOfValueAnalysisRefinements           = 0;
  private int numberOfPredicateRefinements               = 0;
  private int numberOfSuccessfulValueAnalysisRefinements = 0;

  /**
   * the identifier which is used to identify repeated refinements
   */
  private int previousRefinementId = 0;

  private final CFA cfa;

  private final LogManager logger;

  public static ValueAnalysisDelegatingRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " could not find the ValueAnalysisCPA");
    }

    WrapperCPA wrapperCpa = ((WrapperCPA)cpa);

    ValueAnalysisCPA valueAnalysisCpa = wrapperCpa.retrieveWrappedCpa(ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    if(!(wrapperCpa.retrieveWrappedCpa(CompositeCPA.class).getPrecisionAdjustment() instanceof ComponentAwarePrecisionAdjustment)) {
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
    }

    else {
        FormulaManagerFactory factory               = predicateCpa.getFormulaManagerFactory();
        FormulaManagerView formulaManager           = predicateCpa.getFormulaManager();
        Solver solver                               = predicateCpa.getSolver();
        PathFormulaManager pathFormulaManager       = predicateCpa.getPathFormulaManager();
        PredicateStaticRefiner extractor            = predicateCpa.getStaticRefiner();

        InterpolationManager manager = new InterpolationManager(
            formulaManager,
            pathFormulaManager,
            solver,
            factory,
            config,
            predicateCpa.getShutdownNotifier(),
            logger);

        PathChecker pathChecker = new PathChecker(logger, pathFormulaManager, solver);

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
            backupRefinementStrategy);
      }
  }

  private static ValueAnalysisDelegatingRefiner initialiseValueAnalysisRefiner(
      ConfigurableProgramAnalysis cpa, ValueAnalysisCPA pValueAnalysisCpa)
          throws CPAException, InvalidConfigurationException {
    Configuration config              = pValueAnalysisCpa.getConfiguration();
    LogManager logger                 = pValueAnalysisCpa.getLogger();
    PredicateCPARefiner backupRefiner = createBackupRefiner(config, logger, cpa);

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

    interpolatingRefiner  = new ValueAnalysisInterpolationBasedRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    predicatingRefiner    = pBackupRefiner;
    staticRefiner         = pValueAnalysisStaticRefiner;
    cfa                   = pCfa;
    logger                = pLogger;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (!isPathFeasable(errorPath)) {
      if (performValueAnalysisRefinement(reached, errorPath)) {
        return CounterexampleInfo.spurious();
      }
    } else {

    }

    if(predicatingRefiner != null) {
      numberOfPredicateRefinements++;
      return predicatingRefiner.performRefinement(reached, errorPath);
    }

    else {
      return CounterexampleInfo.feasible(errorPath, null);
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
  private boolean performValueAnalysisRefinement(final ARGReachedSet reached, final ARGPath errorPath) throws CPAException, InterruptedException {
    numberOfValueAnalysisRefinements++;

    UnmodifiableReachedSet reachedSet             = reached.asReachedSet();
    Precision precision                           = reachedSet.getPrecision(reachedSet.getLastState());
    ValueAnalysisPrecision valueAnalysisPrecision = Precisions.extractPrecisionByType(precision, ValueAnalysisPrecision.class);
    BDDPrecision bddPrecision                     = Precisions.extractPrecisionByType(precision, BDDPrecision.class);

    ArrayList<Precision> refinedPrecisions = new ArrayList<>(2);
    ArrayList<Class<? extends Precision>> newPrecisionTypes = new ArrayList<>(2);

    ValueAnalysisPrecision refinedValueAnalysisPrecision;
    Pair<ARGState, CFAEdge> refinementRoot;

    if (!initialStaticRefinementDone && staticRefiner != null) {
      refinementRoot                = errorPath.get(1);
      refinedValueAnalysisPrecision = staticRefiner.extractPrecisionFromCfa(reachedSet, errorPath);
      initialStaticRefinementDone   = true;
    }
    else {
      Multimap<CFANode, MemoryLocation> increment = interpolatingRefiner.determinePrecisionIncrement(errorPath);
      refinementRoot                      = interpolatingRefiner.determineRefinementRoot(errorPath, increment, false);

      // no increment - value-analysis refinement was not successful
      if(increment.isEmpty()) {
        return false;
      }

      // if two subsequent refinements are similar (based on some fancy heuristic), choose a different refinement root
      if(checkForRepeatedRefinements && isRepeatedRefinement(increment, refinementRoot)) {
        refinementRoot = interpolatingRefiner.determineRefinementRoot(errorPath, increment, true);
      }

      //      if (valueAnalysisPrecision != null) { // TODO ValueAnalysisRefiner without ValueAnalysisPresicion, possible?
      refinedValueAnalysisPrecision  = new ValueAnalysisPrecision(valueAnalysisPrecision, increment);
      refinedPrecisions.add(refinedValueAnalysisPrecision);
      newPrecisionTypes.add(ValueAnalysisPrecision.class);
      //      }

      if (bddPrecision != null) {
        BDDPrecision refinedBDDPrecision = new BDDPrecision(bddPrecision, increment);
        refinedPrecisions.add(refinedBDDPrecision);
        newPrecisionTypes.add(BDDPrecision.class);
      }
    }

    if (valueAnalysisRefinementWasSuccessful(errorPath, valueAnalysisPrecision, refinedValueAnalysisPrecision)) {
      numberOfSuccessfulValueAnalysisRefinements++;
      reached.removeSubtree(refinementRoot.getFirst(), refinedPrecisions, newPrecisionTypes);
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * The not-so-fancy heuristic to determine if two subsequent refinements are similar
   *
   * @param increment the precision increment
   * @param refinementRoot the current refinement root
   * @return true, if the current refinement is found to be similar to the previous one, else false
   */
  private boolean isRepeatedRefinement(Multimap<CFANode, MemoryLocation> increment, Pair<ARGState, CFAEdge> refinementRoot) {
    int currentRefinementId = refinementRoot.getSecond().getSuccessor().getNodeNumber();
    boolean result         = (previousRefinementId == currentRefinementId);
    previousRefinementId    = currentRefinementId;

    return result;
  }

  /**
   * This helper method checks if the refinement was successful, i.e.,
   * that either the counterexample is not a repeated counterexample, or that the precision did grow.
   *
   * Repeated counterexamples might occur when combining the analysis with thresholding,
   * or when ignoring variable classes, i.e. when combined with BDD analysis (i.e. cpa.value.precision.ignoreBoolean).
   *
   * @param errorPath the current error path
   * @param valueAnalysisPrecision the previous precision
   * @param refinedValueAnalysisPrecision the refined precision
   */
  private boolean valueAnalysisRefinementWasSuccessful(ARGPath errorPath, ValueAnalysisPrecision valueAnalysisPrecision,
      ValueAnalysisPrecision refinedValueAnalysisPrecision) {
    // new error path or precision refined -> success
    boolean success = (errorPath.toString().hashCode() != previousErrorPathID)
        || (refinedValueAnalysisPrecision.getSize() > valueAnalysisPrecision.getSize());

    previousErrorPathID = errorPath.toString().hashCode();

    return success;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
    pStatsCollection.add(interpolatingRefiner);
    if (predicatingRefiner != null) {
      predicatingRefiner.collectStatistics(pStatsCollection);
    }
  }

  @Override
  public String getName() {
    return "ValueAnalysisDelegatingRefiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  number of value analysis refinements:                " + numberOfValueAnalysisRefinements);
    out.println("  number of successful valueAnalysis refinements:      " + numberOfSuccessfulValueAnalysisRefinements);
    out.println("  number of predicate refinements:                     " + numberOfPredicateRefinements);
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
      ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa);

      return checker.isFeasible(path);
    }
    catch (InterruptedException | InvalidConfigurationException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }
}
