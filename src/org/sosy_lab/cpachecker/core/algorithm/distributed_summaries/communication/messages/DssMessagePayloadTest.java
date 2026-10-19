// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DssCallstackEntryPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DssCallstackStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DssCompositeStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DssBlockStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DssFunctionPointerEntryPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DssFunctionPointerStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DssLocationStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;

public class DssMessagePayloadTest {
  private DssMessageFactory defaultFactory() throws InvalidConfigurationException {
    return new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
  }

  private DssMessage roundTrip(DssMessage pMessage) throws IOException {
    Path file = tempFolder.newFile("message.json").toPath();
    pMessage.asJsonPayload().writeJson(file);
    return DssMessageCodec.fromJson(file);
  }

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void fromJsonReadsCurrentJsonShape() throws Exception {
    Path file = tempFolder.newFile("message.json").toPath();
    String jsonContent =
        String.format(
            """
            {
              "%s": {
                "%s": "%s",
                "%s": "%s",
                "%s": "%s",
                "%s": "%s"
              },
              "%s": {
                "%s": "%s"
              }
            }
            """,
            DssMessageKeys.HEADER,
            DssMessageKeys.SENDER_ID,
            "B1",
            DssMessageKeys.MESSAGE_TYPE,
            DssMessage.DssMessageType.RESULT.name(),
            DssMessageKeys.TIMESTAMP,
            "123",
            DssMessageKeys.IDENTIFIER,
            "0",
            DssMessageKeys.CONTENT,
            DssMessageKeys.RESULT,
            "true");
    Files.writeString(file, jsonContent);

    DssMessagePayload payload = DssMessagePayload.fromJson(file);

    assertThat(payload.header().messageType()).isEqualTo(DssMessage.DssMessageType.RESULT);
    assertThat(payload.content()).containsEntry(DssMessageKeys.RESULT, "true");
  }

  @Test
  public void fromJsonRejectsMissingHeader() throws Exception {
    Path file = tempFolder.newFile("message.json").toPath();
    String jsonContent =
        String.format(
            """
            {
              "%s": {
                "%s": "%s"
              }
            }
            """,
            DssMessageKeys.CONTENT, DssMessageKeys.RESULT, "true");
    Files.writeString(file, jsonContent);

    ValueInstantiationException exception =
        assertThrows(ValueInstantiationException.class, () -> DssMessagePayload.fromJson(file));
    assertThat(exception).hasMessageThat().contains(DssMessageKeys.HEADER);
  }

  @Test
  public void fromPayloadCreatesPostConditionMessageWithStatus() {
    DssMessagePayload payload = getPayload();
    DssMessage message = DssMessageCodec.fromPayload(payload);

    assertThat(message).isInstanceOf(DssPostConditionMessage.class);
    assertThat(message).isInstanceOf(DssMessageWithStates.class);
    assertThat(message).isInstanceOf(DssMessageWithStatus.class);
    assertThat(message).isInstanceOf(DssConditionMessage.class);
    DssPostConditionMessage postConditionMessage = (DssPostConditionMessage) message;
    assertThat(postConditionMessage.getAlgorithmStatus())
        .isEqualTo(AlgorithmStatus.SOUND_AND_IMPRECISE);
  }

  private static @NonNull DssMessagePayload getPayload() {
    DssStatePayload statePayload = new DssLocationStatePayload(1);
    DssPrecisionPayload precisionPayload = DssNoPrecisionPayload.INSTANCE;
    DssStateAndPrecisionPayload stateAndPrecisionPayload =
        new DssStateAndPrecisionPayload(statePayload, precisionPayload);
    return new DssMessagePayload(
        new DssHeaderPayload("B1", DssMessage.DssMessageType.POST_CONDITION, "123", 0),
        new DssStatusPayload(true, false, true),
        ImmutableList.of(
            ImmutableMap.of(
                DssComponentKey.LOCATION_STATE.getSerializedName(), stateAndPrecisionPayload)),
        ImmutableMap.of(DssMessageKeys.UNREACHABLE_BLOCK_END, "true"));
  }

  @Test
  public void writeJsonCreatesJsonFile() throws IOException {
    DssMessagePayload payload = getPayload();
    Path file = tempFolder.getRoot().toPath().resolve("missing/message.json");

    payload.writeJson(file);

    assertThat(Files.isRegularFile(file)).isTrue();
    assertThat(Files.readString(file)).contains(DssMessageKeys.HEADER);
  }

  @Test
  public void postConditionRoundTripPreservesData()
      throws InvalidConfigurationException, IOException {
    DssPostConditionMessage original =
        defaultFactory()
            .createDssUnreachableBlockEndMessage("B1", AlgorithmStatus.SOUND_AND_PRECISE);
    DssMessage parsed = roundTrip(original);

    assertThat(parsed.getType()).isEqualTo(original.getType());
    assertThat(parsed.getClass()).isEqualTo(original.getClass());
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    assertThat(parsedPost.getAlgorithmStatus()).isEqualTo(original.getAlgorithmStatus());
    assertThat(parsedPost.getStates()).isEqualTo(original.getStates());
    assertThat(parsed.getSenderId()).isEqualTo(original.getSenderId());
    assertThat(parsedPost.indicatesUnreachableBlockEnd()).isTrue();
  }

  @Test
  public void callstackStateRoundTripPreservesPayload()
      throws InvalidConfigurationException, IOException {
    DssCallstackStatePayload originalCallstackPayload =
        new DssCallstackStatePayload(
            true,
            ImmutableList.of(
                new DssCallstackEntryPayload(12, "main"),
                new DssCallstackEntryPayload(27, "helper")));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    ImmutableMap.of(
                        DssComponentKey.CALLSTACK_STATE.getSerializedName(),
                        new DssStateAndPrecisionPayload(
                            originalCallstackPayload, DssNoPrecisionPayload.INSTANCE))));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    assertThat(parsedPost.getType()).isEqualTo(DssMessage.DssMessageType.POST_CONDITION);
    DssStateAndPrecisionPayload parsedStateAndPrecision =
        parsedPost.getStates().getFirst().get(DssComponentKey.CALLSTACK_STATE.getSerializedName());
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalCallstackPayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(DssNoPrecisionPayload.INSTANCE);
  }

  @Test
  public void compositeStateRoundTripPreservesLocationAndCallstackPayloads()
      throws InvalidConfigurationException, IOException {
    DssCallstackStatePayload callstackPayload =
        new DssCallstackStatePayload(
            true,
            ImmutableList.of(
                new DssCallstackEntryPayload(12, "main"),
                new DssCallstackEntryPayload(27, "helper")));
    DssCompositeStatePayload originalCompositePayload =
        new DssCompositeStatePayload(
            ImmutableMap.of(
                DssComponentKey.LOCATION_STATE.getSerializedName(),
                new DssLocationStatePayload(42),
                DssComponentKey.CALLSTACK_STATE.getSerializedName(),
                callstackPayload));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    ImmutableMap.of(
                        DssComponentKey.COMPOSITE_STATE.getSerializedName(),
                        new DssStateAndPrecisionPayload(
                            originalCompositePayload, DssNoPrecisionPayload.INSTANCE))));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    DssStateAndPrecisionPayload parsedStateAndPrecision =
        parsedPost.getStates().getFirst().get(DssComponentKey.COMPOSITE_STATE.getSerializedName());
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalCompositePayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(DssNoPrecisionPayload.INSTANCE);
  }

  @Test
  public void blockStateRoundTripPreservesPayload()
      throws InvalidConfigurationException, IOException {
    DssBlockStatePayload originalBlockPayload =
        new DssBlockStatePayload("block-1", "witness", ImmutableList.of("entry", "loop", "exit"));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    ImmutableMap.of(
                        DssComponentKey.BLOCK_STATE.getSerializedName(),
                        new DssStateAndPrecisionPayload(
                            originalBlockPayload, DssNoPrecisionPayload.INSTANCE))));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    DssStateAndPrecisionPayload parsedStateAndPrecision =
        parsedPost.getStates().getFirst().get(DssComponentKey.BLOCK_STATE.getSerializedName());
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalBlockPayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(DssNoPrecisionPayload.INSTANCE);
  }

  @Test
  public void functionPointerStateRoundTripPreservesAllEntryVariants()
      throws InvalidConfigurationException, IOException {
    DssFunctionPointerStatePayload originalFunctionPointerPayload =
        new DssFunctionPointerStatePayload(
            ImmutableList.of(
                new DssFunctionPointerEntryPayload(
                    "invalidPointer", DssFunctionPointerEntryPayload.TargetKind.INVALID, null),
                new DssFunctionPointerEntryPayload(
                    "nullPointer", DssFunctionPointerEntryPayload.TargetKind.NULL, null),
                new DssFunctionPointerEntryPayload(
                    "unknownPointer", DssFunctionPointerEntryPayload.TargetKind.UNKNOWN, null),
                new DssFunctionPointerEntryPayload(
                    "namedPointer", DssFunctionPointerEntryPayload.TargetKind.NAMED, "target")));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    ImmutableMap.of(
                        DssComponentKey.FUNCTION_POINTER_STATE.getSerializedName(),
                        new DssStateAndPrecisionPayload(
                            originalFunctionPointerPayload, DssNoPrecisionPayload.INSTANCE))));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    DssStateAndPrecisionPayload parsedStateAndPrecision =
        parsedPost
            .getStates()
            .getFirst()
            .get(DssComponentKey.FUNCTION_POINTER_STATE.getSerializedName());
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalFunctionPointerPayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(DssNoPrecisionPayload.INSTANCE);
  }

  @Test
  public void resultMessageRoundTripPreservesResult()
      throws InvalidConfigurationException, IOException {
    DssResultMessage original = defaultFactory().createDssResultMessage("B1", Result.TRUE);
    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssResultMessage.class);
    assertThat(((DssResultMessage) parsed).getResult()).isEqualTo(Result.TRUE);
  }

  @Test
  public void exceptionMessageRoundTripPreservesExceptionMessage()
      throws InvalidConfigurationException, IOException {
    String message = "Exception Message";
    DssExceptionMessage original =
        defaultFactory().createDssExceptionMessage("B1", new Exception(message));
    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssExceptionMessage.class);
    assertThat(((DssExceptionMessage) parsed).getExceptionMessage()).contains(message);
  }

  @Test
  public void violationConditionMessageRoundTripPreservesData()
      throws InvalidConfigurationException, IOException {
    DssViolationConditionMessage original =
        defaultFactory()
            .createViolationConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_IMPRECISE,
                ImmutableList.of(
                    ImmutableMap.of(
                        DssComponentKey.LOCATION_STATE.getSerializedName(),
                        new DssStateAndPrecisionPayload(
                            new DssLocationStatePayload(42), DssNoPrecisionPayload.INSTANCE))));
    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssViolationConditionMessage.class);
    DssViolationConditionMessage parsedViolation = (DssViolationConditionMessage) parsed;
    assertThat(parsedViolation.getStates()).isEqualTo(original.getStates());
    assertThat(parsedViolation.getAlgorithmStatus()).isEqualTo(original.getAlgorithmStatus());
  }

  @Test
  public void correctnessWitnessRoundTripPreservesData()
      throws InvalidConfigurationException, IOException {
    DssCorrectnessWitnessMessage original =
        defaultFactory()
            .createDssCorrectnessWitnessMessage(
                "B1",
                ImmutableList.of(
                    ImmutableMap.of(
                        DssComponentKey.LOCATION_STATE.getSerializedName(),
                        new DssStateAndPrecisionPayload(
                            new DssLocationStatePayload(42), DssNoPrecisionPayload.INSTANCE))));
    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssCorrectnessWitnessMessage.class);
    DssCorrectnessWitnessMessage parsedWitness = (DssCorrectnessWitnessMessage) parsed;
    assertThat(parsedWitness.getStates()).isEqualTo(original.getStates());
  }

  @Test
  public void fromPayloadRejectsPostConditionWithoutStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessage.DssMessageType.POST_CONDITION, "123", 0),
            null,
            ImmutableList.<ImmutableMap<String, DssStateAndPrecisionPayload>>of(),
            ImmutableMap.of(DssMessageKeys.UNREACHABLE_BLOCK_END, "true"));

    assertThrows(IllegalArgumentException.class, () -> DssMessageCodec.fromPayload(payload));
  }

  @Test
  public void fromPayloadRejectsResultWithStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessage.DssMessageType.RESULT, "123", 0),
            new DssStatusPayload(true, true, true),
            ImmutableList.<ImmutableMap<String, DssStateAndPrecisionPayload>>of(),
            ImmutableMap.of(DssMessageKeys.RESULT, "true"));

    assertThrows(IllegalArgumentException.class, () -> DssMessageCodec.fromPayload(payload));
  }
}
