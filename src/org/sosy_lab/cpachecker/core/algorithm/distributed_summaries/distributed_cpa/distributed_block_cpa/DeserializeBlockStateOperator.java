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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssStatePayload;
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
  public AbstractState deserialize(DssStatePayload pStatePayload, DssMessageType pMessageType)
      throws InterruptedException {

    DssBlockStatePayload payload = (DssBlockStatePayload) pStatePayload;

    ParseResult parsed = ParseResult.fromPayload(payload);
    Preconditions.checkNotNull(parsed.serializedBlockState);
    return new BlockState(
        parsed.id,
        null,
        DeserializeOperator.startLocationFromMessageType(pMessageType, blockNode),
        blockNode,
        BlockStateType.INITIAL,
        ImmutableList.of(),
        parsed.history,
        parsed.witness);
  }

  public static ParseResult parseWitness(String pContent) {
    return ParseResult.fromPayload(DssBlockStatePayload.fromContentString(pContent));
  }

  public record ParseResult(
      String id, String serializedBlockState, SegmentedPaths witness, BlockGraphPath history) {
    public static ParseResult fromPayload(DssBlockStatePayload pPayload) {
      return new ParseResult(
          pPayload.uid(),
          pPayload.blockId(),
          SegmentedPaths.deserialize(pPayload.witness()),
          BlockGraphPath.of(pPayload.history()));
    }
  }
}
