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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class MeasureGCovCoverage {

  private static String INFO = "This program executes a given test suite on the given source and measures the achieved coverage with gcov.";

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length != 2) {
      System.out.println("Usage: java org.sosy_lab.cpachecker.fshell.testcases.MeasureGCovCoverage <source-file> <testsuite-file>");
      System.out.println();
      System.out.println(INFO);

      return;
    }



    String lSourceFileName = args[0];
    String lTestsuiteFileName = args[1];

    File lSourceFile = new File(lSourceFileName);
    File lTestsuiteFile = new File(lTestsuiteFileName);

    if (!lSourceFile.exists()) {
      System.err.println("Source file " + lSourceFileName + " does not exist!");
      return;
    }

    if (!lTestsuiteFile.exists()) {
      System.out.println("Testsuite file " + lTestsuiteFileName + " does not exist!");
      return;
    }

    lSourceFileName = lSourceFile.getAbsolutePath();
    lTestsuiteFileName = lTestsuiteFile.getAbsolutePath();



    // compile input.c to input.o
    System.out.print("Compile input.c to input.o ... ");

    File lTmpInputO = File.createTempFile("input.", ".o");
    lTmpInputO.deleteOnExit();

    LinkedList<String> lCompileInputO = new LinkedList<String>();
    lCompileInputO.add("gcc");
    lCompileInputO.add("-c");
    lCompileInputO.add("-o");
    lCompileInputO.add(lTmpInputO.getAbsolutePath());

    File lTmpTestCaseFile = File.createTempFile("testcase.", ".tc");
    lTmpTestCaseFile.deleteOnExit();

    lCompileInputO.add("-DINPUTFILE=\"" + lTmpTestCaseFile.getAbsolutePath() + "\"");
    lCompileInputO.add("src/org/sosy_lab/cpachecker/fshell/testcases/input.c");

    ProcessBuilder lBuilder = new ProcessBuilder();
    lBuilder.redirectErrorStream(true);
    lBuilder.command(lCompileInputO);

    Process lCompileInputOProcess = lBuilder.start();

    readInputStream(lCompileInputOProcess.getInputStream());

    int lReturnCode = lCompileInputOProcess.waitFor();

    System.out.println("done (" + lReturnCode + ").");



    // compile source
    System.out.print("Compile source ... ");

    File lObjectFile = File.createTempFile("source.", ".o");
    lObjectFile.deleteOnExit();

    LinkedList<String> lCompileSource = new LinkedList<String>();
    lCompileSource.add("gcc");
    lCompileSource.add("-c");
    lCompileSource.add("-g");
    lCompileSource.add("-fprofile-arcs");
    lCompileSource.add("-ftest-coverage");
    lCompileSource.add("-D__CPROVER__"); // use input() function instead of __BLAST_NONDET, necessary for generic sources
    lCompileSource.add("-o");
    lCompileSource.add(lObjectFile.getAbsolutePath());
    lCompileSource.add(lSourceFileName);

    System.out.println(lCompileSource);

    lBuilder.command(lCompileSource);

    Process lCompileSourceProcess = lBuilder.start();
    readInputStream(lCompileSourceProcess.getInputStream());
    lReturnCode = lCompileSourceProcess.waitFor();

    System.out.println("done (" + lReturnCode + ").");


    // building application
    System.out.print("Building application ... ");

    File lTmpExecutable = File.createTempFile("application", "");
    lTmpExecutable.deleteOnExit();

    LinkedList<String> lCompileApplication = new LinkedList<String>();
    lCompileApplication.add("gcc");
    lCompileApplication.add("-g");
    lCompileApplication.add("-fprofile-arcs");
    lCompileApplication.add("-ftest-coverage");
    lCompileApplication.add("-o");
    lCompileApplication.add(lTmpExecutable.getAbsolutePath());
    lCompileApplication.add(lObjectFile.getAbsolutePath());
    lCompileApplication.add(lTmpInputO.getAbsolutePath());
    lBuilder.command(lCompileApplication);

    Process lCompileApplicationProcess = lBuilder.start();
    readInputStream(lCompileApplicationProcess.getInputStream());
    lReturnCode = lCompileApplicationProcess.waitFor();

    System.out.println("done (" + lReturnCode + ").");



    // run testsuite
    System.out.print("Run testsuite ");

    TestSuite lTestSuite = TestSuite.load(lTestsuiteFileName);

    for (TestCase lTestCase : lTestSuite) {
      lTestCase.toInputFile(lTmpTestCaseFile);

      Process lExecutionProcess = Runtime.getRuntime().exec(lTmpExecutable.getAbsolutePath());
      readInputStream(lExecutionProcess.getInputStream());
      lReturnCode = lExecutionProcess.waitFor();

      System.out.print(".");
    }

    System.out.println(" done.");



    // evaluate coverage
    System.out.print("Evaluate coverage ... ");
    LinkedList<String> lEvaluateCoverage = new LinkedList<String>();
    lEvaluateCoverage.add("gcov");
    lEvaluateCoverage.add("-b");
    lEvaluateCoverage.add("-o");
    lEvaluateCoverage.add(lObjectFile.getAbsolutePath());
    lEvaluateCoverage.add(lSourceFile.getName());

    lBuilder.directory(lSourceFile.getParentFile());
    lBuilder.command(lEvaluateCoverage);
    Process lEvaluationProcess = lBuilder.start();
    readInputStream(lEvaluationProcess.getInputStream());

    lReturnCode = lEvaluationProcess.waitFor();
    System.out.println("done (" + lReturnCode + ").");

    File lGCovFile = new File(new File(lSourceFileName) + ".gcov");
    System.out.print("Deleting " + lGCovFile.getAbsolutePath() + " ... ");
    if (lGCovFile.delete()) {
      System.out.println("done.");
    }
    else {
      System.out.println("failed.");
    }

    File lGCdaFile = new File(lObjectFile.getAbsolutePath().substring(0, lObjectFile.getAbsolutePath().length() - 1) + "gcda");
    System.out.print("Deleting " + lGCdaFile.getAbsolutePath() + " ... ");
    if (lGCdaFile.delete()) {
      System.out.println("done.");
    }
    else {
      System.out.println("failed.");
    }

    File lGCnoFile = new File(lObjectFile.getAbsolutePath().substring(0, lObjectFile.getAbsolutePath().length() - 1) + "gcno");
    System.out.print("Deleting " + lGCnoFile.getAbsolutePath() + " ... ");
    if (lGCnoFile.delete()) {
      System.out.println("done.");
    }
    else {
      System.out.println("failed.");
    }
  }

  private static void readInputStream(InputStream lInputStream) throws IOException {
    BufferedReader lReader = new BufferedReader(new InputStreamReader(lInputStream));

    boolean lHasWritten = false;

    String lLine = null;

    while ((lLine = lReader.readLine()) != null) {
      if (!lHasWritten) {
        System.out.println();
        lHasWritten = true;
      }
      System.out.println("* " + lLine);
    }
  }

}
