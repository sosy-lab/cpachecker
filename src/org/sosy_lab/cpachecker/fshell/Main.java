/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.Cilly;

import com.google.common.io.NullOutputStream;

public class Main {

  public static void main(String[] pArguments) throws IOException, InvalidConfigurationException {
    FShell3Result lResult = run(pArguments);

    if (lResult.hasFinished()) {
      // signal that test generation has finished
      System.exit(0);
    }
    else {
      // signal that test generation has not finished
      System.exit(-1);
    }
  }

  public static FShell3Result run(String[] pArguments) throws IOException, InvalidConfigurationException {
    assert(pArguments != null);
    assert(pArguments.length > 1);

    String lFQLSpecificationString = pArguments[0];
    String lSourceFileName = pArguments[1];

    String lEntryFunction = "main";

    if (pArguments.length > 2) {
      lEntryFunction = pArguments[2];
    }

    boolean lCilPreprocessing = true;
    int lMinIndex = 0;
    int lMaxIndex = Integer.MAX_VALUE;
    String lFeasibilityInformationInputFile = null;
    String lFeasibilityInformationOutputFile = null;
    String lTestSuiteOutputFile = null;
    boolean lDoLogging = false;
    boolean lDoAppendingLogging = false;
    boolean lDoRestart = false;
    long lRestartBound = 100000000; // 100 MB
    PrintStream lOutput = System.out;

    for (int lIndex = 3; lIndex < pArguments.length; lIndex++) {
      String lOption = pArguments[lIndex].trim();

      if (lOption.equals("--withoutCilPreprocessing")) {
        lCilPreprocessing = false;
      }
      else if (lOption.startsWith("--min=")) {
        String lTmp = lOption.substring("--min=".length());
        lMinIndex = Integer.valueOf(lTmp);
      }
      else if (lOption.startsWith("--max=")) {
        String lTmp = lOption.substring("--max=".length());
        lMaxIndex = Integer.valueOf(lTmp);
      }
      else if (lOption.startsWith("--in=")) {
        lFeasibilityInformationInputFile = lOption.substring("--in=".length());
      }
      else if (lOption.startsWith("--out=")) {
        lFeasibilityInformationOutputFile = lOption.substring("--out=".length());
      }
      else if (lOption.startsWith("--tout=")) {
        lTestSuiteOutputFile = lOption.substring("--tout=".length());
      }
      else if (lOption.equals("--logging")) {
        lDoLogging = true;
      }
      else if (lOption.equals("--append")) {
        lDoAppendingLogging = true;
      }
      else if (lOption.equals("--restart")) {
        lDoRestart = true;
      }
      else if (lOption.startsWith("--restart-bound=")) {
        String lTmp = lOption.substring("--restart-bound=".length());
        lRestartBound = Long.valueOf(lTmp);
      }
      else if (lOption.startsWith("--output=")) {
        String lTmp = lOption.substring("--output=".length());
        lOutput = new PrintStream(new BufferedOutputStream(new FileOutputStream(lTmp)));
      }
      else if (lOption.equals("--nooutput")) {
        lOutput = new PrintStream(new NullOutputStream());
      }
    }

    if (lCilPreprocessing) {
      // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
      Configuration lConfig = Configuration.defaultConfiguration();
      LogManager lLogger = new LogManager(lConfig);
      Cilly lCilly = new Cilly(lLogger);

      if (!lCilly.isCillyInvariant(lSourceFileName)) {
        File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
        //lCillyProcessedFile.deleteOnExit();

        lSourceFileName = lCillyProcessedFile.getAbsolutePath();

        lOutput.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
      }
    }

    FShell3 lFShell = new FShell3(lSourceFileName, lEntryFunction);

    lFShell.setOutput(lOutput);
    lFShell.setGoalIndices(lMinIndex, lMaxIndex);

    if (lFeasibilityInformationInputFile != null) {
      lFShell.setFeasibilityInformationInputFile(lFeasibilityInformationInputFile);
    }

    if (lFeasibilityInformationOutputFile != null) {
      lFShell.setFeasibilityInformationOutputFile(lFeasibilityInformationOutputFile);
    }

    if (lTestSuiteOutputFile != null) {
      lFShell.setTestSuiteOutputFile(lTestSuiteOutputFile);
    }

    if (lDoLogging) {
      lFShell.doLogging();

      if (lDoAppendingLogging) {
        lFShell.doAppendingLogging();
      }
    }

    if (lDoRestart) {
      lFShell.doRestart();
      lFShell.setRestartBound(lRestartBound);
    }

    FShell3Result lResult = lFShell.run(lFQLSpecificationString);

    lOutput.println("#Goals: " + lResult.getNumberOfTestGoals() + ", #Feasible: " + lResult.getNumberOfFeasibleTestGoals() + ", #Infeasible: " + lResult.getNumberOfInfeasibleTestGoals() + ", #Imprecise: " + lResult.getNumberOfImpreciseTestCases());

    if (!lOutput.equals(System.out)){
      lOutput.close();
    }

    return lResult;
  }

  public static String[] getParameters(String pQuery, String pSource, String pEntryFunction, boolean pDisablePreprocessing) {
    List<String> lArguments = new LinkedList<String>();
    lArguments.add(pQuery);
    lArguments.add(pSource);
    lArguments.add(pEntryFunction);

    String[] lResult;

    if (pDisablePreprocessing) {
      lArguments.add("--withoutCilPreprocessing");

      lResult = new String[4];
    }
    else {
      lResult = new String[3];
    }

    return lArguments.toArray(lResult);
  }

}

