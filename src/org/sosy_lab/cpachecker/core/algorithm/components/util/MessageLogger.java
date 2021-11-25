// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.util;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;

public class MessageLogger {

  public enum Action {
    BACKWARD, FORWARD
  }

  public static final Path output = Path.of("./output/worker_log_" + System.currentTimeMillis() + "/");

  @FileOption(Type.OUTPUT_FILE)
  private final Path reportFile;
  private final String workerId;

  private final List<Map<String, Object>> entries;
  private final LogManager logger;
  private final List<String> predecessors;
  private final List<String> successors;
  private final String codeBlock;

  public MessageLogger(BlockNode pBlock, LogManager pLogger) throws IOException {
    workerId = pBlock.getId();
    reportFile = Path.of(output.toString(), workerId + ".json");
    IO.openOutputFile(reportFile, StandardCharsets.US_ASCII, StandardOpenOption.CREATE);
    entries = new ArrayList<>();
    logger = pLogger;
    predecessors = pBlock.getPredecessors().stream().map(BlockNode::getId).collect(
        ImmutableList.toImmutableList());
    successors = pBlock.getSuccessors().stream().map(BlockNode::getId).collect(
        ImmutableList.toImmutableList());
    codeBlock = pBlock.getCode();
  }

  public synchronized void log(Action action, String received, String result) {
    Map<String, Object> toJSON = new HashMap<>();
    toJSON.put("action", action.toString());
    toJSON.put("time", System.currentTimeMillis());
    toJSON.put("id", workerId);
    toJSON.put("currentMap", received);
    toJSON.put("result", result);
    toJSON.put("predecessors", predecessors);
    toJSON.put("successors", successors);
    toJSON.put("code", codeBlock);
    entries.add(toJSON);
    try {
      JSON.writeJSONString(entries, reportFile);
    } catch (IOException pE) {
      logger.log(Level.SEVERE, "Cannot log worker results", pE);
    }
  }

}
