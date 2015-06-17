/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisRefiner;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolantManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.GenericRefiner;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.collect.Multimap;

/**
 * Refiner for value analysis using symbolic values.
 */
@Options(prefix = "cpa.value.refinement")
public class SymbolicValueAnalysisRefiner
    extends GenericRefiner<ForgettingCompositeState, SymbolicInterpolant> {

  @Option(secure = true, description = "whether or not to do lazy-abstraction", name = "restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

  private final ValueAnalysisRefiner explicitOnlyRefiner;

  public static SymbolicValueAnalysisRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    final ConstraintsCPA constraintsCpa = CPAs.retrieveCPA(pCpa, ConstraintsCPA.class);

    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(SymbolicValueAnalysisRefiner.class.getSimpleName()
          + " needs a ValueAnalysisCPA");
    }

    if (constraintsCpa == null) {
      throw new InvalidConfigurationException(SymbolicValueAnalysisRefiner.class.getSimpleName()
          + " needs a ConstraintsCPA");
    }

    final Configuration config = valueAnalysisCpa.getConfiguration();

    valueAnalysisCpa.injectRefinablePrecision();
    constraintsCpa.injectRefinablePrecision(new RefinableConstraintsPrecision(config));

    final LogManager logger = valueAnalysisCpa.getLogger();
    final CFA cfa = valueAnalysisCpa.getCFA();;
    final ShutdownNotifier shutdownNotifier = valueAnalysisCpa.getShutdownNotifier();

    final Solver solver = Solver.create(config, logger, shutdownNotifier);

    final SymbolicStrongestPostOperator strongestPostOperator =
        new ValueTransferBasedStrongestPostOperator(solver, logger, config, cfa, shutdownNotifier);

    final SymbolicFeasibilityChecker feasibilityChecker =
        new SymbolicValueAnalysisFeasibilityChecker(strongestPostOperator,
                                                    config,
                                                    logger,
                                                    cfa,
                                                    shutdownNotifier);


    final GenericPrefixProvider<ForgettingCompositeState> prefixProvider =
        new GenericPrefixProvider<>(strongestPostOperator,
                                    ForgettingCompositeState.getInitialState(),
                                    logger, cfa, config, ValueAnalysisCPA.class);

    final PrefixSelector prefixSelector =
        new PrefixSelector(cfa.getVarClassification(), cfa.getLoopStructure());

    final SymbolicEdgeInterpolator edgeInterpolator =
        new SymbolicEdgeInterpolator(feasibilityChecker,
                                        strongestPostOperator,
                                        SymbolicInterpolantManager.getInstance(),
                                        config,
                                        shutdownNotifier,
                                        cfa);

    final SymbolicPathInterpolator pathInterpolator =
        new SymbolicPathInterpolator(edgeInterpolator,
                                    feasibilityChecker,
                                    prefixProvider,
                                    prefixSelector,
                                    config,
                                    logger,
                                    shutdownNotifier,
                                    cfa);

    SymbolicValueAnalysisRefiner refiner = new SymbolicValueAnalysisRefiner(
        feasibilityChecker,
        pathInterpolator,
        new PathExtractor(logger),
        config,
        logger,
        shutdownNotifier,
        cfa,
        valueAnalysisCpa);

    return refiner;
  }

  protected SymbolicValueAnalysisRefiner(
      final SymbolicFeasibilityChecker pFeasibilityChecker,
      final SymbolicPathInterpolator pInterpolator,
      final PathExtractor pPathExtractor,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa,
      final ValueAnalysisCPA pValueCpa
  ) throws InvalidConfigurationException {

    super(pFeasibilityChecker,
          pInterpolator,
          SymbolicInterpolantManager.getInstance(),
          pPathExtractor,
          pConfig,
          pLogger,
          pShutdownNotifier,
          pCfa);

    explicitOnlyRefiner = ValueAnalysisRefiner.create(pValueCpa);
  }

  @Override
  public CounterexampleInfo performRefinement(
      final ARGReachedSet pReachedSet
  ) throws CPAException {

    try {
      // Perform refinement without symbolic values first.
      // We require a lot less resources if it is possible to identify the target states as
      // infeasible without using symbolic values (and as such, also without SAT checks).
      final CounterexampleInfo explicitOnlyCex = explicitOnlyRefiner.performRefinement(pReachedSet);
      if (!explicitOnlyCex.isSpurious()) {
        final CounterexampleInfo symbolicValueCex = super.performRefinement(pReachedSet);

        // if the symbolic refiner reports a target state as feasible, we return the value
        // analysis's counterexample. It might describe a path actually infeasible according to the
        // symbolic value analysis, but we get a detailed counterexample, in exchange. (symbolic
        // value analysis's counterexample has an empty model, at the moment)
        if (!symbolicValueCex.isSpurious()) {
          return explicitOnlyCex;
        } else {
          return symbolicValueCex;
        }

      } else {
        return CounterexampleInfo.spurious();
      }
    } catch (InterruptedException e) {
      throw new CPAException("Error while performing refinement", e);
    }
  }

  @Override
  protected void refineUsingInterpolants(
      final ARGReachedSet pReached,
      final InterpolationTree<ForgettingCompositeState, SymbolicInterpolant> pInterpolants
  ) {
    final Collection<ARGState> roots = pInterpolants.obtainRefinementRoots(restartStrategy);

    ARGTreePrecisionUpdater precUpdater = ARGTreePrecisionUpdater.getInstance();

    for (ARGState r : roots) {
      Multimap<CFANode, MemoryLocation> valuePrecInc = pInterpolants.extractPrecisionIncrement(r);
      ConstraintsPrecision.Increment<Constraint> constrPrecInc =
          getConstraintsIncrement(r, pInterpolants);

      precUpdater.updateARGTree(pReached, r, valuePrecInc, constrPrecInc);
    }
  }

  private ConstraintsPrecision.Increment<Constraint> getConstraintsIncrement(
      final ARGState pRefinementRoot,
      final InterpolationTree<ForgettingCompositeState, SymbolicInterpolant> pTree
  ) {
    ConstraintsPrecision.Increment.Builder<Constraint> increment =
        ConstraintsPrecision.Increment.builder();

    Deque<ARGState> todo =
        new ArrayDeque<>(Collections.singleton(pTree.getPredecessor(pRefinementRoot)));

    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (!currentState.isTarget()) {
        SymbolicInterpolant itp = pTree.getInterpolantForState(currentState);

        if (itp != null && !itp.isTrivial()) {
          for (Constraint c : itp.getConstraints()) {
            increment.locallyTracked(AbstractStates.extractLocation(currentState), c);
          }
        }
      }

      Collection<ARGState> successors = pTree.getSuccessors(currentState);
      todo.addAll(successors);
    }

    return increment.build();
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    explicitOnlyRefiner.collectStatistics(pStatsCollection);
    super.collectStatistics(pStatsCollection);
  }

  @Override
  protected void printAdditionalStatistics(PrintStream out, Result pResult,
      ReachedSet pReached) {
    // DO NOTHING for now
  }
}
