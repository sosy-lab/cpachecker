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
package org.sosy_lab.cpachecker.efshell;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.fshell.testcases.TestCase;
import org.sosy_lab.cpachecker.util.Cilly;

public class Main {
  public static int OINTPR=0;
  public static final int CMPLXA=0;
  public static final String STATEMENT_COVERAGE = "COVER \"EDGES(ID)*\".NODES(ID).\"EDGES(ID)*\"";
  public static final String STATEMENT_2_COVERAGE = STATEMENT_COVERAGE + ".NODES(ID).\"EDGES(ID)*\"";
  public static final String STATEMENT_3_COVERAGE = STATEMENT_2_COVERAGE + ".NODES(ID).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_COVERAGE = "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_2_COVERAGE = BASIC_BLOCK_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_3_COVERAGE = BASIC_BLOCK_2_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_4_COVERAGE = BASIC_BLOCK_3_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_5_COVERAGE = BASIC_BLOCK_4_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String CONDITION_COVERAGE = "COVER \"EDGES(ID)*\".EDGES(@CONDITIONEDGE).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_NODES_COVERAGE = "COVER \"EDGES(ID)*\".NODES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_NODES_2_COVERAGE = BASIC_BLOCK_NODES_COVERAGE + ".NODES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  public static final String BASIC_BLOCK_NODES_3_COVERAGE = BASIC_BLOCK_NODES_2_COVERAGE + ".NODES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";

  public static void main(String[] pArguments) throws IOException, InvalidConfigurationException {
    //run(pArguments,TestCase pTestCase);
  }

  public static FShell3Result run(String[] pArguments,TestCase pTestCase, PrintWriter out) throws IOException, InvalidConfigurationException {
    assert(pArguments != null);
    assert(pArguments.length > 1);

    System.out.println(pArguments[0]);

    String lFQLSpecificationString = pArguments[0];
    String lSourceFileName = pArguments[1];

    String lEntryFunction = "main";

    if (pArguments.length > 2) {
      lEntryFunction = pArguments[2];
    }

    // TODO implement nicer mechanism for disabling cilly preprocessing
    if (pArguments.length <= 3) {
      // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
      Configuration lConfig = Configuration.defaultConfiguration();
      LogManager lLogger = new LogManager(lConfig);
      Cilly lCilly = new Cilly(lLogger);

      if (!lCilly.isCillyInvariant(lSourceFileName)) {
        File lCillyProcessedFile = lCilly.cillyfy(lSourceFileName);
        //lCillyProcessedFile.deleteOnExit();

        lSourceFileName = lCillyProcessedFile.getAbsolutePath();

        System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
      }
    }

    FShell3 lFlleSh = new FShell3(lSourceFileName, lEntryFunction);

    FShell3Result lResult = lFlleSh.run(lFQLSpecificationString,pTestCase,out);

   // System.out.println("#Goals: " + lResult.getNumberOfTestGoals() + ", #Feas: " + lResult.getNumberOfFeasibleTestGoals() + ", #Infeas: " + lResult.getNumberOfInfeasibleTestGoals() + ", #Imprecise: " + lResult.getNumberOfImpreciseTestCases());

    return lResult;
  }

  public static String[] getParameters(String pQuery, String pSource, String pEntryFunction, boolean pDisablePreprocessing) {
    List<String> lArguments = new LinkedList<String>();
    lArguments.add(pQuery);
    lArguments.add(pSource);
    lArguments.add(pEntryFunction);

    String[] lResult;

    if (pDisablePreprocessing) {
      lArguments.add("disablecilpreprocessing");

      lResult = new String[4];
    }
    else {
      lResult = new String[3];
    }

    return lArguments.toArray(lResult);
  }

}

