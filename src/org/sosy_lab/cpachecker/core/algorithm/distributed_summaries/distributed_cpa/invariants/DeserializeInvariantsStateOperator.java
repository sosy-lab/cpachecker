// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants.SerializeInvariantsStateOperator.BOOLEAN_FORMULA;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants.SerializeInvariantsStateOperator.STRATEGY;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants.SerializeInvariantsStateOperator.VARIABLE_TYPES;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeParser;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.AbstractionStrategyFactories;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitConjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.StringToBooleanFormulaParser;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptSpecifiedVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DeserializeInvariantsStateOperator implements DeserializeOperator {
  private final CFA cfa;
  private final InvariantsCPA invariantsCPA;

  public DeserializeInvariantsStateOperator(InvariantsCPA pInvariantsCPA, CFA pCFA) {
    cfa = pCFA;
    invariantsCPA = pInvariantsCPA;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    ContentReader stateContent = pMessage.getAbstractStateContent(InvariantsState.class);
    BooleanFormula<CompoundInterval> booleanFormula =
        StringToBooleanFormulaParser.parseBooleanFormula(stateContent.get(BOOLEAN_FORMULA));
    AcceptSpecifiedVariableSelection<CompoundInterval> collectVarsVariableSelection =
        new AcceptSpecifiedVariableSelection<>(booleanFormula.accept(new CollectVarsVisitor<>()));
    List<BooleanFormula<CompoundInterval>> assumptionParts =
        booleanFormula.accept(new SplitConjunctionsVisitor<>());

    for (BooleanFormula<CompoundInterval> assumption : assumptionParts) {
      VariableSelection<CompoundInterval> newSelection =
          collectVarsVariableSelection.acceptAssumption(assumption);
      if (newSelection == null) {
        newSelection = collectVarsVariableSelection.join(collectVarsVariableSelection);
      }
      collectVarsVariableSelection =
          (AcceptSpecifiedVariableSelection<CompoundInterval>) newSelection;
    }

    InvariantsState deserializedInvariantsState =
        new InvariantsState(
            collectVarsVariableSelection,
            invariantsCPA.getCompoundIntervalFormulaManagerFactory(),
            cfa.getMachineModel(),
            extractAbstractionStrategy(stateContent)
                .createStrategy(
                    invariantsCPA.getCompoundIntervalFormulaManagerFactory(), cfa.getMachineModel())
                .getAbstractionState(),
            extractVariableTypes(stateContent),
            true);

    for (BooleanFormula<CompoundInterval> assumption : assumptionParts) {
      deserializedInvariantsState = deserializedInvariantsState.assume(assumption);
    }
    deserializedInvariantsState =
        deserializedInvariantsState.addAssumptions(ImmutableSet.copyOf(assumptionParts));

    return deserializedInvariantsState;
  }

  private Map<MemoryLocation, CType> extractVariableTypes(ContentReader pContentReader) {
    String variableTypesString = pContentReader.get(VARIABLE_TYPES);

    Map<MemoryLocation, CType> variableTypes = new HashMap<>();
    for (String variableTypeEntry : Splitter.on(" && ").split(variableTypesString)) {
      variableTypeEntry = variableTypeEntry.trim();
      if (!variableTypeEntry.isEmpty()) {
        List<String> parts = Splitter.on(".ti").splitToList(variableTypeEntry);
        MemoryLocation memoryLocation = MemoryLocation.parseExtendedQualifiedName(parts.get(0));
        CType type = CTypeParser.parse(parts.get(1));
        variableTypes.put(memoryLocation, type);
      }
    }
    return variableTypes;
  }

  private AbstractionStrategyFactories extractAbstractionStrategy(ContentReader pContentReader) {
    String strategyString = pContentReader.get(STRATEGY);
    return AbstractionStrategyFactories.valueOf(strategyString);
  }
}
