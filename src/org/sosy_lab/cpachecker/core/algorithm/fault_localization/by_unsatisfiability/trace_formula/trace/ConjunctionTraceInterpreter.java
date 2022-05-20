// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

/** Conjuncts the formulas of every entry of a trace to one boolean formula. */
public class ConjunctionTraceInterpreter implements TraceInterpreter {

  private final BooleanFormulaManager bmgr;

  public ConjunctionTraceInterpreter(BooleanFormulaManager pBmgr) {
    bmgr = pBmgr;
  }

  /**
   * Conjuncts the formulas of every entry in {@code pTrace}.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Entry 1: formula_1, ...
   * Entry 2: formula_2, ...
   * }</pre
   * <p>
   *
   * Results in {@code formula_1 & formula_2}
   *
   * @param pTrace transform this trace to a boolean formula by the conjunction of all elements
   * @return trace interpreted as conjunction of formulas
   */
  @Override
  public BooleanFormula interpret(Trace pTrace) {
    return bmgr.and(pTrace.toFormulaList());
  }
}
