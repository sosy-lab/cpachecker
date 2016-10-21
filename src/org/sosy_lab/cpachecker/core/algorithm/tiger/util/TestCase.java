/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;

import javax.management.JMException;

import java.util.List;


public class TestCase {

  private static int testCaseIdSeq = 0;

  private static synchronized int getNextTestCaseId() {
    return testCaseIdSeq++;
  }

  private static long getCurrentCpuTimeMillis()  {
    try {
      return (long) (ProcessCpuTime.read() / 1e6);
    } catch (JMException pE) {
      throw new RuntimeException(pE);
    }
  }

  private int id;
  private long generationTime;
  private int numberOfNewlyCoveredGoals;
  private int numberOfNewlyPartiallyCoveredGoals;

  private List<TestStep> testSteps;
  private ARGPath argPath;
  private List<CFAEdge> errorPath;
  private PresenceCondition presenceCondition;

  public TestCase(List<TestStep> pTestSteps, ARGPath pArgPath,
      List<CFAEdge> pList, PresenceCondition pPresenceCondition) {
    this(getNextTestCaseId(), pTestSteps, pArgPath, pList, pPresenceCondition, getCurrentCpuTimeMillis());
  }

  public TestCase(int pId, List<TestStep> pTestSteps, ARGPath pArgPath,
      List<CFAEdge> pList, PresenceCondition pPresenceCondition, long pGenerationTime) {

    id = pId;
    testSteps = pTestSteps;
    argPath = pArgPath;
    errorPath = pList;
    presenceCondition = pPresenceCondition;
    generationTime = pGenerationTime;
  }

  public int getId() {
    return id;
  }

  public ARGPath getArgPath() {
    return argPath;
  }

  public List<CFAEdge> getErrorPath() {
    return errorPath;
  }

  public List<TestStep> getTestSteps() {
    return testSteps;
  }

  public PresenceCondition getPresenceCondition() {
    return presenceCondition;
  }

  public long getGenerationTime() {
    return generationTime;
  }

  public int getNumberOfNewlyCoveredGoals() {
    return numberOfNewlyCoveredGoals;
  }

  public void incrementNumberOfNewlyCoveredGoals() {
    numberOfNewlyCoveredGoals++;
  }

  public int getNumberOfNewlyPartiallyCoveredGoals() {
    return numberOfNewlyPartiallyCoveredGoals;
  }

  public void incrementNumberOfNewlyPartiallyCoveredGoals() {
    numberOfNewlyPartiallyCoveredGoals++;
  }

  public String toCode() {
    String str = "int input() {\n  static int index = 0;\n  switch (index) {\n";

//    int index = 0;
//    for (BigInteger input : inputs) {
//      str += "  case " + index + ":\n    index++;\n    return " + input + ";\n";
//      index++;
//    }

    str += "  default:\n    return 0;\n  }\n}\n";

    return str;
  }

  @Override
  public String toString() {
    String returnStr = "[]";

    if (presenceCondition != null) {
      returnStr += " with configurations "
          + getPresenceCondition();
    }

    return returnStr;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TestCase) {
      TestCase other = (TestCase) o;
      return (id == other.getId()) && (testSteps.equals(other.testSteps) && errorPath.equals(other.errorPath)
          && (presenceCondition != null ? presenceCondition.equals(other.getPresenceCondition()) : true));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 38495 + 33 * testSteps.hashCode() + 13 * errorPath.hashCode()
        + (presenceCondition != null ? 25 * presenceCondition.hashCode() : 0);
  }

}
