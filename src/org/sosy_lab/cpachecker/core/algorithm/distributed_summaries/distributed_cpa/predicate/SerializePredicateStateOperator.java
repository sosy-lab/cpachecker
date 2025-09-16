// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.DssSerializeObjectUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class SerializePredicateStateOperator implements SerializeOperator {

  public static final String SSA_KEY = "ssa";
  public static final String PTS_KEY = "pts";
  public static final String READABLE_KEY = "readable";

  private final CFA cfa;
  private final PredicateCPA predicateCPA;
  private final boolean writeReadableFormulas;
  private final Solver solver;

  public SerializePredicateStateOperator(
      PredicateCPA pPredicateCPA, CFA pCFA, boolean pWriteReadableFormulas, Solver pSolver) {
    cfa = pCFA;
    predicateCPA = pPredicateCPA;
    writeReadableFormulas = pWriteReadableFormulas;
    solver = pSolver;
  }

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    FormulaManagerView formulaManagerView = solver.getFormulaManager();
    BooleanFormula booleanFormula;
    SSAMap ssaMap = state.getPathFormula().getSsa();
    if (state.isAbstractionState()) {
      booleanFormula = state.getAbstractionFormula().asFormula();
      SSAMapBuilder reset = SSAMap.emptySSAMap().builder();
      for (String variable : ssaMap.allVariables()) {
        reset.setIndex(variable, ssaMap.getType(variable), 1);
      }
      ssaMap = reset.build();
      booleanFormula = solver.getFormulaManager().instantiate(booleanFormula, ssaMap);
    } else {
      booleanFormula = state.getPathFormula().getFormula();
    }
    String serializedFormula = formulaManagerView.dumpFormula(booleanFormula).toString();
    SerializationInfoStorage.storeSerializationInformation(predicateCPA, cfa);
    String serializedSSAMap;
    String pts;
    try {
      serializedSSAMap = DssSerializeObjectUtil.serialize(ssaMap);
      pts = DssSerializeObjectUtil.serialize(state.getPathFormula().getPointerTargetSet());
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize SSAMap " + state.getPathFormula().getSsa());
    } finally {
      SerializationInfoStorage.clear();
    }
    return ContentBuilder.builder()
        .pushLevel(PredicateAbstractState.class.getName())
        .put(STATE_KEY, serializedFormula)
        .put(SSA_KEY, serializedSSAMap)
        .put(PTS_KEY, pts)
        .putIf(writeReadableFormulas, READABLE_KEY, booleanFormula.toString())
        .build();
  }

  @Override
  public BooleanFormula serializeToFormula(AbstractState pState) {
    PredicateAbstractState state = (PredicateAbstractState) pState;
    if (state.isAbstractionState()) {
      return state.getAbstractionFormula().asFormula();
    }
    return state.getPathFormula().getFormula();
  }
}
