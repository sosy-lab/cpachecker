// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.symbolic_execution;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value.DeserializeValueAnalysisStateOperator.havocVariables;

import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineSingletonPrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.NoPrecisionDeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.NoPrecisionSerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.symbolicExecution.SymbolicExecutionCPA;
import org.sosy_lab.cpachecker.cpa.symbolicExecution.SymbolicExecutionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;

public class DistributedSymbolicExecutionCPA implements DistributedConfigurableProgramAnalysis {
  SymbolicExecutionStateCoverageOperator coverageOperator;
  SymbolicExecutionViolationConditionOperator violationConditionOperator;
  SerializePrecisionOperator serializePrecisionOperator;
  DeserializePrecisionOperator deserializePrecisionOperator;
  CombinePrecisionOperator combinePrecisionOperator;
  SerializeSymbolicExecutionStateOperator serializeStateOperator;
  DeserializeSymbolicExecutionStateOperator deserializeStateOperator;
  ProceedOperator proceedOperator;
  SymbolicExecutionCPA cpa;
  BlockNode blockNode;
  CFA cfa;

  public static ConcurrentMap<String, ValueAnalysisState> initialValueStates =
      new ConcurrentHashMap<>();

  public DistributedSymbolicExecutionCPA(
      SymbolicExecutionCPA pCPA, CFA pCFA, BlockNode pBlockNode) {
    cpa = pCPA;
    blockNode = pBlockNode;
    cfa = pCFA;

    deserializeStateOperator = new DeserializeSymbolicExecutionStateOperator(pBlockNode);
    serializeStateOperator = new SerializeSymbolicExecutionStateOperator();

    proceedOperator = ProceedOperator.always();
    deserializePrecisionOperator = new NoPrecisionDeserializeOperator();
    serializePrecisionOperator = new NoPrecisionSerializeOperator();
    combinePrecisionOperator = new CombineSingletonPrecisionOperator();

    violationConditionOperator =
        new SymbolicExecutionViolationConditionOperator(
            pBlockNode,
            pCPA,
            getInitialState(
                pBlockNode.getInitialLocation(), StateSpacePartition.getDefaultPartition()));

    coverageOperator =
        new SymbolicExecutionStateCoverageOperator(
            cpa.getConstraintsCPA().getSolver(), pBlockNode.getInitialLocation().getFunctionName());
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serializeStateOperator;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserializeStateOperator;
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
  public CombineViolationConditionsOperator getCombineViolationConditionsOperator() {
    return states -> Iterables.getOnlyElement(states);
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
  public CombinePreconditionsOperator getCombineOperator() {
    return null;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return SymbolicExecutionState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }

  @Override
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    SymbolicExecutionState state = (SymbolicExecutionState) pAbstractState;
    if (!state.constraintsState().isEmpty()) {
      return false;
    }
    return state.valueAnalysisState().getConstants().stream()
        .allMatch(
            constant ->
                constant.getValue().getValue() instanceof ConstantSymbolicExpression symExp
                    && symExp.getValue() instanceof SymbolicIdentifier);
  }

  @Override
  public int computeProgramPointHash(AbstractState pAbstractState) {
    return Objects.hash(this, pAbstractState);
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    return pAbstractState;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return cpa.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return cpa.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return cpa.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return cpa.getStopOperator();
  }

  @Override
  public SymbolicExecutionState getInitialState(CFANode node, StateSpacePartition partition) {

    if (initialValueStates.containsKey(blockNode.getId())) {
      return new SymbolicExecutionState(
          initialValueStates.get(blockNode.getId()), new ConstraintsState());
    }
    ValueAnalysisState init = new ValueAnalysisState(cfa.getMachineModel());
    Map<String, Type> accessedVars = deserializeStateOperator.getAccessedVariables(blockNode);
    havocVariables(init, accessedVars);
    initialValueStates.put(blockNode.getId(), init);
    return new SymbolicExecutionState(init, new ConstraintsState());
  }
}
