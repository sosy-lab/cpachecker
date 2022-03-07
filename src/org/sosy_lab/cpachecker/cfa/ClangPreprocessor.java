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

import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.llvm_j.binding.LLVMLibrary;

@Options(prefix = "parser")
public class ClangPreprocessor extends Preprocessor {

  @Option(
      description =
          "The command line for calling the clang preprocessor. "
              + "May contain binary name and arguments, but won't be expanded by a shell. "
              + "The source file name will be appended to this string. "
              + "Clang needs to print the output to stdout.")
  private String clang =
      "clang-" + extractVersionNumberFromLlvmJ() + " -S -emit-llvm -o /dev/stdout";

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
      throws CParserException, InterruptedException {
    checkNotNull(dumpDirectory, "Using the clang preprocessor requires a dump directory.");
    String result = preprocess0(file);
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
  protected boolean dumpResults() {
    return dumpResults;
  }

  @Override
  protected String getDumpFileOfFile(String file) {
    return file.replaceFirst("(\\.c|\\.i)$", ".ll");
  }

  private String extractVersionNumberFromLlvmJ() {
    return LLVMLibrary.JNA_LIBRARY_NAME.substring("LLVM-".length());
  }
}
