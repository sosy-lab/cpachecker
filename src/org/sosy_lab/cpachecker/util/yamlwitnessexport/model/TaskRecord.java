// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;

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

  public static TaskRecord getTaskDescription(CFA pCFA, Specification pSpecification)
      throws IOException {
    List<Path> inputFiles = pCFA.getFileNames();
    ImmutableMap.Builder<String, String> inputFileHashes = ImmutableMap.builder();
    for (Path inputFile : inputFiles) {
      inputFileHashes.put(inputFile.toString(), AutomatonGraphmlCommon.computeHash(inputFile));
    }

    String specification =
        pSpecification.getProperties().stream()
            .map(Property::toString)
            .collect(Collectors.joining(" && "));

    return new TaskRecord(
        Collections3.transformedImmutableListCopy(inputFiles, Path::toString),
        inputFileHashes.buildOrThrow(),
        specification,
        pCFA.getMachineModel().getMachineModelForYAMLWitnessSpecification(),
        pCFA.getLanguage().toString());
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
    if (this == o) {
      return true;
    }
    return o instanceof TaskRecord invariantStoreEntryTask
        && Objects.equals(inputFiles, invariantStoreEntryTask.inputFiles)
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