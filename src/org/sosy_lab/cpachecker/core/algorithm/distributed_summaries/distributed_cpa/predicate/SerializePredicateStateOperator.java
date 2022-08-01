// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.io.IOException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class SerializePredicateStateOperator implements SerializeOperator {

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView formulaManagerView;

  public SerializePredicateStateOperator(
      PathFormulaManager pPathFormulaManager, FormulaManagerView pFormulaManagerView) {
    pathFormulaManager = pPathFormulaManager;
    formulaManagerView = pFormulaManagerView;
  }

  @Override
  public Payload serialize(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    PathFormula pathFormula;
    if (state.isAbstractionState()) {
      if (state.getAbstractionFormula().isTrue()) {
        // fall-back
        pathFormula = state.getAbstractionFormula().getBlockFormula();
      } else {
        pathFormula =
            pathFormulaManager.makeEmptyPathFormulaWithContextFrom(
                state.getAbstractionFormula().getBlockFormula());
        pathFormula =
            pathFormulaManager.makeAnd(pathFormula, state.getAbstractionFormula().asFormula());
      }
    } else {
      pathFormula = state.getPathFormula();
    }
    String formula = formulaManagerView.dumpFormula(pathFormula.getFormula()).toString();
    String ssa;
    try {
      ssa = SerializeSSAMap.serialize(pathFormula.getSsa());
    } catch (IOException pE) {
      throw new AssertionError("Unable to serialize SSAMap " + pathFormula.getSsa());
    }
    return new Payload.Builder()
        .addEntry(PredicateCPA.class.getName(), formula)
        .addEntry(Payload.SSA, ssa)
        .buildPayload();
  }
}
