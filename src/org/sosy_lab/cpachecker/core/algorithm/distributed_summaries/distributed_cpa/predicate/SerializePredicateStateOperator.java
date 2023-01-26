// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.io.IOException;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.SerializeUtil;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class SerializePredicateStateOperator implements SerializeOperator {

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView formulaManagerView;
  private final AnalysisDirection direction;

  public SerializePredicateStateOperator(
      PathFormulaManager pPathFormulaManager,
      FormulaManagerView pFormulaManagerView,
      AnalysisDirection pDirection) {
    pathFormulaManager = pPathFormulaManager;
    formulaManagerView = pFormulaManagerView;
    direction = pDirection;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    PathFormula pathFormula;
    if (state.isAbstractionState()) {
      if (state.getAbstractionFormula().isTrue() && direction == AnalysisDirection.BACKWARD) {
        pathFormula = state.getAbstractionFormula().getBlockFormula();
      } else {
        pathFormula =
            pathFormulaManager
                .makeEmptyPathFormula()
                .withFormula(state.getAbstractionFormula().asFormula());
      }
      // pathFormula = state.getAbstractionFormula().getBlockFormula();
    } else {
      pathFormula = state.getPathFormula();
    }
    String formula = formulaManagerView.dumpFormula(pathFormula.getFormula()).toString();
    String ssa;
    // String pts;
    try {
      ssa = SerializeUtil.serialize(state.getPathFormula().getSsa());
      // pts = SerializeUtil.serialize(state.getPathFormula().getPointerTargetSet());
    } catch (IOException pE) {
      throw new AssertionError("Unable to serialize SSAMap " + state.getPathFormula().getSsa());
    }
    return BlockSummaryMessagePayload.builder()
        .addEntry(PredicateCPA.class.getName(), formula)
        .addEntry(BlockSummaryMessagePayload.SSA, ssa)
        // .addEntry(BlockSummaryMessagePayload.PTS, pts)
        .buildPayload();
  }
}
