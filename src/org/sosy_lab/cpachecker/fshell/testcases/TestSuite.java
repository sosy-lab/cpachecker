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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class TestSuite implements Iterable<TestCase> {

  private HashSet<TestCase> mTestSuite;

  public TestSuite() {
    mTestSuite = new HashSet<TestCase>();
  }

  public void add(TestSuite lTestSuite) {
    if (lTestSuite != this) {
      for (TestCase lTestCase : lTestSuite) {
        add(lTestCase);
      }
    }
  }

  public boolean add(TestCase pTestCase) {
    return mTestSuite.add(pTestCase);
  }

  public static TestSuite load(String pString) throws IOException {
    TestSuite lTestSuite = new TestSuite();

    lTestSuite.mTestSuite.addAll(TestCase.fromFile(pString));

    return lTestSuite;
  }

  public void write(String pTestSuiteOutputFilename) throws FileNotFoundException {
    write(new File(pTestSuiteOutputFilename));
  }

  public void write(File pTestSuiteOutputFile) throws FileNotFoundException {
    TestCase.toFile(mTestSuite, pTestSuiteOutputFile);
  }

  @Override
  public Iterator<TestCase> iterator() {
    return mTestSuite.iterator();
  }

}
