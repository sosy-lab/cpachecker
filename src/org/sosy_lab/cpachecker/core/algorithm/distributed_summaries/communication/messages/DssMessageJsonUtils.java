// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;

public final class DssMessageJsonUtils {
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new DssStatePayloadModule())
          .registerModule(new DssMessagePayloadModule());

  public static DssMessagePayload read(Path pPath) throws IOException {
    return OBJECT_MAPPER.readValue(pPath.toFile(), DssMessagePayload.class);
  }

  public static void write(DssMessagePayload pPayload, Path pPath) throws IOException {
    OBJECT_MAPPER.writeValue(pPath.toFile(), pPayload);
  }
}
