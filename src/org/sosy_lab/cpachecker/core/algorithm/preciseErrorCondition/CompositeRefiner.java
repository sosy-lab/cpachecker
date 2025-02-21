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
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.SolverException;

public class CompositeRefiner implements Refiner {
  private static final int TIMEOUT_SECONDS = 180; // timeout for each refiner in seconds
  private final FormulaContext context;
  private final Map<RefinementStrategy, Refiner> refiners = new EnumMap<>(RefinementStrategy.class);
  private final Boolean parallelRefinement;
  private PathFormula exclusionFormula;

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
  }

  @Override
  public PathFormula refine(CounterexampleInfo pCounterexample)
      throws CPATransferException, InterruptedException, InvalidConfigurationException,
             SolverException {
    context.getLogger().log(Level.INFO,
        "******************************** Refinement ********************************");
    // single
    if (refiners.size() == 1) {
      return singleRefinement(pCounterexample);
    }
    // sequential
    if (!parallelRefinement) {
      return sequentialRefinement(pCounterexample);
    }
    // parallel
    if (refiners.size() >= 2) {
      return parallelRefinement(pCounterexample);
    }

    // Fallback TODO better handling fallback
    context.getLogger()
        .log(Level.SEVERE, "All Refiners Failed. Returning An Empty Exclusion Formula.");
    return context.getManager().makeEmptyPathFormula();
  }

  private PathFormula singleRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Single Refinement");
    try {
      // update exclusion formula with result
      exclusionFormula = refineWith(refiners.values().stream().toList().get(0), pCounterexample);
      return exclusionFormula;
    } catch (Exception e) {
      context.getLogger().logfUserException(Level.SEVERE, e, "Error During Refinement.");
    }
    context.getLogger()
        .log(Level.SEVERE, "Error During Refinement. Returning An Empty Exclusion Formula.");
    return context.getManager().makeEmptyPathFormula();
  }

  private PathFormula sequentialRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Sequential Refinement");
    for (Refiner refiner : refiners.values()) {
      try {
        // update exclusion formula with result
        exclusionFormula = refineWith(refiner, pCounterexample);
        return exclusionFormula;
      } catch (Exception e) {
        context.getLogger()
            .log(Level.WARNING, "Refiner Failed: " + refiner.getClass().getSimpleName());
        context.getLogger().logfUserException(Level.WARNING, e, "Error During Refinement.");
        context.getLogger().log(Level.INFO, "Trying With Next Refiner");
      }
    }
    context.getLogger().log(Level.SEVERE,
        "Error During Sequential Refinement. Returning An Empty Exclusion Formula.");
    return context.getManager().makeEmptyPathFormula();
  }


  private PathFormula parallelRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Parallel Refinement");
    ExecutorService executor = Executors.newFixedThreadPool(refiners.size());

    try {
      List<Callable<PathFormula>> tasks = new ArrayList<>();
      refiners.values().forEach(
          (pRefiner) -> tasks.add(() -> refineWith(pRefiner, pCounterexample)));

      // execute tasks with a timeout and return the first successful result
      exclusionFormula = executor.invokeAny(tasks, TIMEOUT_SECONDS,
          TimeUnit.SECONDS);
      executor.shutdown();
      return exclusionFormula;

    } catch (TimeoutException e) {
      context.getLogger()
          .logfUserException(Level.SEVERE, e, "Refinement Timed Out For All Strategies.");
    } catch (Exception e) {
      context.getLogger().logfUserException(Level.SEVERE, e, "Error During Parallel Refinement.");
    }
    context.getLogger().log(Level.SEVERE,
        "Error During Parallel Refinement. Returning An Empty Exclusion Formula.");
    return context.getManager().makeEmptyPathFormula();
  }

  private void initializeRefiners(RefinementStrategy[] pRefiners, Solvers pQuantifierSolver)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    for (RefinementStrategy refiner : pRefiners) {
      refiners.put(refiner, RefinerFactory.createRefiner(refiner, context, pQuantifierSolver));
    }
  }

  private PathFormula refineWith(
      Refiner refiner,
      CounterexampleInfo cex)
      throws SolverException, CPATransferException, InterruptedException,
             InvalidConfigurationException {
    context.getLogger()
        .log(Level.INFO, "Starting Refinement With " + refiner.getClass().getSimpleName());
    PathFormula result = refiner.refine(cex);
    context.getLogger()
        .log(Level.INFO, refiner.getClass().getSimpleName() + " Completed Successfully.");
    return result;
  }

}
