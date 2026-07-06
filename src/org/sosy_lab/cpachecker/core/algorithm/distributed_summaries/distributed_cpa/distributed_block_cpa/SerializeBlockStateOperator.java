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
import java.util.ArrayList;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;

/**
 * Serializes a {@link BlockState} into the wire format shared with {@link
 * DeserializeBlockStateOperator}.
 *
 * <p>The serialized state is stored as a single string under the {@code STATE_KEY} entry of the
 * content map. Its format is:
 *
 * <pre>{@code
 * <blockNodeId> W:<witness> [ H:<history>]
 * }</pre>
 *
 * <ul>
 *   <li>{@code <blockNodeId>} is the id of the block node ({@link
 *       org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode#getId()}).
 *   <li>The {@code W:} marker (preceded by a single space) is followed by the witness elements
 *       joined by {@code ','}.
 *   <li>The {@code H:} marker (preceded by a single space) is followed by the history elements
 *       joined by {@code ','}. This suffix is omitted entirely when the history is empty.
 * </ul>
 *
 * <p>Concrete examples:
 *
 * <pre>{@code
 * "B1 W:w0,w1 H:h0,h1"   // block "B1", witness [w0, w1], history [h0, h1]
 * "B1 W:w0,w1"           // block "B1", witness [w0, w1], empty history (H: suffix omitted)
 * }</pre>
 *
 * <p>The exact reverse parsing is implemented by {@link DeserializeBlockStateOperator}.
 */
public class SerializeBlockStateOperator implements SerializeOperator {

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    if (!(pState instanceof BlockState b)) {
      throw new IllegalArgumentException(
          String.format("Expected state of type %s, got %s", BlockState.class, pState.getClass()));
    }
    String suffix = " W:" + b.getWitness().serialize();
    suffix = suffix + (b.getHistory().isEmpty() ? "" : " H:" + Joiner.on(",").join(b.getHistory()));
    return ContentBuilder.builder()
        .pushLevel(BlockState.class.getName())
        .put(STATE_KEY, b.getBlockNode().getId() + suffix)
        .build();
  }
}
