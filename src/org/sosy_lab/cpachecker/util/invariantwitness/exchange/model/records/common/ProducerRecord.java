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
public class ProducerRecord {
  @JsonProperty("name")
  private final String name;

  @JsonProperty("version")
  private final String version;

  @JsonProperty("configuration")
  private final String configuration;

  @JsonProperty("description")
  private final String description;

  @JsonProperty("command_line")
  private final String commandLine;

  public ProducerRecord(
      @JsonProperty("name") String pName,
      @JsonProperty("version") String pVersion,
      @JsonProperty("configuration") String pConfiguration,
      @JsonProperty("description") String pDescription,
      @JsonProperty("command_line") String pCommandLine) {
    name = pName;
    version = pVersion;
    configuration = pConfiguration;
    description = pDescription;
    commandLine = pCommandLine;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getConfiguration() {
    return configuration;
  }

  public String getDescription() {
    return description;
  }

  public String getCommandLine() {
    return commandLine;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ProducerRecord)) {
      return false;
    }
    ProducerRecord invariantStoreEntryProducer = (ProducerRecord) o;
    return Objects.equals(name, invariantStoreEntryProducer.name)
        && Objects.equals(version, invariantStoreEntryProducer.version)
        && Objects.equals(configuration, invariantStoreEntryProducer.configuration)
        && Objects.equals(description, invariantStoreEntryProducer.description)
        && Objects.equals(commandLine, invariantStoreEntryProducer.commandLine);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, configuration, description, commandLine);
  }

  @Override
  public String toString() {
    return "ProducerRecord{"
        + " name='"
        + getName()
        + "'"
        + ", version='"
        + getVersion()
        + "'"
        + ", configuration='"
        + getConfiguration()
        + "'"
        + ", description='"
        + getDescription()
        + "'"
        + ", commandLine='"
        + getCommandLine()
        + "'"
        + "}";
  }
}
