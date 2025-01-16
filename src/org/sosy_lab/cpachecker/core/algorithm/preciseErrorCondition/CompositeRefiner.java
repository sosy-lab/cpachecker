// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import java.util.ArrayList;
import java.util.List;
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
  private PathFormula exclusionFormula;
  private final QuantiferEliminationRefiner quantifierEliminationRefiner;
  private final GenerateModelRefiner generateModelRefiner;
  private final AllSatRefiner allSatRefiner;
  private final ExecutorService executor;

  public CompositeRefiner(
      FormulaContext pContext, Solvers pQuantifierSolver) // TODO add a list of different refinements and a flag for parallelism
      throws InvalidConfigurationException, CPATransferException, InterruptedException {
    context = pContext;
    exclusionFormula = context.getManager().makeEmptyPathFormula(); // initially empty
    generateModelRefiner = new GenerateModelRefiner(context);
    System.out.printf("Refiner for %s\n",
        generateModelRefiner.getClass().getSimpleName()); // delete later
    allSatRefiner = new AllSatRefiner(context);
    quantifierEliminationRefiner = new QuantiferEliminationRefiner(context, pQuantifierSolver);
    // TODO make nThreads a passable variable
    executor = Executors.newFixedThreadPool(2); // 2 threads for parallel refinement

  }

  @Override
  public PathFormula refine(CounterexampleInfo pCounterexample)
      throws CPATransferException, InterruptedException, InvalidConfigurationException,
             SolverException {
    try {
      List<Callable<RefinerResult>> tasks = new ArrayList<>();
      //tasks.add(() -> refineWith("GenerateModelRefiner", generateModelRefiner, pCounterexample));
      tasks.add(() -> refineWith("QuantifierEliminationRefiner", quantifierEliminationRefiner,
          pCounterexample));
      tasks.add(() -> refineWith("AllSatRefiner", allSatRefiner, pCounterexample));

      // execute tasks with a timeout and return the first successful result
      RefinerResult result = executor.invokeAny(tasks, TIMEOUT_SECONDS, TimeUnit.SECONDS);
      exclusionFormula = result.getExclusionFormula(); // update exclusion formula with result
      return exclusionFormula;

    } catch (TimeoutException e) {
      context.getLogger().log(Level.SEVERE, "Refinement timed out for all strategies.", e);
    } catch (Exception e) {
      context.getLogger().log(Level.SEVERE, "Error during parallel refinement.", e);
    }

    // Fallback TODO better handling fallback
    context.getLogger()
        .log(Level.WARNING, "All refiners failed. Returning an empty exclusion formula.");
    return null;
  }

  private RefinerResult refineWith(String refinerName, Refiner refiner, CounterexampleInfo cex)
      throws SolverException, CPATransferException, InterruptedException,
             InvalidConfigurationException {
    context.getLogger().log(Level.INFO, "Starting refinement with " + refinerName);
    PathFormula result = refiner.refine(cex);
    context.getLogger().log(Level.INFO, refinerName + " completed successfully.");
    return new RefinerResult(refinerName, result);
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
