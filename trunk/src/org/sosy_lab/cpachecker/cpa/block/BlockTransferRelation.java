// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public abstract class BlockTransferRelation implements TransferRelation {

  protected ImmutableSet<CFAEdge> edges;
  protected ImmutableSet<CFANode> nodes;
  protected CFANode targetNode;
  protected BlockNode bNode;

  public void init(BlockNode pBlockNode) {
    assert edges == null;
    assert nodes == null;
    assert targetNode == null;
    assert bNode == null;
    assert pBlockNode != null;
    edges = ImmutableSet.copyOf(pBlockNode.getEdgesInBlock());
    nodes = ImmutableSet.copyOf(pBlockNode.getNodesInBlock());
    targetNode = pBlockNode.getLastNode();
    bNode = pBlockNode;
  }

  private boolean isTargetLoopHead(BlockState pBlockState) {
    return pBlockState.getLocationNode().equals(targetNode);
  }

  protected boolean shouldComputeSuccessor(BlockState pBlockState) {
    if (isTargetLoopHead(pBlockState)) {
      if (pBlockState.hasLoopHeadEncountered()) {
        return false;
      }
    }
    return true;
  }

  protected boolean hasLoopHeadEncountered(BlockState pBlockState) {
    if (isTargetLoopHead(pBlockState)) {
      return !pBlockState.hasLoopHeadEncountered();
    }
    return pBlockState.hasLoopHeadEncountered();
  }

  @Override
  public abstract Collection<BlockState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge);

  @Override
  public abstract Collection<BlockState> getAbstractSuccessors(
      AbstractState element, Precision prec) throws CPATransferException;

  static class ForwardBlockTransferRelation extends BlockTransferRelation {

    /**
     * This transfer relation produces successors iff an edge between two nodes exists in the CFA,
     * and it is part of the block
     */
    public ForwardBlockTransferRelation() {}

    private BlockStateType getType(CFANode pNode) {
      return pNode.equals(bNode.getLastNode()) ? BlockStateType.MID : BlockStateType.FINAL;
    }

    @Override
    public Collection<BlockState> getAbstractSuccessorsForEdge(
        AbstractState element, Precision prec, CFAEdge cfaEdge) {
      checkNotNull(
          edges, "init method must be called before starting the analysis (edges == null)");
      BlockState blockState = (BlockState) element;

      CFANode node = blockState.getLocationNode();
      if (Sets.intersection(ImmutableSet.copyOf(CFAUtils.allLeavingEdges(node)), edges)
          .contains(cfaEdge)) {
        if (!shouldComputeSuccessor(blockState)) {
          return ImmutableSet.of();
        }
        BlockState successor =
            new BlockState(
                cfaEdge.getSuccessor(),
                bNode,
                AnalysisDirection.FORWARD,
                getType(cfaEdge.getSuccessor()),
                hasLoopHeadEncountered(blockState));
        return ImmutableList.of(successor);
      }

      return ImmutableList.of();
    }

    @Override
    public Collection<BlockState> getAbstractSuccessors(AbstractState element, Precision prec)
        throws CPATransferException {
      checkNotNull(
          nodes, "init method must be called before starting the analysis (nodes == null)");
      BlockState blockState = (BlockState) element;

      if (!shouldComputeSuccessor(blockState)) {
        return ImmutableSet.of();
      }

      CFANode node = blockState.getLocationNode();
      return CFAUtils.successorsOf(node)
          .filter(n -> nodes.contains(n))
          .transform(
              n ->
                  new BlockState(
                      n,
                      bNode,
                      AnalysisDirection.FORWARD,
                      getType(n),
                      hasLoopHeadEncountered(blockState)))
          .toList();
    }
  }

  static class BackwardBlockTransferRelation extends BlockTransferRelation {

    @Override
    public void init(BlockNode pBlockNode) {
      super.init(pBlockNode);
      targetNode = pBlockNode.getStartNode();
    }

    private BlockStateType getType(CFANode pNode) {
      return pNode.equals(bNode.getStartNode()) ? BlockStateType.MID : BlockStateType.FINAL;
    }

    @Override
    public Collection<BlockState> getAbstractSuccessorsForEdge(
        AbstractState element, Precision prec, CFAEdge cfaEdge) {

      checkNotNull(
          edges, "init method must be called before starting the analysis (edges == null)");
      BlockState blockState = (BlockState) element;

      CFANode node = blockState.getLocationNode();
      if (Sets.intersection(ImmutableSet.copyOf(CFAUtils.allEnteringEdges(node)), edges)
          .contains(cfaEdge)) {
        if (!shouldComputeSuccessor(blockState)) {
          return ImmutableSet.of();
        }
        BlockState successor =
            new BlockState(
                cfaEdge.getPredecessor(),
                bNode,
                AnalysisDirection.BACKWARD,
                getType(cfaEdge.getPredecessor()),
                hasLoopHeadEncountered(blockState));
        return ImmutableList.of(successor);
      }

      return ImmutableSet.of();
    }

    @Override
    public Collection<BlockState> getAbstractSuccessors(AbstractState element, Precision prec)
        throws CPATransferException {

      checkNotNull(
          nodes, "init method must be called before starting the analysis (nodes == null)");
      BlockState blockState = (BlockState) element;

      if (!shouldComputeSuccessor(blockState)) {
        return ImmutableSet.of();
      }

      CFANode node = blockState.getLocationNode();
      FluentIterable<CFANode> predecessors = CFAUtils.predecessorsOf(node);
      return predecessors
          .filter(n -> nodes.contains(n))
          .transform(
              n ->
                  new BlockState(
                      n,
                      bNode,
                      AnalysisDirection.BACKWARD,
                      getType(n),
                      hasLoopHeadEncountered(blockState)))
          .toList();
    }
  }
}
