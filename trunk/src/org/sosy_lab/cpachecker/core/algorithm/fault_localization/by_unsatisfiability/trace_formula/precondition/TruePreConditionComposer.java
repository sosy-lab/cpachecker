// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.precondition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;

public class TruePreConditionComposer implements PreConditionComposer {

  private final FormulaContext context;

  public TruePreConditionComposer(FormulaContext pContext) {
    context = pContext;
  }

  @Override
  public PreCondition extractPreCondition(List<CFAEdge> pCounterexample) {
    return new PreCondition(
        ImmutableList.of(),
        pCounterexample,
        context.getSolver().getFormulaManager().getBooleanFormulaManager().makeTrue(),
        ImmutableSet.of());
  }
}
