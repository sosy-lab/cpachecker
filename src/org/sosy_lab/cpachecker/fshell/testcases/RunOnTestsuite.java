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
package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.File;
import java.io.IOException;

public class RunOnTestsuite {

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length != 3) {
      System.err.println("Usage: java " + RunOnTestsuite.class.getCanonicalName() + " <executable> <testsuite-file> <testcase-file>");
      return;
    }

    String lExecutableName = args[0];
    String lTestsuiteFileName = args[1];
    String lTestcaseFileName = args[2];

    File lExecutable = new File(lExecutableName);
    File lTestsuiteFile = new File(lTestsuiteFileName);
    File lTestcaseFile = new File(lTestcaseFileName);

    if (!lExecutable.canExecute()) {
      System.err.println("Specified executable can't be executed!");
      return;
    }

    if (!lTestsuiteFile.canRead()) {
      System.err.println("Can't read testsuite file!");
      return;
    }

    if (lTestcaseFile.exists()) {
      System.err.println("Testcase file exists!");
      return;
    }

    // 1) load test suite
    TestSuite lTestSuite = TestSuite.load(lTestsuiteFileName);

    // 2) for each test case run executable
    for (TestCase lTestCase : lTestSuite) {
      System.out.println("Testcase: " + lTestCase.toString());
      lTestCase.toInputFile(lTestcaseFile);

      ProcessBuilder lBuilder = new ProcessBuilder();
      lBuilder.command(lExecutableName);
      lBuilder.redirectErrorStream(true);

      Process lProcess = lBuilder.start();

      MeasureGCovCoverage.readInputStream(lProcess.getInputStream());

      int lReturnValue = lProcess.waitFor();

      System.out.println("Return value: " + lReturnValue);

      lTestcaseFile.delete();
    }

    System.out.println("Bye.");
  }

}
