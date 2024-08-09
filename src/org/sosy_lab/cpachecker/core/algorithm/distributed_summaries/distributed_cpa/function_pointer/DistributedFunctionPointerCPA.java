// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.JoinWidenOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.WidenOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.BackwardsExecutionViolationConditionSynthesizer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationConditionSynthesizer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;

public class DistributedFunctionPointerCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final WidenOperator combine;
  private final ViolationConditionSynthesizer synthesizer;

  private final FunctionPointerCPA functionPointerCPA;

  public DistributedFunctionPointerCPA(
      FunctionPointerCPA pParentCPA,
      BlockNode pNode,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap) {
    functionPointerCPA = pParentCPA;
    serialize = new SerializeFunctionPointerStateOperator();
    combine = new JoinWidenOperator(pParentCPA, pNode, true);
    deserialize = new DeserializeFunctionPointerStateOperator(pParentCPA, pIntegerCFANodeMap);
    synthesizer =
        new BackwardsExecutionViolationConditionSynthesizer(
            pParentCPA.getTransferRelation(), getAbstractStateClass());
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serialize;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserialize;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return ProceedOperator.always();
  }

  @Override
  public WidenOperator getCombineOperator() {
    return combine;
  }

  @Override
  public ViolationConditionSynthesizer getViolationConditionSynthesizer() {
    return synthesizer;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return FunctionPointerState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return functionPointerCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    return true;
  }
}
