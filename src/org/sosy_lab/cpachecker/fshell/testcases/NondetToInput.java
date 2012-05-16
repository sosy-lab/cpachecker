/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.fshell.FShell3;
import org.sosy_lab.cpachecker.fshell.FShell3Result;

public class NondetToInput {

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("Usage: java org.sosy_lab.cpachecker.fshell.testcases.NondetToInput <source-file> <destination-file>");

      return;
    }

    NondetToInput.replace(args[0], args[1]);
  }

  public static void replace(String pSourceFile, String pDestinationFile) throws IOException {
    BufferedReader lReader = new BufferedReader(new FileReader(pSourceFile));
    PrintWriter lWriter = new PrintWriter(pDestinationFile);

    // add a declaration of function input
    lWriter.println("int input(void);");
    lWriter.println();

    Pattern lDeclarationPattern = Pattern.compile("\\s*int\\s*__BLAST_NONDET\\s*;\\s*([/][/])?.*");

    Pattern lDeclarationPattern2 = Pattern.compile("\\s*[{]\\s*int\\s*__BLAST_NONDET\\s*;\\s*([/][/])?.*");

    Pattern lAssignmentPattern = Pattern.compile(".*=\\s*__BLAST_NONDET\\s*;\\s*([/][/])?.*");

    Pattern lAssignmentPattern2 = Pattern.compile(".*=\\s*[(]\\s*[l][o][n][g]\\s*[)]\\s*__BLAST_NONDET\\s*;\\s*([/][/])?.*");

    Pattern lAssignmentPattern3 = Pattern.compile(".*=\\s*[(]\\s*[u][n][s][i][g][n][e][d]\\s+[l][o][n][g]\\s*[)]\\s*__BLAST_NONDET\\s*;\\s*([/][/])?.*");

    Pattern lLinePattern = Pattern.compile("#line .*");

    String lLine;

    while ((lLine = lReader.readLine()) != null) {
      if (lDeclarationPattern.matcher(lLine).matches() || lLinePattern.matcher(lLine).matches()) {
        // if pLine matches int __BLAST_NONDET; remove this line
      }
      else if (lDeclarationPattern2.matcher(lLine).matches()) {
        lWriter.println("{");
      }
      else if (lAssignmentPattern.matcher(lLine).matches()) {
        // if pLine matches ... = __BLAST_NONDET; replace it with ... = input();
        String[] lParts = lLine.split("=");

        lWriter.println(lParts[0] + "= input();");
      }
      else if (lAssignmentPattern2.matcher(lLine).matches()) {
        // if pLine matches ... = (long) __BLAST_NONDET; replace it with ... = (long) input();
        String[] lParts = lLine.split("=");

        lWriter.println(lParts[0] + "= (long)input();");
      }
      else if (lAssignmentPattern3.matcher(lLine).matches()) {
        // if pLine matches ... = (unsigned long) __BLAST_NONDET; replace it with ... = (unsigned long) input();
        String[] lParts = lLine.split("=");

        lWriter.println(lParts[0] + "= (unsigned long)input();");
      }
      else {
        // else write pLine to lWriter
        lWriter.println(lLine);
      }
    }

    lWriter.close();
    lReader.close();
  }

  public static void gcov(String pSourceFileName, String pTestSuiteFileName) throws IOException, InterruptedException {
    gcov(pSourceFileName, TestCase.fromFile(pTestSuiteFileName));
  }

  public static void gcov(String pSourceFileName, Collection<TestCase> pTestSuite) throws IOException, InterruptedException {
    File lTmpSourceFile = File.createTempFile("source", ".c");
    lTmpSourceFile.deleteOnExit();

    NondetToInput.replace(pSourceFileName, lTmpSourceFile.getAbsolutePath());

    File lTmpExecutable = File.createTempFile("main", "");
    lTmpExecutable.deleteOnExit();
    lTmpExecutable.setWritable(true);
    lTmpExecutable.setExecutable(true);

    File lTmpInputObjectFile = File.createTempFile("input", ".o");
    lTmpInputObjectFile.deleteOnExit();
    lTmpInputObjectFile.setWritable(true);

    File lTmpInputFile = File.createTempFile("input", ".txt");
    lTmpInputFile.deleteOnExit();
    lTmpInputFile.setWritable(true);

    LinkedList<String> lCommand2 = new LinkedList<String>();
    lCommand2.add("/usr/bin/gcc");
    lCommand2.add("-c");
    lCommand2.add("-o");
    lCommand2.add(lTmpInputObjectFile.getAbsolutePath());
    lCommand2.add("-DINPUTFILE=\"" + lTmpInputFile.getAbsolutePath() + "\"");
    lCommand2.add("src/org/sosy_lab/cpachecker/fshell/testcases/input.c");

    ProcessBuilder lBuilder = new ProcessBuilder();
    lBuilder.redirectErrorStream(true);

    lBuilder.command(lCommand2);

    Process lProcess1 = lBuilder.start();

    printOutput(lProcess1);

    lProcess1.waitFor();

    // TODO implement checks

    lCommand2.clear();
    lCommand2.add("/usr/bin/gcc");
    lCommand2.add("-o");
    lCommand2.add(lTmpExecutable.getAbsolutePath());
    lCommand2.add("-fprofile-arcs");
    lCommand2.add("-ftest-coverage");
    lCommand2.add(lTmpSourceFile.getAbsolutePath());
    lCommand2.add(lTmpInputObjectFile.getAbsolutePath());

    lBuilder.command(lCommand2);

    lProcess1 = lBuilder.start();

    printOutput(lProcess1);

    lProcess1.waitFor();

    // TODO implement checks

    for (TestCase lTestCase : pTestSuite) {
      lTestCase.toInputFile(lTmpInputFile);

      lBuilder.command(lTmpExecutable.getAbsolutePath());

      lProcess1 = lBuilder.start();

      if (printOutput(lProcess1)) {
        System.err.println(lTestCase);
      }

      lProcess1.waitFor();
    }

    lCommand2.clear();
    lCommand2.add("/usr/bin/gcov");
    lCommand2.add("-b");
    lCommand2.add(lTmpSourceFile.getAbsolutePath());

    lBuilder.command(lCommand2);

    lProcess1 = lBuilder.start();

    printOutput(lProcess1);

    lProcess1.waitFor();

    File lTmpGCovFile = new File(lTmpSourceFile.getName() + ".gcov");
    lTmpGCovFile.delete();

    File lTmpGCdaFile = new File(lTmpSourceFile.getName().substring(0, lTmpSourceFile.getName().length() - 2) + ".gcda");
    lTmpGCdaFile.delete();

    File lTmpGCnoFile = new File(lTmpSourceFile.getName().substring(0, lTmpSourceFile.getName().length() - 2) + ".gcno");
    lTmpGCnoFile.delete();
  }

  private static boolean printOutput(Process pProcess) throws IOException {
    boolean lErrorOccured = false;

    BufferedReader lReader = new BufferedReader(new InputStreamReader(pProcess.getInputStream()));

    String lLine;

    while ((lLine = lReader.readLine()) != null) {
      if (lLine.startsWith("[ERROR] #")) {
        lErrorOccured = true;
        System.err.println(lLine);
      }
      else {
        System.out.println(lLine);
      }
    }

    return lErrorOccured;
  }


  public static void fshell2(String pSourceFile, String pEntryFunction, String pFQLQuery, int pLoopBound) throws IOException, InterruptedException, InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    /* a) prepare source file for FShell 2 test generation
     *    (e.g., replace __BLAST_NONDET by input())
     */
    File lTmpSourceFile = File.createTempFile("source", ".c");
    lTmpSourceFile.deleteOnExit();

    NondetToInput.replace(pSourceFile, lTmpSourceFile.getAbsolutePath());


    /* b) create a query file for FShell 2 */
    File lTmpQueryFile = File.createTempFile("query", ".fql");
    lTmpQueryFile.deleteOnExit();

    PrintWriter lWriter = new PrintWriter(lTmpQueryFile);
    lWriter.println("ADD SOURCECODE '" + lTmpSourceFile.getAbsolutePath() + "'");
    lWriter.println(pFQLQuery);
    lWriter.println("QUIT");

    lWriter.close();


    /* c) run FShell 2 */
    File lTmpOutputFile = File.createTempFile("testsuite", ".tst");
    lTmpOutputFile.deleteOnExit();

    LinkedList<String> lCommand = new LinkedList<String>();
    lCommand.add("/home/andreas/fshell2-1.2/fshell");
    lCommand.add("--query-file");
    lCommand.add(lTmpQueryFile.getAbsolutePath());
    lCommand.add("--outfile");
    lCommand.add(lTmpOutputFile.getAbsolutePath());
    lCommand.add("--unwind");
    lCommand.add("" + pLoopBound);
    lCommand.add("--no-unwinding-assertions");
    lCommand.add("--function");
    lCommand.add(pEntryFunction);

    ProcessBuilder lBuilder = new ProcessBuilder();
    lBuilder.command(lCommand);
    lBuilder.redirectErrorStream(true);

    Process lProcess = lBuilder.start();

    BufferedReader lReader = new BufferedReader(new InputStreamReader(lProcess.getInputStream()));

    String lLine;
    while ((lLine = lReader.readLine()) != null) {
      System.out.println(lLine);
    }

    lProcess.waitFor();


    /* d) translate FShell 2 test suite to FShell 3 test suite */
    File lTmpTestSuite = File.createTempFile("testsuite", ".tst");
    lTmpTestSuite.deleteOnExit();

    FShell2ToFShell3.translateTestsuite(lTmpOutputFile.getAbsolutePath(), lTmpTestSuite.getAbsolutePath());


    /* e) seed FShell 3 run with test suite */
    Collection<TestCase> lTestSuite = TestCase.fromFile(lTmpTestSuite.getAbsolutePath());

    System.out.println(lTestSuite);

    FShell3 lFShell3 = new FShell3(pSourceFile, pEntryFunction);
    lFShell3.seed(lTestSuite);
    //FShell3Result lResult = lFShell3.run("COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"");
    FShell3Result lResult = lFShell3.run("COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"");

    System.out.println("#Goals: " + lResult.getNumberOfTestGoals() + ", #Feas: " + lResult.getNumberOfFeasibleTestGoals() + ", #Infeas: " + lResult.getNumberOfInfeasibleTestGoals() + ", #Imprecise: " + lResult.getNumberOfImpreciseTestCases());
  }

}
