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
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.RefinementResult.RefinementStatus;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.SolverException;

public class CompositeRefiner implements Refiner {
  private final FormulaContext context;
  private final Map<RefinementStrategy, Refiner> refiners = new EnumMap<>(RefinementStrategy.class);
  private final Boolean parallelRefinement;
  private final int refinerTimeout; // timeout for each refiner in seconds
  private RefinementResult refinementResult;

  public CompositeRefiner(
      FormulaContext pContext,
      RefinementStrategy[] pRefiners,
      Solvers pQuantifierSolver,
      Boolean pParallelRefinement,
      Boolean pWithFormatter,
      int pRefinerTimeout)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    context = pContext;
    refinementResult =
        new RefinementResult(RefinementStatus.EMPTY, context.getManager().makeEmptyPathFormula());
    parallelRefinement = pParallelRefinement;
    refinerTimeout = pRefinerTimeout;
    initializeRefiners(pRefiners, pQuantifierSolver, pWithFormatter);
  }

  @Override
  public RefinementResult refine(CounterexampleInfo pCounterexample)
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

    context.getLogger()
        .log(Level.SEVERE, "All Refiners Failed.");
    refinementResult.updateStatus(RefinementStatus.FAILURE);
    return refinementResult;
  }

  private RefinementResult singleRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Single Refinement");
    try {
      // update exclusion formula with result
      Refiner singleRefiner = refiners.values().stream().toList().get(0);
      return refineWith(singleRefiner, pCounterexample);
    } catch (Exception e) {
      context.getLogger().logfUserException(Level.SEVERE, e, "Error During Refinement.");
    }
    context.getLogger()
        .log(Level.SEVERE, "Error During Refinement.");
    refinementResult.updateStatus(RefinementStatus.FAILURE);
    return refinementResult;
  }

  private RefinementResult sequentialRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Sequential Refinement");

    for (Refiner refiner : refiners.values()) {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<RefinementResult> future =
          executor.submit(() -> refineWithInterruptible(refiner, pCounterexample));
      boolean tryNextRefiner = false;

      try {
        // Wait for result of the refiner with a timeout
        refinementResult = future.get(refinerTimeout, TimeUnit.SECONDS);
        if (refinementResult.isSuccessful()) {
          return refinementResult;
        }
        tryNextRefiner = true;

      } catch (TimeoutException e) {
        context.getLogger().log(Level.WARNING,
            "Refiner " + refiner.getClass().getSimpleName() + " timed out after " + refinerTimeout
                + "s");
        future.cancel(true); // interrupt the refiner in case of timeout
        refinementResult.updateStatus(RefinementStatus.TIMEOUT);
        tryNextRefiner = true;

      } catch (Exception e) {
        context.getLogger().log(Level.WARNING,
            "Refiner Failed: " + refiner.getClass().getSimpleName());
        context.getLogger().logfUserException(Level.WARNING, e, "Error During Refinement.");
        refinementResult.updateStatus(RefinementStatus.FAILURE);
        tryNextRefiner = true;

      } finally {
        executor.shutdownNow(); // cleanup
        if (tryNextRefiner) {
          context.getLogger().log(Level.INFO, "Trying With Next Refiner");
        }
      }
    }

    context.getLogger().log(Level.SEVERE,
        "All refiners failed during sequential refinement.");
    return refinementResult;
  }


  private RefinementResult parallelRefinement(CounterexampleInfo pCounterexample) {
    context.getLogger().log(Level.INFO, "Parallel Refinement");
    ExecutorService executor = Executors.newFixedThreadPool(refiners.size());
    CompletionService<RefinementResult> completionService =
        new ExecutorCompletionService<>(executor);
    List<Future<RefinementResult>> futures = new ArrayList<>();

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
        // TODO: remove this calculation and just use the refinerTimeout instead
        // Calculate remaining time
        final long elapsedNanos = System.nanoTime() - startTime;
        final long remainingNanos = timeoutNanos - elapsedNanos;

        if (remainingNanos <= 0) {
          throw new TimeoutException("Parallel refinement timed out");
        }

        // next completed task (waits up to remaining time)
        Future<RefinementResult> future = completionService.poll(
            remainingNanos, TimeUnit.NANOSECONDS
        );

        if (future == null) { // Timeout
          break;
        }

        remainingTasks--;

        try {
          refinementResult = future.get();
          if (refinementResult.isSuccessful()) {
            // Valid result found -> cancel other tasks and return
            cancelAllFutures(futures);
            return refinementResult;
          }
        } catch (ExecutionException e) {
          context.getLogger().log(Level.WARNING,
              "Refiner failed: " + e.getCause().getMessage());
        }
      }

      context.getLogger().log(Level.SEVERE,
          "All parallel refiners failed or timed out");
      refinementResult.updateStatus(RefinementStatus.FAILURE);
      return refinementResult;

    } catch (TimeoutException e) {
      context.getLogger().logfUserException(Level.SEVERE, e, "Parallel Refinement timed out.");
      refinementResult.updateStatus(RefinementStatus.TIMEOUT);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      context.getLogger().log(Level.SEVERE, "Parallel refinement interrupted");
      refinementResult.updateStatus(RefinementStatus.INTERRUPTED);
    } finally {
      // clean-up
      cancelAllFutures(futures);
      executor.shutdownNow();
    }
    refinementResult.updateStatus(RefinementStatus.FAILURE);
    return refinementResult;
  }

  private void cancelAllFutures(List<Future<RefinementResult>> futures) {
    futures.forEach(f -> f.cancel(true));
  }

  private void initializeRefiners(
      RefinementStrategy[] pRefiners,
      Solvers pQuantifierSolver,
      Boolean pWithFormatter)
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    for (RefinementStrategy refiner : pRefiners) {
      refiners.put(refiner,
          RefinerFactory.createRefiner(refiner, context, pQuantifierSolver, pWithFormatter));
    }
  }

  private RefinementResult refineWith(Refiner refiner, CounterexampleInfo cex)
      throws SolverException, CPATransferException, InterruptedException,
             InvalidConfigurationException {
    context.getLogger().log(Level.INFO, String.format("*** Starting Refinement With %s... ***",
        refiner.getClass().getSimpleName()));

    RefinementResult result = refiner.refine(cex);
    context.getLogger().log(Level.INFO,
        String.format("*** %s Completed Successfully. ***", refiner.getClass().getSimpleName()));
    return result;
  }

  private RefinementResult refineWithInterruptible(Refiner refiner, CounterexampleInfo cex)
      throws SolverException, CPATransferException, InterruptedException,
             InvalidConfigurationException {
    context.getLogger().log(Level.INFO, String.format("*** Starting Refinement With %s... ***",
        refiner.getClass().getSimpleName()));
    try {
      RefinementResult result = refiner.refine(cex);
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
