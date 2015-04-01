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

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class RealCExecutor extends ProcessExecutor<CounterexampleAnalysisFailed> {

  private static final Map<String, String> REALC_ENV_VARS = System.getenv();

  private volatile Boolean result = null;

  @SuppressFBWarnings(value = "VO_VOLATILE_INCREMENT",
      justification = "Written only by one thread")
  private volatile int errorOutputCount = 0;

  public RealCExecutor(LogManager logger, List<String> args) throws IOException {
    super(logger, CounterexampleAnalysisFailed.class, REALC_ENV_VARS, getCommandLine(logger, args));
  }

  public RealCExecutor(LogManager logger, List<String> args, String pathToGCC) throws IOException {
    super(logger, CounterexampleAnalysisFailed.class, REALC_ENV_VARS, getCommandLine(logger, args, pathToGCC));
  }

  private static String[] getCommandLine(LogManager logger, List<String> args) {
    String[] cmd = new String[args.size()];
    for (int i = 0; i < args.size(); i++) {
      cmd[i] = args.get(i);
    }
    String commandLine = "";
    for (String s : cmd) {
      commandLine += s + " ";
    }
    logger.log(Level.FINE, "Commandline to execute: " + commandLine);

    return cmd;
  }

  private static String[] getCommandLine(LogManager logger, List<String> args, String pathToGCC) {
    String[] cmd = new String[args.size() + 1];
    if (pathToGCC == null || pathToGCC.equals("")) {
      throw new IllegalArgumentException("Path to gcc not given, must be set via config file.");
    }
    cmd[0] = pathToGCC;
    for (int i = 0; i < args.size(); i++) {
      cmd[i+1] = args.get(i);
    }
    String commandLine = "";
    for (String s : cmd) {
      commandLine += s + " ";
    }
    logger.log(Level.INFO, "Commandline to execute: " + commandLine);

    return cmd;
  }

  @Override
  protected synchronized void handleExitCode(int pCode) throws CounterexampleAnalysisFailed {
    /*
    Exit code 0 is for success (compile ok, no crash in program)
    Exit code 1 is for fail (compile fail, crash in program
     */
    switch (pCode) {
      case 0:
        result = false;
        break;

      case 1:
        result = true;
        break;

      default:
        super.handleExitCode(pCode);
    }
  }

  public synchronized Boolean getResult() {
    checkState(isFinished());

    if (errorOutputCount > 0) {
      logger.log(Level.WARNING, "GCC returned successfully, but printed warnings, ignoring the result. Please check the log above!");
      errorOutputCount = 0; // print warning only once
      result = null;
    }

    return result;
  }
}