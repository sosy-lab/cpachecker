// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;

class MetadataWriter {

  private record InputFileRecord(
      @JsonProperty("name") String pName, @JsonProperty("path") String pPath) {}

  private record MetadataRecord(
      @JsonProperty("cpachecker_version") String pCpaCheckerVersion,
      @JsonProperty("utc_creation_time") String pUtcCreationTime,
      @JsonProperty("input_files") List<InputFileRecord> pInputFiles) {}

  private record RootRecord(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("algorithm_options") Map<String, Object> pAlgorithmOptions) {}

  static void write(MPOROptions pOptions, String pMetadataPath, List<Path> pInputFilePaths)
      throws IOException, IllegalAccessException {

    YAMLMapper yamlMapper = new YAMLMapper();
    File metadataFile = new File(pMetadataPath);
    RootRecord yamlRoot = buildMetadataYamlRoot(pInputFilePaths, pOptions);
    yamlMapper.writeValue(metadataFile, yamlRoot);
  }

  private static RootRecord buildMetadataYamlRoot(List<Path> pInputFilePaths, MPOROptions pOptions)
      throws IllegalAccessException {

    MetadataRecord metadata = buildMetadataRecord(pInputFilePaths);
    ImmutableMap<String, Object> algorithmOptions = pOptions.buildAlgorithmOptionMap(pOptions);
    return new RootRecord(metadata, algorithmOptions);
  }

  private static MetadataRecord buildMetadataRecord(List<Path> pInputFilePaths) {
    String utcCreationTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    ImmutableList<InputFileRecord> inputFiles = buildInputFileRecords(pInputFilePaths);
    return new MetadataRecord(CPAchecker.getPlainVersion(), utcCreationTime, inputFiles);
  }

  private static ImmutableList<InputFileRecord> buildInputFileRecords(List<Path> pInputFilePaths) {
    ImmutableList.Builder<InputFileRecord> rInputFileRecords = ImmutableList.builder();
    for (Path path : pInputFilePaths) {
      rInputFileRecords.add(new InputFileRecord(path.getFileName().toString(), path.toString()));
    }
    return rInputFileRecords.build();
  }
}
