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
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitCPA;
import org.sosy_lab.cpachecker.cpa.explicit.ExplictPathChecker;
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
  extends AbstractInterpolationBasedRefiner<Collection<AbstractionPredicate>, Pair<ARTElement, CFANode>> {

  private final boolean predicateCpaAvailable;

  private IExplicitRefiner refiner;

  private Boolean fullPrecisionCheckIsFeasable              = null;

  @Option(description="whether or not to use explicit interpolation")
  boolean useExplicitInterpolation                          = false;

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
    if (predicateCpaAvailable) {
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

    this.predicateCpaAvailable  = predicateCpaAvailable;

    if(useExplicitInterpolation) {
      refiner = new ExplicitInterpolationBasedExplicitRefiner(config, pathFormulaManager);
    }
    else{
      refiner = new SmtBasedExplicitRefiner(config, pathFormulaManager, formulaManager);
    }
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARTReachedSet reached, final Path errorPath)
      throws CPAException, InterruptedException {

    refiner.setCurrentErrorPath(errorPath);

    if(!refiner.hasMadeProgress(errorPath)) {
      List<CFAEdge> cfaTrace = new ArrayList<CFAEdge>();
      for(Pair<ARTElement, CFAEdge> pathElement : errorPath){
        cfaTrace.add(pathElement.getSecond());
      }

      fullPrecisionCheckIsFeasable = isPathFeasable(cfaTrace);

      throw new RefinementFailedException(Reason.RepeatedCounterexample, null);
    }

    return super.performRefinement(reached, errorPath);
  }

  @Override
  protected final List<Pair<ARTElement, CFANode>> transformPath(Path errorPath) {
    return refiner.transformPath(errorPath);
  }

  @Override
  protected List<Formula> getFormulasForPath(List<Pair<ARTElement, CFANode>> errorPath, ARTElement initialElement)
      throws CPATransferException {
    return refiner.getFormulasForPath(errorPath, initialElement);
  }

  @Override
  protected void performRefinement(
      ARTReachedSet pReached,
      List<Pair<ARTElement, CFANode>> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> counterexampleTraceInfo,
      boolean pRepeatedCounterexample)
      throws CPAException {

    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastElement());

    //precisionUpdate.start();
    Pair<ARTElement, Precision> result = refiner.performRefinement(oldPrecision, errorPath, counterexampleTraceInfo);
    //precisionUpdate.stop();

    //artUpdate.start();
    ARTElement root = result.getFirst();

    logger.log(Level.FINEST, "Found spurious counterexample,",
        "trying strategy 1: remove everything below", root, "from ART.");

    pReached.removeSubtree(root, result.getSecond());

    //artUpdate.stop();
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

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    super.printStatistics(out, result, reached);
/*
    if(useExplicitInterpolation) {
      out.println("Explicit Interpolator:");
      out.println("  number of counter-example checks:         " + numberOfCounterExampleChecks);
      out.println("  total number of elements in error paths:  " + numberOfErrorPathElements);
      out.println("  percentage of elements checked:           " + (Math.round(((double)numberOfCounterExampleChecks / (double)numberOfErrorPathElements) * 10000) / 100.00) + "%");
      out.println("  max. time for singe check:            " + timerCounterExampleChecks.printMaxTime());
      out.println("  total time for checks:                " + timerCounterExampleChecks);
    } else {
      out.println("Explicit Refiner:");
      out.println("  number of explicit refinements:            " + numberOfExplicitRefinements);

      if(predicateCpaAvailable)
        out.println("  number of predicate refinements:           " + numberOfPredicateRefinements);

      out.println("  max. time for syntactical path analysis:   " + timerSyntacticalPathAnalysis.printMaxTime());
      out.println("  total time for syntactical path analysis:  " + timerSyntacticalPathAnalysis);
    }
    out.println("  full-precision-check is feasable:        " + ((fullPrecCheckIsFeasable == null) ? " - " : (fullPrecCheckIsFeasable ? "yes" : "no")));
    */
  }
}