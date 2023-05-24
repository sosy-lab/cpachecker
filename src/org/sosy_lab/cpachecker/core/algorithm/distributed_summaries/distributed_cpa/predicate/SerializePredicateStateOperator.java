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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummarySerializeUtil;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class SerializePredicateStateOperator implements SerializeOperator {
  private final AnalysisDirection direction;
  private final CFA cfa;
  private final PredicateCPA predicateCPA;

  public SerializePredicateStateOperator(
      PredicateCPA pPredicateCPA, CFA pCFA, AnalysisDirection pDirection) {
    cfa = pCFA;
    predicateCPA = pPredicateCPA;
    direction = pDirection;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    FormulaManagerView formulaManagerView = predicateCPA.getSolver().getFormulaManager();
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
          if (formulaEntry.getKey().contains("__VERIFIER_nondet_")) {
            continue;
          }
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
    SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    String serializedSSAMap;
    String pts;
    try {
      serializedSSAMap = BlockSummarySerializeUtil.serialize(ssaMap);
      pts = BlockSummarySerializeUtil.serialize(state.getPathFormula().getPointerTargetSet());
    } catch (IOException pE) {
      throw new AssertionError("Unable to serialize SSAMap " + state.getPathFormula().getSsa());
    } finally {
      SerializationInfoStorage.clear();
    }
    return BlockSummaryMessagePayload.builder()
        .addEntry(PredicateCPA.class.getName(), serializedFormula)
        .addEntry("readable", booleanFormula.toString())
        .addEntry(BlockSummaryMessagePayload.SSA, serializedSSAMap)
        .addEntry(BlockSummaryMessagePayload.PTS, pts)
        .buildPayload();
  }
}
