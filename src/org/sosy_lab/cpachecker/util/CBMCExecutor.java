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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CBMCExecutor extends ProcessExecutor<CounterexampleAnalysisFailed> {

  private static final int MAX_CBMC_ERROR_OUTPUT_SHOWN = 10;
  private static final Map<String, String> CBMC_ENV_VARS = ImmutableMap.of("LANG", "C");

  private volatile Boolean result = null;
  private boolean unwindingAssertionFailed = false;

  @SuppressFBWarnings(value = "VO_VOLATILE_INCREMENT",
      justification = "Written only by one thread")
  private volatile int errorOutputCount = 0;

  public CBMCExecutor(LogManager logger, List<String> args) throws IOException {
    super(logger, CounterexampleAnalysisFailed.class, CBMC_ENV_VARS, getCommandLine(args));
  }

  private static String[] getCommandLine(List<String> args) {
    String[] cmd = new String[args.size() + 1];
    cmd[0] = NativeLibraries.getNativeLibraryPath().resolve("cbmc").toString();
    for (int i = 0; i < args.size(); i++) {
      cmd[i+1] = args.get(i);
    }
    return cmd;
  }

  @Override
  protected synchronized void handleExitCode(int pCode) throws CounterexampleAnalysisFailed {
    switch (pCode) {
    case 0: // Verification successful (Path is infeasible)
      result = false;
      break;

    case 10: // Verification failed (Path is feasible)
      result = true;
      break;

    default:
      super.handleExitCode(pCode);
    }
  }

  @Override
  protected synchronized void handleErrorOutput(String pLine) throws CounterexampleAnalysisFailed {
    // CBMC does not seem to print this anymore to stderr
    //if (!(pLine.startsWith("Verified ") && pLine.endsWith("original clauses.")))

    if (pLine.contains("Out of memory") || pLine.equals("terminate called after throwing an instance of 'Minisat::OutOfMemoryException'")) {
      throw new CounterexampleAnalysisFailed("CBMC run out of memory.");

    } else if (pLine.startsWith("**** WARNING: no body for function ")
             || pLine.contains("warning: #pragma once in main file")) {
      // ignore warning that are not interesting for us

    } else {
      if (errorOutputCount == MAX_CBMC_ERROR_OUTPUT_SHOWN) {
        logger.log(Level.WARNING, "Skipping further CBMC error output...");
        errorOutputCount++;

      } else if (errorOutputCount < MAX_CBMC_ERROR_OUTPUT_SHOWN) {
        errorOutputCount++;
        super.handleErrorOutput(pLine);
      }
    }
  }

  @Override
  protected synchronized void handleOutput(String pLine) throws CounterexampleAnalysisFailed {
    if (pLine.contains("unwinding assertion")) {
      unwindingAssertionFailed = true;
    }
    super.handleOutput(pLine);
  }

  public boolean didUnwindingAssertionFail() {
    return unwindingAssertionFailed;
  }

  public boolean producedErrorOutput() {
    checkState(isFinished());
    return errorOutputCount > 0;
  }

  public synchronized Boolean getResult() {
    checkState(isFinished());

    if (errorOutputCount > 0) {
      logger.log(Level.WARNING, "CBMC returned successfully, but printed warnings, ignoring the result. Please check the log above!");
      errorOutputCount = 0; // print warning only once
      result = null;
    }

    return result;
  }
}