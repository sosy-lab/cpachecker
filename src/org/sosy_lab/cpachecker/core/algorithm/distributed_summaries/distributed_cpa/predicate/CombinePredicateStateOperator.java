// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class CombinePredicateStateOperator implements CombineOperator {

  private final PathFormulaManager manager;
  private final FormulaManagerView formulaManager;

  public CombinePredicateStateOperator(
      PathFormulaManager pPathFormulaManager, FormulaManagerView pFormulaManagerView) {
    manager = pPathFormulaManager;
    formulaManager = pFormulaManagerView;
  }

  @Override
  public List<AbstractState> combine(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    PredicateAbstractState state1 = (PredicateAbstractState) pState1;
    PredicateAbstractState state2 = (PredicateAbstractState) pState2;

    SSAMap ssa1 = state1.getPathFormula().getSsa();
    SSAMap ssa2 = state2.getPathFormula().getSsa();

    SSAMapBuilder builder1 = ssa1.builder();
    SSAMapBuilder builder2 = ssa2.builder();

    BooleanFormula formula1 = state1.getPathFormula().getFormula();
    BooleanFormula formula2 = state2.getPathFormula().getFormula();

    Map<Formula, Formula> substitution1 = new HashMap<>();
    Map<Formula, Formula> substitution2 = new HashMap<>();

    Map<String, Formula> extracted1 = formulaManager.extractVariables(formula1);
    Map<String, Formula> extracted2 = formulaManager.extractVariables(formula2);

    char sep = FormulaManagerView.INDEX_SEPARATOR;

    for (String variable : ssa1.allVariables()) {
      if (ssa2.containsVariable(variable)) {
        int index1 = ssa1.getIndex(variable);
        int index2 = ssa2.getIndex(variable);
        if (index2 > index1) {
          substitution1.put(
              extracted1.get(variable + sep + index1), extracted2.get(variable + sep + index2));
          builder1 = builder1.setIndex(variable, ssa1.getType(variable), index2);
        } else {
          substitution2.put(
              extracted2.get(variable + sep + index2), extracted1.get(variable + sep + index1));
          builder2 = builder2.setIndex(variable, ssa2.getType(variable), index1);
        }
      }
    }

    formula1 = formulaManager.substitute(formula1, substitution1);
    formula2 = formulaManager.substitute(formula2, substitution2);

    PathFormula newFormula =
        manager.makeOr(
            manager
                .makeEmptyPathFormulaWithContext(
                    builder1.build(), PointerTargetSet.emptyPointerTargetSet())
                .withFormula(formula1),
            manager
                .makeEmptyPathFormulaWithContext(
                    builder2.build(), PointerTargetSet.emptyPointerTargetSet())
                .withFormula(formula2));
    return ImmutableList.of(
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            newFormula, state1, state2.getPreviousAbstractionState()));
  }
}
