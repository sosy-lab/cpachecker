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

import java.util.Collection;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitCPA;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateStaticRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.RefinementStrategy;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.collect.Multimap;

/**
 * Refiner implementation that delegates to {@link ExplicitInterpolationBasedExplicitRefiner},
 * and if this fails, optionally delegates also to {@link PredicatingExplicitRefiner}.
 */
@Options(prefix="cpa.explicit.refiner")
public class DelegatingExplicitRefiner extends AbstractARGBasedRefiner implements StatisticsProvider {
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

  private static DelegatingExplicitRefiner initialiseExplicitRefiner(
      ConfigurableProgramAnalysis cpa, ExplicitCPA explicitCpa)
          throws CPAException, InvalidConfigurationException {
    Configuration config                        = explicitCpa.getConfiguration();
    LogManager logger                           = explicitCpa.getLogger();

    PathFormulaManager pathFormulaManager;
    PredicateCPARefiner backupRefiner    = null;

    PredicateCPA predicateCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa != null) {

      FormulaManagerFactory factory               = predicateCpa.getFormulaManagerFactory();
      FormulaManagerView formulaManager           = predicateCpa.getFormulaManager();
      Solver solver                               = predicateCpa.getSolver();
      pathFormulaManager                          = predicateCpa.getPathFormulaManager();
      PredicateStaticRefiner extractor            = predicateCpa.getStaticRefiner();

      InterpolationManager manager = new InterpolationManager(
          formulaManager,
          pathFormulaManager,
          solver,
          factory,
          config,
          logger);

      RefinementStrategy backupRefinementStrategy = new PredicateAbstractionRefinementStrategy(
          config,
          logger,
          formulaManager,
          predicateCpa.getPredicateManager(),
          solver);

      backupRefiner = new PredicateCPARefiner(
          config,
          logger,
          cpa,
          manager,
          formulaManager,
          pathFormulaManager,
          backupRefinementStrategy,
          extractor);

    } else {
      FormulaManagerFactory factory         = new FormulaManagerFactory(config, logger);
      FormulaManagerView formulaManager     = new FormulaManagerView(factory.getFormulaManager(), config, logger);
      pathFormulaManager                    = new PathFormulaManagerImpl(formulaManager, config, logger, explicitCpa.getMachineModel());
    }

    return new DelegatingExplicitRefiner(
        config,
        logger,
        cpa,
        pathFormulaManager,
        backupRefiner,
        explicitCpa.getStaticRefiner());
  }

  protected DelegatingExplicitRefiner(
      final Configuration config,
      final LogManager logger,
      final ConfigurableProgramAnalysis cpa,
      final PathFormulaManager pathFormulaManager,
      @Nullable final PredicateCPARefiner pBackupRefiner,
      ExplicitStaticRefiner explicitStaticRefiner) throws CPAException, InvalidConfigurationException {
    super(cpa);
    config.inject(this);

    interpolatingRefiner  = new ExplicitInterpolationBasedExplicitRefiner(config, pathFormulaManager);
    predicatingRefiner    = pBackupRefiner;
    staticRefiner         = explicitStaticRefiner;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath errorPath)
      throws CPAException, InterruptedException {
    // if path is infeasible, try to refine the precision
    if (!isPathFeasable(errorPath)) {
      if (performExplicitRefinement(reached, errorPath)) {
        return CounterexampleInfo.spurious();
      }
    }

    // if explicit analysis claims that path is feasible, or if explicit refinement failed,
    // refine with predicate analysis if available
    if (predicatingRefiner == null) {
      return CounterexampleInfo.feasible(errorPath, null);
    }
    else {
      return predicatingRefiner.performRefinement(reached, errorPath);
    }
  }

  /**
   * This method performs an explicit refinement.
   *
   * @param reached the current reached set
   * @param errorPath the current error path
   * @throws CPAException when explicit interpolation fails
   */
  private boolean performExplicitRefinement(final ARGReachedSet reached, final ARGPath errorPath) throws CPAException {
    UnmodifiableReachedSet reachedSet   = reached.asReachedSet();
    Precision precision                 = reachedSet.getPrecision(reachedSet.getLastState());
    ExplicitPrecision explicitPrecision = Precisions.extractPrecisionByType(precision, ExplicitPrecision.class);

    ExplicitPrecision refinedExplicitPrecision;
    Pair<ARGState, CFAEdge> interpolationPoint;

    if (!initialStaticRefinementDone && staticRefiner != null) {
      interpolationPoint          = errorPath.get(1);
      refinedExplicitPrecision    = staticRefiner.extractPrecisionFromCfa();
      initialStaticRefinementDone = true;
    }
    else {
      Multimap<CFANode, String> increment = interpolatingRefiner.determinePrecisionIncrement(reachedSet, errorPath);

      interpolationPoint        = interpolatingRefiner.determineInterpolationPoint(errorPath, increment);
      refinedExplicitPrecision  = new ExplicitPrecision(explicitPrecision, increment);
    }

    if (refinementSuccessful(errorPath, explicitPrecision, refinedExplicitPrecision)) {
      reached.removeSubtree(interpolationPoint.getFirst(), refinedExplicitPrecision, ExplicitPrecision.class);
      return true;
    }
    else {
      return false;
    }
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
  private boolean refinementSuccessful(ARGPath errorPath, ExplicitPrecision explicitPrecision, ExplicitPrecision refinedExplicitPrecision){
    // new error path or precision refined -> success
    boolean success = (errorPath.toString().hashCode() != previousErrorPathID)
        || (refinedExplicitPrecision.getSize() > explicitPrecision.getSize());

    previousErrorPathID = errorPath.toString().hashCode();

    return success;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(interpolatingRefiner);
    if (predicatingRefiner != null) {
      predicatingRefiner.collectStatistics(pStatsCollection);
    }
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException if the path check gets interrupted
   */
  private boolean isPathFeasable(ARGPath path) throws CPAException {
    try {
      // create a new ExplicitPathChecker, which does not track any of the given variables
      ExplictFeasibilityChecker checker = new ExplictFeasibilityChecker();

      return checker.isFeasible(path);
    }
    catch (InterruptedException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }
}
