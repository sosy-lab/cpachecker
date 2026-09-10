// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.InvalidTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NamedFunctionTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.NullTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;

public class FunctionPointerSerializationTest {

  private static final String STATE_KEY =
      DssComponentKey.FUNCTION_POINTER_STATE.getSerializedName() + "." + DssMessageKeys.STATE;

  @Test
  public void payloadRoundTrip() {
    DssFunctionPointerStatePayload original =
        new DssFunctionPointerStatePayload(
            ImmutableList.of(
                new DssFunctionPointerEntry(
                    "invalid", DssFunctionPointerEntry.TargetKind.INVALID, null),
                new DssFunctionPointerEntry(
                    "named", DssFunctionPointerEntry.TargetKind.NAMED, "target"),
                new DssFunctionPointerEntry("null", DssFunctionPointerEntry.TargetKind.NULL, null),
                new DssFunctionPointerEntry(
                    "unknown", DssFunctionPointerEntry.TargetKind.UNKNOWN, null)));

    assertThat(DssFunctionPointerStatePayload.fromContentMap(original.asContentMap()))
        .isEqualTo(original);
  }

  @Test
  public void serializesStoredTargetKinds() {
    FunctionPointerState.Builder builder = FunctionPointerState.createEmptyState().createBuilder();
    builder.setTarget("a", InvalidTarget.getInstance());
    builder.setTarget("b", new NamedFunctionTarget("target"));
    builder.setTarget("c", NullTarget.getInstance());
    builder.setTarget("d", UnknownTarget.getInstance());

    assertThat(new SerializeFunctionPointerStateOperator().serialize(builder.build()))
        .containsExactly(STATE_KEY, "I:a, N:b:target, 0:c");
    // Note that it doesn't contain "U:d" since unknown targets are removed by the builder
  }

  @Test
  public void deserializesAllTargetKinds() throws InterruptedException {
    DssFunctionPointerStatePayload payload =
        new DssFunctionPointerStatePayload(
            ImmutableList.of(
                new DssFunctionPointerEntry("a", DssFunctionPointerEntry.TargetKind.INVALID, null),
                new DssFunctionPointerEntry(
                    "b", DssFunctionPointerEntry.TargetKind.NAMED, "target"),
                new DssFunctionPointerEntry("c", DssFunctionPointerEntry.TargetKind.NULL, null),
                new DssFunctionPointerEntry(
                    "d", DssFunctionPointerEntry.TargetKind.UNKNOWN, null)));
    DssPostConditionMessage message =
        new DssPostConditionMessage(
            "B1", AlgorithmStatus.SOUND_AND_PRECISE, ImmutableList.of(payload.asContentMap()));

    FunctionPointerState result =
        (FunctionPointerState)
            new DeserializeFunctionPointerStateOperator(null, null).deserialize(message, 0);

    assertThat(result.getTarget("a")).isEqualTo(InvalidTarget.getInstance());
    assertThat(result.getTarget("b")).isEqualTo(new NamedFunctionTarget("target"));
    assertThat(result.getTarget("c")).isEqualTo(NullTarget.getInstance());
    assertThat(result.getTarget("d")).isEqualTo(UnknownTarget.getInstance());
  }

  @Test
  public void stateSerializationRoundTrip() throws InterruptedException {
    FunctionPointerState.Builder builder = FunctionPointerState.createEmptyState().createBuilder();
    builder.setTarget("unknown", UnknownTarget.getInstance());
    builder.setTarget("a", InvalidTarget.getInstance());
    builder.setTarget("b", new NamedFunctionTarget("target"));
    builder.setTarget("c", NullTarget.getInstance());
    FunctionPointerState original = builder.build();

    ImmutableMap<String, String> serialized =
        new SerializeFunctionPointerStateOperator().serialize(original);
    DssPostConditionMessage message =
        new DssPostConditionMessage(
            "B1", AlgorithmStatus.SOUND_AND_PRECISE, ImmutableList.of(serialized));

    FunctionPointerState deserialized =
        (FunctionPointerState)
            new DeserializeFunctionPointerStateOperator(null, null).deserialize(message, 0);

    assertThat(deserialized.getTarget("unknown")).isEqualTo(UnknownTarget.getInstance());
    assertThat(deserialized.getTarget("a")).isEqualTo(original.getTarget("a"));
    assertThat(deserialized.getTarget("b")).isEqualTo(original.getTarget("b"));
    assertThat(deserialized.getTarget("c")).isEqualTo(original.getTarget("c"));
  }

  @Test
  public void emptyStateSerializesToEmptyPayload() {
    assertThat(
            new SerializeFunctionPointerStateOperator()
                .serialize(FunctionPointerState.createEmptyState()))
        .containsExactly(STATE_KEY, "");
  }
}
