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
package org.sosy_lab.cpachecker.fshell.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.fshell.testcases.KLEEToFShell3;
import org.sosy_lab.cpachecker.fshell.testcases.MeasureGCovCoverage;
import org.sosy_lab.cpachecker.fshell.testcases.TestSuite;

public class RunKLEE {

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length == 0) {
      throw new RuntimeException();
    }

    String lSourceFileName = args[0];

    File lSourceFile = new File(lSourceFileName);

    File lTemporaryDirectory = new File("tmp-testing");

    if (lTemporaryDirectory.exists()) {
      throw new RuntimeException("Temporary directory 'tmp-testing' already exists!");
    }

    if (!lTemporaryDirectory.mkdir()) {
      throw new RuntimeException("Couldn't create temporary directory 'tmp-testing'!");
    }


    String lFilename = lSourceFile.getName();
    int lFilenameLength = lFilename.length();

    // we assume the source file ends with .c
    if (!lFilename.substring(lFilenameLength - 2, lFilenameLength).equals(".c")) {
      throw new RuntimeException();
    }

    String lBasename = lFilename.substring(0, lFilenameLength - 2);

    File lKleeFile = new File(lTemporaryDirectory, lBasename + "-klee.i");

    List<String> lCommand = new LinkedList<String>();

    lCommand.add("gcc");
    lCommand.add("-E");
    lCommand.add("-D__KLEE__");
    lCommand.add(lSourceFile.getAbsolutePath());
    lCommand.add("-o");
    lCommand.add(lKleeFile.getAbsolutePath());

    ProcessBuilder lBuilder = new ProcessBuilder();
    lBuilder.command(lCommand);
    lBuilder.redirectErrorStream(true);

    Process lGCC = lBuilder.start();

    MeasureGCovCoverage.readInputStream(lGCC.getInputStream());

    int lReturnValue = lGCC.waitFor();

    if (lReturnValue != 0) {
      throw new RuntimeException();
    }

    lCommand.clear();
    lCommand.add("llvm-gcc");
    lCommand.add("--emit-llvm");
    lCommand.add("-g");
    lCommand.add("-c");
    lCommand.add(lKleeFile.getAbsolutePath());
    lCommand.add("-o");
    lCommand.add(lBasename + "-klee.bc");

    lBuilder.command(lCommand);
    lBuilder.directory(lTemporaryDirectory);

    Process lLlvmGcc = lBuilder.start();

    MeasureGCovCoverage.readInputStream(lLlvmGcc.getInputStream());

    lReturnValue = lLlvmGcc.waitFor();

    if (lReturnValue != 0) {
      throw new RuntimeException();
    }

    lCommand.clear();
    lCommand.add("klee");
    lCommand.add("-max-time=600");
    lCommand.add(lBasename + "-klee.bc");

    lBuilder.command(lCommand);

    Process lRunKlee = lBuilder.start();

    MeasureGCovCoverage.readInputStream(lRunKlee.getInputStream());

    lReturnValue = lRunKlee.waitFor();

    if (lReturnValue != 0) {
      throw new RuntimeException();
    }

    String lTestSuiteDirectory = lTemporaryDirectory.getAbsolutePath() + File.separator + "klee-out-0";

    System.out.println(lTestSuiteDirectory);

    File lOutputDirectory = new File(lTestSuiteDirectory);
    for (File lFile : lOutputDirectory.listFiles()) {
      if (lFile.getName().endsWith(".ktest")) {
        lCommand.clear();
        lCommand.add("ktest-tool");
        //lCommand.add("--write-ints");
        lCommand.add(lFile.getAbsolutePath());

        lBuilder.directory(lOutputDirectory);

        Process lTranslationProcess = lBuilder.start();

        BufferedReader lReader = new BufferedReader(new InputStreamReader(lTranslationProcess.getInputStream()));

        File lOutputFile = new File(lFile.getAbsolutePath() + ".txt");

        PrintWriter lWriter = new PrintWriter(new BufferedWriter(new FileWriter(lOutputFile)));

        String lLine = null;

        while ((lLine = lReader.readLine()) != null) {
          lWriter.println(lLine);
        }

        lWriter.close();
      }
    }


    TestSuite lGeneratedTestSuite = KLEEToFShell3.translateTestSuite(lTestSuiteDirectory);
    System.out.println(lGeneratedTestSuite);

    if (lGeneratedTestSuite.isEmpty()) {
      throw new RuntimeException("Empty test suite!");
    }

    File lTemporaryTestSuiteFile = File.createTempFile("testsuite.", ".tst");
    lTemporaryTestSuiteFile.deleteOnExit();

    lGeneratedTestSuite.write(lTemporaryTestSuiteFile);

    String[] lArguments = new String[2];
    lArguments[0] = lSourceFileName;
    lArguments[1] = lTemporaryTestSuiteFile.getAbsolutePath();

    MeasureGCovCoverage.main(lArguments);

    // cleanup
    if (!lTemporaryDirectory.delete()) {
      throw new RuntimeException();
    }
  }

}
