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
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {
  @JsonProperty("input_files")
  private final List<String> inputFiles;

  @JsonProperty("input_file_hashes")
  private final Map<String, String> inputFileHashes;

  private final String specification;
  private final String language;

  public Task(
      @JsonProperty("input_files") List<String> pInputFiles,
      @JsonProperty("input_file_hashes") Map<String, String> pInputFileHashes,
      @JsonProperty("specification") String pSpecification,
      @JsonProperty("language") String pLanguage) {
    inputFiles = pInputFiles;
    inputFileHashes = pInputFileHashes;
    specification = pSpecification;
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

  public String getLanguage() {
    return language;
  }
}
