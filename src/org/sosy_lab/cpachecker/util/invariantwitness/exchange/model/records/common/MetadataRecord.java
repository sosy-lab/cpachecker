// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Objects;

@Immutable
public class MetadataRecord {
  @JsonProperty("format_version")
  private final String formatVersion;

  @JsonProperty("uuid")
  private final String uuid;

  @JsonProperty("creation_time")
  private final String creationTime;

  @JsonProperty("producer")
  private final ProducerRecord producer;

  @JsonProperty("task")
  private final TaskRecord task;

  public MetadataRecord(
      @JsonProperty("format_version") String pFormatVersion,
      @JsonProperty("uuid") String pUuid,
      @JsonProperty("creation_time") String pCreationTime,
      @JsonProperty("producer") ProducerRecord pProducer,
      @JsonProperty("task") TaskRecord pTask) {

    formatVersion = pFormatVersion;
    uuid = pUuid;
    creationTime = pCreationTime;
    producer = pProducer;
    task = pTask;
  }

  public String getFormatVersion() {
    return formatVersion;
  }

  public String getUuid() {
    return uuid;
  }

  public String getCreationTime() {
    return creationTime;
  }

  public ProducerRecord getProducer() {
    return producer;
  }

  public TaskRecord getTask() {
    return task;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof MetadataRecord)) {
      return false;
    }
    MetadataRecord invariantStoreEntryMetadata = (MetadataRecord) o;
    return Objects.equals(formatVersion, invariantStoreEntryMetadata.formatVersion)
        && Objects.equals(uuid, invariantStoreEntryMetadata.uuid)
        && Objects.equals(creationTime, invariantStoreEntryMetadata.creationTime)
        && Objects.equals(producer, invariantStoreEntryMetadata.producer)
        && Objects.equals(task, invariantStoreEntryMetadata.task);
  }

  @Override
  public int hashCode() {
    return Objects.hash(formatVersion, uuid, creationTime, producer, task);
  }

  @Override
  public String toString() {
    return "MetadataRecord{"
        + " formatVersion='"
        + getFormatVersion()
        + "'"
        + ", uuid='"
        + getUuid()
        + "'"
        + ", creationTime='"
        + getCreationTime()
        + "'"
        + ", producer='"
        + getProducer()
        + "'"
        + ", task='"
        + getTask()
        + "'"
        + "}";
  }
}
