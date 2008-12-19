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

import cpa.symbpredabs.explicit.ExplicitAbstractElement;

import predicateabstraction.ThreeValuedBoolean;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CProver {

  public static Map<Deque<ExplicitAbstractElement>,ThreeValuedBoolean> checkSat (Map<Deque<ExplicitAbstractElement>, List<String>> translations) {
    Map<Deque<ExplicitAbstractElement>,ThreeValuedBoolean> results = new HashMap<Deque<ExplicitAbstractElement>,ThreeValuedBoolean>();
    
    for (Map.Entry<Deque<ExplicitAbstractElement>,List<String>> lPath : translations.entrySet()) {
      File lFile = null;
      try {
        // TODO shouldn't it be ".i"?
        lFile = File.createTempFile("path", ".c");
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
      lFile.deleteOnExit();
         
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

      try {
        String lFunctionName = lPath.getKey().getFirst().getLocationNode().getFunctionName();                                       
        Process lCBMCProcess = Runtime.getRuntime().exec("cbmc --function " + lFunctionName + "_0 " + lFile.getAbsolutePath());            

        // TODO Remove output --- begin
        BufferedReader lReader = new BufferedReader(new InputStreamReader(lCBMCProcess.getInputStream()));

        String lLine = null;

        while ((lLine = lReader.readLine()) != null) {
          System.out.println(lLine);
        }

        BufferedReader lErrorReader = new BufferedReader(new InputStreamReader(lCBMCProcess.getErrorStream()));

        String lErrorLine = null;

        while ((lErrorLine = lErrorReader.readLine()) != null) {
          System.out.println(lErrorLine);
        }
        // TODO Remove output --- end

        int lCBMCExitValue;
        try {
          lCBMCExitValue = lCBMCProcess.waitFor();
        } catch (InterruptedException e) {
          lCBMCExitValue = -1;
        }

        switch (lCBMCExitValue) {
        case 0: // lCBMCExitValue == 0 : Verification successful (Path is infeasible)
          results.put(lPath.getKey(), ThreeValuedBoolean.FALSE);
          break;
        case 10: // lCBMCExitValue == 10 : Verification failed (Path is feasible)
          results.put(lPath.getKey(), ThreeValuedBoolean.TRUE);
          break;
        default:
          // lCBMCExitValue == 6 : Start function symbol not found
          // more error codes?
          System.err.println("CBMC had exit code " + lCBMCExitValue);
        results.put(lPath.getKey(), ThreeValuedBoolean.DONTKNOW);
        }
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      } 
    }
    
    return results;
  }
}
