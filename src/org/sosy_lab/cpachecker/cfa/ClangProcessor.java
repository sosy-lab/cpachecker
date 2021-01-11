// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2014  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CParserException;

@Options(prefix = "parser")
public class ClangProcessor extends Preprocessor {

  @Option(description="The command line for calling the clang processor. " +
                      "May contain binary name and arguments, but won't be expanded by a shell. " +
                      "The source file name will be appended to this string. " +
                      "The clang needs to print the output to stdout.")

  private String clang = "clang -S -emit-llvm -o /dev/stdout";

  @Option(
    name = "clang.dumpResults",
    description = "Whether to dump the results of the processor to disk for debugging."
  )
  private boolean dumpResults = true;

  @Option(
      name = "clang.dumpDirectory",
      description = "Directory where to dump the results of the processor.")

  /*
   * The class writes its output files in the processed directory.
   */
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path dumpDirectory = Paths.get("processed");

  public ClangProcessor(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(pLogger);
    config.inject(this);
    if (dumpDirectory != null) {
      dumpDirectory = dumpDirectory.toAbsolutePath().normalize();
    }
  }

  public Path preprocessAndGetDumpedFile(String file) throws CParserException, InterruptedException {
    String result = preprocess0(file);
    return getAndWriteDumpedFile(result, file);
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
  protected Path getDumpDirectory() {
    return dumpDirectory;
  }

  @Override
  protected String getDumpFileOfFile(String file) {
    return file.replaceAll("\\.c", ".ll");
  }
}
