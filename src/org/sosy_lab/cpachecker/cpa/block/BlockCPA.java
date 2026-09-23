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
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class BlockCPA extends AbstractCPA {

  /**
   * The lattice of this CPA: a state is below itself and below a state that a merge built from it.
   *
   * <p>{@link BlockState} carries bookkeeping that the distributed-summary-synthesis algorithm
   * reads back from individual states of the reached set -- the {@link BlockState#getPredecessor()
   * back-pointer}, the block-graph {@link BlockState#getHistory() history}, and, indirectly through
   * {@link org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState}, the edges a path traversed. A
   * value-based lattice would ignore all of that and let the stop operator drop states that the
   * algorithm still needs, which loses paths through the block.
   *
   * <p>Being above a merged state is different: {@link
   * org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoin} gives the merged state the parents of both states
   * it was built from, so a merge folds two states into one without losing a path. Only {@link
   * #getMergeOperator()} produces such a state, and it does so only for the pair that the composite
   * CPA is about to merge, so this relation cannot be reached by coverage.
   */
  private static final class BlockStateMergeDomain implements AbstractDomain {

    @Override
    public AbstractState join(AbstractState pState1, AbstractState pState2) {
      throw new UnsupportedOperationException("BlockCPA merges through its merge operator");
    }

    @Override
    public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) {
      return pState1 == pState2
          || (pState1 instanceof BlockState state1
              && pState2 instanceof BlockState state2
              && state2.absorbs(state1));
    }
  }

  /**
   * Folds a successor into a state of the reached set when {@link
   * BlockState#permitsMergeWith(BlockState)} allows it, and refuses otherwise by returning the
   * reached state unchanged.
   *
   * <p>Whether the merge actually happens is not decided here. The composite CPA merges only if
   * every component agrees, so the predicate analysis still decides whether two states at this
   * location are merged at all; this operator only stops the block CPA from vetoing that decision.
   */
  private static final class BlockStateMergeOperator implements MergeOperator {

    @Override
    public AbstractState merge(
        AbstractState pSuccessorState, AbstractState pReachedState, Precision pPrecision) {
      return pSuccessorState instanceof BlockState successor
              && pReachedState instanceof BlockState reached
              && reached.permitsMergeWith(successor)
          ? reached.mergedWith(successor)
          : pReachedState;
    }
  }

  private @LazyInit BlockNode blockNode;
  private final UniqueIdGenerator idGenerator;
  private final TransferRelation transferRelation;

  public BlockCPA(Configuration pConfiguration) throws InvalidConfigurationException {
    super("sep", "sep", new BlockStateMergeDomain(), null);
    idGenerator = new UniqueIdGenerator();
    transferRelation = new BlockTransferRelation(idGenerator);
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
        blockNode == null ? "null" : blockNode.getId() + "#" + idGenerator.getFreshId(),
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
  public MergeOperator getMergeOperator() {
    return new BlockStateMergeOperator();
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
