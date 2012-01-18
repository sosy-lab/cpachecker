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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

// TODO refactor into interfaces instead of using inheritance
public class LoggingTestSuite extends TestSuite {

  private final TestSuite mTestSuite;
  private final PrintWriter mWriter;

  public LoggingTestSuite(TestSuite pTestSuite, String pTestSuiteFilename, boolean pAppend) throws FileNotFoundException {
    this(pTestSuite, new File(pTestSuiteFilename), pAppend);
  }

  @Override
  public void write(String pTestSuiteOutputFilename)
      throws FileNotFoundException {
    mTestSuite.write(pTestSuiteOutputFilename);
  }

  @Override
  public void write(File pTestSuiteOutputFile) throws FileNotFoundException {
    mTestSuite.write(pTestSuiteOutputFile);
  }

  @Override
  public Iterator<TestCase> iterator() {
    return mTestSuite.iterator();
  }

  public LoggingTestSuite(TestSuite pTestSuite, File pTestSuiteFile, boolean pAppend) throws FileNotFoundException {
    mTestSuite = pTestSuite;

    if (!pAppend) {
      mTestSuite.write(pTestSuiteFile);
    }

    mWriter = new PrintWriter(new FileOutputStream(pTestSuiteFile, true));
  }

  @Override
  public boolean add(TestCase pTestCase) {
    if (mTestSuite.add(pTestCase)) {
      mWriter.println(pTestCase);
      mWriter.flush();

      return true;
    }

    return false;
  }

  public void close() {
    mWriter.close();
  }

}
