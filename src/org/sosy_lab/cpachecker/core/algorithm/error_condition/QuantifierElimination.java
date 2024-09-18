// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.error_condition;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;

public class QuantifierElimination {

  /**
   * Eliminate all variables from a given formula that match the predicate in pVariablesToEliminate
   *
   * @param pFormula The method eliminates all variables matching pVariablesToEliminate form this
   *     formula
   * @param pVariablesToEliminate Predicate returning true if a variable should be eliminated
   * @param pSolver A solver capable of performing quantifier elimination
   * @return A BooleanFormula without the variables described py pVariablesToEliminate
   * @throws InterruptedException Thrown if process is interrupted (most likely by the user)
   * @throws SolverException Thrown if the SMT solver runs into an exception
   */
  public static BooleanFormula eliminate(
      BooleanFormula pFormula,
      Predicate<Entry<String, Formula>> pVariablesToEliminate,
      Solver pSolver)
      throws InterruptedException, SolverException {
    Map<String, Formula> formulaNameToFormulaMap =
        pSolver.getFormulaManager().extractVariables(pFormula);
    ImmutableList<@NonNull Formula> eliminate =
        FluentIterable.from(formulaNameToFormulaMap.entrySet())
            .filter(pVariablesToEliminate::test)
            .transform(Entry::getValue)
            .toList();
    BooleanFormula quantified =
        pSolver
            .getFormulaManager()
            .getQuantifiedFormulaManager()
            .mkQuantifier(Quantifier.EXISTS, eliminate, pFormula);
    return pSolver
        .getFormulaManager()
        .getQuantifiedFormulaManager()
        .eliminateQuantifiers(quantified);
  }
}
