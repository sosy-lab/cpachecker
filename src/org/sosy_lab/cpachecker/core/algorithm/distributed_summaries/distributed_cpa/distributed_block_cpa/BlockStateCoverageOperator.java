// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockStateCoverageOperator implements CoverageOperator {

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    BlockState blockState1 = (BlockState) state1;
    BlockState blockState2 = (BlockState) state2;
    return blockState1.equals(blockState2);
  }

  @Override
  public boolean isBasedOnEquality() {
    return true;
  }
}
