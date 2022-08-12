// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CombineBlockStateOperator implements CombineOperator {

  @Override
  public List<AbstractState> combine(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    BlockState state1 = (BlockState) pState1;
    BlockState state2 = (BlockState) pState2;
    Preconditions.checkArgument(state1.equals(state2), "Cannot combine different BlockStates: " + state1 + " and " + state2);
    return ImmutableList.of(state2);
  }
}
