// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.io.IOException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.SerializeUtil;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class SerializePredicateStateOperator implements SerializeOperator {

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView formulaManagerView;
  private final CFA cfa;
  private final PredicateCPA predicateCPA;

  public SerializePredicateStateOperator(PredicateCPA pPredicateCPA, CFA pCFA) {
    pathFormulaManager = pPredicateCPA.getPathFormulaManager();
    formulaManagerView = pPredicateCPA.getSolver().getFormulaManager();
    cfa = pCFA;
    predicateCPA = pPredicateCPA;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
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
    SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    String ssa;
    String pts;
    try {
      ssa = SerializeUtil.serialize(pathFormula.getSsa());
      pts = SerializeUtil.serialize(state.getPathFormula().getPointerTargetSet());
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize SSAMap " + pathFormula.getSsa());
    } finally {
      SerializationInfoStorage.clear();
    }
    return new BlockSummaryMessagePayload.Builder()
        .addEntry(PredicateCPA.class.getName(), formula)
        .addEntry(BlockSummaryMessagePayload.SSA, ssa)
        .addEntry(BlockSummaryMessagePayload.PTS, pts)
        .buildPayload();
  }
}
