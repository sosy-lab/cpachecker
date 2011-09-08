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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class CRESTToFShell3 {

  public static TestSuite translateTestSuite(String pDirectoryName) throws IOException {
    return translateTestSuite(new File(pDirectoryName));
  }

  public static TestSuite translateTestSuite(File pDirectory) throws IOException {
    if (!pDirectory.isDirectory()) {
      throw new IllegalArgumentException("Given file " + pDirectory.getAbsolutePath() + " is not a directory!");
    }

    TestSuite lTestSuite = new TestSuite();

    for (File lFile : pDirectory.listFiles()) {
      if (lFile.getName().matches("input\\.(\\d)*") && !lFile.getName().equals("input.1")) { // input.1 does not contain any data (always?)
        lTestSuite.add(translate(lFile));
      }
    }

    return lTestSuite;
  }

  public static TestCase translate(String pInputFileName) throws IOException {
    return translate(new File(pInputFileName));
  }

  public static TestCase translate(File pInputFile) throws IOException {
    return translate(new FileInputStream(pInputFile));
  }

  public static TestCase translate(InputStream pInputStream) throws IOException {
    BufferedReader lReader = new BufferedReader(new InputStreamReader(pInputStream));

    List<Integer> lInputValues = new LinkedList<Integer>();

    String lLine = null;

    while ((lLine = lReader.readLine()) != null) {
      lLine = lLine.trim();

      if (!lLine.equals("")) {
        lInputValues.add(Integer.valueOf(lLine));
      }
    }

    TestCase lTestCase = new PreciseInputsTestCase(lInputValues);

    return lTestCase;
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: java " + CRESTToFShell3.class.getCanonicalName() + " <crest-output-file/directory>");

      return;
    }

    String lTestCaseFile = args[0];

    File lFile = new File(args[0]);

    if (lFile.isDirectory()) {
      TestSuite lTestSuite = translateTestSuite(lFile);
      System.out.println(lTestSuite);
    }
    else {
      TestCase lFShell3TestCase = translate(lTestCaseFile);
      System.out.println(lFShell3TestCase);
    }
  }
}
