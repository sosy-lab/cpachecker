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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
  private final FormulaContext context;
  private final Map<RefinementStrategy, Refiner> refiners = new EnumMap<>(RefinementStrategy.class);
  private final Boolean parallelRefinement;
  private PathFormula exclusionFormula;
  private final int refinerTimeout; // timeout for each refiner in seconds

  public CompositeRefiner(
      FormulaContext pContext,
      RefinementStrategy[] pRefiners,
      Solvers pQuantifierSolver,
      Boolean pParallelRefinement,
      Boolean pWithFormatter,
      int pRefinerTimeout)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    context = pContext;
    exclusionFormula = context.getManager().makeEmptyPathFormula(); // initially empty
    parallelRefinement = pParallelRefinement;
    refinerTimeout = pRefinerTimeout;
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

    // TODO better fallback handling
    context.getLogger()
        .log(Level.SEVERE, "All Refiners Failed. Returning An Empty Exclusion Formula.");
    return context.getManager().makeEmptyPathFormula();
  }

  private PathFormula singleRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Single Refinement");
    try {
      // update exclusion formula with result
      Refiner singleRefiner = refiners.values().stream().toList().get(0);
      exclusionFormula = refineWith(singleRefiner, pCounterexample);
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
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<PathFormula> future = executor.submit(() -> refineWithInterruptible(refiner, pCounterexample));
      try {

        // Wait for result of the refiner with a timeout
        PathFormula result = future.get(refinerTimeout, TimeUnit.SECONDS);
        // empty formula here means the refinement has not worked as expected
        if (isFormulaEmpty(result)) {
          exclusionFormula = result;
          return exclusionFormula;
        }

      } catch (TimeoutException e) {
        context.getLogger().log(Level.WARNING,
            "Refiner " + refiner.getClass().getSimpleName() + " timed out after " + refinerTimeout
                + "s");
        future.cancel(true); // interrupt the refiner in case of timeout

      } catch (Exception e) {
        context.getLogger().log(Level.WARNING,
            "Refiner Failed: " + refiner.getClass().getSimpleName());
        context.getLogger().logfUserException(Level.WARNING, e, "Error During Refinement.");

      } finally {
        executor.shutdownNow(); // cleanup
        context.getLogger().log(Level.INFO, "Trying With Next Refiner");
      }
    }

    context.getLogger().log(Level.SEVERE,
        "All refiners failed during sequential refinement. Returning an empty exclusion formula.");
    return exclusionFormula;
  }


  private PathFormula parallelRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Parallel Refinement");
    ExecutorService executor = Executors.newFixedThreadPool(refiners.size());
    CompletionService<PathFormula> completionService = new ExecutorCompletionService<>(executor);
    List<Future<PathFormula>> futures = new ArrayList<>();

    try {
      // submit all refinement tasks
      refiners.values().forEach(refiner ->
          futures.add(completionService.submit(() ->
              refineWithInterruptible(refiner, pCounterexample))
          ));

      int remainingTasks = futures.size();
      final long startTime = System.nanoTime();
      final long timeoutNanos = TimeUnit.SECONDS.toNanos(refinerTimeout);

      while (remainingTasks > 0) {
        // Calculate remaining time
        final long elapsedNanos = System.nanoTime() - startTime;
        final long remainingNanos = timeoutNanos - elapsedNanos;

        if (remainingNanos <= 0) {
          throw new TimeoutException("Parallel refinement timed out");
        }

        // next completed task (waits up to remaining time)
        Future<PathFormula> future = completionService.poll(
            remainingNanos, TimeUnit.NANOSECONDS
        );

        if (future == null) { // Timeout occurred
          break;
        }

        remainingTasks--;

        try {
          PathFormula result = future.get();
          if (!isFormulaEmpty(result)) {
            // Valid result found - cancel other tasks and return
            cancelAllFutures(futures);
            exclusionFormula = result;
            return result;
          }
        } catch (ExecutionException e) {
          context.getLogger().log(Level.WARNING,
              "Refiner failed: " + e.getCause().getMessage());
        }
      }

      context.getLogger().log(Level.SEVERE,
          "All parallel refiners failed or timed out");
      return exclusionFormula;

    } catch (TimeoutException e) {
      context.getLogger().logfUserException(Level.SEVERE, e,
          "Parallel refinement timed out after " + refinerTimeout + "s");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      context.getLogger().log(Level.SEVERE, "Parallel refinement interrupted");
    } finally {
      // clean up
      cancelAllFutures(futures);
      executor.shutdownNow();
    }

    return exclusionFormula;
  }

  private void cancelAllFutures(List<Future<PathFormula>> futures) {
    futures.forEach(f -> f.cancel(true));
  }

  /**
   * Checks if a formula is equivalent to an empty formula.
   * TODO: eventually replace with a more robust check if necessary.
   */
  private boolean isFormulaEmpty(PathFormula pFormula) {
    PathFormula emptyFormula = context.getManager().makeEmptyPathFormula();
    return pFormula.getFormula().equals(emptyFormula.getFormula());
  }

  private void initializeRefiners(
      RefinementStrategy[] pRefiners,
      Solvers pQuantifierSolver,
      Boolean pWithFormatter)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    for (RefinementStrategy refiner : pRefiners) {
      FormulaContext newContext = context;
      if (pRefiners.length > 1) {
        // create new context for each refiner when in parallel and sequential mode
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
      context.getLogger().log(Level.WARNING,
          String.format("%s Interrupted.", refiner.getClass().getSimpleName()));
      throw e;
    }
  }

}
