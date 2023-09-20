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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<BlockState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    BlockState blockState = (BlockState) element;
    CFANode node = blockState.getLocationNode();

    // block end cannot be reached directly before processing the first edge
    if (blockState.getType().equals(BlockStateType.INITIAL) && cfaEdge instanceof GhostEdge) {
      return ImmutableSet.of();
    }

    final BlockState successor =
        new BlockState(
            cfaEdge.getSuccessor(),
            blockState.getBlockNode(),
            getBlockStateTypeOfLocation(blockState.getBlockNode(), cfaEdge.getSuccessor()),
            blockState.getErrorCondition());

    Set<CFAEdge> intersection =
        Sets.intersection(
            CFAUtils.leavingEdges(node).toSet(), blockState.getBlockNode().getEdges());

    if (intersection.contains(cfaEdge)) {
      return ImmutableList.of(successor);
    }

    return ImmutableList.of();
  }

  private static BlockStateType getBlockStateTypeOfLocation(BlockNode pBlockNode, CFANode pNode) {
    if (pNode.equals(pBlockNode.getLast())) {
      return BlockStateType.FINAL;
    }
    if (pNode.equals(pBlockNode.getAbstractionLocation())) {
      return BlockStateType.ABSTRACTION;
    }
    return BlockStateType.MID;
  }
}
