// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.SolverException;

public class CompositeRefiner implements Refiner {
  private static final int TIMEOUT_SECONDS = 180; // timeout for each refiner in seconds
  private final FormulaContext context;
  private PathFormula exclusionFormula;
  private final Map<RefinementStrategy, Refiner> refiners = new EnumMap<>(RefinementStrategy.class);
  private final ExecutorService executor;
  private final Boolean parallelRefinement;
  private final StatTimer refinementTimer =
      new StatTimer("Total time for refinement.");

  public CompositeRefiner(
      FormulaContext pContext,
      RefinementStrategy[] pRefiners,
      Solvers pQuantifierSolver,
      Boolean pParallelRefinement)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    context = pContext;
    exclusionFormula = context.getManager().makeEmptyPathFormula(); // initially empty
    initializeRefiners(pRefiners, pQuantifierSolver);
    parallelRefinement = pParallelRefinement;
    executor = Executors.newFixedThreadPool(pRefiners.length); // 2 threads for parallel refinement
  }

  @Override
  public PathFormula refine(CounterexampleInfo pCounterexample)
      throws CPATransferException, InterruptedException, InvalidConfigurationException,
             SolverException {

    if (refiners.size() == 1) {
      refinementTimer.start();
      context.getLogger().log(Level.INFO, "Parallel Refinement is disabled, one refiner");

        RefinerResult result = refineWith(refiners.get(RefinementStrategy.QUANTIFIER_ELIMINATION), pCounterexample);

        // execute tasks with a timeout and return the first successful result
        exclusionFormula = result.getExclusionFormula(); // update exclusion formula with result
        refinementTimer.stop();
        return exclusionFormula;
    }

    if (parallelRefinement && refiners.size() >= 2) {
      refinementTimer.start();
      context.getLogger().log(Level.INFO, "Parallel Refinement is enabled");
      try {
        List<Callable<RefinerResult>> tasks = new ArrayList<>();
        refiners.values().forEach(
            (pRefiner)
                -> tasks.add(() -> refineWith(pRefiner, pCounterexample)));

        // execute tasks with a timeout and return the first successful result
        RefinerResult result = executor.invokeAny(tasks, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        exclusionFormula = result.getExclusionFormula(); // update exclusion formula with result
        refinementTimer.stop();
        return exclusionFormula;

      } catch (TimeoutException e) {
        context.getLogger().logfUserException(Level.SEVERE, e, "Refinement timed out for all strategies.");
      } catch (Exception e) {
        context.getLogger().logfUserException(Level.SEVERE, e, "Error during parallel refinement.");
      }

    }
    if (!parallelRefinement) {
      // TODO: handle non parallel
    }

    // Fallback TODO better handling fallback
    {
      context.getLogger()
          .log(Level.WARNING, "All refiners failed. Returning an empty exclusion formula.");
    }
    return context.getManager().makeEmptyPathFormula();
  }

  private void initializeRefiners(RefinementStrategy[] pRefiners, Solvers pQuantifierSolver)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    for (RefinementStrategy refiner : pRefiners) {
      refiners.put(refiner, RefinerFactory.createRefiner(refiner, context, pQuantifierSolver));
    }
  }

  private RefinerResult refineWith(
      Refiner refiner,
      CounterexampleInfo cex)
      throws SolverException, CPATransferException, InterruptedException,
             InvalidConfigurationException {
    context.getLogger()
        .log(Level.INFO, "Starting refinement with " + refiner.getClass().getSimpleName());
    PathFormula result = refiner.refine(cex);
    context.getLogger()
        .log(Level.INFO, refiner.getClass().getSimpleName() + " completed successfully.");
    return new RefinerResult(refiner.getClass().getSimpleName(), result);
  }


  public PathFormula getExclusionFormula() {
    return exclusionFormula;
  }

  public void shutdown() {
    executor.shutdown();
  }

  private static class RefinerResult {
    private final String refinerName;
    private final PathFormula exclusionFormula;

    public RefinerResult(String pRefinerName, PathFormula pExclusionFormula) {
      this.refinerName = pRefinerName;
      this.exclusionFormula = pExclusionFormula;
    }

    public String getRefinerName() {
      return refinerName;
    }

    public PathFormula getExclusionFormula() {
      return exclusionFormula;
    }
  }
}
