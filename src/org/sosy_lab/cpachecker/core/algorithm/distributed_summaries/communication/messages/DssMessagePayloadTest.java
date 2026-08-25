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
import com.google.common.collect.ImmutableMap;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

public class DssMessagePayloadTest {
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void asLegacyMapReturnsLegacyShape() {
    DssMessagePayload payload =
        new DssMessagePayload(
            ImmutableMap.of(
                DssMessageFormat.SENDER_ID_KEY, "B1", DssMessageFormat.HEADER_TYPE_KEY, "RESULT"),
            new DssStatusPayload(true, true, true),
            ImmutableMap.of(DssMessageFormat.RESULT_KEY, "TRUE"));
    ImmutableMap.Builder<String, String> legacyContent = ImmutableMap.builder();
    legacyContent.putAll(payload.status().asLegacyContent());
    legacyContent.putAll(payload.content());
    assertThat(payload.asLegacyMap())
        .containsExactly(
            DssMessageFormat.HEADER_KEY, payload.header(),
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
            "RESULT",
            DssMessageFormat.HEADER_TIMESTAMP_KEY,
            "123",
            DssMessageFormat.HEADER_IDENTIFIER_KEY,
            "0",
            DssMessageFormat.CONTENT_KEY,
            DssMessageFormat.RESULT_KEY,
            "TRUE");
    Files.writeString(file, jsonContent);

    DssMessagePayload payload = DssMessagePayload.fromJson(file);

    assertThat(payload.header()).containsEntry(DssMessageFormat.HEADER_TYPE_KEY, "RESULT");
    assertThat(payload.content()).containsEntry(DssMessageFormat.RESULT_KEY, "TRUE");
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
            "RESULT",
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

    assertThat(payload.header()).containsEntry(DssMessageFormat.HEADER_TYPE_KEY, "RESULT");
    assertThat(payload.status().toAlgorithmStatus() == AlgorithmStatus.SOUND_AND_PRECISE).isTrue();
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
            DssMessageFormat.CONTENT_KEY, DssMessageFormat.RESULT_KEY, "TRUE");
    Files.writeString(file, jsonContent);

    ValueInstantiationException exception =
        assertThrows(ValueInstantiationException.class, () -> DssMessagePayload.fromJson(file));
    assertThat(exception).hasMessageThat().contains("header");
  }
}
