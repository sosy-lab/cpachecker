// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.JoinWidenOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.WidenOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.AlwaysViolationConditionSynthesizer;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationConditionSynthesizer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public class DistributedValueAnalysisCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final ValueAnalysisCPA valueAnalysisCPA;
  private final SerializeOperator serializeOperator;
  private final DeserializeOperator deserializeOperator;
  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final WidenOperator widenOperator;
  private final ViolationConditionSynthesizer synthesizer;

  public DistributedValueAnalysisCPA(
      ValueAnalysisCPA pValueAnalysisCPA, BlockNode pNode, CFA pCFA) {
    valueAnalysisCPA = pValueAnalysisCPA;
    serializeOperator = new SerializeValueAnalysisStateOperator();
    deserializeOperator = new DeserializeValueAnalysisStateOperator(pCFA);
    serializePrecisionOperator = new SerializeVariableTrackingPrecision();
    deserializePrecisionOperator = new DeserializeVariableTrackingPrecision(pValueAnalysisCPA);
    widenOperator = new JoinWidenOperator(pValueAnalysisCPA, pNode, true);
    synthesizer = new AlwaysViolationConditionSynthesizer(pValueAnalysisCPA, pNode.getFirst());
  }

  @Override
  public DeserializePrecisionOperator getDeserializePrecisionOperator() {
    return deserializePrecisionOperator;
  }

  @Override
  public SerializePrecisionOperator getSerializePrecisionOperator() {
    return serializePrecisionOperator;
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
    return synthesizer;
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
}
