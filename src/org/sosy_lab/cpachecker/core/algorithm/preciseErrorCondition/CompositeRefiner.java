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
      Boolean pParallelRefinement,
      Boolean pWithFormatter)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    context = pContext;
    exclusionFormula = context.getManager().makeEmptyPathFormula(); // initially empty
    parallelRefinement = pParallelRefinement;
    initializeRefiners(pRefiners, pQuantifierSolver, pWithFormatter);
  }

  @Override
  public PathFormula refine(CounterexampleInfo pCounterexample)
      throws CPATransferException, InterruptedException, InvalidConfigurationException,
             SolverException {
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
    PathFormula emptyFormula = context.getManager().makeEmptyPathFormula();

    for (Refiner refiner : refiners.values()) {
      try {
        PathFormula result = refineWith(refiner, pCounterexample);

        // check if the result is valid (not empty)
        if (!isFormulaEmpty(result, emptyFormula)) {
          exclusionFormula = result;
          return exclusionFormula;
        } else {
          context.getLogger().log(Level.WARNING,
              "Refiner " + refiner.getClass().getSimpleName() + " returned an invalid result.");
          context.getLogger().log(Level.INFO, "Trying With Next Refiner");
        }

      } catch (Exception e) {
        context.getLogger()
            .log(Level.WARNING, "Refiner Failed: " + refiner.getClass().getSimpleName());
        context.getLogger().logfUserException(Level.WARNING, e, "Error During Refinement.");
        context.getLogger().log(Level.INFO, "Trying With Next Refiner");
      }
    }

    context.getLogger().log(Level.SEVERE,
        "All refiners failed during sequential refinement. Returning an empty exclusion formula.");
    return emptyFormula;
  }


  private PathFormula parallelRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Parallel Refinement");
    ExecutorService executor = Executors.newFixedThreadPool(refiners.size());

    try {
      List<Callable<PathFormula>> tasks = new ArrayList<>();
      refiners.values().forEach((pRefiner) -> tasks.add(() -> {
        // Check for interruption before starting
        if (Thread.interrupted()) {
          throw new InterruptedException("Task interrupted before execution");
        }
        return refineWithInterruptible(pRefiner, pCounterexample);
      }));

      // execute tasks with a timeout and return the first successful result
      exclusionFormula = executor.invokeAny(tasks, TIMEOUT_SECONDS, TimeUnit.SECONDS);
      return exclusionFormula;

    } catch (TimeoutException e) {
      context.getLogger()
          .logfUserException(Level.SEVERE, e, "Refinement Timed Out For All Strategies.");
    } catch (Exception e) {
      context.getLogger().logfUserException(Level.SEVERE, e, "Error During Parallel Refinement.");
    } finally {
      // shutdown the executor to cancel all ongoing tasks
      executor.shutdownNow(); // This sends interrupt signals to all threads
      try {
        // wait and allow tasks to respond to interrupts
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          context.getLogger()
              .log(Level.WARNING, "Some refinement tasks did not terminate promptly");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    context.getLogger().log(Level.SEVERE,
        "Error During Parallel Refinement. Returning An Empty Exclusion Formula.");
    return context.getManager().makeEmptyPathFormula();
  }

  private void initializeRefiners(
      RefinementStrategy[] pRefiners,
      Solvers pQuantifierSolver,
      Boolean pWithFormatter)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    for (RefinementStrategy refiner : pRefiners) {
      FormulaContext newContext = context;
      if (parallelRefinement && pRefiners.length > 1) {
        // create new context for each refiner when in parallel mode
        newContext = context.createContextFromThis(context.getSolver().getSolverName().toString());
      }
      refiners.put(refiner,
          RefinerFactory.createRefiner(refiner, newContext, pQuantifierSolver, pWithFormatter));
    }
  }

  private PathFormula refineWith(Refiner refiner, CounterexampleInfo cex)
      throws SolverException, CPATransferException, InterruptedException,
             InvalidConfigurationException {
    context.getLogger().log(Level.INFO, String.format("*** Starting Refinement With %s... ***",
        refiner.getClass().getSimpleName()));
    PathFormula result = refiner.refine(cex);
    context.getLogger().log(Level.INFO,
        String.format("*** %s Completed Successfully. ***", refiner.getClass().getSimpleName()));
    return result;
  }

  /**
   * Checks if the result is equivalent to the empty formula.
   * TODO: eventually replace with a more robust check if necessary.
   */
  private boolean isFormulaEmpty(PathFormula result, PathFormula emptyFormula) {
    return result.getFormula().equals(emptyFormula.getFormula());
  }

  private PathFormula refineWithInterruptible(Refiner refiner, CounterexampleInfo cex)
      throws SolverException, CPATransferException, InterruptedException,
             InvalidConfigurationException {
    context.getLogger().log(Level.INFO, String.format("*** Starting Refinement With %s... ***",
        refiner.getClass().getSimpleName()));
    try {
      PathFormula result = refiner.refine(cex);
      //  check for interruption
      if (Thread.interrupted()) {
        throw new InterruptedException("Refiner was interrupted");
      }
      context.getLogger().log(Level.INFO,
          String.format("*** %s Completed Successfully. ***", refiner.getClass().getSimpleName()));
      return result;
    } catch (InterruptedException e) {
      context.getLogger().log(Level.INFO,
          String.format("*** %s Interrupted. ***", refiner.getClass().getSimpleName()));
      throw e; // Re-throw to handle in invokeAny
    }
  }

}
