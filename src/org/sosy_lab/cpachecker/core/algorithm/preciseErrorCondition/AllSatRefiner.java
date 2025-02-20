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
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
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
  private final BooleanFormulaManager bmgr;
  private final FormulaManagerView fmgr;
  private final PathFormulaManagerImpl pathManager;
  private final ErrorConditionFormatter formatter;

  public AllSatRefiner(FormulaContext pContext) throws InvalidConfigurationException {
    context = pContext;
    solver = pContext.getSolver();
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    fmgr = solver.getFormulaManager();
    pathManager = context.getManager();
    exclusionModelFormula = pathManager.makeEmptyPathFormula();
    formatter = new ErrorConditionFormatter(context);
  }

  @Override
  public PathFormula refine(CounterexampleInfo cex)
      throws SolverException, InterruptedException, CPATransferException {

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT)) {

      // get the formula for the counterexample path
      PathFormula pathFormula =
          pathManager.makeFormulaForPath(cex.getTargetPath().getFullPath());

      prover.push(pathFormula.getFormula());

      // extract atoms from the formula
      ImmutableList<BooleanFormula> atoms = ImmutableList.copyOf(
          fmgr.extractAtoms(pathFormula.getFormula(), false)
      );


      context.getLogger().log(Level.INFO,
          String.format("Iteration %d: Atoms: \n%s \n",
              currentRefinementIteration,
              atoms));


      // Invoke allSat
      AllSatCallback callback = new AllSatCallback(bmgr);
      // TODO not all atoms should be passed to the prover, but rather only the 'important' ones
      List<BooleanFormula> models = prover.allSat(callback, atoms);

      context.getLogger().log(Level.INFO,
          String.format("Iteration %d: Found Models With AllSat Prover : \n%s \n",
              currentRefinementIteration,
              models));

      // combine the found models into a disjunction (OR)
      BooleanFormula modelsCombined = bmgr.makeFalse();
      for (BooleanFormula model : models) {
        modelsCombined = bmgr.or(modelsCombined, model);
      }

      context.getLogger().log(Level.FINE,
          String.format("Iteration %d: Combined Exclusion with OR : \n%s \n",
              currentRefinementIteration,
              modelsCombined));

      // Update exclusion formula
      exclusionModelFormula = exclusionModelFormula.withFormula(bmgr.not(modelsCombined));

      formatter.reformat(pathFormula, exclusionModelFormula.getFormula(),
          currentRefinementIteration);

      currentRefinementIteration++;
      return exclusionModelFormula;
    } catch (SolverException e) {
      context.getLogger().log(Level.WARNING, "Solver error during refinement: ", e);
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