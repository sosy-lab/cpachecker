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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BlockViolationConditionOperator implements ViolationConditionOperator {

  private final boolean trackHistory;

  BlockViolationConditionOperator(boolean pTrackHistory) {
    trackHistory = pTrackHistory;
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition) {
    BlockState topMost =
        Objects.requireNonNull(
            AbstractStates.extractStateByType(pARGPath.getFirstState(), BlockState.class));
    if (!trackHistory) {
      return Optional.of(topMost);
    }
    List<String> previousHistory =
        pPreviousCondition
            .map(
                state ->
                    Objects.requireNonNull(
                            AbstractStates.extractStateByType(state, BlockState.class))
                        .getHistory())
            .orElse(ImmutableList.of());
    BlockState withHistory =
        new BlockState(
            topMost.getLocationNode(),
            topMost.getBlockNode(),
            topMost.getType(),
            topMost.getViolationConditions(),
            listAndElement(previousHistory, topMost.getBlockNode().getId()));
    return Optional.of(withHistory);
  }
}
