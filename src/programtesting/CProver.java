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
package programtesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.lang.ProcessBuilder;

import cpa.symbpredabs.explicit.ExplicitAbstractElement;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CProver {

    public static boolean isFeasible(String pFunctionName, String pProgram) {
    File lFile = null;
    
    try {
      lFile = File.createTempFile("path", ".i");
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    PrintWriter lWriter = null;

    try {
      lWriter = new PrintWriter(lFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert (false);
    }

    lWriter.print(pProgram);
    
    lWriter.close();

    boolean retval = isFeasible(lFile, pFunctionName + "_0");
    lFile.delete();

    return retval;
  }
  
  public static boolean isFeasible(String pFunctionName, List<String> pProgram) {
    File lFile = null;
    
    try {
      lFile = File.createTempFile("path", ".i");
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    PrintWriter lWriter = null;

    try {
      lWriter = new PrintWriter(lFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert (false);
    }

    for (String lFunctionString : pProgram) {
      lWriter.print(lFunctionString);
    }

    lWriter.close();

    boolean retval = isFeasible(lFile, pFunctionName + "_0");
    lFile.delete();

    return retval;
  }
  
  public static Map<Deque<ExplicitAbstractElement>, Boolean> checkSat (Map<Deque<ExplicitAbstractElement>, List<String>> translations) {
    Map<Deque<ExplicitAbstractElement>, Boolean> results = new HashMap<Deque<ExplicitAbstractElement>, Boolean>();
    
    for (Map.Entry<Deque<ExplicitAbstractElement>,List<String>> lPath : translations.entrySet()) {
      File lFile = null;
      try {
        // TODO shouldn't it be ".i"?
        lFile = File.createTempFile("path", ".c");
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
         
      PrintWriter lWriter = null;
      try {
        lWriter = new PrintWriter(lFile);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        System.exit(1);
      }

      for (String lFunctionString : lPath.getValue()) {
        lWriter.print(lFunctionString);
      }

      lWriter.close();

      // TODO not sure about the negation here, so make sure we don't use this
      // function before checking again
      assert(false);
      results.put(lPath.getKey(), !isFeasible(lFile, lPath.getKey().getFirst().getLocationNode().getFunctionName() + "_0"));

      lFile.delete();
    }
    
    return results;
  }

  private static boolean isFeasible(File pFile, String pFunctionName) {
    try {
      ProcessBuilder pb = new ProcessBuilder("cbmc", "--no-pointer-check --no-bounds-check --no-div-by-zero-check --function " + pFunctionName + " " + pFile.getAbsolutePath());
      pb.redirectErrorStream(true);
      Process lCBMCProcess = pb.start();
      
      int lCBMCExitValue;
      try {
        lCBMCExitValue = lCBMCProcess.waitFor();
      } catch (InterruptedException e) {
        lCBMCExitValue = -1;
      }
      // we need to read the output of CBMC, otherwise it will block at some
      // point in time when the buffer is full (OS-dependent)
      BufferedReader br = new BufferedReader(new InputStreamReader(lCBMCProcess.getInputStream()));

      switch (lCBMCExitValue) {
        case 0: // lCBMCExitValue == 0 : Verification successful (Path is infeasible)
          while (br.readLine() != null); // discard the output
          br.close();
          return false;

        case 10: // lCBMCExitValue == 10 : Verification failed (Path is feasible)
          while (br.readLine() != null); // discard the output
          br.close();
          return true;

        default:
          // lCBMCExitValue == 6 : Start function symbol not found, but also gcc not found
          // more error codes?
          System.err.println("CBMC had exit code " + lCBMCExitValue + ", output was:");

          String line = null;
          while ((line = br.readLine()) != null) {
            System.err.println(line);
          }
          br.close();

          assert (false);
          break;
        }
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    assert(false); // we don't reach here, and if we ever do: think again about the return value
    return false;
  }
}
