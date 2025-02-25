// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import java.util.HashMap;
import java.util.Map;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DistributedValueAnalysisCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final ValueAnalysisCPA valueAnalysisCPA;
  private final SerializeOperator serializeOperator;
  private final DeserializeOperator deserializeOperator;
  private final Map<MemoryLocation, CType> variableTypes;

  private final BlockNode blockNode;

  public DistributedValueAnalysisCPA(
      ValueAnalysisCPA pValueAnalysisCPA,
      BlockNode pNode,
      CFA pCFA,
      Configuration pConfig,
      org.sosy_lab.common.log.LogManager pLogManager,
      ShutdownNotifier shutdownNotifier,
      Map<MemoryLocation, CType> pVariableTypes)
      throws InvalidConfigurationException {
    valueAnalysisCPA = pValueAnalysisCPA;
    variableTypes = new HashMap<>(pVariableTypes);
    serializeOperator =
        new SerializeValueAnalysisStateOperator(
            valueAnalysisCPA, pConfig, pLogManager, shutdownNotifier);
    deserializeOperator =
        new DeserializeValueAnalysisStateOperator(
            pCFA, variableTypes, pConfig, pLogManager, shutdownNotifier);
    blockNode = pNode;
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
    return ValueAnalysisState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return valueAnalysisCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    if (pAbstractState instanceof ValueAnalysisState v) {
      return v.getConstants().isEmpty();
    }
    return false;
  }

  @Override
  public AbstractState computeVerificationCondition(ARGPath pARGPath, ARGState pPreviousCondition) {
    return valueAnalysisCPA.getInitialState(
        blockNode.getFirst(), StateSpacePartition.getDefaultPartition());
  }
}
