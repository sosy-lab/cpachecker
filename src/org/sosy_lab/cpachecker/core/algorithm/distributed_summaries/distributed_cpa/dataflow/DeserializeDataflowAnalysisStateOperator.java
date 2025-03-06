// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.SMTToBooleanIntervalFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.SMTToNumeralIntervalFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DeserializeDataflowAnalysisStateOperator implements DeserializeOperator {
  private final CFA cfa;
  private final InvariantsCPA invariantsCPA;
  private final BlockNode blockNode;
  private final Map<MemoryLocation, CType> variableTypes;
  private final Solver solver;
  private final FormulaManagerView formulaManager;

  public DeserializeDataflowAnalysisStateOperator(
      InvariantsCPA pInvariantsCPA,
      CFA pCFA,
      BlockNode pBlockNode,
      Map<MemoryLocation, CType> pVariableTypes,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    cfa = pCFA;
    invariantsCPA = pInvariantsCPA;
    blockNode = pBlockNode;
    variableTypes = pVariableTypes;
    solver = Solver.create(config, pLogger, shutdownNotifier);
    formulaManager = solver.getFormulaManager();
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    Optional<Object> abstractStateOptional = pMessage.getAbstractState(InvariantsCPA.class);
    if (abstractStateOptional.isEmpty()) {
      return invariantsCPA.getInitialState(
          blockNode.getFirst(), StateSpacePartition.getDefaultPartition());
    }
    String booleanFormulaString = (String) abstractStateOptional.orElseThrow();
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
            variableTypes,
            true);

    for (BooleanFormula<CompoundInterval> assumption : assumptionParts) {
      deserializedInvariantsState = deserializedInvariantsState.assume(assumption);
    }
    deserializedInvariantsState =
        deserializedInvariantsState.addAssumptions(ImmutableSet.copyOf(assumptionParts));

    return deserializedInvariantsState;
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

  @Override
  public InvariantsState deserializeFromFormula(org.sosy_lab.java_smt.api.BooleanFormula pFormula) {
    SMTToNumeralIntervalFormulaVisitor smtToNumeralFormulaVisitor =
        new SMTToNumeralIntervalFormulaVisitor(
            formulaManager, variableTypes, cfa.getMachineModel());

    SMTToBooleanIntervalFormulaVisitor SMTToBooleanIntervalFormulaVisitor =
        new SMTToBooleanIntervalFormulaVisitor(formulaManager, smtToNumeralFormulaVisitor);
    BooleanFormula<CompoundInterval> compoundInterval =
        formulaManager.visit(pFormula, SMTToBooleanIntervalFormulaVisitor);
    AcceptSpecifiedVariableSelection<CompoundInterval> collectVarsVariableSelection =
        new AcceptSpecifiedVariableSelection<>(compoundInterval.accept(new CollectVarsVisitor<>()));
    List<BooleanFormula<CompoundInterval>> assumptionParts =
        compoundInterval.accept(new SplitConjunctionsVisitor<>());
    InvariantsState deserializedInvariantsState =
        new InvariantsState(
            collectVarsVariableSelection,
            invariantsCPA.getCompoundIntervalFormulaManagerFactory(),
            cfa.getMachineModel(),
            invariantsCPA
                .getAbstractionStrategy()
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
