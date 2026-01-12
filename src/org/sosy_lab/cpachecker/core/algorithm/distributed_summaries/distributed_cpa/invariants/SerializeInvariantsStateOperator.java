// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.SerializeCTypeVisitor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SerializeBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SerializeNumeralFormulaVisitor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SerializeInvariantsStateOperator implements SerializeOperator {

  public static final String STRATEGY = "abstractionStrategy";
  public static final String BOOLEAN_FORMULA = "booleanFormula";
  public static final String VARIABLE_TYPES = "vtypes";

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
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

    ContentBuilder builder = ContentBuilder.builder();

    builder
        .pushLevel(InvariantsState.class.getName())
        .put(STRATEGY, abstractionStrategy)
        .put(BOOLEAN_FORMULA, booleanFormulaString);

    if (!serializedVariableTypes.isEmpty()) {
      builder.put(VARIABLE_TYPES, serializedVariableTypes);
    }

    return builder.build();
  }
}
