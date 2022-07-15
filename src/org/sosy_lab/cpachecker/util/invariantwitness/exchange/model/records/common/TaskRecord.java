// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Immutable
public class TaskRecord {
  @JsonProperty("input_files")
  private final ImmutableList<String> inputFiles;

  @JsonProperty("input_file_hashes")
  private final ImmutableMap<String, String> inputFileHashes;

  @JsonProperty("specification")
  private final String specification;

  @JsonProperty("data_model")
  private final String dataModel;

  @JsonProperty("language")
  private final String language;

  public TaskRecord(
      @JsonProperty("input_files") List<String> pInputFiles,
      @JsonProperty("input_file_hashes") Map<String, String> pInputFileHashes,
      @JsonProperty("specification") String pSpecification,
      @JsonProperty("data_model") String pDataModel,
      @JsonProperty("language") String pLanguage) {
    inputFiles = ImmutableList.copyOf(pInputFiles);
    inputFileHashes = ImmutableMap.copyOf(pInputFileHashes);
    specification = pSpecification;
    dataModel = pDataModel;
    language = pLanguage;
  }

  public List<String> getInputFiles() {
    return inputFiles;
  }

  public Map<String, String> getInputFileHashes() {
    return inputFileHashes;
  }

  public String getSpecification() {
    return specification;
  }

  public String getDataModel() {
    return dataModel;
  }

  public String getLanguage() {
    return language;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof TaskRecord)) {
      return false;
    }
    TaskRecord invariantStoreEntryTask = (TaskRecord) o;
    return Objects.equals(inputFiles, invariantStoreEntryTask.inputFiles)
        && Objects.equals(inputFileHashes, invariantStoreEntryTask.inputFileHashes)
        && Objects.equals(specification, invariantStoreEntryTask.specification)
        && Objects.equals(dataModel, invariantStoreEntryTask.dataModel)
        && Objects.equals(language, invariantStoreEntryTask.language);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputFiles, inputFileHashes, specification, dataModel, language);
  }

  @Override
  public String toString() {
    return "TaskRecord{"
        + " inputFiles='"
        + getInputFiles()
        + "'"
        + ", inputFileHashes='"
        + getInputFileHashes()
        + "'"
        + ", specification='"
        + getSpecification()
        + "'"
        + ", dataModel='"
        + getDataModel()
        + "'"
        + ", language='"
        + getLanguage()
        + "'"
        + "}";
  }
}
