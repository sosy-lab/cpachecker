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

import common.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Deque;

import cpa.symbpredabs.explicit.ExplicitAbstractElement;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CProver {
  private static int mNumberOfCallsToCBMC = 0;

  // based on the code from
  // http://www.velocityreviews.com/forums/t130884-process-runtimeexec-causes-subprocess-hang.html
  public static class StreamGobbler implements Runnable {

    private final InputStream is;
    private StringBuilder stream;
    private Thread mThread;

    public StreamGobbler(InputStream is) {
      this.is = is;
      stream = new StringBuilder();
      mThread = new Thread(this);
      mThread.start();
    }

    @Override
    public void run() {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line;

      try {
        while ((line = br.readLine()) != null) {
          stream.append(line + "\n");
        }
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
        assert (false);
      }
    }

    public String getStream() {
      try {
        mThread.join();
      } catch (InterruptedException ex) {
        ex.printStackTrace();
        assert(false);
      }

      return stream.toString();
    }
  }

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

  public static Pair<Boolean, String> getFeasibilityAndOutput(String pFunctionName, String pProgram) {
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

    Pair<Boolean, String> retval = getFeasibilityAndOutput(lFile, pFunctionName);
    //lFile.delete();

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
      mNumberOfCallsToCBMC++;

      ProcessBuilder pb = new ProcessBuilder("cbmc", "--no-pointer-check",  "--no-bounds-check",  "--no-div-by-zero-check",  "--function", pFunctionName, pFile.getAbsolutePath());
      pb.redirectErrorStream(true);
      Process lCBMCProcess = pb.start();
      // we need to read the output of CBMC, otherwise it will block at some
      // point in time when the buffer is full (OS-dependent)
      StreamGobbler lGobbler = new StreamGobbler (lCBMCProcess.getInputStream ());
      
      int lCBMCExitValue;
      try {
        lCBMCExitValue = lCBMCProcess.waitFor();
      } catch (InterruptedException e) {
        lCBMCExitValue = -1;
      }

      switch (lCBMCExitValue) {
        case 0: // lCBMCExitValue == 0 : Verification successful (Path is infeasible)
          return false;

        case 10: // lCBMCExitValue == 10 : Verification failed (Path is feasible)
          return true;

        default:
          // lCBMCExitValue == 6 : Start function symbol not found, but also gcc not found
          // more error codes?
          System.err.println("CBMC had exit code " + lCBMCExitValue + ", output was:");
          System.err.println(lGobbler.getStream());

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

  private static Pair<Boolean, String> getFeasibilityAndOutput(File pFile, String pFunctionName) {
    try {
      mNumberOfCallsToCBMC++;

      ProcessBuilder pb = new ProcessBuilder("cbmc", "--no-pointer-check",  "--no-bounds-check",  "--no-div-by-zero-check",  "--function", pFunctionName, pFile.getAbsolutePath());
      pb.redirectErrorStream(true);
      Process lCBMCProcess = pb.start();
      // we need to read the output of CBMC, otherwise it will block at some
      // point in time when the buffer is full (OS-dependent)
      StreamGobbler lGobbler = new StreamGobbler (lCBMCProcess.getInputStream ());

      int lCBMCExitValue;
      try {
        lCBMCExitValue = lCBMCProcess.waitFor();
      } catch (InterruptedException e) {
        lCBMCExitValue = -1;
      }

      switch (lCBMCExitValue) {
        case 0: // lCBMCExitValue == 0 : Verification successful (Path is infeasible)
          return new Pair<Boolean, String>(false, lGobbler.getStream());

        case 10: // lCBMCExitValue == 10 : Verification failed (Path is feasible)
          return new Pair<Boolean, String>(true, lGobbler.getStream());

        default:
          // lCBMCExitValue == 6 : Start function symbol not found, but also gcc not found
          // more error codes?
          System.err.println("CBMC had exit code " + lCBMCExitValue + ", output was:");
          System.err.println(lGobbler.getStream());

          assert (false);
          break;
        }
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    assert(false); // we don't reach here, and if we ever do: think again about the return value
    return new Pair<Boolean, String>(false, "");
  }
  
  public static int getNumberOfCallsToCBMC() {
    return mNumberOfCallsToCBMC;
  }
}
