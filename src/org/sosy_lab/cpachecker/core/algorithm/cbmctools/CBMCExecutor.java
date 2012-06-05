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

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;

public class CBMCExecutor extends ProcessExecutor<RuntimeException> {

  private Boolean result = null;
  private boolean unwindingAssertionFailed = false;
  private boolean gaveErrorOutput = false;

  public CBMCExecutor(LogManager logger, String[] args) throws IOException {
    super(logger, RuntimeException.class, args);
  }

  @Override
  protected void handleExitCode(int pCode) {
    switch (pCode) {
    case 0: // Verification successful (Path is infeasible)
      if (gaveErrorOutput) {
        logger.log(Level.WARNING, "CBMC returned successfully, but printed warnings. Please check the log above!");
      } else {
        result = false;
      }
      break;

    case 10: // Verification failed (Path is feasible)
      if (gaveErrorOutput) {
        logger.log(Level.WARNING, "CBMC returned successfully, but printed warnings. Please check the log above!");
      } else {
        result = true;
      }
      break;

    default:
      super.handleExitCode(pCode);
    }
  }

  @Override
  protected void handleErrorOutput(String pLine) throws RuntimeException {
    if (!(pLine.startsWith("Verified ") && pLine.endsWith("original clauses."))) {
      // exclude the normal status output of CBMC

      gaveErrorOutput = true;
      super.handleErrorOutput(pLine);
    }
  }

  @Override
  protected void handleOutput(String pLine) throws RuntimeException {
    if(pLine.contains("unwinding assertion")){
      unwindingAssertionFailed = true;
    }
    super.handleOutput(pLine);
  }

  public boolean didUnwindingAssertionFailed(){
    return unwindingAssertionFailed;
  }

  public Boolean getResult() {
    checkState(isFinished());
    return result;
  }
}