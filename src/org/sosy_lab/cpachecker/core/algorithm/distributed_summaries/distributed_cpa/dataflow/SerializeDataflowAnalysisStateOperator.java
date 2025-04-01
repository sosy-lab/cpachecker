// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.SerializeCTypeVisitor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SerializeBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SerializeNumeralFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SerializeDataflowAnalysisStateOperator implements SerializeOperator {

  private final FormulaManagerView formulaManager;

  public SerializeDataflowAnalysisStateOperator(Solver solver)
      throws InvalidConfigurationException {
    formulaManager = solver.getFormulaManager();
  }

  public FormulaManagerView getFormulaManager() {
    return formulaManager;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    InvariantsState state = (InvariantsState) pState;
    BooleanFormula<CompoundInterval> booleanFormula = state.asFormula();
    SerializeNumeralFormulaVisitor numeralFormulaVisitor = new SerializeNumeralFormulaVisitor();
    SerializeBooleanFormulaVisitor booleanFormulaVisitor =
        new SerializeBooleanFormulaVisitor(numeralFormulaVisitor);
    String abstractionStrategy = state.getAbstractionState().getAbstractionStrategyName();
    String booleanFormulaString = booleanFormula.accept(booleanFormulaVisitor);
    SerializeCTypeVisitor cTypeVisitor = new SerializeCTypeVisitor();
    List<String> serializedVariableTypeEntries = new ArrayList<>();

    for (Entry<MemoryLocation, CType> entry : state.getVariableTypes().entrySet()) {
      String key = entry.getKey().getExtendedQualifiedName();
      if (entry.getValue().isIncomplete()) {
        continue;
      }
      String cType = entry.getValue().accept(cTypeVisitor);
      serializedVariableTypeEntries.add(key + ".ti" + cType);
    }

    String serializedVariableTypes = Joiner.on(" && ").join(serializedVariableTypeEntries);
    BlockSummaryMessagePayload.Builder payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(InvariantsCPA.class.getName(), booleanFormulaString)
            .addEntry(BlockSummaryMessagePayload.STRATEGY, abstractionStrategy);

    if (!serializedVariableTypes.isEmpty()) {
      payload.addEntry(BlockSummaryMessagePayload.VTYPES, serializedVariableTypes);
    }

    return payload.buildPayload();
  }

  @Override
  public org.sosy_lab.java_smt.api.BooleanFormula serializeToFormula(AbstractState pState) {
    InvariantsState state = (InvariantsState) pState;
    return state.getFormulaApproximation(formulaManager);
  }
}
