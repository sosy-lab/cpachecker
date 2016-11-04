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
package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.MoreFiles.DeleteOnCloseFile;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.CBMCExecutor;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * Counterexample checker that creates a C program for the counterexample
 * and calls CBMC on it.
 */
@Options()
public class CBMCChecker implements CounterexampleChecker, Statistics {

  private final LogManager logger;

  private final Timer cbmcTime = new Timer();

  @Option(secure=true, name = "cbmc.dumpCBMCfile",
      description = "File name where to put the path program that is generated "
      + "as input for CBMC. A temporary file is used if this is unspecified. "
      + "If specified, the file name should end with '.i' because otherwise CBMC runs the pre-processor on the file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private @Nullable Path cbmcFile;

  @Option(secure=true, name="cbmc.timelimit",
      description="maximum time limit for CBMC (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
        defaultUserUnit=TimeUnit.MILLISECONDS,
        min=0)
  private TimeSpan timelimit = TimeSpan.ofMillis(0);

  private final MachineModel machineModel;

  public CBMCChecker(Configuration config, LogManager logger, CFA cfa) throws InvalidConfigurationException {
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
      // Suffix .i tells CBMC to not call the pre-processor on this file.
      try (DeleteOnCloseFile tempFile = MoreFiles.createTempFile("path", ".i")) {
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
    try (Writer w = MoreFiles.openOutputFile(cFile, Charset.defaultCharset())) {
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
      cbmcArgs.add("--stop-on-fail");

      // Our paths are loop-free, but there might be hidden loops in stdlib functions like memcpy.
      // CBMC would sometimes endlessly unroll them, so its better to break the loops.
      cbmcArgs.add("--unwind");
      cbmcArgs.add("3");
      cbmcArgs.add("--partial-loops");
      cbmcArgs.add("--no-unwinding-assertions");

      cbmcArgs.add("--function");
      cbmcArgs.add(mainFunctionName + "_0");

      cbmcArgs.add(cFile.toAbsolutePath().toString());

      cbmc = new CBMCExecutor(logger, cbmcArgs);
      exitCode = cbmc.join(timelimit.asMillis());

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
    // CBMC provides --32 and --64 to specify the machine model,
    // but some parameters are not specified by them (e.g., endianess)
    // and are taken from the current system.
    // For 32bit code, there is the additional switch --i386-linux
    // that sets these additional parameters.
    // However, using --i386-linux lets CBMC call the C pre-processor
    // (not for the program but for its internal C library)
    // and the libc 32bit development headers need to be installed,
    // so maybe this is not what we always want (e.g., on Windows)?
    // For --64 there is no switch similar to --i386-linux,
    // because it would require the 64bit headers, and we cannot expect
    // them on a 32bit system.
    case LINUX32:
      return ImmutableList.of("--32", "--i386-linux");
    case LINUX64:
      return ImmutableList.of("--64");
    default:
      throw new AssertionError("Unknown machine model value " + machineModel);
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    out.println("Time for running CBMC:              " + cbmcTime);
  }

  @Override
  public String getName() {
    return "CBMC Counterexample Check";
  }
}
