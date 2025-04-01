// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DistributedDataFlowAnalysisCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final InvariantsCPA invariantsCPA;
  private final SerializeOperator serializeOperator;
  private final DeserializeOperator deserializeOperator;
  private final BlockNode blockNode;
  private final Map<MemoryLocation, CType> variableTypes;
  private final Solver solver;

  public DistributedDataFlowAnalysisCPA(
      InvariantsCPA pInvariantsCPA,
      BlockNode pNode,
      CFA pCFA,
      Configuration pConfig,
      org.sosy_lab.common.log.LogManager pLogManager,
      ShutdownNotifier shutdownNotifier,
      Map<MemoryLocation, CType> pVariableTypes)
      throws InvalidConfigurationException {
    invariantsCPA = pInvariantsCPA;
    blockNode = pNode;
    variableTypes = new HashMap<>(pVariableTypes);
    solver = Solver.create(pConfig, pLogManager, shutdownNotifier);
    serializeOperator = new SerializeDataflowAnalysisStateOperator(solver);
    deserializeOperator =
        new DeserializeDataflowAnalysisStateOperator(
            invariantsCPA, pCFA, pNode, variableTypes, solver);
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
  public ProceedOperator getProceedOperator() {
    return ProceedOperator.always();
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return InvariantsState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return invariantsCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    return false;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return new ViolationConditionOperator() {
      @Override
      public Optional<AbstractState> computeViolationCondition(
          ARGPath pARGPath, Optional<ARGState> pPreviousCondition) throws InterruptedException {
        return Optional.of(
            invariantsCPA.getInitialState(
                blockNode.getInitialLocation(), StateSpacePartition.getDefaultPartition()));
      }
    };
  }
}
