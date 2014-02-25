/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.exceptions.CParserException;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@Options(prefix="parser")
public class CPreprocessor {

  @Option(description="The command line for calling the preprocessor. " +
                      "May contain binary name and arguments, but won't be expanded by a shell. " +
                      "The source file name will be appended to this string. " +
                      "The preprocessor needs to print the output to stdout.")

  private String preprocessor = "cpp";

  private final LogManager logger;

  public CPreprocessor(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
  }

  public char[] preprocess(String file) throws CParserException, InterruptedException {
    // create command line
    List<String> argList = Lists.newArrayList(Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().split(preprocessor));
    argList.add(file);
    String[] args = argList.toArray(new String[argList.size()]);

    logger.log(Level.FINE, "Running preprocessor", argList);
    try {
      CPreprocessorExecutor executor = new CPreprocessorExecutor(logger, args);
      executor.sendEOF();
      int exitCode = executor.join();
      logger.log(Level.FINE, "Preprocessor finished");

      if (exitCode != 0) {
        throw new CParserException("Preprocessor failed with exit code " + exitCode);
      }

      if (executor.errorOutputCount > 0) {
        logger.log(Level.WARNING, "Preprocessor returned successfully, but printed warnings. Please check the log above!");
      }

      if (executor.buffer == null) {
        return "".toCharArray();
      }
      return executor.buffer.toString().toCharArray();

    } catch (IOException e) {
      throw new CParserException("Preprocessor failed", e);
    }
  }


  private static class CPreprocessorExecutor extends ProcessExecutor<IOException> {

    private static final int MAX_ERROR_OUTPUT_SHOWN = 10;
    private static final Map<String, String> ENV_VARS = ImmutableMap.of("LANG", "C");

    private volatile int errorOutputCount = 0;
    private volatile StringBuffer buffer;

    public CPreprocessorExecutor(LogManager logger, String[] args) throws IOException {
      super(logger, IOException.class, ENV_VARS, args);
    }

    @Override
    protected void handleErrorOutput(String pLine) throws IOException {
      if (errorOutputCount == MAX_ERROR_OUTPUT_SHOWN) {
        logger.log(Level.WARNING, "Skipping further preprocessor error output...");
        errorOutputCount++;

      } else if (errorOutputCount < MAX_ERROR_OUTPUT_SHOWN) {
        errorOutputCount++;
        super.handleErrorOutput(pLine);
      }
    }

    @Override
    protected void handleOutput(String pLine) throws IOException {
      if (buffer == null) {
        buffer = new StringBuffer();
      }
      buffer.append(pLine);
      buffer.append('\n');
    }

    @Override
    protected void handleExitCode(int pCode) throws IOException { }
  }
}
