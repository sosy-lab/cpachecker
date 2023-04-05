// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import java.io.IOException;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.SerializeUtil;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SerializePredicateStateOperator implements SerializeOperator {
  private final FormulaManagerView formulaManagerView;
  private final AnalysisDirection direction;

  public SerializePredicateStateOperator(
      FormulaManagerView pFormulaManagerView, AnalysisDirection pDirection) {
    formulaManagerView = pFormulaManagerView;
    direction = pDirection;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    BooleanFormula booleanFormula;
    SSAMap ssaMap;
    if (state.isAbstractionState()) {
      if (direction == AnalysisDirection.BACKWARD) {
        booleanFormula = state.getAbstractionFormula().getBlockFormula().getFormula();
        ssaMap = state.getAbstractionFormula().getBlockFormula().getSsa();
      } else {
        booleanFormula = state.getAbstractionFormula().asFormula();
        SSAMapBuilder ssaMapBuilder = SSAMap.emptySSAMap().builder();
        for (Entry<String, Formula> formulaEntry :
            formulaManagerView.extractVariables(booleanFormula).entrySet()) {
          CType variableType =
              state
                  .getAbstractionFormula()
                  .getBlockFormula()
                  .getSsa()
                  .getType(formulaEntry.getKey());
          ssaMapBuilder.setIndex(formulaEntry.getKey(), variableType, 1);
        }
        ssaMap = ssaMapBuilder.build();
      }
    } else {
      booleanFormula = state.getPathFormula().getFormula();
      ssaMap = state.getPathFormula().getSsa();
    }
    String serializedFormula = formulaManagerView.dumpFormula(booleanFormula).toString();
    String serializedSSAMap;
    // String pts;
    try {
      serializedSSAMap = SerializeUtil.serialize(ssaMap);
      // pts = SerializeUtil.serialize(state.getPathFormula().getPointerTargetSet());
    } catch (IOException pE) {
      throw new AssertionError("Unable to serialize SSAMap " + state.getPathFormula().getSsa());
    }
    return BlockSummaryMessagePayload.builder()
        .addEntry(PredicateCPA.class.getName(), serializedFormula)
        .addEntry("readable", booleanFormula.toString())
        .addEntry(BlockSummaryMessagePayload.SSA, serializedSSAMap)
        // .addEntry(BlockSummaryMessagePayload.PTS, pts)
        .buildPayload();
  }
}
