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

public class DssMessagePayloadTest {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void asLegacyMapReturnsCurrentShape() {
    DssMessagePayload payload =
        new DssMessagePayload(
            ImmutableMap.of(
                DssMessage.DSS_MESSAGE_HEADER_SENDER_ID_KEY,
                "B1",
                DssMessage.DSS_MESSAGE_HEADER_TYPE_KEY,
                "RESULT"
            ),
            ImmutableMap.of(
                DssResultMessage.DSS_MESSAGE_RESULT_KEY, "TRUE"
            )
        );
    assertThat(payload.asLegacyMap()).containsExactly(
        DssMessage.DSS_MESSAGE_HEADER_ID, payload.header(),
        DssMessage.DSS_MESSAGE_CONTENT_ID, payload.content()
    );
  }

  @Test
  public void fromJsonReadsCurrentJsonShape() throws Exception {
    Path file = tempFolder.newFile("message.json").toPath();
    String jsonTemplate = """
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
        """;
    String jsonContent = String.format(
        jsonTemplate,
        DssMessage.DSS_MESSAGE_HEADER_ID,
        DssMessage.DSS_MESSAGE_HEADER_SENDER_ID_KEY, "B1",
        DssMessage.DSS_MESSAGE_HEADER_TYPE_KEY, "RESULT",
        DssMessage.DSS_MESSAGE_HEADER_TIMESTAMP_KEY, "123",
        DssMessage.DSS_MESSAGE_HEADER_IDENTIFIER_KEY, "0",
        DssMessage.DSS_MESSAGE_CONTENT_ID,
        DssResultMessage.DSS_MESSAGE_RESULT_KEY, "TRUE"
    );
    Files.writeString(file, jsonContent);

    DssMessagePayload payload = DssMessagePayload.fromJson(file);

    assertThat(payload.header())
        .containsEntry(DssMessage.DSS_MESSAGE_HEADER_TYPE_KEY, "RESULT");
    assertThat(payload.content())
        .containsEntry(DssResultMessage.DSS_MESSAGE_RESULT_KEY, "TRUE");
  }

  @Test
  public void fromJsonRejectsMissingHeader() throws Exception {
    Path file = tempFolder.newFile("message.json").toPath();
    String jsonTemplate = """
        {
          "%s": {
            "%s": "%s"
          }
        }
        """;
    String jsonContent = String.format(
        jsonTemplate,
        DssMessage.DSS_MESSAGE_CONTENT_ID,
        DssResultMessage.DSS_MESSAGE_RESULT_KEY, "TRUE");
    Files.writeString(file, jsonContent);

    ValueInstantiationException exception = assertThrows(
        ValueInstantiationException.class, () -> DssMessagePayload.fromJson(file));
    assertThat(exception).hasMessageThat().contains("header");
  }
}
