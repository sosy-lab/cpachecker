// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializePredicateStateOperator.PTS_KEY;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializePredicateStateOperator.READABLE_KEY;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.SerializePredicateStateOperator.SSA_KEY;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class SerializeValueAnalysisStateOperator implements SerializeOperator {

  public static final String FORMULA_KEY = "violationCondition";
  private final FormulaManagerView formulaManager;
  private final ValueAnalysisCPA valueCpa;
  private final CFA cfa;

  public SerializeValueAnalysisStateOperator(ValueAnalysisCPA pValueAnalysisCPA, CFA pCFA) {
    valueCpa = pValueAnalysisCPA;
    cfa = pCFA;
    formulaManager =
        pValueAnalysisCPA.getBlockStrengtheningOperator().getSolver().getFormulaManager();
  }

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    ValueAnalysisState state = (ValueAnalysisState) pState;
    String serializedState;
    String ssa = "";
    String pts = "";
    String formula = "";
    SerializationInfoStorage.storeSerializationInformation(valueCpa, cfa);
    try {
      serializedState = DssSerializeObjectUtil.serialize(state);
      if (state.getViolationCondition() != null) {
        ssa = DssSerializeObjectUtil.serialize(state.getViolationCondition().getSsa());
        pts = DssSerializeObjectUtil.serialize(state.getViolationCondition().getPointerTargetSet());
        formula =
            formulaManager
                .dumpFormula(((ValueAnalysisState) pState).getViolationCondition().getFormula())
                .toString();
      }
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize value analysis state " + state.toString());
    } finally {
      SerializationInfoStorage.clear();
    }
    return ContentBuilder.builder()
        .pushLevel(ValueAnalysisState.class.getName())
        .put(STATE_KEY, serializedState)
        .put(READABLE_KEY, state.getConstants().toString())
        .pushLevel(STATE_KEY)
        .put(SSA_KEY, ssa)
        .put(PTS_KEY, pts)
        .put(FORMULA_KEY, formula)
        .popLevel()
        .pushLevel(PredicateAbstractState.class.getName())
        .put(
            READABLE_KEY,
            state.getViolationCondition() == null
                ? state.toDOTLabel()
                : state.getViolationCondition().getFormula().toString())
        .build();
  }
}
