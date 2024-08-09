// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.JoinWidenOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.WidenOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.AlwaysViolationConditionSynthesizer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationConditionSynthesizer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;

public class DistributedDataFlowAnalysisCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final InvariantsCPA invariantsCPA;
  private final SerializeOperator serializeOperator;
  private final DeserializeOperator deserializeOperator;
  private final WidenOperator widenOperator;
  private final BlockNode blockNode;

  public DistributedDataFlowAnalysisCPA(InvariantsCPA pInvariantsCPA, BlockNode pNode, CFA pCFA) {
    invariantsCPA = pInvariantsCPA;
    blockNode = pNode;
    serializeOperator = new SerializeDataflowAnalysisStateOperator();
    deserializeOperator = new DeserializeDataflowAnalysisStateOperator(invariantsCPA, pCFA);
    widenOperator = new JoinWidenOperator(invariantsCPA, blockNode, false);
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
  public WidenOperator getCombineOperator() {
    return widenOperator;
  }

  @Override
  public ViolationConditionSynthesizer getViolationConditionSynthesizer() {
    return new AlwaysViolationConditionSynthesizer(invariantsCPA, blockNode.getFirst());
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
}
