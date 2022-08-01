// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

public class CombinePredicateStateOperator implements CombineOperator {

  private final PathFormulaManager manager;

  public CombinePredicateStateOperator(PathFormulaManager pPathFormulaManager) {
    manager = pPathFormulaManager;
  }

  @Override
  public List<AbstractState> combine(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    PredicateAbstractState state1 = (PredicateAbstractState) pState1;
    PredicateAbstractState state2 = (PredicateAbstractState) pState2;

    SSAMap ssa1 = state1.getPathFormula().getSsa();
    SSAMap ssa2 = state2.getPathFormula().getSsa();

    SSAMapBuilder merged = SSAMap.emptySSAMap().builder();

    for (String variable : ssa1.allVariables()) {
      if (ssa2.containsVariable(variable)) {
        merged =
            merged.setIndex(
                variable,
                ssa1.getType(variable),
                Integer.max(ssa1.getIndex(variable), ssa2.getIndex(variable)));
      } else {
        merged = merged.setIndex(variable, ssa1.getType(variable), ssa1.getIndex(variable));
      }
    }

    for (String variable : ssa2.allVariables()) {
      if (ssa1.allVariables().contains(variable)) {
        continue;
      }
      merged = merged.setIndex(variable, ssa2.getType(variable), ssa2.getIndex(variable));
    }

    PathFormula newFormula = manager.makeOr(state1.getPathFormula(), state2.getPathFormula());
    return ImmutableList.of(
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            newFormula, state1, state2.getPreviousAbstractionState()));
  }
}
