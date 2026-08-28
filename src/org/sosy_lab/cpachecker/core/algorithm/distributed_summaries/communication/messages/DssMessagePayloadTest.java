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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;

public class DssMessagePayloadTest {
  private DssMessageFactory defaultFactory() throws InvalidConfigurationException {
    return new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
  }

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void asLegacyMapReturnsLegacyShape() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessageType.RESULT, "123", 0),
            new DssStatusPayload(true, true, true),
            ImmutableList.<ImmutableMap<String, String>>of(),
            ImmutableMap.of(DssMessageFormat.RESULT_KEY, "true"));
    ImmutableMap.Builder<String, String> legacyContent = ImmutableMap.builder();
    legacyContent.putAll(payload.status().asLegacyContent());
    legacyContent.putAll(payload.content());
    assertThat(payload.asLegacyMap())
        .containsExactly(
            DssMessageFormat.HEADER_KEY, payload.header().asLegacyHeader(),
            DssMessageFormat.CONTENT_KEY, legacyContent.buildKeepingLast());
  }

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
            DssMessageFormat.HEADER_KEY,
            DssMessageFormat.SENDER_ID_KEY,
            "B1",
            DssMessageFormat.HEADER_TYPE_KEY,
            DssMessageType.RESULT.name(),
            DssMessageFormat.HEADER_TIMESTAMP_KEY,
            "123",
            DssMessageFormat.HEADER_IDENTIFIER_KEY,
            "0",
            DssMessageFormat.CONTENT_KEY,
            DssMessageFormat.RESULT_KEY,
            "true");
    Files.writeString(file, jsonContent);

    DssMessagePayload payload = DssMessagePayload.fromJson(file);

    assertThat(payload.header().messageType()).isEqualTo(DssMessageType.RESULT);
    assertThat(payload.content()).containsEntry(DssMessageFormat.RESULT_KEY, "true");
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
            DssMessageFormat.HEADER_KEY,
            DssMessageFormat.SENDER_ID_KEY,
            "B1",
            DssMessageFormat.HEADER_TYPE_KEY,
            DssMessageType.RESULT.name(),
            DssMessageFormat.HEADER_TIMESTAMP_KEY,
            "123",
            DssMessageFormat.HEADER_IDENTIFIER_KEY,
            "0",
            DssMessageFormat.STATUS_KEY,
            DssMessageFormat.SOUND_KEY,
            "true",
            DssMessageFormat.PRECISE_KEY,
            "true",
            DssMessageFormat.PROPERTY_KEY,
            "true",
            DssMessageFormat.CONTENT_KEY,
            DssMessageFormat.RESULT_KEY,
            "true");
    Files.writeString(file, jsonContent);

    DssMessagePayload payload = DssMessagePayload.fromJson(file);

    assertThat(payload.header().messageType()).isEqualTo(DssMessageType.RESULT);
    assertThat(payload.status().toAlgorithmStatus()).isEqualTo(AlgorithmStatus.SOUND_AND_PRECISE);
    assertThat(payload.content()).containsEntry(DssMessageFormat.RESULT_KEY, "true");
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
            DssMessageFormat.CONTENT_KEY, DssMessageFormat.RESULT_KEY, "true");
    Files.writeString(file, jsonContent);

    ValueInstantiationException exception =
        assertThrows(ValueInstantiationException.class, () -> DssMessagePayload.fromJson(file));
    assertThat(exception).hasMessageThat().contains(DssMessageFormat.HEADER_KEY);
  }

  @Test
  public void fromPayloadUsesTopLevelStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessageType.POST_CONDITION, "123", 0),
            new DssStatusPayload(true, false, true),
            ImmutableList.of(ImmutableMap.of("dummy", "state")),
            ImmutableMap.of(DssMessageFormat.UNREACHABLE_BLOCK_END_KEY, "true"));
    DssMessage message = DssMessage.fromPayload(payload);

    assertThat(message.getAlgorithmStatus()).isEqualTo(AlgorithmStatus.SOUND_AND_IMPRECISE);
  }

  @Test
  public void asJsonPayloadMovesStatusOutOfContent() throws InvalidConfigurationException {
    DssMessage message =
        defaultFactory()
            .createDssUnreachableBlockEndMessage("B1", AlgorithmStatus.UNSOUND_AND_PRECISE);
    DssMessagePayload payload = message.asJsonPayload();

    assertThat(payload.status().toAlgorithmStatus()).isEqualTo(AlgorithmStatus.UNSOUND_AND_PRECISE);
    assertThat(payload.content())
        .doesNotContainKey(DssMessageFormat.STATUS_KEY + "." + DssMessageFormat.SOUND_KEY);
    assertThat(payload.content()).containsEntry(DssMessageFormat.UNREACHABLE_BLOCK_END_KEY, "true");
  }

  @Test
  public void writeJsonCreatesJsonFile() throws IOException {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessageType.RESULT, "123", 0),
            null,
            ImmutableList.<ImmutableMap<String, String>>of(),
            ImmutableMap.of(DssMessageFormat.RESULT_KEY, "true"));
    Path file = tempFolder.getRoot().toPath().resolve("missing/message.json");

    payload.writeJson(file);

    assertThat(Files.isRegularFile(file)).isTrue();
    assertThat(Files.readString(file)).contains(DssMessageFormat.HEADER_KEY);
  }

  @Test
  public void roundTripPreservesStatusAndContent()
      throws InvalidConfigurationException, IOException {
    DssMessage original =
        defaultFactory()
            .createDssUnreachableBlockEndMessage("B1", AlgorithmStatus.SOUND_AND_PRECISE);
    Path file = tempFolder.newFile("message.json").toPath();

    original.asJsonPayload().writeJson(file);
    DssMessage parsed = DssMessage.fromJson(file);

    assertThat(parsed.getType()).isEqualTo(original.getType());
    assertThat(parsed.getAlgorithmStatus()).isEqualTo(original.getAlgorithmStatus());
    assertThat(parsed.getSenderId()).isEqualTo(original.getSenderId());
    assertThat(parsed.indicatesUnreachableBlockEnd()).isTrue();
  }

  @Test
  public void fromPayloadRejectsPostConditionWithoutStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessageType.POST_CONDITION, "123", 0),
            null,
            ImmutableList.<ImmutableMap<String, String>>of(),
            ImmutableMap.of(DssMessageFormat.UNREACHABLE_BLOCK_END_KEY, "true"));

    assertThrows(IllegalArgumentException.class, () -> DssMessage.fromPayload(payload));
  }

  @Test
  public void fromPayloadRejectsResultWithStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessageType.RESULT, "123", 0),
            new DssStatusPayload(true, true, true),
            ImmutableList.<ImmutableMap<String, String>>of(),
            ImmutableMap.of(DssMessageFormat.RESULT_KEY, "true"));

    assertThrows(IllegalArgumentException.class, () -> DssMessage.fromPayload(payload));
  }

  @Test
  public void advancePreservesStatus() {
    DssMessagePayload payload =
        new DssMessagePayload(
            new DssHeaderPayload("B1", DssMessageType.POST_CONDITION, "123", 0),
            new DssStatusPayload(true, true, true),
            ImmutableList.of(ImmutableMap.of("dummy", "state")),
            ImmutableMap.of("state0.precision", "true"));

    DssMessage message = DssMessage.fromPayload(payload);
    DssMessage advanced = message.advance("state0");

    assertThat(advanced.getAlgorithmStatus()).isEqualTo(message.getAlgorithmStatus());
  }
}
