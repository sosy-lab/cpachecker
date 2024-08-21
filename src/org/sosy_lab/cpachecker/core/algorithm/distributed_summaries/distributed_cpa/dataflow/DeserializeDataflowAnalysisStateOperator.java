// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
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
import org.sosy_lab.cpachecker.cfa.types.c.StringToCTypeParser;
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
    if (!abstractStateOptional.isPresent()) {
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

    String abstractionStrategy = "";
    String variableTypesString = "";

    if (pMessage instanceof BlockSummaryPostConditionMessage) {
      abstractionStrategy = ((BlockSummaryPostConditionMessage) pMessage).getAbstractionStrategy();
      variableTypesString = ((BlockSummaryPostConditionMessage) pMessage).getVTypes();
    } else if (pMessage instanceof BlockSummaryErrorConditionMessage) {
      abstractionStrategy = ((BlockSummaryErrorConditionMessage) pMessage).getAbstractionStrategy();
      variableTypesString = ((BlockSummaryErrorConditionMessage) pMessage).getVTypes();
    }

    Map<MemoryLocation, CType> variableTypes = new HashMap<>();

    for (String variableTypeEntry : Splitter.on(" && ").split(variableTypesString)) {
      variableTypeEntry = variableTypeEntry.trim();
      if (variableTypeEntry.isEmpty()) {
        continue;
      }
      List<String> parts = Splitter.on("-typeInfo>").splitToList(variableTypeEntry);
      MemoryLocation memoryLocation = MemoryLocation.parseExtendedQualifiedName(parts.get(0));
      CType type = StringToCTypeParser.parse(parts.get(1));
      variableTypes.put(memoryLocation, type);
    }

    InvariantsState deserializedInvariantsState =
        new InvariantsState(
            collectVarsVariableSelection,
            invariantsCPA.getCompoundIntervalFormulaManagerFactory(),
            cfa.getMachineModel(),
            AbstractionStrategyFactories.valueOf(abstractionStrategy)
                .createStrategy(
                    invariantsCPA.getCompoundIntervalFormulaManagerFactory(), cfa.getMachineModel())
                .getAbstractionState(),
            variableTypes,
            true);

    for (BooleanFormula<CompoundInterval> assumption : assumptionParts) {
      deserializedInvariantsState = deserializedInvariantsState.assume(assumption);
    }
    deserializedInvariantsState =
        deserializedInvariantsState.addAssumptions(ImmutableSet.copyOf(assumptionParts));

    return deserializedInvariantsState;
  }
}
