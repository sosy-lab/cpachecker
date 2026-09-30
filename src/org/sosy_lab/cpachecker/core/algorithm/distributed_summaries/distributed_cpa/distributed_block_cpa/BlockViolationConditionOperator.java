// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.MergeableViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.pathrestriction.DecisionGraph;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Records the decisions taken on the way to the violation as the witness of the condition. The
 * witness only restricts which paths a predecessor explores, so the witnesses of two paths are
 * always merged.
 */
public class BlockViolationConditionOperator
    implements MergeableViolationConditionOperator<DecisionGraph> {

  private final boolean trackHistory;

  BlockViolationConditionOperator(boolean pTrackHistory) {
    trackHistory = pTrackHistory;
  }

  @Override
  public DecisionGraph initialCondition(ARGState pTarget, Optional<ARGState> pPreviousCondition) {
    return DecisionGraph.EMPTY;
  }

  @Override
  public Optional<DecisionGraph> prepend(DecisionGraph pCondition, List<CFAEdge> pEdges) {
    return Optional.of(pCondition.prepend(pEdges));
  }

  @Override
  public Optional<DecisionGraph> merge(DecisionGraph pFirst, DecisionGraph pSecond) {
    return Optional.of(DecisionGraph.union(ImmutableList.of(pFirst, pSecond)));
  }

  @Override
  public Optional<AbstractState> finish(
      ARGPath pPath, Optional<ARGState> pPreviousCondition, DecisionGraph pCondition) {
    SegmentedPaths previous = previousWitness(pPreviousCondition);
    // A single path keeps its compact representation as the list of its decisions.
    SegmentedPaths witness =
        pPath instanceof DssARGPathGraph
            ? previous.addGraphToFront(pCondition)
            : previous.addEdgesToFront(pPath.getFullPath());
    return Optional.of(finish(pPath.getFirstState(), pPreviousCondition, witness));
  }

  private SegmentedPaths previousWitness(Optional<ARGState> previous) {
    return previous
        .map(
            state ->
                Objects.requireNonNull(AbstractStates.extractStateByType(state, BlockState.class))
                    .getWitness())
        .orElse(SegmentedPaths.EMPTY);
  }

  private AbstractState finish(
      ARGState root, Optional<ARGState> pPreviousCondition, SegmentedPaths currentWitness) {
    BlockState topMost =
        Objects.requireNonNull(AbstractStates.extractStateByType(root, BlockState.class));

    if (!trackHistory) {
      return new BlockState(
          topMost.getUniqueId(),
          null,
          topMost.getLocationNode(),
          topMost.getBlockNode(),
          topMost.getType(),
          topMost.getViolationConditions(),
          topMost.getHistory(),
          currentWitness);
    }
    List<String> previousHistory =
        pPreviousCondition
            .map(
                state ->
                    AbstractStates.extractStateByType(state, BlockState.class).getHistory().path())
            .orElse(ImmutableList.of());
    return new BlockState(
        topMost.getUniqueId(),
        null,
        topMost.getLocationNode(),
        topMost.getBlockNode(),
        topMost.getType(),
        topMost.getViolationConditions(),
        BlockGraphPath.of(listAndElement(previousHistory, topMost.getBlockNode().getId())),
        currentWitness);
  }
}
