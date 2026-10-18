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
