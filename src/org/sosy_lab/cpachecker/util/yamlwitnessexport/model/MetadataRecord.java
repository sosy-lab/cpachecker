// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.errorprone.annotations.Immutable;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.YAMLWitnessVersion;

@Immutable
@JsonPropertyOrder({"format_version", "uuid", "creation_time", "producer", "task"})
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

  public static MetadataRecord createMetadataRecord(
      ProducerRecord producerDescription, TaskRecord taskDescription, YAMLWitnessVersion pVersion) {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    String creationTime = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    return new MetadataRecord(
        pVersion.toString(),
        // To generate a unique UUID which is also deterministic and reproducible, we use the
        // input files and the Version to generate a UUID.
        MetadataRecord.getUUID(taskDescription.getInputFiles().toString() + pVersion).toString(),
        creationTime,
        producerDescription,
        taskDescription);
  }

  private static UUID getUUID(String pSeed) {
    return UUID.nameUUIDFromBytes(pSeed.getBytes(StandardCharsets.UTF_8));
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
    if (this == o) {
      return true;
    }
    return o instanceof MetadataRecord invariantStoreEntryMetadata
        && Objects.equals(formatVersion, invariantStoreEntryMetadata.formatVersion)
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
