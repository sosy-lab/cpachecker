// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SerializeBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SerializeNumeralFormulaVisitor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SerializeDataflowAnalysisStateOperator implements SerializeOperator {

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    InvariantsState state = (InvariantsState) pState;
    StringBuilder stringBuilder = new StringBuilder();

    for (Entry<MemoryLocation, CType> entry : state.getVariableTypes().entrySet()) {
      String key = entry.getKey().getExtendedQualifiedName();
      String value = entry.getValue().toString();
      stringBuilder.append(key).append("->").append(value).append(" && ");
    }
    String serializedVariableTypes = stringBuilder.toString();

    BooleanFormula<CompoundInterval> booleanFormula = state.asFormula();
    SerializeNumeralFormulaVisitor numeralFormulaVisitor = new SerializeNumeralFormulaVisitor();
    SerializeBooleanFormulaVisitor booleanFormulaVisitor =
        new SerializeBooleanFormulaVisitor(numeralFormulaVisitor);
    String abstractionStrategy = state.getAbstractionState().getAbstractionStrategyName();
    String booleanFormulaString = booleanFormula.accept(booleanFormulaVisitor);
    BlockSummaryMessagePayload.Builder payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(InvariantsCPA.class.getName(), booleanFormulaString)
            .addEntry(BlockSummaryMessagePayload.STRATEGY, abstractionStrategy);

    if (!serializedVariableTypes.isEmpty()) {
      serializedVariableTypes =
          serializedVariableTypes.substring(0, serializedVariableTypes.length() - 4);
      payload.addEntry(BlockSummaryMessagePayload.VTYPES, serializedVariableTypes);
    }

    return payload.buildPayload();
  }
}
