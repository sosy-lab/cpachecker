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

import java.math.BigInteger;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;


public class TestCase {

  private int id;
  private List<BigInteger> inputs;
  private List<TestStep> testSteps;
  private ARGPath argPath;
  private List<CFAEdge> errorPath;
  private NamedRegionManager bddCpaNamedRegionManager;
  private Region presenceCondition;
  private long generationTime;
  private int numberOfNewlyCoveredGoals;
  private int numberOfNewlyPartiallyCoveredGoals;

  public TestCase(int pId, List<TestStep> pTestSteps, ARGPath pArgPath, List<CFAEdge> pList, Region pPresenceCondition,
      NamedRegionManager pBddCpaNamedRegionManager, long pGenerationTime, List<BigInteger> pInputs) {
    id = pId;
    testSteps = pTestSteps;
    argPath = pArgPath;
    errorPath = pList;
    bddCpaNamedRegionManager = pBddCpaNamedRegionManager;
    presenceCondition = pPresenceCondition;
    generationTime = pGenerationTime;
    inputs = pInputs;
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

  public Region getPresenceCondition() {
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

  public List<BigInteger> getInputs() {
    return inputs;
  }

  public String toCode() {
    String str = "int input() {\n  static int index = 0;\n  switch (index) {\n";

    int index = 0;
    for (BigInteger input : inputs) {
      str += "  case " + index + ":\n    index++;\n    return " + input + ";\n";
      index++;
    }

    str += "  default:\n    return 0;\n  }\n}\n";

    return str;
  }

  @Override
  public String toString() {
    String returnStr = inputs.toString();

    if (presenceCondition != null) {
      returnStr += " with configurations " + bddCpaNamedRegionManager.dumpRegion(getPresenceCondition());
    }

    return returnStr;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TestCase) {
      TestCase other = (TestCase) o;
      return (id == other.getId()) && (inputs.equals(other.inputs) && errorPath.equals(other.errorPath)
          && (presenceCondition != null ? presenceCondition.equals(other.getPresenceCondition()) : true));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 38495 + 33 * inputs.hashCode() + 13 * errorPath.hashCode()
        + (presenceCondition != null ? 25 * presenceCondition.hashCode() : 0);
  }

}
