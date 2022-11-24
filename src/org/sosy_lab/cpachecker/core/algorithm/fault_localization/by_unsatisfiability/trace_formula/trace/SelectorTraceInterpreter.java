// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class SelectorTraceInterpreter implements TraceInterpreter {

  private final BooleanFormulaManager bmgr;

  /**
   * When interpreted as selector traces, (ir)relevant parts of the counterexample can easily be
   * (de)activated.
   *
   * @param pBmgr same manager for boolean formulas as used for the creation of the {@link Trace}
   */
  public SelectorTraceInterpreter(BooleanFormulaManager pBmgr) {
    bmgr = pBmgr;
  }

  /**
   * Calculate the selector form of a trace. Selectors imply the corresponding formula in the entry.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Entry 1: selector_1, formula_1, ...
   * Entry 2: selector_2, formula_2, ...
   * }</pre>
   *
   * <p>Will result in: {@code (selector_1 => formula_1) & (selector_2 => formula_2)}
   *
   * @param pTrace the entries of this trace will be transformed to a selector formula
   * @return trace transformed to a selector formula
   */
  @Override
  public BooleanFormula interpret(Trace pTrace) {
    return bmgr.and(
        transformedImmutableListCopy(
            pTrace, entry -> bmgr.implication(entry.getSelector(), entry.getFormula())));
  }
}
