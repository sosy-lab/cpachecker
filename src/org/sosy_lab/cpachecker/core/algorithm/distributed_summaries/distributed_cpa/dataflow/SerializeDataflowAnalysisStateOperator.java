// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SerializeBooleanFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SerializeNumeralFormulaVisitor;

public class SerializeDataflowAnalysisStateOperator implements SerializeOperator {

  @Override
  public DSSMessagePayload serialize(AbstractState pState) {
    InvariantsState state = (InvariantsState) pState;
    BooleanFormula<CompoundInterval> booleanFormula = state.asFormula();
    SerializeNumeralFormulaVisitor numeralFormulaVisitor = new SerializeNumeralFormulaVisitor();
    SerializeBooleanFormulaVisitor booleanFormulaVisitor =
        new SerializeBooleanFormulaVisitor(numeralFormulaVisitor);
    String abstractionStrategy = state.getAbstractionState().getAbstractionStrategyName();
    DSSMessagePayload.Builder payload =
        DSSMessagePayload.builder()
            .addEntry(InvariantsCPA.class.getName(), booleanFormula.accept(booleanFormulaVisitor))
            .addEntry(DSSMessagePayload.STRATEGY, abstractionStrategy);

    return payload.buildPayload();
  }
}
