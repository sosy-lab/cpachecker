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
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.ProcessExecutor;


/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CProver {

  public static boolean checkSat (String pTranslatedProgram, LogManager logger) throws IOException {

    File cFile = Files.createTempFile("path", ".c", pTranslatedProgram);
    try {
      
      // TODO function name
      String[] args = {"cbmc", "--function", "main_0", "--no-bounds-check", "--no-div-by-zero-check", "--no-pointer-check", cFile.getAbsolutePath()}; 
      
      logger.log(Level.FINER, "Starting CBMC verification.");
      ProcessExecutor<IOException> cbmc = new ProcessExecutor<IOException>(logger, args);
      cbmc.read();
      logger.log(Level.FINER, "CBMC finished.");
      
      switch (cbmc.getExitCode()) {
      case 0: // Verification successful (Path is infeasible)
        return false;
      case 10: // Verification failed (Path is feasible)
        return true;
      default:
        // exit code == 6 : Start function symbol not found, but also gcc not found
        // more error codes?
        
        // exit code and stderr are already logged with level WARNING by ProcessExecutor
        throw new UnsupportedOperationException("CBMC could not verify the program (CBMC exit code was " + cbmc.getExitCode() + ")!");
      }
      
    } finally {
      if (cFile.exists()) {
        cFile.delete();
      }
    }
  }
}
