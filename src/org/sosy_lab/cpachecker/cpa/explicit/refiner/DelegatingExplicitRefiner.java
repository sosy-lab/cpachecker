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
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGElement;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitCPA;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictPathChecker;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.HashMultimap;

@Options(prefix="cpa.explict.refiner")
public class DelegatingExplicitRefiner
  extends AbstractInterpolationBasedRefiner<Collection<AbstractionPredicate>, Pair<ARGElement, CFANode>> {

  private final boolean predicateCpaAvailable;

  private final IExplicitRefiner explicitRefiner;

  private IExplicitRefiner currentRefiner = null;

  private IExplicitRefiner predicateRefiner = null;

  private Boolean fullPrecisionCheckIsFeasable              = null;

  @Option(description="whether or not to use explicit interpolation")
  boolean useExplicitInterpolation                          = false;

  // statistics
  final Timer precisionUpdate                               = new Timer();
  final Timer argUpdate                                     = new Timer();

  public static DelegatingExplicitRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(DelegatingExplicitRefiner.class.getSimpleName() + " could not find the ExplicitCPA");
    }

    ExplicitCPA explicitCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(ExplicitCPA.class);
    if (explicitCpa == null) {
      throw new InvalidConfigurationException(DelegatingExplicitRefiner.class.getSimpleName() + " needs a ExplicitCPA");
    }

    DelegatingExplicitRefiner refiner = initialiseExplicitRefiner(cpa, explicitCpa.getConfiguration(), explicitCpa.getLogger());
    explicitCpa.getStats().addRefiner(refiner);

    return refiner;
  }

  private static DelegatingExplicitRefiner initialiseExplicitRefiner(ConfigurableProgramAnalysis cpa, Configuration config, LogManager logger) throws CPAException, InvalidConfigurationException {
    FormulaManagerFactory factory               = null;
    ExtendedFormulaManager formulaManager       = null;
    PathFormulaManager pathFormulaManager       = null;
    Solver solver                               = null;
    AbstractionManager absManager               = null;
    PredicateRefinementManager manager          = null;

    PredicateCPA predicateCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);

    boolean predicateCpaAvailable = predicateCpa != null;
    if(predicateCpaAvailable) {
      factory                     = predicateCpa.getFormulaManagerFactory();
      formulaManager              = predicateCpa.getFormulaManager();
      pathFormulaManager          = predicateCpa.getPathFormulaManager();
      solver                      = predicateCpa.getSolver();
      absManager                  = predicateCpa.getAbstractionManager();
    } else {
      factory                     = new FormulaManagerFactory(config, logger);
      TheoremProver theoremProver = factory.createTheoremProver();
      RegionManager regionManager = BDDRegionManager.getInstance();
      formulaManager              = new ExtendedFormulaManager(factory.getFormulaManager(), config, logger);
      pathFormulaManager          = new PathFormulaManagerImpl(formulaManager, config, logger);
      solver                      = new Solver(formulaManager, theoremProver);
      absManager                  = new AbstractionManager(regionManager, formulaManager, config, logger);
    }

    manager = new PredicateRefinementManager(
        formulaManager,
        pathFormulaManager,
        solver,
        absManager,
        factory,
        config,
        logger);

    return new DelegatingExplicitRefiner(
        config,
        logger,
        cpa,
        formulaManager,
        pathFormulaManager,
        manager,
        predicateCpaAvailable);
  }

  protected DelegatingExplicitRefiner(
      final Configuration config,
      final LogManager logger,
      final ConfigurableProgramAnalysis cpa,
      final ExtendedFormulaManager formulaManager,
      final PathFormulaManager pathFormulaManager,
      final PredicateRefinementManager interpolationManager,
      final boolean predicateCpaAvailable) throws CPAException, InvalidConfigurationException {

    super(config, logger, cpa, interpolationManager);

    config.inject(this, DelegatingExplicitRefiner.class);

    if(useExplicitInterpolation) {
      explicitRefiner = new ExplicitInterpolationBasedExplicitRefiner(config, pathFormulaManager);
    }
    else{
      explicitRefiner = new SmtBasedExplicitRefiner(config, pathFormulaManager, formulaManager);
    }

    this.predicateCpaAvailable = predicateCpaAvailable;
  }

  /**
   * This method chooses the refiner to be used in the current iteration.
   *
   * The selection is based on the current error path, i.e. if it is a repeated counter-example, and the fact,
   * if a PredicateCPA is available - if so, this PredicateCPA is refined if the ExplicitCPA is not powerful
   * enough to prove a path as infeasible
   *
   * @param errorPath the current error path
   * @return the refiner to be used in the current iteration
   */
  private IExplicitRefiner chooseCurrentRefiner(final Path errorPath, Precision precision) {
    // if explicit refiner made progress, continue with that
    if(explicitRefiner.hasMadeProgress(errorPath, precision)) {
      return explicitRefiner;
    }

    // ... otherwise, try to fall back to predicate refiner, if available
    else {
      if(predicateCpaAvailable) {
        if(predicateRefiner == null) {
          predicateRefiner = new PredicatingExplicitRefiner();
        }

        if(predicateRefiner.hasMadeProgress(errorPath, precision)) {
          return predicateRefiner;
        }
      }
    }

    return null;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final Path errorPath)
      throws CPAException, InterruptedException {

    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision = reachedSet.getPrecision(reachedSet.getLastElement());

    explicitRefiner.setCurrentErrorPath(errorPath);

    currentRefiner = chooseCurrentRefiner(errorPath, precision);

    // no refiner is able to to disprove the current path, so stop the analysis
    if(currentRefiner == null) {
      stopRefinement(errorPath);
    }

    return super.performRefinement(reached, errorPath);
  }

  @Override
  protected final List<Pair<ARGElement, CFANode>> transformPath(Path errorPath) {
    return currentRefiner.transformPath(errorPath);
  }

  @Override
  protected List<Formula> getFormulasForPath(List<Pair<ARGElement, CFANode>> errorPath, ARGElement initialElement)
      throws CPATransferException {
    return currentRefiner.getFormulasForPath(errorPath, initialElement);
  }

  @Override
  protected void performRefinement(
      ARGReachedSet pReached,
      List<Pair<ARGElement, CFANode>> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> counterexampleTraceInfo,
      boolean pRepeatedCounterexample)
      throws CPAException {

    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastElement());

    precisionUpdate.start();
    Pair<ARGElement, Precision> result = currentRefiner.performRefinement(oldPrecision, errorPath, counterexampleTraceInfo);
    precisionUpdate.stop();

    argUpdate.start();
    ARGElement root = result.getFirst();
    logger.log(Level.FINEST, "Found spurious counterexample,",
        "trying strategy 1: remove everything below", root, "from ARG.");
    pReached.removeSubtree(root, result.getSecond());
    argUpdate.stop();
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException
   */
  private boolean isPathFeasable(List<CFAEdge> path) throws CPAException {
    try {
      // create a new ExplicitPathChecker, which does not track any of the given variables
      return new ExplictPathChecker().checkPath(path, HashMultimap.<CFANode, String>create());
    }
    catch (InterruptedException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }

  /**
   * This method stops the refinement, and does a full precision check along the path to reason why no progress was made.
   *
   * @param errorPath the error path to reason on why no progress was made.
   * @throws CPAException when the full precision check fails
   */
  private void stopRefinement(Path errorPath) throws CPAException {
    List<CFAEdge> cfaTrace = new ArrayList<CFAEdge>();
    for(Pair<ARGElement, CFAEdge> pathElement : errorPath){
      cfaTrace.add(pathElement.getSecond());
    }

    fullPrecisionCheckIsFeasable = isPathFeasable(cfaTrace);

    throw new RefinementFailedException(Reason.RepeatedCounterexample, null);
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    super.printStatistics(out, result, reached);

    out.println("Explicit Refinement:");
    out.println("total time for refining precision:       " + precisionUpdate);
    out.println("total time used for updating ARG:        " + argUpdate);

    if(fullPrecisionCheckIsFeasable != null) {
      out.println("full-precision-check is feasable:        " + (fullPrecisionCheckIsFeasable ? "yes" : "no"));
    }

    explicitRefiner.printStatistics(out, result, reached);

    if(predicateRefiner != null) {
      predicateRefiner.printStatistics(out, result, reached);
    }
  }
}