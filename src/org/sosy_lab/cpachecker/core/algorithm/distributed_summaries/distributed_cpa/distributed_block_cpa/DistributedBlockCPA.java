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
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
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

  private final ConfigurableProgramAnalysis blockCPA;
  private final Function<CFANode, BlockState> blockStateSupplier;

  public DistributedBlockCPA(ConfigurableProgramAnalysis pBlockCPA, BlockNode pNode) {
    checkArgument(
        pBlockCPA instanceof BlockCPA, "%s is no %s", pBlockCPA.getClass(), BlockCPA.class);
    blockCPA = pBlockCPA;
    serializeOperator = new SerializeBlockStateOperator();
    deserializeOperator = new DeserializeBlockStateOperator(pNode);
    proceedOperator = new ProceedBlockStateOperator(pNode);
    blockStateSupplier =
        node -> new BlockState(node, pNode, BlockStateType.INITIAL, Optional.empty());
    verificationConditionOperator = new BlockViolationConditionOperator();
    coverageOperator = new BlockStateCoverageOperator();
    serializePrecisionOperator = new NoPrecisionSerializeOperator();
    deserializePrecisionOperator = new NoPrecisionDeserializeOperator();
    combineOperator = new EqualityCombineOperator(coverageOperator, getAbstractStateClass());
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
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return BlockState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return blockCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    return true;
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
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return Objects.requireNonNull(blockStateSupplier.apply(node));
  }
}
