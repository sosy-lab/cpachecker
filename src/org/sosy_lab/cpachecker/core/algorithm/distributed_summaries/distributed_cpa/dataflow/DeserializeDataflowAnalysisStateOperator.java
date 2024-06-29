// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.StringToBooleanFormulaParser;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptSpecifiedVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;

public class DeserializeDataflowAnalysisStateOperator implements DeserializeOperator {
  private final CFA cfa;
  private final InvariantsCPA invariantsCPA;

  public DeserializeDataflowAnalysisStateOperator(InvariantsCPA pInvariantsCPA, CFA pCFA) {
    cfa = pCFA;
    invariantsCPA = pInvariantsCPA;
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    String booleanFormulaString =
        (String) pMessage.getAbstractState(InvariantsCPA.class).orElseThrow();

    BooleanFormula<CompoundInterval> booleanFormula =
        StringToBooleanFormulaParser.parseBooleanFormula(booleanFormulaString);

    VariableSelection<CompoundInterval> variableSelection =
        new AcceptSpecifiedVariableSelection<>(booleanFormula.accept(new CollectVarsVisitor<>()));

    List<BooleanFormula<CompoundInterval>> assumptionParts =
        booleanFormula.accept(new SplitConjunctionsVisitor<>());
    for (BooleanFormula<CompoundInterval> assumption : assumptionParts) {
      variableSelection = variableSelection.acceptAssumption(assumption);
    }

    InvariantsState deserializedInvariantsState =
        new InvariantsState(
            variableSelection,
            invariantsCPA.getCompoundIntervalFormulaManagerFactory(),
            cfa.getMachineModel(),
            null,
            false);
    deserializedInvariantsState =
        deserializedInvariantsState.addAssumptions(Set.copyOf(assumptionParts));

    return deserializedInvariantsState;
  }
}
