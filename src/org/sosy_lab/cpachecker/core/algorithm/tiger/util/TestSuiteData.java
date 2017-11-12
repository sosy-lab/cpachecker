/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.collect.Lists;
import java.util.List;

public class TestSuiteData {

  private int numberOfTestCases;

  private List<TestCaseData> testCases;

  private List<String> infeasibleGoals;

  private List<String> timedOutGoals;

  public TestSuiteData() {
    numberOfTestCases = -1;

    testCases = Lists.newLinkedList();

    infeasibleGoals = Lists.newLinkedList();

    timedOutGoals = Lists.newLinkedList();
  }

  public int getNumberOfTestCases() {
    return numberOfTestCases;
  }

  public void setNumberOfTestCases(int pNumberOfTestCases) {
    numberOfTestCases = pNumberOfTestCases;
  }

  public List<TestCaseData> getTestCases() {
    return testCases;
  }

  public void setTestCases(List<TestCaseData> pTestCases) {
    testCases = pTestCases;
  }

  public List<String> getInfeasibleGoals() {
    return infeasibleGoals;
  }

  public void setInfeasibleGoals(List<String> pInfeasibleGoals) {
    infeasibleGoals = pInfeasibleGoals;
  }

  public List<String> getTimedOutGoals() {
    return timedOutGoals;
  }

  public void setTimedOutGoals(List<String> pTimedOutGoals) {
    timedOutGoals = pTimedOutGoals;
  }

  @Override
  public String toString() {
    StringBuffer str = new StringBuffer();

    str.append("Number of Testcases: ").append(numberOfTestCases).append("\n\n");

    for (TestCaseData tc : testCases) {
      str.append(tc.toString());
    }

    if (!infeasibleGoals.isEmpty()) {
      str.append("infeasible:\n");

      for (String g : infeasibleGoals) {
        str.append("Goal ").append(g);
        str.append("\n");
      }
      str.append("\n");
    }

    if (!timedOutGoals.isEmpty()) {
      str.append("timed out:\n");

      for (String g : timedOutGoals) {
        str.append("Goal ").append(g);
        str.append("\n");
      }
      str.append("\n");
    }
    return str.toString();
  }

}
