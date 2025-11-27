// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.export;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;

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

  private static final String NOT_EXPORTED_MESSAGE = "Sequentialized program was not exported.";

  public static void handleExport(
      @Nullable PathTemplate pExportPath,
      String pOutputProgram,
      String pOutputProgramName,
      LogManager pLogger) {

    // write output program, if the path is successfully determined
    Optional<Path> programPath = buildOutputPath(pExportPath, pOutputProgramName, FileExtension.I);
    programPath.ifPresentOrElse(
        path -> {
          try {
            try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {
              writer.write(pOutputProgram);
              pLogger.log(Level.INFO, "Sequentialized program exported to: ", path.toString());
            }
          } catch (IOException e) {
            pLogger.logUserException(
                Level.WARNING,
                e,
                "An IO error occurred while writing the output program. " + NOT_EXPORTED_MESSAGE);
          }
        },
        () ->
            pLogger.log(
                Level.WARNING,
                "Could not determine path for sequentialization. " + NOT_EXPORTED_MESSAGE));
  }

  public static void handleExportWithMetadata(
      @Nullable PathTemplate pExportPath,
      String pOutputProgram,
      String pOutputProgramName,
      List<Path> pInputFilePaths,
      LogManager pLogger) {

    handleExport(pExportPath, pOutputProgram, pOutputProgramName, pLogger);
    MetadataWriter.write(pExportPath, pOutputProgramName, pInputFilePaths, pLogger);
  }

  static Optional<Path> buildOutputPath(
      @Nullable PathTemplate pPathTemplate, String pProgramName, FileExtension pFileExtension) {

    if (pPathTemplate == null) {
      return Optional.empty();
    }
    PathTemplate pathTemplate = Objects.requireNonNull(pPathTemplate);
    return Optional.of(Path.of(pathTemplate.getPath(pProgramName) + pFileExtension.getSuffix()));
  }
}
