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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;

public class DssMessagePayloadTest {
  private DssMessageFactory defaultFactory() throws InvalidConfigurationException {
    return new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
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
  public void fromJsonReadsCurrentJsonShapeWithStatus() throws Exception {
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
            DssMessageKeys.STATUS,
            DssMessageKeys.SOUND,
            "true",
            DssMessageKeys.PRECISE,
            "true",
            DssMessageKeys.PROPERTY,
            "true",
            DssMessageKeys.CONTENT,
            DssMessageKeys.RESULT,
            "true");
    Files.writeString(file, jsonContent);

    DssMessagePayload payload = DssMessagePayload.fromJson(file);

    assertThat(payload.header().messageType()).isEqualTo(DssMessage.DssMessageType.RESULT);
    assertThat(payload.status().toAlgorithmStatus()).isEqualTo(AlgorithmStatus.SOUND_AND_PRECISE);
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
  public void fromPayloadUsesTopLevelStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessage.DssMessageType.POST_CONDITION, "123", 0),
            new DssStatusPayload(true, false, true),
            ImmutableList.of(ImmutableMap.of("dummy", "state")),
            ImmutableMap.of(DssMessageKeys.UNREACHABLE_BLOCK_END, "true"));
    DssMessage message = DssMessageCodec.fromPayload(payload);

    assertThat(message.getClass()).isEqualTo(DssPostConditionMessage.class);
    DssPostConditionMessage postConditionMessage = (DssPostConditionMessage) message;
    assertThat(postConditionMessage.getAlgorithmStatus())
        .isEqualTo(AlgorithmStatus.SOUND_AND_IMPRECISE);
  }

  @Test
  public void asJsonPayloadMovesStatusOutOfContent() throws InvalidConfigurationException {
    DssMessage message =
        defaultFactory()
            .createDssUnreachableBlockEndMessage("B1", AlgorithmStatus.UNSOUND_AND_PRECISE);
    DssMessagePayload payload = message.asJsonPayload();

    assertThat(payload.status().toAlgorithmStatus()).isEqualTo(AlgorithmStatus.UNSOUND_AND_PRECISE);
    assertThat(payload.content())
        .doesNotContainKey(DssMessageKeys.STATUS + "." + DssMessageKeys.SOUND);
    assertThat(payload.content()).containsEntry(DssMessageKeys.UNREACHABLE_BLOCK_END, "true");
  }

  @Test
  public void writeJsonCreatesJsonFile() throws IOException {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessage.DssMessageType.RESULT, "123", 0),
            null,
            ImmutableList.<ImmutableMap<String, String>>of(),
            ImmutableMap.of(DssMessageKeys.RESULT, "true"));
    Path file = tempFolder.getRoot().toPath().resolve("missing/message.json");

    payload.writeJson(file);

    assertThat(Files.isRegularFile(file)).isTrue();
    assertThat(Files.readString(file)).contains(DssMessageKeys.HEADER);
  }

  @Test
  public void roundTripPreservesStatusAndContent()
      throws InvalidConfigurationException, IOException {
    DssPostConditionMessage original =
        defaultFactory()
            .createDssUnreachableBlockEndMessage("B1", AlgorithmStatus.SOUND_AND_PRECISE);
    Path file = tempFolder.newFile("message.json").toPath();

    original.asJsonPayload().writeJson(file);
    DssMessage parsed = DssMessageCodec.fromJson(file);

    assertThat(parsed.getType()).isEqualTo(original.getType());
    assertThat(parsed.getClass()).isEqualTo(original.getClass());
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    assertThat(parsedPost.getAlgorithmStatus()).isEqualTo(original.getAlgorithmStatus());
    assertThat(parsed.getSenderId()).isEqualTo(original.getSenderId());
    assertThat(parsedPost.indicatesUnreachableBlockEnd()).isTrue();
  }

  @Test
  public void fromPayloadRejectsPostConditionWithoutStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessage.DssMessageType.POST_CONDITION, "123", 0),
            null,
            ImmutableList.<ImmutableMap<String, String>>of(),
            ImmutableMap.of(DssMessageKeys.UNREACHABLE_BLOCK_END, "true"));

    assertThrows(IllegalArgumentException.class, () -> DssMessageCodec.fromPayload(payload));
  }

  @Test
  public void fromPayloadRejectsResultWithStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessage.DssMessageType.RESULT, "123", 0),
            new DssStatusPayload(true, true, true),
            ImmutableList.<ImmutableMap<String, String>>of(),
            ImmutableMap.of(DssMessageKeys.RESULT, "true"));

    assertThrows(IllegalArgumentException.class, () -> DssMessageCodec.fromPayload(payload));
  }
}
