// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.SerializeValueVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SerializeValueAnalysisStateOperator implements SerializeOperator {

  private final Solver solver;
  private final FormulaManagerView formulaManager;

  public SerializeValueAnalysisStateOperator(Solver pSolver) {
    solver = pSolver;
    formulaManager = solver.getFormulaManager();
  }

  @Override
  public DssMessagePayload serialize(AbstractState pState) {
    ValueAnalysisState valueState = (ValueAnalysisState) pState;
    StringBuilder stringBuilder = new StringBuilder();
    SerializeValueVisitor visitor = new SerializeValueVisitor();
    for (Entry<MemoryLocation, ValueAndType> entry : valueState.getConstants()) {
      stringBuilder
          .append(entry.getKey().getExtendedQualifiedName())
          .append("->")
          .append(entry.getValue().getType())
          .append("=")
          .append(entry.getValue().getValue().accept(visitor))
          .append(" && ");
    }
    String serializedValueString = stringBuilder.toString();
    if (serializedValueString.isEmpty()) {
      serializedValueString = "No constants";
    } else {
      serializedValueString =
          serializedValueString.substring(0, serializedValueString.length() - 4);
    }

    DssMessagePayload.Builder payload =
        DssMessagePayload.builder()
            .addEntry(ValueAnalysisCPA.class.getName(), serializedValueString);

    return payload.buildPayload();
  }

  @Override
  public org.sosy_lab.java_smt.api.BooleanFormula serializeToFormula(AbstractState pState) {
    ValueAnalysisState valueState = (ValueAnalysisState) pState;
    org.sosy_lab.java_smt.api.BooleanFormula formula =
        valueState.getFormulaApproximation(formulaManager);
    return formula;
  }
}
