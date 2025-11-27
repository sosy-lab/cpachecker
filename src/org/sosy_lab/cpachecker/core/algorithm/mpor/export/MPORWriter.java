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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAchecker;

/** A class to write the sequentialized program to a file. */
public class MPORWriter {

  enum FileExtension {
    I(".i"),
    YML(".yml");

    private final String suffix;

    FileExtension(String pSuffix) {
      suffix = pSuffix;
    }

    private String getSuffix() {
      return suffix;
    }
  }

  private static final String PROGRAM_NOT_EXPORTED_MESSAGE =
      "Sequentialized program was not exported.";

  private static final String METADATA_NOT_EXPORTED_MESSAGE =
      "Sequentialization metadata was not exported.";

  public static void handleExport(
      PathTemplate pProgramPath,
      PathTemplate pMetadataPath,
      String pOutputProgram,
      List<Path> pInputFilePaths,
      LogManager pLogger) {

    // write output program, if the path is successfully determined
    Optional<Path> programPath = tryBuildExportPath(pProgramPath, FileExtension.I);
    if (programPath.isPresent()) {
      Path path = programPath.orElseThrow();
      try {
        try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {
          writer.write(pOutputProgram);
          pLogger.log(Level.INFO, "Sequentialized program exported to: ", path.toString());
        }
      } catch (IOException e) {
        pLogger.logUserException(
            Level.WARNING,
            e,
            "An IO error occurred while writing the output program. "
                + PROGRAM_NOT_EXPORTED_MESSAGE);
      }
    } else {
      pLogger.log(
          Level.WARNING,
          "Could not determine path for sequentialization. " + PROGRAM_NOT_EXPORTED_MESSAGE);
    }

    // write metadata, if the path is successfully determined
    Optional<Path> metadataPath = MPORWriter.tryBuildExportPath(pMetadataPath, FileExtension.YML);
    if (metadataPath.isPresent()) {
      Path path = metadataPath.orElseThrow();
      YAMLMapper yamlMapper = new YAMLMapper();
      MetadataRecord metadataRecord = buildMetadataRecord(pInputFilePaths);
      try {
        yamlMapper.writeValue(path.toFile(), metadataRecord);
        pLogger.log(Level.INFO, "Sequentialization metadata exported to: ", path.toString());
      } catch (IOException e) {
        pLogger.logUserException(
            Level.WARNING,
            e,
            "An error occurred while writing metadata. " + METADATA_NOT_EXPORTED_MESSAGE);
      }
    } else {
      pLogger.log(
          Level.WARNING,
          "Could not determine path for sequentialization metadata. "
              + METADATA_NOT_EXPORTED_MESSAGE);
    }
  }

  private record InputFileRecord(
      @JsonProperty("name") String pName, @JsonProperty("path") String pPath) {}

  private record MetadataRecord(
      @JsonProperty("cpachecker_version") String pCpaCheckerVersion,
      @JsonProperty("utc_creation_time") String pUtcCreationTime,
      @JsonProperty("input_files") List<InputFileRecord> pInputFiles) {}

  private static MetadataRecord buildMetadataRecord(List<Path> pInputFilePaths) {
    String utcCreationTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    ImmutableList<InputFileRecord> inputFiles =
        FluentIterable.from(pInputFilePaths)
            .transform(
                path ->
                    new InputFileRecord(
                        Objects.requireNonNull(path).getFileName().toString(), path.toString()))
            .toList();
    return new MetadataRecord(CPAchecker.getPlainVersion(), utcCreationTime, inputFiles);
  }

  private static Optional<Path> tryBuildExportPath(
      @Nullable PathTemplate pPathTemplate, FileExtension pFileExtension) {

    if (pPathTemplate == null) {
      return Optional.empty();
    }
    PathTemplate pathTemplate = Objects.requireNonNull(pPathTemplate);
    return Optional.of(Path.of(pathTemplate.getPath() + pFileExtension.getSuffix()));
  }
}
