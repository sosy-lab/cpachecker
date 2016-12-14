/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.ProcessExecutor;
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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.cwriter.PathToConcreteProgramTranslator;

/**
 * Counterexample checker that creates a C program out of a given path program.
 * The generated C program is ONE concrete path. There may and will be many other
 * possible concrete paths but only one is checked.
 */
@Options(prefix = "counterexample.concrete")
@SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
public class ConcretePathExecutionChecker implements CounterexampleChecker, Statistics {

  @Option(secure = false, description = "Path to the compiler. Can be absolute or"
                            + " only the name of the program if it is in the PATH")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path pathToCompiler = Paths.get("/usr/bin/gcc");

  @Option(secure=true, description = "The file in which the generated C code is saved.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private @Nullable Path dumpFile = null;

  @Option(secure=true, description="Maximum time limit for the concrete execution checker.\n"
                                 + "This limit is used for compilation as well as execution "
                                 + "so overall, twice the time of this limit may be consumed.\n"
                                 + "(use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS, defaultUserUnit = TimeUnit.MILLISECONDS, min = 0)
  private TimeSpan timelimit = TimeSpan.ofMillis(0);

  private final LogManager logger;
  private final Timer timer = new Timer();

  public ConcretePathExecutionChecker(Configuration config, LogManager logger, CFA cfa)
      throws InvalidConfigurationException {
    if (cfa.getLanguage() != Language.C) {
      throw new UnsupportedOperationException("Concrete execution checker can only be used with C.");
    }

    config.inject(this);
    this.logger = logger;
  }

  @Override
  public boolean checkCounterexample(ARGState pRootState, ARGState pErrorState,
      Set<ARGState> pErrorPathStates) throws CPAException, InterruptedException {

    if (dumpFile != null) {
      return checkCounterexample(pRootState, pErrorState, pErrorPathStates, dumpFile);

    } else {

      // This temp file will be automatically deleted when the try block terminates.
      try (DeleteOnCloseFile tempFile = MoreFiles.createTempFile("concretePath", ".c")) {
        return checkCounterexample(pRootState, pErrorState, pErrorPathStates, tempFile.toPath());

      } catch (IOException e) {
        throw new CounterexampleAnalysisFailed("Could not create temporary file " + e.getMessage(), e);
      }
    }
  }

  /**
   * Compiles the program given in the global variable "dumpFile" and saves the
   * output in the file given in the global variable "executable". The return
   * value indicates the success of the compilation process.
   */
  private void compilePathProgram(String absFilePath) throws CounterexampleAnalysisFailed, InterruptedException, IOException, TimeoutException {
    logger.log(Level.FINE, "Compiling concrete error path.");
    String[] cmdLine = {
      pathToCompiler.toAbsolutePath().toString(), absFilePath, "-o", absFilePath + ".exe", "-w"
    };

    ProcessExecutor<CounterexampleAnalysisFailed> exec = new ProcessExecutor<>(logger, CounterexampleAnalysisFailed.class, System.getenv(), cmdLine);
    // 0 means compilation terminated without errors
    int exitCode = exec.join(timelimit.asMillis());
    if(exitCode != 0) {
      StringBuilder errorOut = new StringBuilder();
      for (String str : exec.getErrorOutput()) {
        errorOut.append(str);
      }
      throw new CounterexampleAnalysisFailed("Could not compile the concrete error path. The compiler finished with exitCode "
                                             + exitCode + "\n The output was: \n" + errorOut.toString());
    }
  }

  private boolean runConcretePathProgram(String absFilePath) throws CounterexampleAnalysisFailed, InterruptedException, IOException, TimeoutException {
    String[] cmdLine = {absFilePath + ".exe"};

    ProcessExecutor<CounterexampleAnalysisFailed> exec = new ProcessExecutor<>(logger, CounterexampleAnalysisFailed.class, System.getenv(), cmdLine);
    int exitCode = exec.join(timelimit.asMillis());

    switch (exitCode) {
      case 0: // Verification successful (Path is infeasible)
        logger.log(Level.FINER, "Concrete path program was executed, the error location was infeasible.");
        return false;
      case 1: // Verification failed (Path is feasible)
        logger.log(Level.FINER, "Concrete path program was executed, the error location was reached.");
        return true;
      default:
        // as only 0 and 1 should occur as exit codes this is probably a bug with the code generation
        throw new CounterexampleAnalysisFailed("Executing the concrete path program lead to invalid exitcode: " + exitCode);
    }
  }

  private boolean checkCounterexample(ARGState pRootState, ARGState pErrorState,
      Set<ARGState> pErrorPathStates, Path cFile) throws CPAException, InterruptedException {
    assert cFile != null;

    timer.start();
    CounterexampleInfo ceInfo = pErrorState.getCounterexampleInformation().get();

    Appender pathProgram = PathToConcreteProgramTranslator.translatePaths(pRootState, pErrorPathStates, ceInfo.getCFAPathWithAssignments());

    // write program to disk
    try (Writer w = MoreFiles.openOutputFile(cFile, Charset.defaultCharset())) {
      pathProgram.appendTo(w);
    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed("Could not write path program to file " + e.getMessage(), e);
    }

    String absFile = cFile.toAbsolutePath().toString();
    try {
      // run compiler
      compilePathProgram(absFile);

      // run program (if successfully compiled)
      return runConcretePathProgram(absFile);

    } catch (IOException e) {
      throw new CounterexampleAnalysisFailed(e.getMessage(), e);

    } catch (TimeoutException e) {
      throw new CounterexampleAnalysisFailed("Execution of concrete counterexample path program took too long.");

    } finally {
      timer.stop();
      logger.log(Level.FINER, "Execution of concrete error path program finished.");
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
    out.println("Time for running concrete path check: " + timer);
  }

  @Override
  public String getName() {
    return "Concrete-Execution Counterexample Check";
  }

}
