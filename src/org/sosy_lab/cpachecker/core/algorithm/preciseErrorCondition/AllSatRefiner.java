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
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition.RefinementResult.RefinementStatus;
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

public class AllSatRefiner implements Refiner {

  private final FormulaContext context;
  private final BooleanFormulaManager bmgr;
  private final ErrorConditionFormatter formatter;
  private final Solver solver;
  private final Boolean withFormatter;
  private final RefinementResult exclusionModelFormula;
  private int currentRefinementIteration = 0;

  public AllSatRefiner(FormulaContext pContext, Boolean pWithFormatter)
      throws InvalidConfigurationException {
    context = pContext;
    solver = pContext.getSolver();
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    exclusionModelFormula =
        new RefinementResult(RefinementStatus.EMPTY, Optional.empty());
    withFormatter = pWithFormatter;
    formatter = new ErrorConditionFormatter(context);
  }

  @Override
  public RefinementResult refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT)) {

      // get the formula for the counterexample path
      PathFormula cexPathFormula =
          context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath());

      Utility.logWithIteration(currentRefinementIteration,
          Level.FINE, context,
          String.format("Current CEX FORMULA: \n%s", cexPathFormula.getFormula()));

      prover.push(cexPathFormula.getFormula());

      // extract atoms from the cex
      List<BooleanFormula> atoms = List.copyOf(
          solver
              .getFormulaManager()
              .extractAtoms(cexPathFormula.getFormula(), false)
      );

      Utility.logWithIteration(currentRefinementIteration,
          Level.FINE, context, String.format("Extracted Atoms From CEX:\n%s", atoms));


      Utility.logWithIteration(currentRefinementIteration,
          Level.FINE, context,
          String.format("Visited Found Atoms:\n%s", formatter.visitBooleanFormulaList(atoms)));


      // Invoke allSat
      AllSatCallback callback = new AllSatCallback(bmgr);
      // TODO not all atoms should be passed to the allsat method, but rather only the 'important'
      //  predicates, which are usually the predicates that are connected to a nondet var.
      //  for example an assignment like {@code x = 2} won't be necessary to pass, since it will
      //  always be the same in the found model
      List<BooleanFormula> models = prover.allSat(callback, atoms);

      Utility.logWithIteration(currentRefinementIteration,
          Level.INFO, context, String.format("Found Models In Current Iteration:\n%s", models)
      );

      Utility.logWithIteration(currentRefinementIteration,
          Level.FINE, context,
          String.format("Visited Found Models:\n%s", formatter.visitBooleanFormulaList(models)));

      // combine the found models into a disjunction (OR)
      BooleanFormula combinedModels = bmgr.makeFalse();
      for (BooleanFormula model : models) {
        combinedModels = bmgr.or(combinedModels, model);
      }

      Utility.logWithIteration(currentRefinementIteration,
          Level.FINE, context,
          String.format("Disjunction Of Models:\n%s", combinedModels));

      Utility.logWithIteration(currentRefinementIteration,
          Level.INFO, context, "Updating Exclusion Formula With Disjunct Combined Model...");
      // Update exclusion formula with the found models

      PathFormula negatedModelFormula =
          exclusionModelFormula.getOptionalFormula().get().withFormula(bmgr.not(combinedModels));
      exclusionModelFormula.updateFormula(negatedModelFormula);
      exclusionModelFormula.updateStatus(RefinementStatus.SUCCESS);

      Utility.logWithIteration(currentRefinementIteration,
          Level.INFO, context, String.format("Exclusion Formula In This Iteration: \n%s",
              exclusionModelFormula.getBooleanFormula()));

      if (withFormatter) {
        formatter.setupSSAMap(cexPathFormula);
        formatter.reformat(cexPathFormula, exclusionModelFormula.getBooleanFormula(),
            currentRefinementIteration);
      }

      currentRefinementIteration++;
      return exclusionModelFormula;
    } catch (SolverException e) {
      Utility.logWithIteration(currentRefinementIteration,
          Level.WARNING, context,
          String.format("Solver Error During Refinement: %s", e.getMessage()));
      throw e;
    }
  }


  private static class AllSatCallback
      implements BasicProverEnvironment.AllSatCallback<List<BooleanFormula>> {

    private final List<BooleanFormula> models = new ArrayList<>();
    private final BooleanFormulaManager bmgr;

    public AllSatCallback(BooleanFormulaManager pBmgr) {
      bmgr = pBmgr;
    }

    @Override
    public void apply(List<BooleanFormula> modelLiterals) {
      // TODO: potential optimization here is to not add every conjunction directly,
      //  but do some refinement/clean up prior
      if (!modelLiterals.isEmpty()) { // Skip empty models

        // combine literals into a conjunction (AND) to represent the found model
        BooleanFormula conjunction = bmgr.makeTrue();
        for (BooleanFormula lit : modelLiterals) {
          conjunction = bmgr.and(conjunction, lit);
        }
        // add model to the list of models
        models.add(conjunction);
      }
    }

    @Override
    public List<BooleanFormula> getResult() {
      return models;
    }
  }

}