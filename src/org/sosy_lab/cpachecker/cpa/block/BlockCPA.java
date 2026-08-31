// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.concurrent.LazyInit;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class BlockCPA extends AbstractCPA {

  private @LazyInit BlockNode blockNode;
  private final UniqueIdGenerator idGenerator;
  private final TransferRelation transferRelation;

  public BlockCPA(Configuration pConfiguration) throws InvalidConfigurationException {
    super("sep", "sep", new BlockDomain(), null);
    idGenerator = new UniqueIdGenerator();
    transferRelation = new BlockTransferRelation(pConfiguration, idGenerator);
  }

  /**
   * Domain of {@link BlockCPA}, which compares block states structurally.
   *
   * <p>{@link BlockTransferRelation} creates a fresh block state for every CFA edge, so the
   * identity-based equality that {@link BlockState} inherits from {@link Object} would make every
   * coverage check fail. Since {@link
   * org.sosy_lab.cpachecker.cpa.composite.CompositeMergeAgreeOperator} only merges if every
   * component covers its successor, that would also prevent all merges of the enclosing composite
   * analysis, and a block analysis would enumerate every path through its block.
   */
  private static final class BlockDomain implements AbstractDomain {

    @Override
    public AbstractState join(AbstractState pState1, AbstractState pState2) {
      throw new UnsupportedOperationException("Block states are never joined, BlockCPA uses 'sep'");
    }

    @Override
    public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) {
      return ((BlockState) pState1).isEqualTo((BlockState) pState2);
    }
  }

  public void init(BlockNode pBlockNode) {
    assert pBlockNode != null;
    assert blockNode == null;
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new BlockState(
        blockNode.getId() + "#" + idGenerator.getFreshId(),
        null,
        node,
        blockNode,
        BlockStateType.INITIAL,
        ImmutableList.of(),
        BlockGraphPath.of(),
        SegmentedPaths.EMPTY);
  }

  public UniqueIdGenerator getIdGenerator() {
    return idGenerator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new BlockPrecisionAdjustment();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BlockCPA.class);
  }
}
