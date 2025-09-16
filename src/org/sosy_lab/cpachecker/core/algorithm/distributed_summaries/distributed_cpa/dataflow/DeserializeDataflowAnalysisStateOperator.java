// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow.SerializeDataflowAnalysisStateOperator.STRATEGY;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
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
  private final FormulaManagerView formulaManager;

  public DeserializeDataflowAnalysisStateOperator(
      InvariantsCPA pInvariantsCPA,
      CFA pCFA,
      BlockNode pBlockNode,
      Map<MemoryLocation, CType> pVariableTypes,
      Solver pSolver) {
    cfa = pCFA;
    invariantsCPA = pInvariantsCPA;
    blockNode = pBlockNode;
    variableTypes = pVariableTypes;
    formulaManager = pSolver.getFormulaManager();
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    ContentReader stateContent = pMessage.getAbstractStateContent(InvariantsState.class);
    if (!stateContent.getContent().containsKey(STATE_KEY)) {
      return invariantsCPA.getInitialState(
          blockNode.getInitialLocation(), StateSpacePartition.getDefaultPartition());
    }
    String booleanFormulaString = stateContent.get(STATE_KEY);
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
            AbstractionStrategyFactories.valueOf(stateContent.get(STRATEGY))
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

  @Override
  public InvariantsState deserializeFromFormula(org.sosy_lab.java_smt.api.BooleanFormula pFormula) {
    SMTToNumeralIntervalFormulaVisitor smtToNumeralFormulaVisitor =
        new SMTToNumeralIntervalFormulaVisitor(
            formulaManager, variableTypes, cfa.getMachineModel());

    SMTToBooleanIntervalFormulaVisitor smtToBooleanIntervalFormulaVisitor =
        new SMTToBooleanIntervalFormulaVisitor(formulaManager, smtToNumeralFormulaVisitor);
    BooleanFormula<CompoundInterval> compoundInterval =
        formulaManager.visit(pFormula, smtToBooleanIntervalFormulaVisitor);
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
