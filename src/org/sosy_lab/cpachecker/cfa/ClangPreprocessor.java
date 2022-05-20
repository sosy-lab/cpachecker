// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2014  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.parser.llvm.LlvmUtils;
import org.sosy_lab.cpachecker.exceptions.ClangParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

@Options(prefix = "parser")
public class ClangPreprocessor extends Preprocessor {

  @Option(
      description =
          "The command line for calling the clang preprocessor. "
              + "May contain binary name and arguments, but won't be expanded by a shell. "
              + "The source file name will be appended to this string. "
              + "Clang needs to print the output to stdout.")
  private String clang =
      "clang-" + LlvmUtils.extractVersionNumberFromLlvmJ() + " -S -emit-llvm -o /dev/stdout";

  @Option(
      name = "clang.dumpResults",
      description = "Whether to dump the results of the preprocessor to disk.")
  private boolean dumpResults = true;

  public ClangPreprocessor(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    super(config, pLogger);
    config.inject(this);
  }

  /**
   * Preprocess the given file and return the file to which the result has been dumped.
   *
   * @param file The file to preprocess.
   * @param dumpDirectory The required dump directory where the dump file will be written to.
   * @return The path denoting the dump file.
   */
  public @Nullable Path preprocessAndGetDumpedFile(Path file, Path dumpDirectory)
      throws ParserException, InterruptedException {
    checkNotNull(dumpDirectory, "Using the clang preprocessor requires a dump directory.");
    if (Files.getFileExtension(file.toString()).isEmpty()) {
      assumeLanguageC();
      logger.log(Level.FINE, "Assuming language C for preprocessing with clang");
    }
    String result = preprocess0(file);
    if (Strings.isNullOrEmpty(result)) {
      throw new ClangParserException("Clang could not preprocess the given file.");
    }
    return getAndWriteDumpFile(result, file, dumpDirectory);
  }

  @Override
  protected String getName() {
    return "clang";
  }

  @Override
  protected String getCommandLine() {
    return clang;
  }

  @Override
  protected ParserException createCorrespondingParserException(String pMsg) {
    return new ClangParserException(pMsg);
  }

  @Override
  protected ParserException createCorrespondingParserException(String pMsg, Throwable pCause) {
    return new ClangParserException(pMsg, pCause);
  }

  @Override
  protected boolean dumpResults() {
    return dumpResults;
  }

  @Override
  protected String getDumpFileOfFile(String file) {
    String llvmSuffix = ".ll";
    if (Files.getFileExtension(file).isEmpty()) {
      return file + llvmSuffix;
    }
    return file.replaceFirst("(\\.c|\\.i)$", llvmSuffix);
  }

  private void assumeLanguageC() {
    clang += " -x c";
  }
}
