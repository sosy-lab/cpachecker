/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
/**
 *
 */
package org.sosy_lab.cpachecker.core.algorithm.cbmctools;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;

import com.google.common.base.Preconditions;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CProver {
  
  private static class CBMCExecutor extends ProcessExecutor<RuntimeException> {
    
    // TODO function name
    private static final String[] CBMC_ARGS = {"cbmc", "--function", "main_0",
            "--no-bounds-check", "--no-div-by-zero-check", "--no-pointer-check"};
    
    private Boolean result = null;
    
    public CBMCExecutor(LogManager logger, File file) throws IOException {
      super(logger, getCmdline(file));
    }
    
    private static String[] getCmdline(File file) {
      Preconditions.checkArgument(file.canRead());
      
      String[] result = Arrays.copyOf(CBMC_ARGS, CBMC_ARGS.length + 1);
      result[result.length-1] = file.getAbsolutePath();
      return result;
    }
    
    @Override
    protected void handleExitCode(int pCode) {
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
  }

  public static boolean checkFeasibility(String program, LogManager logger) throws IOException {
    File cFile = Files.createTempFile("path", ".c", program);
    try {
      logger.log(Level.FINER, "Starting CBMC verification.");
      CBMCExecutor cbmc = new CBMCExecutor(logger, cFile);
      cbmc.read();
      logger.log(Level.FINER, "CBMC finished.");
      
      if (cbmc.result == null) {
        // exit code and stderr are already logged with level WARNING
        throw new UnsupportedOperationException("CBMC could not verify the program (CBMC exit code was " + cbmc.getExitCode() + ")!");
      }
      return cbmc.result;
      
    } finally {
      cFile.delete();
    }
  }
}