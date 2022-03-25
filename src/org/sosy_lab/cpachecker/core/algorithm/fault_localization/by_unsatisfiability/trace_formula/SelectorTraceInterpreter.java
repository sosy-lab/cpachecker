// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.collect.FluentIterable;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class SelectorTraceInterpreter implements TraceInterpreter {

  private final BooleanFormulaManager bmgr;

  public SelectorTraceInterpreter(BooleanFormulaManager pBmgr) {
    bmgr = pBmgr;
  }

  @Override
  public BooleanFormula interpret(Trace pTrace) {
    return bmgr.and(
        FluentIterable.from(pTrace)
            .transform(entry -> bmgr.implication(entry.getSelector(), entry.getFormula()))
            .toList());
  }

  /*  public BooleanFormula interpret(TraceFormula pTraceFormula) {
    BooleanFormula preconditionFormula = pTraceFormula.getPrecondition().getPrecondition();
    BooleanFormula traceFormula = interpret(pTraceFormula.getTrace());
    BooleanFormula postConditionFormula = pTraceFormula.getPostCondition().getPostCondition();
    postConditionFormula =
        negatePostCondition ? bmgr.not(postConditionFormula) : postConditionFormula;
    return bmgr.and(preconditionFormula, traceFormula, postConditionFormula);
  }*/

}
