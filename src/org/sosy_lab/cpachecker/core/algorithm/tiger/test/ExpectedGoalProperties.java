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
package org.sosy_lab.cpachecker.core.algorithm.tiger.test;

import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;

public class ExpectedGoalProperties {

  public enum Comparators {
    LE,
    LT,
    EQ,
    GE,
    GT
  }

  public class RelativeVariableProperty {

    private String variable1;

    private String variable2;

    private Comparators comp;

    public RelativeVariableProperty(String pV1, String pV2, Comparators pComp) {
      super();
      variable1 = pV1;
      variable2 = pV2;
      comp = pComp;
    }

    public boolean checkProperty(Map<String, BigInteger> listToCheck) {
      BigInteger value1 = listToCheck.get(variable1);
      BigInteger value2 = listToCheck.get(variable2);

      if (value1 == null || value2 == null) { return false; }

      switch (comp) {
        case EQ:
          return value1.compareTo(value2) == 0;
        case LE:
          return value1.compareTo(value2) <= 0;
        case LT:
          return value1.compareTo(value2) < 0;
        case GE:
          return value1.compareTo(value2) >= 0;
        case GT:
          return value1.compareTo(value2) > 0;
        default:
          return true;
      }
    }

    @Override
    public String toString() {
      switch (comp) {
        case EQ:
          return variable1 + " == " + variable2;
        case LE:
          return variable1 + " <= " + variable2;
        case LT:
          return variable1 + " < " + variable2;
        case GE:
          return variable1 + " >= " + variable2;
        case GT:
          return variable1 + " > " + variable2;
        default:
          return "incorrect property";
      }
    }
  }

  class ConcreteVariableProperty {

    private String variable;

    private BigInteger expectedValue;

    private Comparators comp;

    ConcreteVariableProperty(String pVariable, BigInteger pExpectedValue,
        Comparators pComp) {
      super();
      variable = pVariable;
      expectedValue = pExpectedValue;
      comp = pComp;
    }

    boolean checkProperty(Map<String, BigInteger> listToCheck) {
      BigInteger value = listToCheck.get(variable);

      if (value == null) { return false; }

      switch (comp) {
        case EQ:
          return value.compareTo(expectedValue) == 0;
        case LE:
          return value.compareTo(expectedValue) <= 0;
        case LT:
          return value.compareTo(expectedValue) < 0;
        case GE:
          return value.compareTo(expectedValue) >= 0;
        case GT:
          return value.compareTo(expectedValue) > 0;
        default:
          return true;
      }
    }

    @Override
    public String toString() {
      switch (comp) {
        case EQ:
          return variable + " == " + expectedValue;
        case LE:
          return variable + " <= " + expectedValue;
        case LT:
          return variable + " < " + expectedValue;
        case GE:
          return variable + " >= " + expectedValue;
        case GT:
          return variable + " > " + expectedValue;
        default:
          return "incorrect property";
      }
    }
  }

  private String goalName;

  private List<RelativeVariableProperty> relativeVariableProperties;

  private List<ConcreteVariableProperty> concreteVariableProperties;

  private boolean isFeasible;

  public ExpectedGoalProperties(String pGoalName, boolean pFeasible) {
    goalName = pGoalName;
    isFeasible = pFeasible;
    relativeVariableProperties = Lists.newLinkedList();
  }

  public boolean checkProperties(TestSuite testSuite) {
    Goal goal = testSuite.getGoalByName(goalName);

    if (goal == null) { throw new AssertionError(
        "Expected goal " + goalName + " not included in testSuite!"); }

    if (isFeasible && testSuite.isInfeasible(goal)) { throw new AssertionError(
        "Goal " + goalName + " should be feasible but was found infeasible!"); }

    if (!isFeasible && testSuite.isGoalCovered(goal)) { throw new AssertionError(
        "Goal " + goalName + " should be infeasible but was found fasible!"); }

    List<TestCase> testCases = testSuite.getCoveringTestCases(goal);

    for (TestCase testCase : testCases) {
      Map<String, BigInteger> inputs = testCase.getInputs();
      Map<String, BigInteger> outputs = testCase.getOutputs();

      for (RelativeVariableProperty r : relativeVariableProperties) {
        if (!r.checkProperty(inputs)) { throw new AssertionError(
            "Expected input property (" + r.toString() + ") for goal " + goalName
                + " is not fullfilled in testCase " + testCase.getId()); }
        if (!r.checkProperty(outputs)) { throw new AssertionError(
            "Expected output property (" + r.toString() + ") for goal " + goalName
                + " is not fullfilled in testCase " + testCase.getId()); }
      }

      for (ConcreteVariableProperty r : concreteVariableProperties) {
        if (!r.checkProperty(inputs)) { throw new AssertionError(
            "Expected input property (" + r.toString() + ") for goal " + goalName
                + " is not fullfilled in testCase " + testCase.getId()); }
        if (!r.checkProperty(outputs)) { throw new AssertionError(
            "Expected output property (" + r.toString() + ") for goal " + goalName
                + " is not fullfilled in testCase " + testCase.getId()); }
      }
    }
    return true;
  }

  public void addRelativeValueProperty(RelativeVariableProperty relativeVariableProperty) {
    relativeVariableProperties.add(relativeVariableProperty);
  }

  public void addConcreteValueProperty(ConcreteVariableProperty concreteVariableProperty) {
    concreteVariableProperties.add(concreteVariableProperty);
  }
}



