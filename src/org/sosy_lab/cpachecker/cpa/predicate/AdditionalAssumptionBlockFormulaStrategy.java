// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AdditionalAssumptionReportingState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AdditionalAssumptionBlockFormulaStrategy extends BlockFormulaStrategy {

  private final FormulaManagerView formulaManagerView;

  public AdditionalAssumptionBlockFormulaStrategy(FormulaManagerView pFormulaManagerView) {
    formulaManagerView = pFormulaManagerView;
  }

  @Override
  BlockFormulas getFormulasForPath(ARGState argRoot, List<ARGState> abstractionStates) {
    SSAMap latestSSAMap = SSAMap.emptySSAMap();
    List<BooleanFormula> result = new ArrayList<>(abstractionStates.size() + 1);
    for (ARGState abstractionState : abstractionStates) {
      PredicateAbstractState predicateAbstractState = toState(PredicateAbstractState.class).apply(abstractionState);
      PathFormula pathFormula = predicateAbstractState.getAbstractionFormula().getBlockFormula();
      latestSSAMap = pathFormula.getSsa();
      result.add(pathFormula.getFormula());
    }
    if (!abstractionStates.isEmpty()) {
      AbstractState last = abstractionStates.get(abstractionStates.size() - 1);
      AdditionalAssumptionReportingState additional = toState(AdditionalAssumptionReportingState.class).apply(last);
      if (additional != null) {
        result.add(formulaManagerView.instantiate(additional.getAdditionalAssumption(formulaManagerView), latestSSAMap));
      }
    }
    return new BlockFormulas(result);
  }

}
