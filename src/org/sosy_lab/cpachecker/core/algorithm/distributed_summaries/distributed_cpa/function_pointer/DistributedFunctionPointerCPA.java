// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineSingletonPrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.EqualityCombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.NoPrecisionDeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.NoPrecisionSerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.BackwardTransferViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;

public class DistributedFunctionPointerCPA
    implements ForwardingDistributedConfigurableProgramAnalysis {

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final ViolationConditionOperator verificationConditionOperator;
  private final CoverageOperator coverageOperator;
  private final CombineOperator combineOperator;
  private final CombinePrecisionOperator combinePrecisionOperator;

  private final FunctionPointerCPA functionPointerCPA;

  public DistributedFunctionPointerCPA(FunctionPointerCPA pParentCPA, BlockNode pNode) {
    functionPointerCPA = pParentCPA;
    serialize = new SerializeFunctionPointerStateOperator();
    deserialize = new DeserializeFunctionPointerStateOperator(pParentCPA, pNode);
    serializePrecisionOperator = new NoPrecisionSerializeOperator();
    deserializePrecisionOperator = new NoPrecisionDeserializeOperator();
    verificationConditionOperator =
        new BackwardTransferViolationConditionOperator(
            pParentCPA.getTransferRelation(), pParentCPA);
    coverageOperator = new FunctionPointerStateCoverageOperator();
    combineOperator = new EqualityCombineOperator(coverageOperator, getAbstractStateClass());
    combinePrecisionOperator = new CombineSingletonPrecisionOperator();
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
    return ProceedOperator.always();
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
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    return true;
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    Preconditions.checkArgument(
        pAbstractState instanceof FunctionPointerState,
        "Expected FunctionPointerState, but got %s",
        pAbstractState.getClass().getSimpleName());
    return pAbstractState;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return verificationConditionOperator;
  }

  @Override
  public CoverageOperator getCoverageOperator() {
    return coverageOperator;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return combineOperator;
  }
}
