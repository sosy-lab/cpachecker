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
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Files.DeleteOnCloseFile;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;

import com.google.common.collect.ImmutableList;

/**
 * Counterexample checker that creates a C program for the counterexample
 * and calls CBMC on it.
 */
@Options()
public class CBMCChecker implements CounterexampleChecker, Statistics {

  private final LogManager logger;

  private final Timer cbmcTime = new Timer();

  @Option(name = "cbmc.dumpCBMCfile",
      description = "File name where to put the path program that is generated "
      + "as input for CBMC. A temporary file is used if this is unspecified. "
      + "If specified, the file name should end with '.i'.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path cbmcFile;

  @Option(name="cbmc.timelimit",
      description="maximum time limit for CBMC (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
        defaultUserUnit=TimeUnit.MILLISECONDS,
        min=0)
  private int timelimit = 0; // milliseconds

  private final MachineModel machineModel;

  public CBMCChecker(Configuration config, LogManager logger, CFA cfa) throws InvalidConfigurationException, CPAException {
    this.logger = logger;

    if (cfa.getLanguage() == Language.JAVA) {
      throw new UnsupportedOperationException("CBMC can't be used with the language Java");
    }

    config.inject(this);
    this.machineModel = cfa.getMachineModel();
  }

  @Override
  public boolean checkCounterexample(ARGState pRootState, ARGState pErrorState,
      Set<ARGState> pErrorPathStates) throws CPAException, InterruptedException {

    if (cbmcFile != null) {
      return checkCounterexample(pRootState, pErrorPathStates, cbmcFile);

    } else {

      // This temp file will be automatically deleted when the try block terminates.
      try (DeleteOnCloseFile tempFile = Files.createTempFile("path", ".i")) {
        return checkCounterexample(pRootState, pErrorPathStates, tempFile.toPath());

      } catch (IOException e) {
        throw new CounterexampleAnalysisFailed("Could not create temporary file " + e.getMessage(), e);
      }
    }
  }

  private boolean checkCounterexample(ARGState pRootState,
      Set<ARGState> pErrorPathStates, Path cFile) throws CPAException, InterruptedException {
    assert cFile != null;

    Appender pathProgram = PathToCTranslator.translatePaths(pRootState, pErrorPathStates);

    // write program to disk
    try (Writer w = Files.openOutputFile(cFile)) {
      pathProgram.appendTo(w);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed("Could not write path program to file " + e.getMessage(), e);
    }

    String mainFunctionName = extractLocation(pRootState).getFunctionName();

    // run CBMC
    logger.log(Level.FINE, "Starting CBMC verification.");
    cbmcTime.start();
    CBMCExecutor cbmc;
    int exitCode;
    try {
      List<String> cbmcArgs = new ArrayList<>();
      cbmcArgs.addAll(getParamForMachineModel());

      // Our paths are loop-free, but there might be hidden loops in stdlib functions like memcpy.
      // CBMC would sometimes endlessly unroll them, so its better to break the loops.
      cbmcArgs.add("--unwind");
      cbmcArgs.add("3");
      cbmcArgs.add("--partial-loops");
      cbmcArgs.add("--no-unwinding-assertions");

      cbmcArgs.add("--function");
      cbmcArgs.add(mainFunctionName + "_0");

      cbmcArgs.add(cFile.getAbsolutePath());

      cbmc = new CBMCExecutor(logger, cbmcArgs);
      exitCode = cbmc.join(timelimit);

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);

    } catch (TimeoutException e) {
      throw new CounterexampleAnalysisFailed("CBMC took too long to verify the counterexample.");

    } finally {
      cbmcTime.stop();
      logger.log(Level.FINER, "CBMC finished.");
    }

    if (!cbmc.producedErrorOutput()) {
      switch (exitCode) {
      case 0: // Verification successful (Path is infeasible)
        return false;

      case 10: // Verification failed (Path is feasible)
        return true;

      default:
      }

    } else {
      logger.log(Level.WARNING, "CBMC returned successfully, but printed warnings, ignoring the result. Please check the log above!");
    }

    // exit code and stderr are already logged with level WARNING
    throw new CounterexampleAnalysisFailed("CBMC could not verify the program (CBMC exit code was " + exitCode + ")!");
  }

  private List<String> getParamForMachineModel() {
    switch (machineModel) {
    case LINUX32:
      // The second parameter was recommended by Michael Tautschnig because
      // --32 doesn't force everything we assume (e.g., endianess).
      return ImmutableList.of("--32", "--i386-linux");
    case LINUX64:
      // Unfortunately there is no similar switch for --64
      return ImmutableList.of("--64");
    default:
      throw new AssertionError("Unknown machine model value " + machineModel);
    }
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
