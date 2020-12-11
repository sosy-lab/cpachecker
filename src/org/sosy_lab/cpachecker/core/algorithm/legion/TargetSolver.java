// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class TargetSolver {

  private final LogManager logger;
  private final Solver solver;
  private final int maxSolverAsks;
  private final StatInt successfullPrimarySolves =
      new StatInt(StatKind.COUNT, "successfull_primary_solves");
  private final StatInt successfullSecondarySolves =
      new StatInt(StatKind.COUNT, "successfull_secondary_solves");
  private final StatInt unsuccessfullSolves = new StatInt(StatKind.COUNT, "unsuccessfull_solves");
  private final LegionComponentStatistics stats = new LegionComponentStatistics("targeting");
  private static final String VERIFIER_NONDET = "__VERIFIER_nondet_";

  /**
   * @param pLogger The logging instance to use.
   * @param pSolver The solver to use.
   * @param pMaxSolverAsks The maximum amount of times to bother the SMT-Solver.
   */
  public TargetSolver(LogManager pLogger, Solver pSolver, int pMaxSolverAsks) {
    logger = pLogger;
    solver = pSolver;
    maxSolverAsks = pMaxSolverAsks;
  }

  /**
   * Phase targeting Solve for the given targets and return matching values.
   *
   * @param pTarget The target formula to solve for.
   */
  List<List<ValueAssignment>> target(PathFormula pTarget)
      throws InterruptedException, SolverException {

    this.stats.start();
    List<List<ValueAssignment>> preloadedValues = new ArrayList<>();

    try (ProverEnvironment prover =
        this.solver.newProverEnvironment(
            ProverOptions.GENERATE_MODELS, ProverOptions.GENERATE_UNSAT_CORE)) {

      FormulaManagerView fmgr = this.solver.getFormulaManager();
      BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();

      // Ask solver for the first set of Values
      try {
        Optional<Model> constraints = solvePathConstrains(pTarget.getFormula(), Optional.absent(), prover);
        if (constraints.isPresent()) {
          preloadedValues.add(computePreloadValues(constraints.get()));
          this.successfullPrimarySolves.setNextValue(1);
        } else {
          this.unsuccessfullSolves.setNextValue(1);
        }
      } finally {
        this.stats.finish();
      }

      // Don't try for additional values if none could be produced
      if (preloadedValues.isEmpty()) {
        return preloadedValues;
      }

      // Repeats the solving at most pMaxSolverAsks amount of times
      // or the size of preloadedValues
      for (int i = 0; i < Math.min(this.maxSolverAsks - 1, preloadedValues.get(0).size()); i++) {

        ValueAssignment assignment = preloadedValues.get(0).get(i);

        // Create negated assignment formula
        BooleanFormula f = assignment.getAssignmentAsFormula();
        BooleanFormula notF = bmgr.not(f);

        try {
          Optional<Model> additionalConstraints = solvePathConstrains(pTarget.getFormula(), Optional.of(notF), prover);
          if (additionalConstraints.isPresent()) {
            preloadedValues.add(computePreloadValues(additionalConstraints.get()));
            this.successfullSecondarySolves.setNextValue(1);
          } else {
            this.unsuccessfullSolves.setNextValue(1);
            this.logger.log(Level.FINE, "Could not solve for more solutions.");
          }
        } finally {
          this.stats.finish();
        }
      }
    }
    this.stats.finish();
    return preloadedValues;
  }

  /**
   * Ask the SAT-solver to compute path constraints for the pTarget.
   *
   * @param target The formula leading to the selected state.
   * @param pProver The prover to use.
   * @throws InterruptedException, SolverException
   */
  private Optional<Model> solvePathConstrains(
      BooleanFormula target,
      Optional<BooleanFormula> additionalConditions,
      ProverEnvironment pProver)
      throws InterruptedException, SolverException {

    logger.log(Level.FINE, "Solve path constraints. ");
    logger.log(Level.FINER, "Formula is ", target.toString());
    pProver.push(target);

    // Add additional conditions if present
    if (additionalConditions.isPresent()) {
      pProver.push(additionalConditions.get());
    }

    // Let the solver work
    Optional<Model> model = Optional.absent();
    boolean isUnsat = pProver.isUnsat();
    if (!isUnsat) {
      model = Optional.of(pProver.getModel());
    }

    // Remove previously added additional constraints
    if (additionalConditions.isPresent()) {
      pProver.pop();
    }

    return model;
  }

  /**
   * Pushes the values from the model into the value assigner.
   *
   * @param pConstraints The source of values to assign.
   */
  private List<ValueAssignment> computePreloadValues(Model pConstraints) {
    List<ValueAssignment> values = new ArrayList<>();
    for (ValueAssignment assignment : pConstraints) {
      String name = assignment.getName();

      if (!name.startsWith(VERIFIER_NONDET)) {
        continue;
      }

      Value value = ValueConverter.toValue(assignment.getValue());
      logger.log(Level.FINE, "Loaded Value", name, value);
      values.add(assignment);
    }
    return values;
  }

  public LegionComponentStatistics getStats() {
    this.stats.setOther(this.successfullPrimarySolves);
    this.stats.setOther(this.successfullSecondarySolves);
    this.stats.setOther(this.unsuccessfullSolves);

    return this.stats;
  }
}
