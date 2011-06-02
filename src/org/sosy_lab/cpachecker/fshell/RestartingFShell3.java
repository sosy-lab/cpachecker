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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

import org.sosy_lab.cpachecker.fshell.testcases.TestSuite;

public class RestartingFShell3 {

  public static void main(String[] args) throws IOException, InterruptedException {
    String lCoverageSpecification = args[0];
    String lCSourceFilename = args[1];
    String lEntryFunction = args[2];
    
    LinkedList<String> lCommand = new LinkedList<String>();
    lCommand.add("java");
    lCommand.add("-Djava.library.path=" + System.getProperty("java.library.path"));
    lCommand.add("-cp");
    lCommand.add(System.getProperty("java.class.path"));
    lCommand.add(Main.class.getName());
    lCommand.add(lCoverageSpecification);
    lCommand.add(lCSourceFilename);
    lCommand.add(lEntryFunction);
    lCommand.add("--withoutCilPreprocessing");
    lCommand.add("--restart");
    lCommand.add("--restart-bound=5000000000"); // 1 GB ram
    
    File lTmpTestsuiteFile = File.createTempFile("testsuite", ".tst");
    lTmpTestsuiteFile.deleteOnExit();
    
    File lTmpFeasibilityFile = File.createTempFile("feasibility", ".fs3");
    lTmpFeasibilityFile.deleteOnExit();
    
    FeasibilityInformation lFeasibilityInformation = new FeasibilityInformation();
    lFeasibilityInformation.setTestsuiteFilename(lTmpTestsuiteFile.getCanonicalPath());
    lFeasibilityInformation.write(lTmpFeasibilityFile);
    
    lCommand.add("--in=" + lTmpFeasibilityFile.getCanonicalPath());
    lCommand.add("--out=" + lTmpFeasibilityFile.getCanonicalPath());
    lCommand.add("--tout=" + lTmpTestsuiteFile.getCanonicalPath());
    lCommand.add("--logging");
    lCommand.add("--append");
    
    
    for (int lIndex = 3; lIndex < args.length; lIndex++) {
      String lArgument = args[lIndex];
      
      if (lArgument.startsWith("--in=")) {
        String lFeasibilityFile = lArgument.substring("--in=".length());
        
        FeasibilityInformation lFeasibilityInformation2 = FeasibilityInformation.load(lFeasibilityFile);
        
        String lTestSuiteFilename = lFeasibilityInformation2.getTestsuiteFilename();
        
        TestSuite lTestSuite = TestSuite.load(lTestSuiteFilename);
        lTestSuite.write(lTmpTestsuiteFile);

        // overwrite temporary feasibility information
        lFeasibilityInformation2.write(lTmpFeasibilityFile);
      }
      else {
        lCommand.add(args[lIndex]);
      }
    }
    
    
    int lReturnValue;
    
    ProcessBuilder lBuilder = new ProcessBuilder(lCommand);
    lBuilder.redirectErrorStream(true);
    
    do {
      Process lFShell3Process = lBuilder.start();
      
      BufferedReader lInput = new BufferedReader(new InputStreamReader(lFShell3Process.getInputStream()));
      
      String lLine = null;
      
      while ((lLine = lInput.readLine()) != null) {
        System.out.println(lLine);
      }
      
      lReturnValue = lFShell3Process.waitFor();
      
      if (lReturnValue != 0) {
        System.out.println("++++++++++++++ RESTART ++++++++++++++");
      }
    }
    while (lReturnValue != 0);
    
    System.out.println("Finished.");
  }
  
}
