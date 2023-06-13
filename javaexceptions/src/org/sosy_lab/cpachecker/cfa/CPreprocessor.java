// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

@Options(prefix = "parser")
public class CPreprocessor extends Preprocessor {

  @Option(
      description =
          "The command line for calling the preprocessor. "
              + "May contain binary name and arguments, but won't be expanded by a shell. "
              + "The source file name will be appended to this string. "
              + "The preprocessor needs to print the output to stdout.")
  private String preprocessor = "cpp";

  @Option(
      name = "preprocessor.dumpResults",
      description = "Whether to dump the results of the preprocessor to disk for debugging.")
  private boolean dumpResults = false;

  public CPreprocessor(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    super(config, pLogger);
    config.inject(this);
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
  protected ParserException createCorrespondingParserException(String pMsg) {
    return new CParserException(pMsg);
  }

  @Override
  protected ParserException createCorrespondingParserException(String pMsg, Throwable pCause) {
    return new CParserException(pMsg, pCause);
  }

  @Override
  protected boolean dumpResults() {
    return dumpResults;
  }

  @Override
  protected String getDumpFileOfFile(String file) {
    return file;
  }
}
