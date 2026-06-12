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
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.path.PathState;
import org.sosy_lab.cpachecker.cpa.path.PathTransferRelation;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix = "cpa.block.transfer")
public class BlockTransferRelation extends SingleEdgeTransferRelation {

  @Option(description = "whether to travel over the ghost edge", secure = true)
  private boolean standardVcs = true;

  private final PathTransferRelation pathTranferRelation = new PathTransferRelation();

  public BlockTransferRelation(Configuration pConfiguration) throws InvalidConfigurationException {
    pConfiguration.inject(this);
  }

  @Override
  public Collection<BlockState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision prec, CFAEdge cfaEdge) {
    BlockState blockState = (BlockState) element;
    CFANode node = blockState.getLocationNode();

    // block end cannot be reached directly before processing the first edge
    if (blockState.getType().equals(BlockStateType.INITIAL)
        && cfaEdge.getDescription().equals(BlockGraph.GHOST_EDGE_DESCRIPTION)) {
      return ImmutableSet.of();
    }

    if (blockState.getType() == BlockStateType.FINAL) {
      if (standardVcs) {
        if (!cfaEdge
            .getSuccessor()
            .equals(blockState.getBlockNode().getViolationConditionLocation())) {
          return ImmutableList.of();
        }
      } else {
        return FluentIterable.from(blockState.getViolationConditions())
            .transform(
                violationCondition ->
                    AbstractStates.extractStateByType(violationCondition, BlockState.class)
                        .getWitness())
            .transformAndConcat(w -> PathState.initialStates(w))
            .transformAndConcat(
                ps -> pathTranferRelation.getAbstractSuccessorsForEdge(ps, prec, cfaEdge))
            .transform(pnew -> copyBlockStateWithPath(cfaEdge, blockState, pnew))
            .toList();
      }
    }

    if (blockState.getType() == BlockStateType.FINAL
        && cfaEdge.getSuccessor().equals(blockState.getBlockNode().getViolationConditionLocation())
        && blockState.getViolationConditions().isEmpty()) {
      return ImmutableList.of();
    }

    if (blockState.getType() == BlockStateType.ABSTRACTION) {
      return ImmutableList.of();
    }

    if (blockState.getType() == BlockStateType.WITNESS) {
      return FluentIterable.from(
              pathTranferRelation.getAbstractSuccessorsForEdge(
                  blockState.getWitnessCheckPathState(), prec, cfaEdge))
          .transform(pnew -> copyBlockStateWithPath(cfaEdge, blockState, pnew))
          .toList();
    }

    Set<CFAEdge> intersection =
        Sets.intersection(node.getLeavingEdges().toSet(), blockState.getBlockNode().getEdges());

    if (intersection.contains(cfaEdge)) {
      if (!blockState.getViolationConditions().isEmpty()
          && cfaEdge
              .getSuccessor()
              .equals(blockState.getBlockNode().getViolationConditionLocation())) {
        ImmutableList.Builder<BlockState> successors = ImmutableList.builder();
        for (AbstractState vc : blockState.getViolationConditions()) {
          successors.add(
              new BlockState(
                  cfaEdge.getSuccessor(),
                  blockState.getBlockNode(),
                  getBlockStateTypeOfLocation(blockState.getBlockNode(), cfaEdge.getSuccessor()),
                  ImmutableList.of(vc),
                  blockState.getHistory(),
                  blockState.getWitness(),
                  blockState.hasNonTrivialSummaryForEachPredecessor()));
        }
        return successors.build();
      }
      return ImmutableList.of(
          new BlockState(
              cfaEdge.getSuccessor(),
              blockState.getBlockNode(),
              getBlockStateTypeOfLocation(blockState.getBlockNode(), cfaEdge.getSuccessor()),
              blockState.getViolationConditions(),
              blockState.getHistory(),
              blockState.getWitness(),
              blockState.hasNonTrivialSummaryForEachPredecessor()));
    }
    return ImmutableList.of();
  }

  private BlockState copyBlockStateWithPath(
      CFAEdge cfaEdge, BlockState blockState, PathState pnew) {
    return new BlockState(
        cfaEdge.getSuccessor(),
        blockState.getBlockNode(),
        BlockStateType.WITNESS,
        blockState.getViolationConditions(),
        blockState.getHistory(),
        blockState.getWitness(),
        blockState.hasNonTrivialSummaryForEachPredecessor(),
        pnew);
  }

  private static BlockStateType getBlockStateTypeOfLocation(BlockNode pBlockNode, CFANode pNode) {
    if (pNode.equals(pBlockNode.getFinalLocation())) {
      return BlockStateType.FINAL;
    }
    if (pNode.equals(pBlockNode.getViolationConditionLocation())) {
      return BlockStateType.ABSTRACTION;
    }
    return BlockStateType.MID;
  }
}
