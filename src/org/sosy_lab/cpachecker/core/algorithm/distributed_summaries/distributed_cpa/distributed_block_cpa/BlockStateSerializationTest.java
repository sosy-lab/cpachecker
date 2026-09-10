// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class BlockStateSerializationTest {
  private BlockState mockBlockState(
      BlockNode pBlockNode, SegmentedPaths pWitness, BlockGraphPath pHistory) {
    BlockState state = Mockito.mock(BlockState.class);
    Mockito.when(state.getBlockNode()).thenReturn(pBlockNode);
    Mockito.when(state.getWitness()).thenReturn(pWitness);
    Mockito.when(state.getHistory()).thenReturn(pHistory);

    return state;
  }

  @Test
  public void payloadParsesBlockStateWithoutHistory() {
    DssBlockStatePayload payload = DssBlockStatePayload.fromContentString("B1 W:N1N2");

    assertThat(payload.blockId()).isEqualTo("B1");
    assertThat(payload.witness()).isEqualTo("N1N2");
    assertThat(payload.history()).isEmpty();
  }

  @Test
  public void payloadParsesBlockStateWithHistory() {
    DssBlockStatePayload payload = DssBlockStatePayload.fromContentString("B1 W:N1N2 H:B0,B1");

    assertThat(payload.blockId()).isEqualTo("B1");
    assertThat(payload.witness()).isEqualTo("N1N2");
    assertThat(payload.history()).containsExactly("B0", "B1");
  }

  @Test
  public void contentMapRoundTrip() {
    DssBlockStatePayload original = new DssBlockStatePayload("B1", "N1N2", ImmutableList.of("B0"));

    DssBlockStatePayload parsed = DssBlockStatePayload.fromContentMap(original.asContentMap());

    assertThat(parsed).isEqualTo(original);
  }

  @Test
  public void payloadRejectsMissingWitnessMarker() {
    assertThrows(
        IllegalArgumentException.class, () -> DssBlockStatePayload.fromContentString("B1"));
  }

  @Test
  public void payloadRejectsEmptyContentMap() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DssBlockStatePayload.fromContentMap(ImmutableMap.of()));
  }

  @Test
  public void serializerSerializesBlockState() {
    BlockNode blockNode = Mockito.mock(BlockNode.class);
    Mockito.when(blockNode.getId()).thenReturn("B1");

    SegmentedPaths witness = SegmentedPaths.deserialize("N1N2");
    BlockGraphPath history = BlockGraphPath.of("B0", "B1");

    ImmutableMap<String, String> result =
        new SerializeBlockStateOperator().serialize(mockBlockState(blockNode, witness, history));

    assertThat(result)
        .containsEntry(
            DssComponentKey.BLOCK_STATE.getSerializedName() + "." + DssMessageKeys.STATE,
            "B1 W:N1N2 H:B0,B1");
  }

  @Test
  public void serializerSerializesBlockStateWithoutHistory() {
    BlockNode blockNode = Mockito.mock(BlockNode.class);
    Mockito.when(blockNode.getId()).thenReturn("B1");

    SegmentedPaths witness = SegmentedPaths.deserialize("N1N2");
    BlockGraphPath history = BlockGraphPath.of();

    ImmutableMap<String, String> result =
        new SerializeBlockStateOperator().serialize(mockBlockState(blockNode, witness, history));

    assertThat(result)
        .containsEntry(
            DssComponentKey.BLOCK_STATE.getSerializedName() + "." + DssMessageKeys.STATE,
            "B1 W:N1N2");
  }

  @Test
  public void serializerRejectsWrongStateType() {
    AbstractState state = Mockito.mock(AbstractState.class);
    SerializeBlockStateOperator operator = new SerializeBlockStateOperator();
    assertThrows(IllegalArgumentException.class, () -> operator.serialize(state));
  }

  @Test
  public void serializationRoundTrip() throws InterruptedException {
    BlockNode blockNode = Mockito.mock(BlockNode.class);
    Mockito.when(blockNode.getId()).thenReturn("B1");

    SegmentedPaths witness = SegmentedPaths.deserialize("N1N2");
    BlockGraphPath history = BlockGraphPath.of("B0", "B1");

    BlockState originalState = mockBlockState(blockNode, witness, history);
    ImmutableMap<String, String> serializedState =
        new SerializeBlockStateOperator().serialize(originalState);
    DssPostConditionMessage postConditionMessage =
        new DssPostConditionMessage(
            "B1", AlgorithmStatus.SOUND_AND_PRECISE, ImmutableList.of(serializedState));

    AbstractState deserializedState =
        new DeserializeBlockStateOperator(blockNode).deserialize(postConditionMessage, 0);

    assertThat(deserializedState).isInstanceOf(BlockState.class);
    BlockState deserializedBlockState = (BlockState) deserializedState;
    assertThat(deserializedBlockState.getBlockNode()).isEqualTo(blockNode);
    assertThat(deserializedBlockState.getWitness()).isEqualTo(witness);
    assertThat(deserializedBlockState.getHistory()).isEqualTo(history);
  }
}
