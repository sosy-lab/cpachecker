/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cmdline;

import static org.sosy_lab.common.DuplicateOutputStream.mergeStreams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

import com.google.common.io.Closeables;

/**
 * This class is a thread which should be register as a VM shutdown hook.
 * It will print all the statistics when CPAchecker terminates.
 * It will also try to stop the analysis when the user presses Ctrl+C.
 */
@Options(prefix="statistics")
class ShutdownHook extends Thread {

  @Option(name="export", description="write some statistics to disk")
  private boolean exportStatistics = true;

  @Option(name="file",
      description="write some statistics to disk")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File exportStatisticsFile = new File("Statistics.txt");

  @Option(name="print", description="print statistics to console")
  private boolean printStatistics = false;

  private final Configuration config;
  private final LogManager logManager;
  private final Thread mainThread;

  // if still null when run() is executed, analysis has been interrupted by user
  private CPAcheckerResult mResult = null;

  /**
   * Create a shutdown hook. This constructor needs to be called from the
   * thread in which CPAchecker is run.
   */
  public ShutdownHook(Configuration pConfig, LogManager logger) throws InvalidConfigurationException {
    config = pConfig;
    logManager = logger;

    config.inject(this);
    mainThread = Thread.currentThread();
  }

  /**
   * Set the CPAchecker result after CPAchecker finished
   * so that it can be used for printing statistics.
   */
  public void setResult(CPAcheckerResult pResult) {
    assert mResult == null;
    mResult = pResult;
  }

  // We want to use Thread.stop() to force the main thread to stop
  // when interrupted by the user.
  @SuppressWarnings("deprecation")
  @Override
  public void run() {
    boolean cancelled = false;

    if (mainThread.isAlive()) {
      // probably the user pressed Ctrl+C
      mainThread.interrupt();
      logManager.log(Level.INFO, "Stop signal received, waiting 2s for analysis to stop cleanly...");
      cancelled = true;

      try {
        mainThread.join(2000);
      } catch (InterruptedException e) {}
      if (mainThread.isAlive()) {
        logManager.log(Level.WARNING, "Analysis did not stop fast enough, forcing immediate termination now. This might prevent the statistics from being generated.");
        mainThread.stop();
      }
    }

    logManager.flush();
    System.out.flush();
    System.err.flush();
    if (mResult != null) {

      // setup output streams
      PrintStream console = printStatistics ? System.out : null;
      FileOutputStream file = null;

      if (exportStatistics && exportStatisticsFile != null) {
        try {
          com.google.common.io.Files.createParentDirs(exportStatisticsFile);
          file = new FileOutputStream(exportStatisticsFile);
        } catch (IOException e) {
          logManager.logUserException(Level.WARNING, e, "Could not write statistics to file");
        }
      }

      PrintStream stream = makePrintStream(mergeStreams(console, file));

      try {
        // print statistics
        mResult.printStatistics(stream);
        stream.println();

        if (cancelled) {
          stream.println(
              "***********************************************************************\n" +
              "* WARNING: Analysis interrupted!! The statistics might be unreliable! *\n" +
              "***********************************************************************\n");
        }

        // print result
        if (!printStatistics) {
          stream = makePrintStream(mergeStreams(System.out, file)); // ensure that result is printed to System.out
        }
        mResult.printResult(stream);

        String outputDirectory = config.getOutputDirectory();
        if (outputDirectory != null && mResult.getResult() != Result.NOT_YET_STARTED) {
          stream.println("More details about the verification run can be found in the directory \"" + outputDirectory + "\".");
        }

        stream.flush();

      } finally {
        // close only file, not System.out
        if (file != null) {
          Closeables.closeQuietly(file);
        }
      }
    }
    logManager.flush();
  }

  private static PrintStream makePrintStream(OutputStream stream) {
    if (stream instanceof PrintStream) {
      return (PrintStream)stream;
    } else {
      return new PrintStream(stream);
    }
  }
}