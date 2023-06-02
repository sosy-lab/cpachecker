// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.GhostEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public abstract class BlockTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<BlockState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    BlockState blockState = (BlockState) element;
    CFANode node = blockState.getLocationNode();
    // block end cannot be reached directly after processing the first
    if (blockState.getType().equals(BlockStateType.INITIAL)) {
      if (cfaEdge instanceof GhostEdge) {
        return ImmutableSet.of();
      }
    }

    final BlockState successor = computeSuccessorFor(blockState, cfaEdge);

    // we are at the final location of the block and have already seen every
    // CFANode in the BlockNode once => transition only over the "BlockendEdge"
    if (node.equals(getBlockEnd(blockState.getBlockNode()))
        && !blockState.getType().equals(BlockStateType.INITIAL)) {
      if (cfaEdge instanceof GhostEdge) {
        return onTransitionToBlockEnd(successor);
      }
      return ImmutableSet.of();
    }

    Set<CFAEdge> intersection =
        Sets.intersection(computePossibleSuccessors(node), blockState.getBlockNode().getEdges());

    if (intersection.contains(cfaEdge)
        || (cfaEdge instanceof CFunctionCallEdge callEdge
            && intersection.contains(callEdge.getSummaryEdge()))) {
      return ImmutableList.of(successor);
    }

    return ImmutableList.of();
  }

  abstract Set<CFAEdge> computePossibleSuccessors(CFANode pNode);

  abstract BlockState computeSuccessorFor(BlockState pBlockState, CFAEdge pCFAEdge);

  abstract Collection<BlockState> onTransitionToBlockEnd(BlockState pPossibleSuccessor);

  abstract CFANode getBlockEnd(BlockNode pNode);

  private static BlockStateType getBlockStateTypeOfLocation(
      AnalysisDirection pDirection, BlockNode pBlockNode, CFANode pNode) {
    if (pNode.equals(
        pDirection == AnalysisDirection.FORWARD ? pBlockNode.getLast() : pBlockNode.getFirst())) {
      return BlockStateType.FINAL;
    }
    if (pNode.equals(pBlockNode.getAbstractionLocation())) {
      return BlockStateType.ABSTRACTION;
    }
    return BlockStateType.MID;
  }

  static class ForwardBlockTransferRelation extends BlockTransferRelation {

    /**
     * This transfer relation produces successors iff an edge between two nodes exists in the CFA,
     * and it is part of the block
     */
    public ForwardBlockTransferRelation() {}

    @Override
    Set<CFAEdge> computePossibleSuccessors(CFANode pNode) {
      return CFAUtils.leavingEdges(pNode).toSet();
    }

    @Override
    BlockState computeSuccessorFor(BlockState pBlockState, CFAEdge pCFAEdge) {
      return new BlockState(
          pCFAEdge.getSuccessor(),
          pBlockState.getBlockNode(),
          AnalysisDirection.FORWARD,
          getBlockStateTypeOfLocation(
              AnalysisDirection.FORWARD, pBlockState.getBlockNode(), pCFAEdge.getSuccessor()),
          pBlockState.getErrorCondition());
    }

    @Override
    Collection<BlockState> onTransitionToBlockEnd(BlockState pPossibleSuccessor) {
      return ImmutableSet.of(pPossibleSuccessor);
    }

    @Override
    CFANode getBlockEnd(BlockNode pNode) {
      return pNode.getLast();
    }
  }

  static class BackwardBlockTransferRelation extends BlockTransferRelation {

    @Override
    Set<CFAEdge> computePossibleSuccessors(CFANode pCFANode) {
      return CFAUtils.enteringEdges(pCFANode).toSet();
    }

    @Override
    BlockState computeSuccessorFor(BlockState pBlockState, CFAEdge pCFAEdge) {
      return new BlockState(
          pCFAEdge.getPredecessor(),
          pBlockState.getBlockNode(),
          AnalysisDirection.BACKWARD,
          getBlockStateTypeOfLocation(
              AnalysisDirection.BACKWARD, pBlockState.getBlockNode(), pCFAEdge.getPredecessor()),
          pBlockState.getErrorCondition());
    }

    @Override
    Collection<BlockState> onTransitionToBlockEnd(BlockState pPossibleSuccessor) {
      throw new AssertionError("Backward analysis does not support abstraction");
    }

    @Override
    CFANode getBlockEnd(BlockNode pNode) {
      return pNode.getFirst();
    }
  }
}
