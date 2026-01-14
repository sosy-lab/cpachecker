// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DssMetadata {
  @JsonProperty("format_version")
  private final String formatVersion;

  @JsonProperty("creation_time")
  private final String creationTime;

  private final Producer producer;
  private final Task task;

  DssMetadata(
      @JsonProperty("format_version") String pFormatVersion,
      @JsonProperty("creation_time") String pCreationTime,
      Producer pProducer,
      Task pTask) {
    formatVersion = pFormatVersion;
    creationTime = pCreationTime;
    producer = pProducer;
    task = pTask;
  }

  public String getFormatVersion() {
    return formatVersion;
  }

  public String getCreationTime() {
    return creationTime;
  }

  public Producer getProducer() {
    return producer;
  }

  public Task getTask() {
    return task;
  }
}
