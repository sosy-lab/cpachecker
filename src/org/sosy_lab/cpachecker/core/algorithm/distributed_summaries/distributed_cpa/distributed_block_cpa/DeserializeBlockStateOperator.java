// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

/**
 * Reverses the serialization performed by {@link SerializeBlockStateOperator}; see there for the
 * documentation of the wire format (the {@code W:} witness and {@code H:} history markers and the
 * omission of the history suffix when empty).
 */
public class DeserializeBlockStateOperator implements DeserializeOperator {

  private final BlockNode blockNode;

  public DeserializeBlockStateOperator(BlockNode pBlockNode) {
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage, int pStateIndex)
      throws InterruptedException {
    String content = pMessage.getAbstractStateContent(BlockState.class, pStateIndex).get(STATE_KEY);

    ParseResult parsed = parseWitness(content);
    Preconditions.checkNotNull(parsed.serializedBlockState);
    return new BlockState(
        null,
        DeserializeOperator.startLocationFromMessageType(pMessage, blockNode),
        blockNode,
        BlockStateType.INITIAL,
        ImmutableList.of(),
        parsed.history,
        parsed.witness);
  }

  public static ParseResult parseWitness(String content) {
    List<String> idAndWitnessAndMaybeHistory = Splitter.on(" W:").limit(2).splitToList(content);
    Preconditions.checkArgument(idAndWitnessAndMaybeHistory.size() == 2);
    String serializedBlockState = idAndWitnessAndMaybeHistory.getFirst();
    List<String> witnessAndMaybeHistory =
        Splitter.on(" H:").limit(2).splitToList(idAndWitnessAndMaybeHistory.getLast());
    SegmentedPaths finalWitness = SegmentedPaths.deserialize(witnessAndMaybeHistory.getFirst());
    List<String> history =
        witnessAndMaybeHistory.size() == 2
            ? Splitter.on(",").splitToList(witnessAndMaybeHistory.getLast())
            : ImmutableList.of();
    return new ParseResult(serializedBlockState, finalWitness, BlockGraphPath.of(history));
  }

  public record ParseResult(
      String serializedBlockState, SegmentedPaths witness, BlockGraphPath history) {}
}
