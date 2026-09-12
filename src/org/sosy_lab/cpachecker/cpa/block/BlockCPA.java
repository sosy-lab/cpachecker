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
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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

@Options(prefix = "cpa.block")
public class BlockCPA extends AbstractCPA {

  private enum DomainType {
    IDENTITY,
    VALUE
  }

  @Option(
      secure = true,
      description =
          "Use identity coverage, or compare immutable interior block values. VALUE requires ARG"
              + " path preservation and the DSS callstack domain; boundary occurrences remain"
              + " separate.")
  private DomainType domain = DomainType.IDENTITY;

  /**
   * Local coverage must preserve every path that DSS reads back from the analysis.
   *
   * <p>{@link BlockState} carries bookkeeping that the distributed-summary-synthesis algorithm
   * reads back from individual states of the reached set -- the {@link BlockState#getPredecessor()
   * back-pointer}, the block-graph {@link BlockState#getHistory() history}, and, indirectly through
   * {@link org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState}, the edges a path traversed. A
   * value-based lattice is safe only with ARG path preservation and callstack-effect coverage.
   * Those components retain incoming paths and prevent mixing different backwards callstack
   * behaviours. Boundary occurrences keep their own processing records in both modes.
   */
  private final class BlockStateDomain implements AbstractDomain {

    @Override
    public AbstractState join(AbstractState pState1, AbstractState pState2) {
      throw new UnsupportedOperationException("BlockCPA does not support merging states");
    }

    @Override
    public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) {
      return pState1 == pState2 || (domain == DomainType.VALUE && pState1.equals(pState2));
    }
  }

  private @LazyInit BlockNode blockNode;
  private final UniqueIdGenerator idGenerator;
  private final TransferRelation transferRelation;

  public BlockCPA(Configuration pConfiguration) throws InvalidConfigurationException {
    super("sep", "sep", null);
    pConfiguration.inject(this);
    idGenerator = new UniqueIdGenerator();
    transferRelation = new BlockTransferRelation(pConfiguration, idGenerator);
  }

  public boolean usesValueDomain() {
    return domain == DomainType.VALUE;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new BlockStateDomain();
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
