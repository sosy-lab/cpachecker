// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class DefaultTraceInterpreter implements TraceInterpreter {

  private final BooleanFormulaManager bmgr;

  public DefaultTraceInterpreter(BooleanFormulaManager pBmgr) {
    bmgr = pBmgr;
  }

  @Override
  public BooleanFormula interpret(Trace pTrace) {
    return bmgr.and(pTrace.toFormulaList());
  }

}
