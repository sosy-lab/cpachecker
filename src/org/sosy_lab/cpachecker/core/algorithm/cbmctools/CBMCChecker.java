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
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;

/**
 * Counterexample checker that creates a C program for the counterexample
 * and calls CBMC on it.
 */
@Options()
public class CBMCChecker implements CounterexampleChecker, Statistics {

  private final LogManager logger;

  private final Timer cbmcTime = new Timer();

  @Option(name = "cbmc.dumpCBMCfile",
      description = "file name where to put the path program that is generated "
      + "as input for CBMC. A temporary file is used if this is unspecified.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File cbmcFile;

  @Option(name="cbmc.timelimit",
      description="maximum time limit for CBMC (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
        defaultUserUnit=TimeUnit.MILLISECONDS,
        min=0)
  private int timelimit = 0; // milliseconds

  public CBMCChecker(Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    this.logger = logger;
    config.inject(this);
  }

  @Override
  public boolean checkCounterexample(ARGState pRootState, ARGState pErrorState,
      Set<ARGState> pErrorPathStates) throws CPAException, InterruptedException {

    String mainFunctionName = extractLocation(pRootState).getFunctionName();

    String pathProgram = PathToCTranslator.translatePaths(pRootState, pErrorPathStates);

    // write program to disk
    File cFile = cbmcFile;
    try {
      if (cFile != null) {
        Files.writeFile(cFile, pathProgram);
      } else {
        cFile = Files.createTempFile("path", ".c", pathProgram);
      }
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed("Could not write path program to file " + e.getMessage(), e);
    }
    assert(cFile != null);

    // run CBMC
    logger.log(Level.FINE, "Starting CBMC verification.");
    cbmcTime.start();
    CBMCExecutor cbmc;
    int exitCode;
    try {
      String cbmcArgs[] = {"cbmc", "--function", mainFunctionName + "_0", "--32", cFile.getAbsolutePath()};
      cbmc = new CBMCExecutor(logger, cbmcArgs);
      exitCode = cbmc.join(timelimit);

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);

    } catch (TimeoutException e) {
      throw new CounterexampleAnalysisFailed("CBMC took too long to verify the counterexample.");

    } finally {
      if (cbmcFile == null) {
        // delete temp file so it is gone even if JVM is killed
        cFile.delete();
      }

      cbmcTime.stop();
      logger.log(Level.FINER, "CBMC finished.");
    }

    if (cbmc.getResult() == null) {
      // exit code and stderr are already logged with level WARNING
      throw new CounterexampleAnalysisFailed("CBMC could not verify the program (CBMC exit code was " + exitCode + ")!");
    }
    return cbmc.getResult();
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    out.println("Time for running CBMC:              " + cbmcTime);
  }

  @Override
  public String getName() {
    return "CBMC Counterexample Check";
  }
}
