// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.output;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;

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

  public static void write(
      MPOROptions pOptions, String pOutputProgram, List<Path> pInputFilePaths, LogManager pLogger) {

    // use first input file name as output program name
    String programName =
        SeqNameUtil.getFileNameWithoutExtension(pInputFilePaths.getFirst().getFileName());
    // write output program
    Path programPath = buildOutputPath(pOptions, programName, FileExtension.I);
    try {
      try (Writer writer = IO.openOutputFile(programPath, Charset.defaultCharset())) {
        writer.write(pOutputProgram);
        pLogger.log(Level.INFO, "Sequentialization created in: ", programPath.toString());
      }
    } catch (IOException e) {
      pLogger.logUserException(
          Level.WARNING,
          e,
          "An IO error occurred while writing the output program. Sequentialization was not"
              + " created.");
    }
    // if enabled: write metadata file
    MetadataWriter.tryWrite(pOptions, programName, pInputFilePaths, pLogger);
  }

  static Path buildOutputPath(
      MPOROptions pOptions, String pProgramName, FileExtension pFileExtension) {

    String templateWithExtension = pOptions.outputPath().getTemplate() + pFileExtension.getSuffix();
    return PathTemplate.ofFormatString(templateWithExtension).getPath(pProgramName);
  }
}
