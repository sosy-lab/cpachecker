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
import org.sosy_lab.cpachecker.cfa.export.DOTBuilder;
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
      name = "exportToDot.enable",
      description = "Whether to export program slices as DOT files.")
  private boolean exportToDot = true;

  @Option(
      secure = true,
      name = "exportToDot.file",
      description = "File template for exported program slice DOT files.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportToDotFile = PathTemplate.ofFormatString("programSlice.%d.dot");

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

  private final Configuration config;
  private final LogManager logger;
  private int exportCount = 0;
  private final CFAToCTranslator translator;

  public SliceExporter(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    translator = new CFAToCTranslator(pConfig);
    config = pConfig;
    logger = pLogger;
  }

  private void exportToC(Slice pSlice, Path pPath) {

    CFA sliceCfa = SliceToCfaConversion.convert(config, logger, pSlice);

    try (Writer writer = IO.openOutputFile(pPath, Charset.defaultCharset())) {

      String code = translator.translateCfa(sliceCfa);
      writer.write(code);

    } catch (CPAException | IOException | InvalidConfigurationException ex) {
      logger.logUserException(
          Level.WARNING, ex, "Could not write program slice to C file: " + pPath);
    }
  }

  private void exportAsDotFile(Slice pSlice, Path pPath) {

    CFA sliceCfa = SliceToCfaConversion.convert(config, logger, pSlice);

    try (Writer writer = IO.openOutputFile(pPath, Charset.defaultCharset())) {
      DOTBuilder.generateDOT(writer, sliceCfa);
    } catch (IOException ex) {
      logger.logUserException(
          Level.WARNING, ex, "Could not write program slice to DOT file: " + pPath);
    }
  }

  private void exportCriteria(Slice pSlice, Path pPath) {

    try (Writer writer = IO.openOutputFile(pPath, Charset.defaultCharset())) {

      for (CFAEdge edge : pSlice.getSlicingCriteria()) {

        FileLocation fileLoc = edge.getFileLocation();
        writer.append(String.valueOf(fileLoc.getFileName()));
        writer.append(":");
        writer.append(String.valueOf(fileLoc.getStartingLineNumber()));
        writer.append(":");
        writer.append(edge.getCode());
        writer.append("\n");
      }

    } catch (IOException ex) {
      logger.logUserException(
          Level.WARNING, ex, "Could not write slicing criteria to file: " + pPath);
    }
  }

  private void export(Slice pSlice, int pCurrentExportCount) {

    if (exportToC && exportToCFile != null) {
      exportToC(pSlice, exportToCFile.getPath(pCurrentExportCount));
    }

    if (exportToDot && exportToDotFile != null) {
      exportAsDotFile(pSlice, exportToDotFile.getPath(pCurrentExportCount));
    }

    if (exportCriteria && exportCriteriaFile != null) {
      exportCriteria(pSlice, exportCriteriaFile.getPath(pCurrentExportCount));
    }
  }

  /**
   * Executes the slice-exporter, which exports, depending on the configuration, various parts of a
   * program {@link Slice} in different output formats.
   *
   * @param pSlice program slice to export
   */
  public void execute(Slice pSlice) {
    int currentExportCount = exportCount++;
    Concurrency.newThread("Slice-Exporter", () -> export(pSlice, currentExportCount)).start();
  }
}
