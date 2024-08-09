// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverException;

public class QuantifierElimination {

  public static BooleanFormula eliminateAllVariablesExceptNondets(
      BooleanFormula pFormula, Solver pSolver) throws InterruptedException, SolverException {
    Map<String, Formula> formulaNameToFormulaMap =
        pSolver.getFormulaManager().extractVariables(pFormula);
    ImmutableList<@NonNull Formula> eliminate =
        FluentIterable.from(formulaNameToFormulaMap.entrySet())
            .filter(entry -> !entry.getKey().contains("__VERIFIER_nondet"))
            .transform(entry -> entry.getValue())
            .toList();
    BooleanFormula quantified =
        pSolver
            .getFormulaManager()
            .getQuantifiedFormulaManager()
            .mkQuantifier(Quantifier.EXISTS, eliminate, pFormula);
    BooleanFormula eliminated =
        pSolver.getFormulaManager().getQuantifiedFormulaManager().eliminateQuantifiers(quantified);
    return eliminated;
  }
}
