/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.coverage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class CoveragePrinterGcov implements CoveragePrinter {

  class FunctionInfo {
    final String name;
    final int firstLine;
    final int lastLine;

    FunctionInfo(String pName, int pFirstLine, int pLastLine) {
      name = pName;
      firstLine = pFirstLine;
      lastLine = pLastLine;
    }
  }

  Set<Integer> visitedLines;
  Set<Integer> allLines;
  Set<String> visitedFunctions;
  Set<FunctionInfo> allFunctions;
  Map<Integer, String> functionBeginnings;

  //String constants from gcov format
  private final static String TEXTNAME = "TN:";
  private final static String SOURCEFILE = "SF:";
  private final static String FUNCTION = "FN:";
  private final static String FUNCTIONDATA = "FNDA:";
  private final static String LINEDATA = "DA:";

  public CoveragePrinterGcov() {
    visitedLines = new HashSet<>();
    allLines = new HashSet<>();
    visitedFunctions = new HashSet<>();
    allFunctions = new HashSet<>();
    functionBeginnings = new HashMap<>();
  }

  @Override
  public void addVisitedFunction(String pName) {
    visitedFunctions.add(pName);
  }

  @Override
  public void addExistingFunction(String pName, int pFirstLine, int pLastLine) {
    allFunctions.add(new FunctionInfo(pName, pFirstLine, pLastLine));
    functionBeginnings.put(pFirstLine, pName);
  }

  @Override
  public void addVisitedLine(int pLine) {
    if (pLine > 0) {
      visitedLines.add(pLine);
    }
  }

  @Override
  public void addExistingLine(int pLine) {
    if (pLine > 0) {
      allLines.add(pLine);
    }
  }

  private void createDir(File file) throws IOException {
    File previousDir = file.getParentFile();
    if (!previousDir.exists()) {
      createDir(previousDir);
    }
    file.mkdir();
  }

  @Override
  public void print(String outputFile, String originFile) {
    try {
      File file = new File(outputFile);
      if (!file.exists()) {
        File previousDir = file.getParentFile();
        if (!previousDir.exists()) {
          createDir(previousDir);
        }
        file.createNewFile();
      }

      PrintWriter out = new PrintWriter(outputFile);

      //Convert ./test.c -> /full/path/test.c
      file = new File(originFile);
      out.println(TEXTNAME);
      out.println(SOURCEFILE + file.getAbsolutePath());

      for (FunctionInfo info : allFunctions) {
        out.println(FUNCTION + info.firstLine + "," + info.name);
        //Information about function end isn't used by lcov, but it is useful for some postprocessing
        //But lcov ignores all unknown lines, so, this additional information can't affect on its work
        out.println("#" + FUNCTION + info.lastLine);
      }

      for (String name : visitedFunctions) {
        out.println(FUNCTIONDATA + "1," + name);
      }

      /* Now save information about lines
       */
      for (Integer line : allLines) {
        /* Some difficulties: all function beginnings are visited at the beginning of analysis
         * without entering function.
         * So, we should mark these lines, as visited, if the function is really visited later.
         */
        String functionName;
        if ((functionName = functionBeginnings.get(line)) != null) {
          //We should mark it, as visited, if the function is analyzed
          out.println(LINEDATA + line + "," + (visitedFunctions.contains(functionName) ? 1 : 0));
        } else {
          out.println(LINEDATA + line + "," + (visitedLines.contains(line) ? 1 : 0));
        }
      }
      out.println("end_of_record");
      out.close();
    } catch(FileNotFoundException e) {
      System.err.println("Cannot open output file " + outputFile);
    } catch(IOException e) {
      System.err.println("Cannot create file " + outputFile + ": " + e.getMessage());
    }
  }


}
