// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.core.algorithm.components.parallel.Message;

public class ActionLogger {

  public enum Action {
    RECEIVE, TAKE, FORWARD, BACKWARD, FINISH, DUMP, BROADCAST, ALREADY_ENQUEUED
  }

  @FileOption(Type.OUTPUT_FILE)
  private final Path reportFile;
  private final String workerId;

  private final List<Map<String, Object>> entries;

  public ActionLogger(String pWorkerId) throws IOException {
    workerId = pWorkerId;
    reportFile = Path.of("./output/worker_log/" + workerId + ".json");
    IO.openOutputFile(reportFile, StandardCharsets.US_ASCII, StandardOpenOption.CREATE);
    entries = new ArrayList<>();
  }

  public synchronized void log(Action action, Message message) throws IOException {
    Map<String, Object> toJSON = new HashMap<>();
    toJSON.put("action", action.toString());
    toJSON.put("time", System.currentTimeMillis());
    toJSON.put("id", workerId);
    toJSON.put("message", messageToMap(message));
    entries.add(toJSON);
    JSON.writeJSONString(entries, reportFile);
  }

  private synchronized Map<String, Object> messageToMap(Message pMessage) {
    Map<String, Object> toJSON = new HashMap<>();
    toJSON.put("sender", pMessage.getUniqueBlockId());
    toJSON.put("condition", pMessage.getPayload());
    toJSON.put("type", pMessage.getType().toString());
    toJSON.put("target", pMessage.getTargetNodeNumber());
    return toJSON;
  }

}
