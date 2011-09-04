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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;

public class MeasureGCovCoverage {

  private static String INFO = "This program executes a given test suite on the given source and measures the achieved coverage with gcov.";

  private static void printUsage() {
    System.out.println("Usage: java org.sosy_lab.cpachecker.fshell.testcases.MeasureGCovCoverage [--fshell2] <source-file> <testsuite-file>");
    System.out.println();
    System.out.println(INFO);
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length != 2 && args.length != 3) {
      printUsage();

      return;
    }

    String lSourceFileName;
    String lTestsuiteFileName;

    if (args.length == 2) {
      lSourceFileName = args[0];
      lTestsuiteFileName = args[1];
    }
    else {
      if (!args[0].equals("--fshell2")) {
        printUsage();

        return;
      }

      lSourceFileName = args[1];
      lTestsuiteFileName = args[2];

      File lTmpTestsuiteFile = File.createTempFile("testsuite.", ".tst");
      lTmpTestsuiteFile.deleteOnExit();

      System.out.print("Translate FShell2 output to FShell3 test case ... ");

      FShell2ToFShell3.translateTestsuite(lTestsuiteFileName, lTmpTestsuiteFile.getAbsolutePath());

      System.out.println("done.");

      lTestsuiteFileName = lTmpTestsuiteFile.getAbsolutePath();
    }

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

    //lCompileInputO.add("-DINPUTFILE=\"" + lTmpTestCaseFile.getAbsolutePath() + "\"");
    lCompileInputO.add("-DINPUTFILE=" + lTmpTestCaseFile.getAbsolutePath());
    lCompileInputO.add("src/org/sosy_lab/cpachecker/fshell/testcases/input.c");

    ProcessBuilder lBuilder = new ProcessBuilder();
    lBuilder.redirectErrorStream(true);
    lBuilder.command(lCompileInputO);

    Process lCompileInputOProcess = lBuilder.start();
    readInputStream(lCompileInputOProcess.getInputStream());
    int lReturnCode = lCompileInputOProcess.waitFor();

    System.out.println("done (" + lReturnCode + ").");



    // pre-process source I)
    System.out.print("Preprocess source step A ... ");

    File lPreprocessedSourceFile = File.createTempFile("source.", ".c");
    lPreprocessedSourceFile.deleteOnExit();

    LinkedList<String> lPreprocessSource = new LinkedList<String>();
    lPreprocessSource.add("gcc");
    lPreprocessSource.add("-E");
    lPreprocessSource.add("-D__CPROVER__"); // use input() function instead of __BLAST_NONDET, necessary for generic sources
    lPreprocessSource.add("-o");
    lPreprocessSource.add(lPreprocessedSourceFile.getAbsolutePath());
    lPreprocessSource.add(lSourceFileName);

    lBuilder.command(lPreprocessSource);

    Process lPreprocessingProcess = lBuilder.start();
    readInputStream(lPreprocessingProcess.getInputStream());
    lReturnCode = lPreprocessingProcess.waitFor();

    System.out.println("done (" + lReturnCode + ").");



    // pre-process source II)
    System.out.print("Preprocess source step B ... ");

    File lPreprocessedSourceFile2 = File.createTempFile("source.", ".c");
    lPreprocessedSourceFile2.deleteOnExit();

    BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(lPreprocessedSourceFile)));

    PrintWriter lWriter = new PrintWriter(new FileOutputStream(lPreprocessedSourceFile2));

    String lLine = null;

    while ((lLine = lReader.readLine()) != null) {
      if (!lLine.startsWith("# ")) {
        lWriter.println(lLine);
      }
    }

    lReader.close();
    lWriter.close();

    System.out.println("done.");



    // compile source
    System.out.print("Compile source ... ");

    //File lObjectFile = File.createTempFile("source.", ".o");
    File lObjectFile = new File(lPreprocessedSourceFile2.getAbsolutePath().substring(0, lPreprocessedSourceFile2.getAbsolutePath().length() - 1)  + "o");
    lObjectFile.deleteOnExit();

    LinkedList<String> lCompileSource = new LinkedList<String>();
    lCompileSource.add("gcc");
    lCompileSource.add("-c");
    lCompileSource.add("-g");
    lCompileSource.add("-fprofile-arcs");
    lCompileSource.add("-ftest-coverage");
    lCompileSource.add("-O0");
    lCompileSource.add("-o");
    lCompileSource.add(lObjectFile.getAbsolutePath());
    lCompileSource.add(lPreprocessedSourceFile2.getAbsolutePath());

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
    lCompileApplication.add("-O0");
    lCompileApplication.add("-o");
    lCompileApplication.add(lTmpExecutable.getAbsolutePath());
    lCompileApplication.add(lObjectFile.getAbsolutePath());
    lCompileApplication.add(lTmpInputO.getAbsolutePath());
    lBuilder.command(lCompileApplication);

    Process lCompileApplicationProcess = lBuilder.start();
    readInputStream(lCompileApplicationProcess.getInputStream());
    lReturnCode = lCompileApplicationProcess.waitFor();

    if (lReturnCode != 0) {
      System.err.println("failed (" + lReturnCode + ").");

      return;
    }
    else {
      System.out.println("done (" + lReturnCode + ").");
    }



    // run testsuite
    System.out.print("Run testsuite ");

    TestSuite lTestSuite = TestSuite.load(lTestsuiteFileName);

    for (TestCase lTestCase : lTestSuite) {
      lTestCase.toInputFile(lTmpTestCaseFile);

      LinkedList<String> lRunApplication = new LinkedList<String>();
      lRunApplication.add(lTmpExecutable.getAbsolutePath());
      lBuilder.command(lRunApplication);

      Process lExecutionProcess = lBuilder.start();
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
    lEvaluateCoverage.add(lPreprocessedSourceFile2.getAbsolutePath());

    lBuilder.directory(lPreprocessedSourceFile2.getParentFile().getAbsoluteFile());
    lBuilder.command(lEvaluateCoverage);
    Process lEvaluationProcess = lBuilder.start();
    readInputStream(lEvaluationProcess.getInputStream());

    lReturnCode = lEvaluationProcess.waitFor();
    System.out.println("done (" + lReturnCode + ").");

    File lGCovFile = new File(lPreprocessedSourceFile2.getAbsolutePath() + ".gcov");
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
