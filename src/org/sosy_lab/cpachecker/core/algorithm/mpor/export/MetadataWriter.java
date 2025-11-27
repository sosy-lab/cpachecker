// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.algorithm.mpor.export.MPORWriter.FileExtension;

class MetadataWriter {

  private record InputFileRecord(
      @JsonProperty("name") String pName, @JsonProperty("path") String pPath) {}

  private record MetadataRecord(
      @JsonProperty("cpachecker_version") String pCpaCheckerVersion,
      @JsonProperty("utc_creation_time") String pUtcCreationTime,
      @JsonProperty("input_files") List<InputFileRecord> pInputFiles) {}

  private static final String NOT_EXPORTED_MESSAGE = "Sequentialization metadata was not exported.";

  static void write(
      @Nullable PathTemplate pPathTemplate,
      String pOutputProgramName,
      List<Path> pInputFilePaths,
      LogManager pLogger) {

    Optional<Path> metadataPath =
        MPORWriter.tryBuildExportPath(pPathTemplate, pOutputProgramName, FileExtension.YML);

    if (metadataPath.isPresent()) {
      Path path = metadataPath.orElseThrow();
      YAMLMapper yamlMapper = new YAMLMapper();
      MetadataRecord metadataRecord = buildMetadataRecord(pInputFilePaths);
      try {
        yamlMapper.writeValue(path.toFile(), metadataRecord);
        pLogger.log(Level.INFO, "Sequentialization metadata exported to: ", path.toString());
      } catch (IOException e) {
        pLogger.logUserException(
            Level.WARNING, e, "An error occurred while writing metadata. " + NOT_EXPORTED_MESSAGE);
      }
    } else {
      pLogger.log(
          Level.WARNING,
          "Could not determine path for sequentialization metadata. " + NOT_EXPORTED_MESSAGE);
    }
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
