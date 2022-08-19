// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

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

  protected boolean shouldComputeSuccessor(BlockState pBlockState) {
    if (pBlockState.isTargetLoopHead()) {
      if (pBlockState.hasLoopHeadEncountered()) {
        return false;
      }
    }
    return true;
  }

  protected boolean hasLoopHeadEncountered(BlockState pBlockState) {
    if (pBlockState.isTargetLoopHead()) {
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

    private BlockStateType getType(BlockNode pBlockNode, CFANode pNode) {
      return pNode.equals(pBlockNode.getLastNode()) ? BlockStateType.FINAL : BlockStateType.MID;
    }

    @Override
    public Collection<BlockState> getAbstractSuccessorsForEdge(
        AbstractState element, Precision prec, CFAEdge cfaEdge) {
      BlockState blockState = (BlockState) element;

      CFANode node = blockState.getLocationNode();
      if (Sets.intersection(
              ImmutableSet.copyOf(CFAUtils.allLeavingEdges(node)),
              blockState.getBlockNode().getEdgesInBlock())
          .contains(cfaEdge)) {
        if (!shouldComputeSuccessor(blockState)) {
          return ImmutableSet.of();
        }
        BlockState successor =
            new BlockState(
                cfaEdge.getSuccessor(),
                blockState.getBlockNode(),
                AnalysisDirection.FORWARD,
                getType(blockState.getBlockNode(), cfaEdge.getSuccessor()),
                hasLoopHeadEncountered(blockState),
                blockState.getErrorCondition());
        return ImmutableList.of(successor);
      }

      return ImmutableList.of();
    }

    @Override
    public Collection<BlockState> getAbstractSuccessors(AbstractState element, Precision prec)
        throws CPATransferException {
      BlockState blockState = (BlockState) element;

      if (!shouldComputeSuccessor(blockState)) {
        return ImmutableSet.of();
      }

      CFANode node = blockState.getLocationNode();
      return CFAUtils.successorsOf(node)
          .filter(n -> blockState.getBlockNode().getNodesInBlock().contains(n))
          .transform(
              n ->
                  new BlockState(
                      n,
                      blockState.getBlockNode(),
                      AnalysisDirection.FORWARD,
                      getType(blockState.getBlockNode(), n),
                      hasLoopHeadEncountered(blockState),
                      blockState.getErrorCondition()))
          .toList();
    }
  }

  static class BackwardBlockTransferRelation extends BlockTransferRelation {

    private BlockStateType getType(BlockNode pBlockNode, CFANode pNode) {
      return pNode.equals(pBlockNode.getStartNode()) ? BlockStateType.FINAL : BlockStateType.MID;
    }

    @Override
    public Collection<BlockState> getAbstractSuccessorsForEdge(
        AbstractState element, Precision prec, CFAEdge cfaEdge) {
      BlockState blockState = (BlockState) element;

      CFANode node = blockState.getLocationNode();
      if (Sets.intersection(
              ImmutableSet.copyOf(CFAUtils.allEnteringEdges(node)),
              blockState.getBlockNode().getEdgesInBlock())
          .contains(cfaEdge)) {
        if (!shouldComputeSuccessor(blockState)) {
          return ImmutableSet.of();
        }
        BlockState successor =
            new BlockState(
                cfaEdge.getPredecessor(),
                blockState.getBlockNode(),
                AnalysisDirection.BACKWARD,
                getType(blockState.getBlockNode(), cfaEdge.getPredecessor()),
                hasLoopHeadEncountered(blockState),
                blockState.getErrorCondition());
        return ImmutableList.of(successor);
      }

      return ImmutableSet.of();
    }

    @Override
    public Collection<BlockState> getAbstractSuccessors(AbstractState element, Precision prec)
        throws CPATransferException {
      BlockState blockState = (BlockState) element;

      if (!shouldComputeSuccessor(blockState)) {
        return ImmutableSet.of();
      }

      CFANode node = blockState.getLocationNode();
      FluentIterable<CFANode> predecessors = CFAUtils.predecessorsOf(node);
      return predecessors
          .filter(n -> blockState.getBlockNode().getNodesInBlock().contains(n))
          .transform(
              n ->
                  new BlockState(
                      n,
                      blockState.getBlockNode(),
                      AnalysisDirection.BACKWARD,
                      getType(blockState.getBlockNode(), n),
                      hasLoopHeadEncountered(blockState),
                      blockState.getErrorCondition()))
          .toList();
    }
  }
}
