// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
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

@Options(prefix = "parser")
public class CPreprocessor extends Preprocessor {

  @Option(description="The command line for calling the preprocessor. " +
                      "May contain binary name and arguments, but won't be expanded by a shell. " +
                      "The source file name will be appended to this string. " +
                      "The preprocessor needs to print the output to stdout.")

  private String preprocessor = "cpp";

  @Option(
    name = "preprocessor.dumpResults",
    description = "Whether to dump the results of the preprocessor to disk for debugging."
  )
  private boolean dumpResults = false;

  @Option(
    name = "preprocessor.dumpDirectory",
    description = "Directory where to dump the results of the preprocessor."
  )
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path dumpDirectory = Paths.get("preprocessed");

  public CPreprocessor(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(pLogger);
    config.inject(this);
    if (dumpDirectory != null) {
      dumpDirectory = dumpDirectory.toAbsolutePath().normalize();
    }
  }

  @Override
  protected String getName() {
    return "preprocessor";
  }

  @Override
  protected String getCommandLine() {
    return preprocessor;
  }

  @Override
  protected boolean dumpResults() {
    return dumpResults;
  }

  @Override
  protected Path getDumpDirectory() {
    return dumpDirectory;
  }
}
