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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssSerializeUtil;
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
  private final boolean writeReadableFormulas;

  public SerializePredicateStateOperator(
      PredicateCPA pPredicateCPA, CFA pCFA, boolean pWriteReadableFormulas) {
    cfa = pCFA;
    predicateCPA = pPredicateCPA;
    writeReadableFormulas = pWriteReadableFormulas;
  }

  @Override
  public DssMessagePayload serialize(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    FormulaManagerView formulaManagerView = predicateCPA.getSolver().getFormulaManager();
    BooleanFormula booleanFormula;
    SSAMap ssaMap = state.getPathFormula().getSsa();
    if (state.isAbstractionState()) {
      booleanFormula = state.getAbstractionFormula().asFormula();
      SSAMapBuilder reset = SSAMap.emptySSAMap().builder();
      for (String variable : ssaMap.allVariables()) {
        reset.setIndex(variable, ssaMap.getType(variable), 1);
      }
      ssaMap = reset.build();
      booleanFormula =
          predicateCPA.getSolver().getFormulaManager().instantiate(booleanFormula, ssaMap);
    } else {
      booleanFormula = state.getPathFormula().getFormula();
    }
    String serializedFormula = formulaManagerView.dumpFormula(booleanFormula).toString();
    SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    String serializedSSAMap;
    String pts;
    try {
      serializedSSAMap = DssSerializeUtil.serialize(ssaMap);
      pts = DssSerializeUtil.serialize(state.getPathFormula().getPointerTargetSet());
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize SSAMap " + state.getPathFormula().getSsa());
    } finally {
      SerializationInfoStorage.clear();
    }
    DssMessagePayload.Builder payload =
        DssMessagePayload.builder()
            .addEntry(PredicateCPA.class.getName(), serializedFormula)
            .addEntry(DssMessagePayload.SSA, serializedSSAMap)
            .addEntry(DssMessagePayload.PTS, pts);
    if (writeReadableFormulas) {
      payload.addEntry("readable", booleanFormula.toString());
    }
    return payload.buildPayload();
  }
}
