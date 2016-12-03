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

import com.google.common.collect.Multimap;
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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolantManager;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.GenericRefiner;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.PathInterpolator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Refiner for value analysis using symbolic values.
 */
@Options(prefix = "cpa.value.refinement")
public class SymbolicValueAnalysisRefiner
    extends GenericRefiner<ForgettingCompositeState, SymbolicInterpolant> {

  @Option(secure = true, description = "whether or not to do lazy-abstraction", name = "restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

  public static SymbolicValueAnalysisRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ARGCPA argCpa = retrieveCPA(pCpa, ARGCPA.class);
    final ValueAnalysisCPA valueAnalysisCpa = retrieveCPA(pCpa, ValueAnalysisCPA.class);
    final ConstraintsCPA constraintsCpa = retrieveCPA(pCpa, ConstraintsCPA.class);

    final Configuration config = valueAnalysisCpa.getConfiguration();

    valueAnalysisCpa.injectRefinablePrecision();
    constraintsCpa.injectRefinablePrecision(new RefinableConstraintsPrecision(config));

    final LogManager logger = valueAnalysisCpa.getLogger();
    final CFA cfa = valueAnalysisCpa.getCFA();
    final ShutdownNotifier shutdownNotifier = valueAnalysisCpa.getShutdownNotifier();

    final Solver solver = constraintsCpa.getSolver();

    final SymbolicStrongestPostOperator strongestPostOperator =
        new ValueTransferBasedStrongestPostOperator(solver, logger, config, cfa, shutdownNotifier);

    final SymbolicFeasibilityChecker feasibilityChecker =
        new SymbolicValueAnalysisFeasibilityChecker(strongestPostOperator,
                                                    config,
                                                    logger,
                                                    cfa);


    final GenericPrefixProvider<ForgettingCompositeState> prefixProvider =
        new GenericPrefixProvider<>(
            strongestPostOperator,
            ForgettingCompositeState.getInitialState(cfa.getMachineModel()),
            logger,
            cfa,
            config,
            ValueAnalysisCPA.class);

    final ElementTestingSymbolicEdgeInterpolator edgeInterpolator =
        new ElementTestingSymbolicEdgeInterpolator(feasibilityChecker,
                                        strongestPostOperator,
                                        SymbolicInterpolantManager.getInstance(),
                                        config,
                                        shutdownNotifier,
                                        cfa);

    final SymbolicPathInterpolator pathInterpolator =
        new SymbolicPathInterpolator(edgeInterpolator,
                                    feasibilityChecker,
                                    prefixProvider,
                                    config,
                                    logger,
                                    shutdownNotifier,
                                    cfa);

    return new SymbolicValueAnalysisRefiner(argCpa,
        feasibilityChecker,
        pathInterpolator,
        new PathExtractor(logger, config),
        config,
        logger);
  }

  public SymbolicValueAnalysisRefiner(final ARGCPA pCpa,
      final FeasibilityChecker<ForgettingCompositeState> pFeasibilityChecker,
      final PathInterpolator<SymbolicInterpolant> pInterpolator,
      final PathExtractor pPathExtractor,
      final Configuration pConfig,
      final LogManager pLogger
  ) throws InvalidConfigurationException {

    super(pCpa,
          pFeasibilityChecker,
          pInterpolator,
          SymbolicInterpolantManager.getInstance(),
          pPathExtractor,
          pConfig,
          pLogger);
  }

  @Override
  protected void refineUsingInterpolants(
      final ARGReachedSet pReached,
      final InterpolationTree<ForgettingCompositeState, SymbolicInterpolant> pInterpolants)
      throws InterruptedException {
    final Collection<ARGState> roots = pInterpolants.obtainRefinementRoots(restartStrategy);

    ARGTreePrecisionUpdater precUpdater = ARGTreePrecisionUpdater.getInstance();

    for (ARGState r : roots) {
      Multimap<CFANode, MemoryLocation> valuePrecInc = pInterpolants.extractPrecisionIncrement(r);
      ConstraintsPrecision.Increment constrPrecInc =
          getConstraintsIncrement(r, pInterpolants);

      precUpdater.updateARGTree(pReached, r, valuePrecInc, constrPrecInc);
    }
  }

  private ConstraintsPrecision.Increment getConstraintsIncrement(
      final ARGState pRefinementRoot,
      final InterpolationTree<ForgettingCompositeState, SymbolicInterpolant> pTree
  ) {
    ConstraintsPrecision.Increment.Builder increment =
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
  protected void printAdditionalStatistics(
      PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    // DO NOTHING for now
  }
}
