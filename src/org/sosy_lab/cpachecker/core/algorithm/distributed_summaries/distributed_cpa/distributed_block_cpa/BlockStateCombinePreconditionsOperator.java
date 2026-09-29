// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.EqualityCombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** Combines equal block states only when their path metadata can also be retained unchanged. */
final class BlockStateCombinePreconditionsOperator extends EqualityCombinePreconditionsOperator {

  BlockStateCombinePreconditionsOperator() {
    super(new BlockStateCoverageOperator(), BlockState.class);
  }

  @Override
  public Optional<AbstractState> combineIfPossible(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    BlockState first = (BlockState) Iterables.get(states, 0);
    for (AbstractState state : states) {
      BlockState block = (BlockState) state;
      if (!first.getHistory().equals(block.getHistory())
          || !first.getWitness().equals(block.getWitness())
          || !first.getViolationConditions().equals(block.getViolationConditions())) {
        return Optional.empty();
      }
    }
    return super.combineIfPossible(states);
  }
}
