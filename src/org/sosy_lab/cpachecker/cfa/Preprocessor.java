// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CParserException;

@Options(prefix = "parser")
public abstract class Preprocessor {

  @Option(
      name = "preprocessor.dumpDirectory",
      description = "Directory where to dump the results of the preprocessor.")
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path dumpDirectory = Paths.get("preprocessed");

  private final LogManager logger;

  protected Preprocessor(Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    config.inject(this, Preprocessor.class);
    logger = pLogger;
    if (dumpDirectory != null) {
      dumpDirectory = dumpDirectory.toAbsolutePath().normalize();
    }
  }

  public String preprocess(String file) throws CParserException, InterruptedException {
    String result = preprocess0(file);
    getAndWriteDumpedFile(result, file);
    return result;
  }

  @SuppressWarnings("JdkObsolete") // buffer is accessed from several threads
  protected String preprocess0(String file) throws CParserException, InterruptedException {
    // create command line
    List<String> argList =
        Lists.newArrayList(
            Splitter.on(CharMatcher.whitespace()).omitEmptyStrings().split(getCommandLine()));
    argList.add(file);
    String[] args = argList.toArray(new String[0]);

    logger.log(Level.FINE, "Running", MoreStrings.lazyString(this::getName), argList);
    try {
      PreprocessorExecutor executor = new PreprocessorExecutor(logger, args);
      executor.sendEOF();
      int exitCode = executor.join();
      logger.log(Level.FINE, () -> getCapitalizedName() + " finished");

      if (exitCode != 0) {
        throw new CParserException(getCapitalizedName() + " failed with exit code " + exitCode);
      }

      if (executor.errorOutputCount > 0) {
        logger.log(
            Level.WARNING,
            MoreStrings.lazyString(
                () ->
                    getCapitalizedName()
                        + " returned successfully, but printed warnings. Please check the log"
                        + " above!"));
      }

      if (executor.buffer == null) {
        return "";
      }
      return executor.buffer.toString();

    } catch (IOException e) {
      throw new CParserException(getCapitalizedName() + " failed", e);
    }
  }

  protected Path getAndWriteDumpedFile(String programCode, String file) {
    if (dumpResults() && dumpDirectory != null) {
      final Path dumpFile = dumpDirectory.resolve(getDumpFileOfFile(file)).normalize();
      if (dumpFile.startsWith(dumpDirectory)) {
        try {
          IO.writeFile(dumpFile, Charset.defaultCharset(), programCode);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Cannot write result of preprocessing to file");
        }
      } else {
        logger.logf(
            Level.WARNING,
            "Cannot dump result of preprocessing file %s, because path is outside the current"
                + " directory and the result would end up outside the output directory.",
            file);
      }
      return dumpFile;
    }
    return null;
  }

  protected abstract String getName();

  protected abstract String getCommandLine();

  protected abstract boolean dumpResults();

  protected abstract String getDumpFileOfFile(String file);

  private String getCapitalizedName() {
    return getName().substring(0, 1).toUpperCase() + getName().substring(1);
  }

  private static class PreprocessorExecutor extends ProcessExecutor<IOException> {

    private static final int MAX_ERROR_OUTPUT_SHOWN = 10;
    private static final ImmutableMap<String, String> ENV_VARS = ImmutableMap.of("LANG", "C");

    @SuppressFBWarnings(
        value = "VO_VOLATILE_INCREMENT",
        justification = "Written only by one thread")
    private volatile int errorOutputCount = 0;

    private volatile StringBuffer buffer;

    public PreprocessorExecutor(LogManager logger, String[] args) throws IOException {
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
    @SuppressWarnings("JdkObsolete") // buffer is accessed from several threads
    protected void handleOutput(String pLine) {
      if (buffer == null) {
        buffer = new StringBuffer();
      }
      buffer.append(pLine);
      buffer.append('\n');
    }

    @Override
    protected void handleExitCode(int pCode) {}
  }
}
