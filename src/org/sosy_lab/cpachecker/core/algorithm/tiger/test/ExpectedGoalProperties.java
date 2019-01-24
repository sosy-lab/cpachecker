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
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.test.VariableProperty.GoalPropertyType;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCaseVariable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;

public class ExpectedGoalProperties {

  private String goalName;

  private List<VariableProperty> variableProperties;

  private boolean isFeasible;

  public ExpectedGoalProperties(String pGoalName, boolean pFeasible) {
    goalName = pGoalName;
    isFeasible = pFeasible;
    variableProperties = Lists.newLinkedList();
  }

  public <T extends Goal> boolean checkProperties(TestSuite<T> testSuite) {
    T goal = testSuite.getGoalByName(goalName);

    if (goal == null) { throw new AssertionError(
        "Expected goal " + goalName + " not included in testSuite!"); }

    if (isFeasible && testSuite.isInfeasible(goal)) { throw new AssertionError(
        "Goal " + goalName + " should be feasible but was found infeasible!"); }

    if (!isFeasible && testSuite.isGoalCovered(goal)) { throw new AssertionError(
        "Goal " + goalName + " should be infeasible but was found fasible!"); }

    if (testSuite.isInfeasible(goal)) { return true; }

    List<TestCase> testCases = testSuite.getCoveringTestCases(goal);

    for (TestCase testCase : testCases) {
      List<TestCaseVariable> inputs = testCase.getInputs();
      List<TestCaseVariable> outputs = testCase.getOutputs();
      List<TestCaseVariable> inputsAndOutputs = new ArrayList<>();
      inputsAndOutputs.addAll(inputs);
      inputsAndOutputs.addAll(outputs);
      for (VariableProperty r : variableProperties) {
        if (!r.checkProperty(inputs, GoalPropertyType.INPUT)) { throw new AssertionError(
            "Expected input property (" + r.toString() + ") for goal " + goalName
                + " is not fullfilled in testCase " + testCase.getId()); }
        if (!r.checkProperty(outputs, GoalPropertyType.OUTPUT)) { throw new AssertionError(
            "Expected output property (" + r.toString() + ") for goal " + goalName
                + " is not fullfilled in testCase " + testCase.getId()); }
        if (!r.checkProperty(inputsAndOutputs, GoalPropertyType.INPUTANDOUTPUT)) { throw new AssertionError(
            "Expected input/output property (" + r.toString() + ") for goal " + goalName
                + " is not fullfilled in testCase " + testCase.getId()); }
      }
      }

    return true;
  }

  public void addVariableProperty(VariableProperty variableProperty) {
    variableProperties.add(variableProperty);
  }
}



