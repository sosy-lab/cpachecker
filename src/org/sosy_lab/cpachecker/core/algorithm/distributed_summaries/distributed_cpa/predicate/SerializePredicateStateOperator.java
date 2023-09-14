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

public class SerializePredicateStateOperator implements SerializeOperator {
  private final CFA cfa;
  private final PredicateCPA predicateCPA;

  public SerializePredicateStateOperator(PredicateCPA pPredicateCPA, CFA pCFA) {
    cfa = pCFA;
    predicateCPA = pPredicateCPA;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    FormulaManagerView formulaManagerView = predicateCPA.getSolver().getFormulaManager();
    BooleanFormula booleanFormula;
    SSAMap ssaMap = state.getPathFormula().getSsa();
    if (state.isAbstractionState()) {
      booleanFormula = state.getAbstractionFormula().asFormula();
      SSAMapBuilder reset = SSAMap.emptySSAMap().builder();
      for (String variable : ssaMap.allVariables()) {
        reset.setIndex(variable, ssaMap.getType(variable), ssaMap.getIndex(variable));
      }
      ssaMap = reset.build();
    } else {
      booleanFormula = state.getPathFormula().getFormula();
    }
    String serializedFormula = formulaManagerView.dumpFormula(booleanFormula).toString();
    SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    String serializedSSAMap;
    String pts;
    try {
      serializedSSAMap = BlockSummarySerializeUtil.serialize(ssaMap);
      pts = BlockSummarySerializeUtil.serialize(state.getPathFormula().getPointerTargetSet());
    } catch (IOException e) {
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
