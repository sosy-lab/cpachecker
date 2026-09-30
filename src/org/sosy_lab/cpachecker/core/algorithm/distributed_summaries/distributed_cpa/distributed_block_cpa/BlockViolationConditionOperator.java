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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.pathrestriction.DecisionGraph;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BlockViolationConditionOperator implements ViolationConditionOperator {

  private final boolean trackHistory;

  BlockViolationConditionOperator(boolean pTrackHistory) {
    trackHistory = pTrackHistory;
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition) {
    return finish(
        pARGPath.getFirstState(),
        pPreviousCondition,
        previousWitness(pPreviousCondition).prepend(pARGPath.getFullPath()));
  }

  public Optional<AbstractState> withGraph(
      ARGState root, Optional<ARGState> previous, DecisionGraph graph) {
    return finish(root, previous, graph.then(previousWitness(previous)));
  }

  private DecisionGraph previousWitness(Optional<ARGState> previous) {
    return previous
        .map(
            state ->
                Objects.requireNonNull(AbstractStates.extractStateByType(state, BlockState.class))
                    .getWitness())
        .orElse(DecisionGraph.EMPTY);
  }

  private Optional<AbstractState> finish(
      ARGState root, Optional<ARGState> pPreviousCondition, DecisionGraph currentWitness) {
    BlockState topMost =
        Objects.requireNonNull(AbstractStates.extractStateByType(root, BlockState.class));

    if (!trackHistory) {
      return Optional.of(
          new BlockState(
              topMost.getUniqueId(),
              null,
              topMost.getLocationNode(),
              topMost.getBlockNode(),
              topMost.getType(),
              topMost.getViolationConditions(),
              topMost.getHistory(),
              currentWitness));
    }
    List<String> previousHistory =
        pPreviousCondition
            .map(
                state ->
                    AbstractStates.extractStateByType(state, BlockState.class).getHistory().path())
            .orElse(ImmutableList.of());
    BlockState withHistory =
        new BlockState(
            topMost.getUniqueId(),
            null,
            topMost.getLocationNode(),
            topMost.getBlockNode(),
            topMost.getType(),
            topMost.getViolationConditions(),
            BlockGraphPath.of(listAndElement(previousHistory, topMost.getBlockNode().getId())),
            currentWitness);
    return Optional.of(withHistory);
  }
}
