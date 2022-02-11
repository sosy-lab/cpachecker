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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
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
  private boolean first;

  public void init(BlockNode pBlockNode) {
    edges = validEdgesIn(pBlockNode);
    nodes = ImmutableSet.copyOf(pBlockNode.getNodesInBlock());
    targetNode = pBlockNode.getLastNode();
    first = false;
    bNode = pBlockNode;
  }

  private ImmutableSet<CFAEdge> validEdgesIn(BlockNode pBlockNode) {
    ImmutableSet.Builder<CFAEdge> setBuilder = ImmutableSet.builder();
    Set<CFANode> nodesInBlock = pBlockNode.getNodesInBlock();
    for(CFANode node: nodesInBlock) {
      for(CFAEdge edge: CFAUtils.allLeavingEdges(node)) {
        if (nodesInBlock.contains(edge.getSuccessor())) {
          setBuilder.add(edge);
        }
      }
    }
    return setBuilder.build();
  }

  protected boolean shouldComputeSuccessor(BlockState pBlockState) {
    boolean isTargetLoopHead = pBlockState.getLocationNode().equals(targetNode);
    if (isTargetLoopHead) {
      if (first) {
        return first = false;
      }
      return first = true;
    }
    return true;
  }

  @Override
  public abstract Collection<BlockState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge);

  @Override
  public abstract Collection<BlockState> getAbstractSuccessors(AbstractState element, Precision prec)
      throws CPATransferException;

  static class ForwardBlockTransferRelation extends BlockTransferRelation {

    /**
     * This transfer relation produces successors iff an edge between two nodes exists in the CFA
     * and it is part of the block
     */
    public ForwardBlockTransferRelation() {
    }

    private BlockStateType getType(CFANode pNode) {
      return pNode.equals(bNode.getLastNode()) ? BlockStateType.MID : BlockStateType.FINAL;
    }

    @Override
    public Collection<BlockState> getAbstractSuccessorsForEdge(
        AbstractState element, Precision prec, CFAEdge cfaEdge) {
      checkNotNull(edges, "init method must be called before starting the analysis (edges == null)");
      BlockState blockState = (BlockState) element;

      CFANode node = blockState.getLocationNode();
      if (Sets.intersection(ImmutableSet.copyOf(CFAUtils.allLeavingEdges(node)), edges).contains(cfaEdge)) {
        if (!shouldComputeSuccessor(blockState)) {
          return ImmutableSet.of();
        }
        BlockState successor = new BlockState(cfaEdge.getSuccessor(), targetNode, AnalysisDirection.FORWARD, getType(cfaEdge.getSuccessor()));
        return ImmutableList.of(successor);
      }

      return ImmutableList.of();
    }

    @Override
    public Collection<BlockState> getAbstractSuccessors(AbstractState element, Precision prec) throws CPATransferException {
      checkNotNull(nodes, "init method must be called before starting the analysis (nodes == null)");
      BlockState blockState = (BlockState) element;

      if (!shouldComputeSuccessor(blockState)) {
        return ImmutableSet.of();
      }

      CFANode node = blockState.getLocationNode();
      return CFAUtils.successorsOf(node).filter(n -> nodes.contains(n)).transform(n -> new BlockState(n, targetNode, AnalysisDirection.FORWARD, getType(n))).toList();
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

      checkNotNull(edges, "init method must be called before starting the analysis (edges == null)");
      BlockState blockState = (BlockState) element;

      CFANode node = blockState.getLocationNode();
      if (Sets.intersection(ImmutableSet.copyOf(CFAUtils.allEnteringEdges(node)), edges).contains(cfaEdge)) {
        if (!shouldComputeSuccessor(blockState)) {
          return ImmutableSet.of();
        }
        BlockState successor = new BlockState(cfaEdge.getPredecessor(), targetNode, AnalysisDirection.BACKWARD, getType(
            cfaEdge.getPredecessor()));
        return ImmutableList.of(successor);
      }

      return ImmutableSet.of();
    }

    @Override
    public Collection<BlockState> getAbstractSuccessors(AbstractState element, Precision prec) throws CPATransferException {

      checkNotNull(nodes, "init method must be called before starting the analysis (nodes == null)");
      BlockState blockState = (BlockState) element;

      if (!shouldComputeSuccessor(blockState)) {
        return ImmutableSet.of();
      }

      CFANode node = blockState.getLocationNode();
      FluentIterable<CFANode> predecessors = CFAUtils.predecessorsOf(node);
      return predecessors.filter(n -> nodes.contains(n)).transform(n -> new BlockState(n, targetNode, AnalysisDirection.BACKWARD, getType(n))).toList();
    }
  }

}
