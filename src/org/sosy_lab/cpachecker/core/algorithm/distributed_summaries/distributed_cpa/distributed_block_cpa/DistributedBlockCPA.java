// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;

public class DistributedBlockCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final DeserializeOperator deserializeOperator;
  private final SerializeOperator serializeOperator;
  private final ProceedOperator proceedOperator;
  private final ViolationConditionOperator verificationConditionOperator;
  private final CoverageOperator coverageOperator;
  private final SerializePrecisionOperator serializePrecisionOperator;
  private final DeserializePrecisionOperator deserializePrecisionOperator;
  private final CombineOperator combineOperator;

  private final ConfigurableProgramAnalysis blockCpa;
  private final BlockNode node;
  private final Function<CFANode, BlockState> blockStateSupplier;
  private final CombinePrecisionOperator combinePrecisionOperator;

  public DistributedBlockCPA(
      ConfigurableProgramAnalysis pBlockCpa, BlockNode pNode, DssAnalysisOptions pOptions) {
    checkArgument(
        pBlockCpa instanceof BlockCPA, "%s is no %s", pBlockCpa.getClass(), BlockCPA.class);
    blockCpa = pBlockCpa;
    node = pNode;
    blockStateSupplier =
        location ->
            new BlockState(
                location, pNode, BlockStateType.INITIAL, ImmutableList.of(), ImmutableList.of());

    serializeOperator = new SerializeBlockStateOperator();
    deserializeOperator = new DeserializeBlockStateOperator(pNode);
    proceedOperator = new ProceedBlockStateOperator(pNode);
    verificationConditionOperator =
        new BlockViolationConditionOperator(pOptions.isDebugModeEnabled());
    coverageOperator = new BlockStateCoverageOperator();
    serializePrecisionOperator = new NoPrecisionSerializeOperator();
    deserializePrecisionOperator = new NoPrecisionDeserializeOperator();
    combineOperator = new EqualityCombineOperator(coverageOperator, getAbstractStateClass());
    combinePrecisionOperator = new CombineSingletonPrecisionOperator();
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
  public Class<? extends AbstractState> getAbstractStateClass() {
    return BlockState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return blockCpa;
  }

  @Override
  public boolean isMostGeneralBlockEntryState(AbstractState pAbstractState) {
    Preconditions.checkArgument(
        pAbstractState instanceof BlockState,
        "Expected BlockState, but got %s",
        pAbstractState.getClass().getSimpleName());
    CFANode location = ((BlockState) pAbstractState).getLocationNode();
    return location.equals(node.getInitialLocation()) || location.equals(node.getFinalLocation());
  }

  @Override
  public AbstractState reset(AbstractState pAbstractState) {
    Preconditions.checkArgument(
        pAbstractState instanceof BlockState,
        "Expected BlockState, but got %s",
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

  @Override
  public AbstractState getInitialState(CFANode location, StateSpacePartition partition)
      throws InterruptedException {
    return Objects.requireNonNull(blockStateSupplier.apply(location));
  }
}
