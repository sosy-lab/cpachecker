/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitCPA;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateStaticRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
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
 * Refiner implementation that delegates to {@link ExplicitInterpolationBasedExplicitRefiner},
 * and if this fails, optionally delegates also to {@link PredicatingExplicitRefiner}.
 */
@Options(prefix="cpa.explicit.refiner")
public class DelegatingExplicitRefiner extends AbstractARGBasedRefiner implements Statistics, StatisticsProvider {
  /**
   * the flag to determine if initial refinement was done already
   */
  private boolean initialStaticRefinementDone = false;

  /**
   * refiner used for (optional) initial static refinement, based on information extracted solely from the CFA
   */
  private ExplicitStaticRefiner staticRefiner;

  /**
   * refiner used for explicit interpolation refinement
   */
  private ExplicitInterpolationBasedExplicitRefiner interpolatingRefiner;

  /**
   * backup-refiner used for predicate refinement, when explicit refinement fails (due to lack of expressiveness)
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
  private int numberOfExplicitRefinements           = 0;
  private int numberOfPredicateRefinements          = 0;
  private int numberOfSuccessfulExplicitRefinements = 0;

  /**
   * the identifier which is used to identify repeated refinements
   */
  private int previousRefinementId = 0;

  private final CFA cfa;

  private final LogManager logger;

  public static DelegatingExplicitRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(DelegatingExplicitRefiner.class.getSimpleName() + " could not find the ExplicitCPA");
    }

    ExplicitCPA explicitCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(ExplicitCPA.class);
    if (explicitCpa == null) {
      throw new InvalidConfigurationException(DelegatingExplicitRefiner.class.getSimpleName() + " needs a ExplicitCPA");
    }

    DelegatingExplicitRefiner refiner = initialiseExplicitRefiner(cpa, explicitCpa);
    explicitCpa.getStats().addRefiner(refiner);

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

  private static DelegatingExplicitRefiner initialiseExplicitRefiner(
      ConfigurableProgramAnalysis cpa, ExplicitCPA explicitCpa)
          throws CPAException, InvalidConfigurationException {
    Configuration config              = explicitCpa.getConfiguration();
    LogManager logger                 = explicitCpa.getLogger();
    PredicateCPARefiner backupRefiner = createBackupRefiner(config, logger, cpa);

    return new DelegatingExplicitRefiner(
        config,
        logger,
        explicitCpa.getShutdownNotifier(),
        cpa,
        backupRefiner,
        explicitCpa.getStaticRefiner(),
        explicitCpa.getCFA());
  }

  protected DelegatingExplicitRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa,
      @Nullable final PredicateCPARefiner pBackupRefiner,
      ExplicitStaticRefiner pExplicitStaticRefiner,
      final CFA pCfa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);

    interpolatingRefiner  = new ExplicitInterpolationBasedExplicitRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    predicatingRefiner    = pBackupRefiner;
    staticRefiner         = pExplicitStaticRefiner;
    cfa                   = pCfa;
    logger                = pLogger;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (!isPathFeasable(errorPath)) {
      if (performExplicitRefinement(reached, errorPath)) {
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
   * This method performs an explicit refinement.
   *
   * @param reached the current reached set
   * @param errorPath the current error path
   * @returns true, if the explicit refinement was successful, else false
   * @throws CPAException when explicit interpolation fails
   */
  private boolean performExplicitRefinement(final ARGReachedSet reached, final ARGPath errorPath) throws CPAException, InterruptedException {
    numberOfExplicitRefinements++;

    UnmodifiableReachedSet reachedSet   = reached.asReachedSet();
    Precision precision                 = reachedSet.getPrecision(reachedSet.getLastState());
    ExplicitPrecision explicitPrecision = Precisions.extractPrecisionByType(precision, ExplicitPrecision.class);
    BDDPrecision bddPrecision           = Precisions.extractPrecisionByType(precision, BDDPrecision.class);

    ArrayList<Precision> refinedPrecisions = new ArrayList<>(2);
    ArrayList<Class<? extends Precision>> newPrecisionTypes = new ArrayList<>(2);

    ExplicitPrecision refinedExplicitPrecision;
    Pair<ARGState, CFAEdge> refinementRoot;

    if (!initialStaticRefinementDone && staticRefiner != null) {
      refinementRoot              = errorPath.get(1);
      refinedExplicitPrecision    = staticRefiner.extractPrecisionFromCfa(reachedSet, errorPath);
      initialStaticRefinementDone = true;
    }
    else {
      Multimap<CFANode, MemoryLocation> increment = interpolatingRefiner.determinePrecisionIncrement(errorPath);
      refinementRoot                      = interpolatingRefiner.determineRefinementRoot(errorPath, increment, false);

      // no increment - explicit refinement was not successful
      if(increment.isEmpty()) {
        return false;
      }

      // if two subsequent refinements are similar (based on some fancy heuristic), choose a different refinement root
      if(checkForRepeatedRefinements && isRepeatedRefinement(increment, refinementRoot)) {
        refinementRoot = interpolatingRefiner.determineRefinementRoot(errorPath, increment, true);
      }

      //      if (explicitPrecision != null) { // TODO ExplicitRefiner without ExplicitPresicion, possible?
      refinedExplicitPrecision  = new ExplicitPrecision(explicitPrecision, increment);
      refinedPrecisions.add(refinedExplicitPrecision);
      newPrecisionTypes.add(ExplicitPrecision.class);
      //      }

      if (bddPrecision != null) {
        BDDPrecision refinedBDDPrecision = new BDDPrecision(bddPrecision, increment);
        refinedPrecisions.add(refinedBDDPrecision);
        newPrecisionTypes.add(BDDPrecision.class);
      }
    }

    if (explicitRefinementWasSuccessful(errorPath, explicitPrecision, refinedExplicitPrecision)) {
      numberOfSuccessfulExplicitRefinements++;
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
    int currentRefinementId = refinementRoot.getSecond().getLineNumber();
    boolean result          = (previousRefinementId == currentRefinementId);
    previousRefinementId    = currentRefinementId;

    return result;
  }

  /**
   * This helper method checks if the refinement was successful, i.e.,
   * that either the counterexample is not a repeated counterexample, or that the precision did grow.
   *
   * Repeated counterexamples might occur when combining the analysis with thresholding,
   * or when ignoring variable classes, i.e. when combined with BDD analysis (i.e. cpa.explicit.precision.ignoreBoolean).
   *
   * @param errorPath the current error path
   * @param explicitPrecision the previous precision
   * @param refinedExplicitPrecision the refined precision
   */
  private boolean explicitRefinementWasSuccessful(ARGPath errorPath, ExplicitPrecision explicitPrecision,
      ExplicitPrecision refinedExplicitPrecision) {
    // new error path or precision refined -> success
    boolean success = (errorPath.toString().hashCode() != previousErrorPathID)
        || (refinedExplicitPrecision.getSize() > explicitPrecision.getSize());

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
    return "Delegating Explicit-Interpolation-Based Refiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  number of explicit refinements:                      " + numberOfExplicitRefinements);
    out.println("  number of successful explicit refinements:           " + numberOfSuccessfulExplicitRefinements);
    out.println("  number of predicate refinements:                     " + numberOfPredicateRefinements);
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException if the path check gets interrupted
   */
  boolean isPathFeasable(ARGPath path) throws CPAException {
    try {
      // create a new ExplicitPathChecker, which does not track any of the given variables
      ExplictFeasibilityChecker checker = new ExplictFeasibilityChecker(logger, cfa);

      return checker.isFeasible(path);
    }
    catch (InterruptedException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }
}
