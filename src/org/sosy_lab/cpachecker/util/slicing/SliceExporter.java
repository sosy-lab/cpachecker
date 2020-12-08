// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;


import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.cwriter.CFAToCTranslator;

@Options(prefix = "slicing")
public class SliceExporter {

  @Option(
      secure = true,
      name = "exportToC.enable",
      description = "Whether to export slices as C program files")
  private boolean exportToC = false;

  @Option(
      secure = true,
      name = "exportToC.file",
      description = "File template for exported C program slices")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportToCFile = PathTemplate.ofFormatString("programSlice.%d.c");

  @Option(
      secure = true,
      name = "exportCriteria.enable",
      description = "Export the used slicing criteria to file")
  private boolean exportCriteria = false;

  @Option(
      secure = true,
      name = "exportCriteria.file",
      description = "File template for export of used slicing criteria")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportCriteriaFile =
      PathTemplate.ofFormatString("programSlice.%d.criteria.txt");

  private final LogManager logger;
  private int exportCount = -1;
  private final CFAToCTranslator translator;

  public SliceExporter(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    translator = new CFAToCTranslator(pConfig);
    logger = pLogger;
  }

  /**
   * Executes the slice-exporter, which (depending on the configuration) exports program slices to C
   * program files.
   *
   * @param pSlice program slice to export
   */
  public void execute(Slice pSlice) {
    exportCount++;
    if (exportCriteria && exportCriteriaFile != null) {
      Concurrency.newThread(
              "Slice-criteria-Exporter",
              () -> {
                Path path = exportCriteriaFile.getPath(exportCount);

                try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {
                  StringBuilder output = new StringBuilder();
                  for (CFAEdge e : pSlice.getUsedCriteria()) {
                    FileLocation loc = e.getFileLocation();
                    output
                        .append(loc.getFileName())
                        .append(":")
                        .append(loc.getStartingLineNumber())
                        .append(":")
                        .append(e.getCode())
                        .append("\n");
                  }
                  writer.write(output.toString());

                } catch (IOException e) {
                  logger.logUserException(
                      Level.WARNING, e, "Could not write slicing criteria to file " + path);
                }
              })
          .start();
    }

    if (exportToC && exportToCFile != null) {
      Concurrency.newThread(
              "Slice-Exporter",
              () -> {
                CFA sliceCfa = SliceToCfaConverter.convert(pSlice);

                Path path = exportToCFile.getPath(exportCount);

                try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {

                  assert translator != null;
                  String code = translator.translateCfa(sliceCfa);
                  writer.write(code);

                } catch (CPAException | IOException | InvalidConfigurationException e) {
                  logger.logUserException(Level.WARNING, e, "Could not write CFA to C file.");
                }
              })
          .start();
    }
  }
}
