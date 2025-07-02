// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;

public class SerializeBlockStateOperator implements SerializeOperator {

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    Preconditions.checkArgument(pState instanceof BlockState);
    return ContentBuilder.builder()
        .pushLevel(BlockState.class.getName())
        .put(STATE_KEY, ((BlockState) pState).getBlockNode().getId())
        .build();
  }
}
