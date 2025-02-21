// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.preciseErrorCondition;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class AllSatRefiner implements Refiner {

  private static final Logger log = LoggerFactory.getLogger(AllSatRefiner.class);
  private final FormulaContext context;
  private final BooleanFormulaManager bmgr;
  private final ErrorConditionFormatter formatter;
  private PathFormula exclusionModelFormula;
  private final Solver solver;
  private int currentRefinementIteration = 0;

  public AllSatRefiner(FormulaContext pContext) throws InvalidConfigurationException {
    context = pContext;
    solver = pContext.getSolver();
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    exclusionModelFormula = context.getManager().makeEmptyPathFormula();
    formatter = new ErrorConditionFormatter(context);
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT)) {

      // get the formula for the counterexample path
      PathFormula pathFormula =
          context.getManager().makeFormulaForPath(cex.getTargetPath().getFullPath());

      prover.push(pathFormula.getFormula());

      // extract atoms from the formula
      ImmutableList<BooleanFormula> atoms = ImmutableList.copyOf(
          solver.getFormulaManager().extractAtoms(pathFormula.getFormula(), false)
      );

      formatter.loggingWithIteration(currentRefinementIteration,
          Level.INFO, String.format("Found Atoms In Formula:\n%s", atoms));

      // Invoke allSat
      AllSatCallback callback = new AllSatCallback(bmgr);
      // TODO not all atoms should be passed to the allsat method, but rather only the 'important'
      //  predicates, which are usually the predicates that are connected to a nondet var.
      //  for example an assignment like {@code x = 2} won't be necessary to pass, since it will
      //  always be the same in the found model
      List<BooleanFormula> models = prover.allSat(callback, atoms);

      formatter.loggingWithIteration(currentRefinementIteration,
          Level.INFO, String.format("Found Models:\n%s", models)
      );

      // combine the found models into a disjunction (OR)
      BooleanFormula combinedModels = bmgr.makeFalse();
      for (BooleanFormula model : models) {
        combinedModels = bmgr.or(combinedModels, model);
      }

      formatter.loggingWithIteration(currentRefinementIteration,
          Level.FINE, String.format("Combined Models Into Boolean Formula:\n%s", combinedModels));


      // Update exclusion formula with the found models
      exclusionModelFormula = exclusionModelFormula.withFormula(bmgr.not(combinedModels));
      formatter.setupSSAMap(pathFormula);
      formatter.reformat(pathFormula, exclusionModelFormula.getFormula(),
          currentRefinementIteration);

      currentRefinementIteration++;
      return exclusionModelFormula;
    } catch (SolverException e) {
      formatter.loggingWithIteration(currentRefinementIteration,
          Level.WARNING, String.format("Solver Error During Refinement: %s", e.getMessage()));
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