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
package org.sosy_lab.cpachecker.core.algorithm.realctools;

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
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.NativeLibraries;
import org.sosy_lab.cpachecker.util.cwriter.PathToRealCTranslator;

/**
 * Counterexample checker that creates a real C program for the counterexample.
 */
@Options()
public class RealCChecker implements CounterexampleChecker, Statistics {

  private static final String OPERATING_SYSTEM = System.getProperty("os.name").toLowerCase();
  private final LogManager logger;
  private final Timer realCTime = new Timer();

  @Option(secure = false, name = "realC.pathToCompiler",
      description = "Path to the compiler. Can be absolute or on PATH")
  private String pathToCompiler = null;

  @Option(secure=false, name = "realC.dumpRealCfile",
      description = "File name where to put the path program that is generated "
          + "as input for a common c compiler. A temporary file is used if this is unspecified. ")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path realCFile;

  @Option(secure=true, name="realC.timelimit",
      description="maximum time limit for realC (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit = TimeUnit.MILLISECONDS,
      min = 0)
  private TimeSpan timelimit = TimeSpan.ofMillis(0);
  private ARGCPA cpa;

  public RealCChecker(Configuration config, LogManager logger, CFA cfa, ARGCPA cpa) throws InvalidConfigurationException, CPAException {
    this.logger = logger;

    if (cfa.getLanguage() != Language.C) {
      throw new UnsupportedOperationException("RealC can only be used with C.");
    }

    this.cpa = cpa;

    config.inject(this);
  }

  @Override
  public boolean checkCounterexample(ARGState pRootState, ARGState pErrorState,
      Set<ARGState> pErrorPathStates) throws CPAException, InterruptedException {

    if (realCFile != null) {
      return checkCounterexample(pRootState, pErrorState, pErrorPathStates, realCFile);

    } else {

      // This temp file will be automatically deleted when the try block terminates.
      try (DeleteOnCloseFile tempFile = Files.createTempFile("path", ".i")) {
        return checkCounterexample(pRootState, pErrorState, pErrorPathStates, tempFile.toPath());

      } catch (IOException e) {
        throw new CounterexampleAnalysisFailed("Could not create temporary file " + e.getMessage(), e);
      }
    }
  }

  private boolean checkCounterexample(ARGState pRootState, ARGState pErrorState,
      Set<ARGState> pErrorPathStates, Path cFile) throws CPAException, InterruptedException {
    assert cFile != null;

    CounterexampleInfo ceInfo = cpa.getCounterexamples().get(pErrorState);

    Appender pathProgram = PathToRealCTranslator.translatePaths(pRootState, pErrorPathStates, ceInfo.getTargetPathModel());

    // write program to disk
    try (Writer w = Files.openOutputFile(cFile)) {
      pathProgram.appendTo(w);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed("Could not write path program to file " + e.getMessage(), e);
    }

    // run compiler
    logger.log(Level.FINE, "Starting Real C verification.");
    realCTime.start();
    RealCExecutor realC;
    int exitCode;
    String executable =
        cFile.getAbsolutePath().substring(0, cFile.getAbsolutePath().length() - 2).replace("\\", "/").replace("./", "");
    try {
      List<String> realCArgs = new ArrayList<>();

      realCArgs.add(pathToCompiler);                                               // 1. path to compiler
      realCArgs.add(cFile.getAbsolutePath().replace("\\", "/").replace("./", "")); // 2. path to source code
      realCArgs.add(executable);                                                   // 3. path to executable

      // different file endings for compiler script for Windows or real operating systems
      String scriptPath = "realc";
      if (OPERATING_SYSTEM.contains("windows")) {
        scriptPath += ".bat";
      } else {
        scriptPath += ".sh";
      }

      realC =
          new RealCExecutor(logger, realCArgs, NativeLibraries.getNativeLibraryPath().resolve(scriptPath).toString());
      exitCode = realC.join(timelimit.asMillis());

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);

    } catch (TimeoutException e) {
      throw new CounterexampleAnalysisFailed("RealC took too long to call the compiler.");

    } finally {
      realCTime.stop();
      logger.log(Level.FINER, "Compiler finished.");
    }

    boolean compilerSuccess;

    switch (exitCode) {
      case 0: // compiler succeeded
        logger.log(Level.FINER, "Compiler successfully started.");
        compilerSuccess = true;
        break;
      case 1: // compiler did not succeed
        logger.log(Level.WARNING, "Compiler did not compile the program.");
      default:
        compilerSuccess = false;
        break;
    }
    exitCode = -1;

    // run program (if successfully compiled)
    if (compilerSuccess) {
      try {
        realCTime.start();
        List<String> realCArgs = new ArrayList<>();

        // finding compiled executable extra sausage for Windows
        if (OPERATING_SYSTEM.contains("windows")) {
          realCArgs.add(executable + ".exe");
        } else {
          realCArgs.add(executable);
        }

        realC = new RealCExecutor(logger, realCArgs);
        exitCode = realC.join(timelimit.asMillis());

      } catch (IOException e) {
        throw new CounterexampleAnalysisFailed(e.getMessage(), e);

      } catch (TimeoutException e) {
        throw new CounterexampleAnalysisFailed("RealC took too long to call the compiler.");

      } finally {
        realCTime.stop();
        logger.log(Level.FINER, "RealC Compiler finished.");
      }

      switch (exitCode) {
        case 0: // Verification successful (Path is infeasible)
          logger.log(Level.FINER, "RealC successfully executed the program and error path is infeasible.");
          return false;
        case 1: // Verification failed (Path is feasible)
          logger.log(Level.FINER, "RealC successfully executed the program and error path was reached.");
          return true;
        default:
          logger.log(Level.WARNING, "RealC executed the program but it did not return a valid exitcode: " + exitCode);
      }

    }

    throw new CounterexampleAnalysisFailed("RealC could not successfully compile the program.");
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    out.println("Time for running Real C:              " + realCTime);
  }

  @Override
  public String getName() {
    return "Real C Counterexample Check";
  }
}
