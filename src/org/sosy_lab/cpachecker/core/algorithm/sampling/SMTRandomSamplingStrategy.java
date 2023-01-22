// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

public class SMTRandomSamplingStrategy implements SamplingStrategy {

  @Override
  public List<ValueAssignment> getModel(
      FormulaManagerView fmgr, Iterable<Formula> variableFormulas, BasicProverEnvironment<?> prover)
      throws InterruptedException, SolverException {
    BitvectorFormulaManagerView bvmgr = fmgr.getBitvectorFormulaManager();
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    Set<BooleanFormula> variableConstraints = new HashSet<>();
    List<ValueAssignment> model = null;

    // Add some constraints to keep the variable values small
    for (Formula variableFormula : variableFormulas) {
      // TODO: We assume all variables are bitvectors for now
      BitvectorFormula variable = (BitvectorFormula) variableFormula;
      boolean unsat = true;
      long max_value = 10;
      // If the formula becomes UNSAT the constraint was too tight, so loosen it or remove
      // completely.
      // TODO: Could use a more sophisticated approach
      while (unsat && max_value <= 1000) {
        BitvectorFormula limit = bvmgr.makeBitvector(32, max_value);
        BooleanFormula upper = bvmgr.lessOrEquals(variable, limit, true);
        BooleanFormula lower = bvmgr.lessOrEquals(bvmgr.negate(variable), limit, true);

        prover.push(upper);
        prover.addConstraint(lower);
        prover.addConstraint(bfmgr.and(variableConstraints));
        unsat = prover.isUnsat();
        prover.pop();

        if (unsat) {
          max_value *= 10;
        } else {
          variableConstraints.add(bfmgr.and(upper, lower));
        }
      }
    }

    prover.push(bfmgr.and(variableConstraints));
    if (!prover.isUnsat()) {
      model = prover.getModelAssignments();
    }
    prover.pop();
    return model;
  }
}
