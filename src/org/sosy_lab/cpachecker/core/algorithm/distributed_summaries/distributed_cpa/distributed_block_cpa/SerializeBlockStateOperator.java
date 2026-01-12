// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;

public class SerializeBlockStateOperator implements SerializeOperator {

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    if (!(pState instanceof BlockState b)) {
      throw new IllegalArgumentException(
          String.format("Expected state of type %s, got %s", BlockState.class, pState.getClass()));
    }
    String suffix = b.getHistory().isEmpty() ? "" : ", " + Joiner.on(", ").join(b.getHistory());
    return ContentBuilder.builder()
        .pushLevel(BlockState.class.getName())
        .put(STATE_KEY, b.getBlockNode().getId() + suffix)
        .build();
  }
}
