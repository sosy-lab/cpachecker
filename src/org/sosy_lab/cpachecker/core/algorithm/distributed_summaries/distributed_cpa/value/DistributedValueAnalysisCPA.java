// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.termination.ClassVariables;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DistributedValueAnalysisCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {
  private final ValueAnalysisCPA valueCPA;
  private final CFA cfa;
  private final BlockNode blockNode;
  private final SerializeValueAnalysisStateOperator serializeOperator;
  private final DeserializeValueAnalysisStateOperator deserializeOperator;
  private final ValueViolationConditionOperator violationConditionOperator;
  private final SerializeValuePrecisionOperator serializePrecisionOperator;
  private final DeserializeValuePrecisionOperator deserializePrecisionOperator;
  private final ProceedValueStateOperator proceedOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;
  private final ValueStateCoverageOperator coverageOperator;
  private final FormulaManagerView formulaManager;
  static Map<String, ValueAnalysisState> initialState = new HashMap<>();
  static Optional<Map<String, Type>> globals = Optional.empty();

  public DistributedValueAnalysisCPA(
      ValueAnalysisCPA pValueCPA,
      CFA pCFA,
      Configuration pConfiguration,
      BlockNode pBlockNode,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    valueCPA = pValueCPA;
    cfa = pCFA;
    Solver solver = Solver.create(pConfiguration, pLogManager, pShutdownNotifier);
    formulaManager = solver.getFormulaManager();
    serializeOperator = new SerializeValueAnalysisStateOperator();
    deserializeOperator = new DeserializeValueAnalysisStateOperator(pBlockNode, pCFA);
    violationConditionOperator =
        new ValueViolationConditionOperator(cfa.getMachineModel(), pBlockNode);

    serializePrecisionOperator = new SerializeValuePrecisionOperator();
    deserializePrecisionOperator =
        new DeserializeValuePrecisionOperator(pConfiguration, pCFA.getVarClassification());
    proceedOperator = new ProceedValueStateOperator();
    combinePrecisionOperator = new CombineValuePrecisionOperator();
    coverageOperator = new ValueStateCoverageOperator(solver);
    blockNode = pBlockNode;

    if (globals.isEmpty()) {
      initializeGlobals(pCFA);
    }
  }

  private void initializeGlobals(CFA pCFA) {
    Map<String, Type> newGlobals = new HashMap<>();
    ImmutableSet<CVariableDeclaration> declarations =
        ClassVariables.collectDeclarations(pCFA).getGlobalDeclarations();

    for (CVariableDeclaration decl : declarations) {
      newGlobals.put(decl.getQualifiedName(), decl.getType());
    }
    globals = Optional.of(newGlobals);
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serializeOperator;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserializeOperator;
  }

  @Override
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return deserializePrecisionOperator;
  }

  @Override
  public CombinePrecisionOperator getCombinePrecisionOperator() {
    return combinePrecisionOperator;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return violationConditionOperator;
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return coverageOperator;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return null;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return ValueAnalysisState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return valueCPA;
  }

  @Override
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    BooleanFormula formula =
        ((ValueAnalysisState) pAbstractState).getFormulaApproximation(formulaManager);
    return formulaManager.getBooleanFormulaManager().isTrue(formula);
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    return pAbstractState;
  }

  @Override
  public ValueAnalysisState getInitialState(CFANode node, StateSpacePartition partition) {
    ValueAnalysisState init = new ValueAnalysisState(cfa.getMachineModel());
    Map<String, Type> accessedVars = deserializeOperator.getAccessedVariables(blockNode);
    DeserializeValueAnalysisStateOperator.havocVariables(init, globals.get());
    DeserializeValueAnalysisStateOperator.havocVariables(init, accessedVars);
    return init;
  }
}
