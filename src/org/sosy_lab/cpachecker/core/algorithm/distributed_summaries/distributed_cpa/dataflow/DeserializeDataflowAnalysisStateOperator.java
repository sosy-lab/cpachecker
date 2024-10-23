// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeParser;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
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

public class DeserializeDataflowAnalysisStateOperator implements DeserializeOperator {
  private final CFA cfa;
  private final InvariantsCPA invariantsCPA;
  private final BlockNode blockNode;

  public DeserializeDataflowAnalysisStateOperator(
      InvariantsCPA pInvariantsCPA, CFA pCFA, BlockNode pBlockNode) {
    cfa = pCFA;
    invariantsCPA = pInvariantsCPA;
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    Optional<Object> abstractStateOptional = pMessage.getAbstractState(InvariantsCPA.class);
    if (abstractStateOptional.isEmpty()) {
      return invariantsCPA.getInitialState(
          blockNode.getFirst(), StateSpacePartition.getDefaultPartition());
    }
    String booleanFormulaString = (String) abstractStateOptional.get();
    BooleanFormula<CompoundInterval> booleanFormula =
        StringToBooleanFormulaParser.parseBooleanFormula(booleanFormulaString);
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
            extractAbstractionStrategy(pMessage)
                .createStrategy(
                    invariantsCPA.getCompoundIntervalFormulaManagerFactory(), cfa.getMachineModel())
                .getAbstractionState(),
            extractVariableTypes(pMessage),
            true);

    for (BooleanFormula<CompoundInterval> assumption : assumptionParts) {
      deserializedInvariantsState = deserializedInvariantsState.assume(assumption);
    }
    deserializedInvariantsState =
        deserializedInvariantsState.addAssumptions(ImmutableSet.copyOf(assumptionParts));

    return deserializedInvariantsState;
  }

  private Map<MemoryLocation, CType> extractVariableTypes(BlockSummaryMessage pMessage) {
    String variableTypesString = "";
    if (pMessage instanceof BlockSummaryPostConditionMessage postConditionMessage) {
      variableTypesString = postConditionMessage.getVTypes();
    } else if (pMessage instanceof BlockSummaryErrorConditionMessage errorConditionMessage) {
      variableTypesString = errorConditionMessage.getVTypes();
    }

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

  private AbstractionStrategyFactories extractAbstractionStrategy(BlockSummaryMessage pMessage) {
    String strategyString = "";
    if (pMessage instanceof BlockSummaryPostConditionMessage postMessage) {
      strategyString = postMessage.getAbstractionStrategy();
    } else if (pMessage instanceof BlockSummaryErrorConditionMessage errorMessage) {
      strategyString = errorMessage.getAbstractionStrategy();
    }
    return AbstractionStrategyFactories.valueOf(strategyString);
  }
}
