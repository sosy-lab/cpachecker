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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CParserException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Options(prefix="parser")
public class CPreprocessor {

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
  @FileOption(Type.OUTPUT_FILE)
  private Path dumpDirectory = Paths.get("preprocessed");

  private final LogManager logger;

  public CPreprocessor(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    if (dumpDirectory != null) {
      dumpDirectory = dumpDirectory.toAbsolutePath().normalize();
    }
  }

  public String preprocess(String file) throws CParserException, InterruptedException {
    String result = preprocess0(file);

    if (dumpResults && dumpDirectory != null) {
      final Path dumpFile = dumpDirectory.resolve(file).normalize();
      if (dumpFile.startsWith(dumpDirectory)) {
        try {
          MoreFiles.writeFile(dumpFile, Charset.defaultCharset(), result);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Cannot write result of preprocessing to file");
        }
      } else {
        logger.logf(
            Level.WARNING,
            "Cannot dump result of preprocessing file %s, because path is outside the current directory and the result would end up outside the output directory.",
            file);
      }
    }

    return result;
  }

  private String preprocess0(String file) throws CParserException, InterruptedException {
    // create command line
    List<String> argList =
        Lists.newArrayList(
            Splitter.on(CharMatcher.whitespace()).omitEmptyStrings().split(preprocessor));
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
        return "";
      }
      return executor.buffer.toString();

    } catch (IOException e) {
      throw new CParserException("Preprocessor failed", e);
    }
  }


  private static class CPreprocessorExecutor extends ProcessExecutor<IOException> {

    private static final int MAX_ERROR_OUTPUT_SHOWN = 10;
    private static final Map<String, String> ENV_VARS = ImmutableMap.of("LANG", "C");

    @SuppressFBWarnings(value = "VO_VOLATILE_INCREMENT",
        justification = "Written only by one thread")
    private volatile int errorOutputCount = 0;
    private volatile StringBuffer buffer;

    public CPreprocessorExecutor(LogManager logger, String[] args) throws IOException {
      super(logger, IOException.class, ENV_VARS, args);
    }

    @Override
    @SuppressWarnings("NonAtomicVolatileUpdate") // errorOutputCount written only by one thread
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
