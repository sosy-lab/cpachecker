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
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitCPA;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefinementManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Precisions;
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

import com.google.common.collect.Multimap;

public class DelegatingExplicitRefiner
  extends AbstractInterpolationBasedRefiner<Collection<AbstractionPredicate>> {

  /**
   * refiner used for explicit interpolation refinement
   */
  private ExplicitInterpolationBasedExplicitRefiner explicitInterpolatingRefiner;

  /**
   * backup-refiner used for predicate refinement, when explicit refinement fails (due to lack of expressiveness)
   */
  private PredicatingExplicitRefiner predicatingRefiner;

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

    FormulaManagerFactory factory               = null;
    ExtendedFormulaManager formulaManager       = null;
    PathFormulaManager pathFormulaManager       = null;
    Solver solver                               = null;
    AbstractionManager absManager               = null;
    PredicateRefinementManager manager          = null;

    PredicateCPA predicateCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);
    if(predicateCpa != null) {
      factory                     = predicateCpa.getFormulaManagerFactory();
      formulaManager              = predicateCpa.getFormulaManager();
      pathFormulaManager          = predicateCpa.getPathFormulaManager();
      solver                      = predicateCpa.getSolver();
      absManager                  = predicateCpa.getAbstractionManager();
    } else {
      factory                     = new FormulaManagerFactory(config, logger);
      TheoremProver theoremProver = factory.createTheoremProver();
      RegionManager regionManager = BDDRegionManager.getInstance(config, logger);
      formulaManager              = new ExtendedFormulaManager(factory.getFormulaManager(), config, logger);
      pathFormulaManager          = new PathFormulaManagerImpl(formulaManager, config, logger, explicitCpa.getMachineModel());
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
        manager);
  }

  protected DelegatingExplicitRefiner(
      final Configuration config,
      final LogManager logger,
      final ConfigurableProgramAnalysis cpa,
      final ExtendedFormulaManager formulaManager,
      final PathFormulaManager pathFormulaManager,
      final PredicateRefinementManager interpolationManager) throws CPAException, InvalidConfigurationException {

    super(config, logger, cpa, interpolationManager);

    explicitInterpolatingRefiner = new ExplicitInterpolationBasedExplicitRefiner(config, pathFormulaManager);

    if(((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class) != null) {
      predicatingRefiner = new PredicatingExplicitRefiner();
    }
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final Path errorPath)
      throws CPAException, InterruptedException {

    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    Precision precision = reachedSet.getPrecision(reachedSet.getLastState());

    Multimap<CFANode, String> precisionIncrement = null;

    ARGState interpolationPoint = null;

    precisionIncrement = explicitInterpolatingRefiner.determinePrecisionIncrement(reachedSet, errorPath);
    interpolationPoint = explicitInterpolatingRefiner.determineInterpolationPoint(errorPath);

    if(precisionIncrement.size() > 0) {
      ExplicitPrecision explicitPrecision = Precisions.extractPrecisionByType(precision, ExplicitPrecision.class);
      explicitPrecision                   = new ExplicitPrecision(explicitPrecision);
      explicitPrecision.getCegarPrecision().addToMapping(precisionIncrement);

      reached.removeSubtree(interpolationPoint, explicitPrecision);

      return CounterexampleInfo.spurious();
    }

    else if(predicatingRefiner == null) {
      return CounterexampleInfo.feasible(errorPath, null);
    }

    else {
      return super.performRefinement(reached, errorPath);
    }
  }

  @Override
  protected final List<ARGState> transformPath(Path errorPath) {
    return predicatingRefiner.transformPath(errorPath);
  }

  @Override
  protected List<Formula> getFormulasForPath(List<ARGState> errorPath, ARGState initialElement)
      throws CPATransferException {
    return predicatingRefiner.getFormulasForPath(errorPath, initialElement);
  }

  @Override
  protected void performRefinement(
      ARGReachedSet pReached,
      List<ARGState> errorPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> counterexampleTraceInfo,
      boolean pRepeatedCounterexample)
      throws CPAException {

    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastState());

    Pair<ARGState, Precision> result = predicatingRefiner.performRefinement(reached, oldPrecision, errorPath, counterexampleTraceInfo);

    ARGState root = result.getFirst();
    logger.log(Level.FINEST, "Found spurious counterexample,",
        "trying strategy 1: remove everything below", root, "from ART.");
    pReached.removeSubtree(root, result.getSecond());
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    super.printStatistics(out, result, reached);

    out.println("Explicit Refinement:");

    explicitInterpolatingRefiner.printStatistics(out, result, reached);

    if(predicatingRefiner != null) {
      predicatingRefiner.printStatistics(out, result, reached);
    }
  }
}