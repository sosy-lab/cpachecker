/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 * 
 */
package cpa.common.algorithm.cbmctools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Level;

import cmdline.CPAMain;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CProver {

  public static int checkSat (String pTranslatedProgram) {

    File lFile = null;
//    lFile = new File("/localhome/erkan/path.c");
    try {
      lFile = File.createTempFile("path", ".c");
    } catch (IOException e1) {
//      If this is activated again, it should call CPAMain.logManager.logException for
//      documenting the exception. This automatically prints the stack trace as well.
      e1.printStackTrace();
    }
    lFile.deleteOnExit();

    PrintWriter lWriter = null;
    try {
      lWriter = new PrintWriter(lFile);
    } catch (FileNotFoundException e) {
      CPAMain.logManager.logException(Level.SEVERE, e, "");
      System.exit(1);
    }

//    for (String lFunctionString : lPath.getValue()) {
      lWriter.print(pTranslatedProgram);
//    }

    lWriter.close();

    try {
      // TODO function name
      String lFunctionName = "main";
      // TODO we check for assertion errors
      System.out.println(" --- Starting CBMC verification --- ");
      
      Process lCBMCProcess = Runtime.getRuntime().exec("cbmc --function " + 
          lFunctionName + "_0 --no-bounds-check --no-div-by-zero-check --no-pointer-check " + lFile.getAbsolutePath());            

      // TODO Remove output --- begin
      /*BufferedReader lReader = new BufferedReader(new InputStreamReader(lCBMCProcess.getInputStream()));

        String lLine = null;

        while ((lLine = lReader.readLine()) != null) {
          System.out.println(lLine);
        }

        BufferedReader lErrorReader = new BufferedReader(new InputStreamReader(lCBMCProcess.getErrorStream()));

        String lErrorLine = null;

        while ((lErrorLine = lErrorReader.readLine()) != null) {
          System.out.println(lErrorLine);
        }*/
      // TODO Remove output --- end

      int lCBMCExitValue;
      try {
        lCBMCExitValue = lCBMCProcess.waitFor();
      } catch (InterruptedException e) {
        lCBMCExitValue = -1;
      }

      switch (lCBMCExitValue) {
      case 0: // lCBMCExitValue == 0 : Verification successful (Path is infeasible)
        return 0;
      case 10: // lCBMCExitValue == 10 : Verification failed (Path is feasible)
        return 10;
      default:
        // lCBMCExitValue == 6 : Start function symbol not found, but also gcc not found
        // more error codes?
        CPAMain.logManager.log(Level.WARNING, "CBMC had exit code " + lCBMCExitValue + ", output was:");
      BufferedReader br = new BufferedReader(new InputStreamReader(lCBMCProcess.getErrorStream()));
      String line = null;

      while ((line = br.readLine()) != null) {
        CPAMain.logManager.log(Level.WARNING, line);
      }
      br.close();

      br = new BufferedReader(new InputStreamReader(lCBMCProcess.getInputStream()));
      while ((line = br.readLine()) != null) {
        CPAMain.logManager.log(Level.WARNING, line);
      }

      br.close();
//      assert(false);
      break;
      }
    } catch (IOException e) {
      CPAMain.logManager.logException(Level.SEVERE, e, "");
      System.exit(1);
    } 

    return -99;
  }
}
