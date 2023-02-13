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
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockEndUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public abstract class BlockTransferRelation extends SingleEdgeTransferRelation {

  protected boolean shouldComputeSuccessor(BlockState pBlockState) {
    if (pBlockState.targetIsLastState()) {
      if (pBlockState.hasLoopHeadEncountered()) {
        return false;
      }
    }
    return true;
  }

  protected boolean hasLoopHeadEncountered(BlockState pBlockState) {
    if (pBlockState.targetIsLastState()) {
      return !pBlockState.hasLoopHeadEncountered();
    }
    return pBlockState.hasLoopHeadEncountered();
  }

  @Override
  public abstract Collection<BlockState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge);

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
      Set<CFAEdge> intersection =
          Sets.intersection(
              CFAUtils.allLeavingEdges(node).toSet(),
              blockState.getBlockNode().getEdgesInBlock());

      // last node was already seen
      if (!shouldComputeSuccessor(blockState)) {
        // edge is block end and block actually has a block end (1 last successor)
        if (!intersection.isEmpty()
            && cfaEdge.getDescription().equals(BlockEndUtil.UNIQUE_DESCRIPTION)) {
          BlockState successor =
              new BlockState(
                  cfaEdge.getSuccessor(),
                  blockState.getBlockNode(),
                  AnalysisDirection.FORWARD,
                  getType(blockState.getBlockNode(), cfaEdge.getSuccessor()),
                  true,
                  blockState.getErrorCondition());
          return ImmutableSet.of(successor);
        }
        // do not compute successor if last node was seen *and*
        return ImmutableSet.of();
      }
      if (cfaEdge.getDescription().equals(BlockEndUtil.UNIQUE_DESCRIPTION) && intersection.size() > 1) {
        return ImmutableSet.of();
      }
      if (intersection.contains(cfaEdge)
          || (cfaEdge instanceof CFunctionCallEdge callEdge
              && intersection.contains(callEdge.getSummaryEdge()))) {
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
      Set<CFAEdge> entering =
          Sets.intersection(
              CFAUtils.allEnteringEdges(node).toSet(),
              blockState.getBlockNode().getEdgesInBlock());
      if (entering.contains(cfaEdge)
          || (cfaEdge instanceof CFunctionCallEdge callEdge
              && entering.contains(callEdge.getSummaryEdge()))) {
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
  }
}
