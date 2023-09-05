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
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.HashSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPABackward;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;

public class DistributedBlockCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  private final DeserializeOperator deserializeOperator;
  private final SerializeOperator serializeOperator;
  private final ProceedOperator proceedOperator;

  private final ConfigurableProgramAnalysis blockCPA;
  private final Function<CFANode, BlockState> blockStateSupplier;

  public DistributedBlockCPA(
      ConfigurableProgramAnalysis pBlockCPA,
      BlockNode pNode,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap,
      AnalysisDirection pDirection) {
    checkArgument(
        pBlockCPA instanceof BlockCPA || pBlockCPA instanceof BlockCPABackward,
        "%s is no %s",
        pBlockCPA.getClass(),
        BlockCPA.class);
    blockCPA = pBlockCPA;
    serializeOperator = new SerializeBlockStateOperator();
    deserializeOperator = new DeserializeBlockStateOperator(pNode, pIntegerCFANodeMap, pDirection);
    proceedOperator = new ProceedBlockStateOperator(pNode, pDirection);
    blockStateSupplier =
        node ->
            new BlockState(
                node,
                pNode,
                pDirection,
                BlockStateType.INITIAL,
                new HashMap<>(),
                new HashSet<>(),
                new HashMap<>());
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
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return blockStateSupplier.apply(node);
  }
}
