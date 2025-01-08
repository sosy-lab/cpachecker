// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import java.util.ArrayList;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
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
  private PathFormula exclusionFormula;

  public AllSatRefiner(FormulaContext pContext) {
    context = pContext;
    exclusionFormula = context.getManager().makeEmptyPathFormula();
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {
    BooleanFormulaManager bmgr = context.getSolver().getFormulaManager().getBooleanFormulaManager();
    context.setProverOptions(ProverOptions.GENERATE_ALL_SAT);

    try (ProverEnvironment prover = context.getProver()) {
      BooleanFormula formula =
          context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath()).getFormula();
      prover.push(formula);

      AllSatCallback callback = new AllSatCallback();
      // extract relevant variables
      List<BooleanFormula> importantPredicates = List.copyOf(
          (java.util.Collection<? extends BooleanFormula>)
              context
                  .getSolver()
                  .getFormulaManager()
                  .extractVariables(formula)
                  .values());
      // invoke AllSAT
      prover.allSat(callback, importantPredicates);


      // retrieve satisfying assignments
      List<BooleanFormula> satisfyingAssignments = callback.getResult();
      // refine exclusion formula
      for (BooleanFormula assignment : satisfyingAssignments) {
        exclusionFormula = context.getManager().makeAnd(exclusionFormula, bmgr.not(assignment));
        context.getLogger()
            .log(Level.INFO, "Added satisfying assignment to exclusion formula: " + assignment);
      }
    }

    return exclusionFormula;
  }
}

class AllSatCallback implements BasicProverEnvironment.AllSatCallback<List<BooleanFormula>> {

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
