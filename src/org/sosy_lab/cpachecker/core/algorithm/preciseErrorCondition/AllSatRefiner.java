// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import java.util.ArrayList;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.List;
import java.util.logging.Level;

public class AllSatRefiner implements Refiner {

  private final FormulaContext context;
  private PathFormula exclusionModelFormula;
  private Solver solver;
  private int currentRefinementIteration = 0;

  public AllSatRefiner(FormulaContext pContext) throws InvalidConfigurationException {
    context = pContext;
    exclusionModelFormula = context.getManager().makeEmptyPathFormula();
    solver = context.getSolver();
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex, PathFormula pExclusionModelFormula)
      throws SolverException, InterruptedException, CPATransferException {

    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT)) {
      BooleanFormula formula =
          context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath()).getFormula();
      prover.push(formula);

      if (!prover.isUnsat()) { // only feasible cex
        AllSatCallback callback = new AllSatCallback();

        // extract relevant variables
        List<BooleanFormula> importantPredicates = solver
            .getFormulaManager()
            .extractVariables(formula)
            .values()
            .stream()
            .filter(BooleanFormula.class::isInstance) // Filter to Boolean formulas
            .map(BooleanFormula.class::cast)
            .collect(Collectors.toList());

        context.getLogger().log(Level.INFO,
            String.format("Iteration %d: Important Predicates:\n%s.",
                currentRefinementIteration, importantPredicates));

        // invoke AllSAT
        List<BooleanFormula> assignments = prover.allSat(callback, importantPredicates);

        for (BooleanFormula assignment : assignments) {
          exclusionModelFormula =
              context.getManager().makeAnd(exclusionModelFormula, bmgr.not(assignment));
          context.getLogger()
              .log(Level.INFO, "Added satisfying assignment to exclusion formula: " + assignment);
        }
        return exclusionModelFormula;
      } else {
        context.getLogger()
            .log(Level.WARNING, "Counterexample is infeasible. Returning an empty formula.");
        currentRefinementIteration++;
        return exclusionModelFormula; // empty
      }
    }
  }


  private static class AllSatCallback
      implements BasicProverEnvironment.AllSatCallback<List<BooleanFormula>> {

    private final List<BooleanFormula> assignments = new ArrayList<>();

    @Override
    public void apply(List<BooleanFormula> model) {
      // combine the assignments into a single formula
      assignments.addAll(model);
    }

    @Override
    public List<BooleanFormula> getResult() {
      return assignments;
    }
  }
}
