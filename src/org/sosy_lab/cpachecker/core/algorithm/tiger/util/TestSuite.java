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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class TestSuite {

  private Map<TestCase, List<Goal>> mapping;
  private Map<Goal, Region> infeasibleGoals;
  private Map<Integer, Pair<Goal, Region>> timedOutGoals;
  private int numberOfFeasibleGoals = 0;
  private NamedRegionManager bddCpaNamedRegionManager;
  private boolean printLabels;

  public TestSuite(NamedRegionManager pBddCpaNamedRegionManager, boolean pPrintLabels) {
    mapping = new HashMap<>();
    infeasibleGoals = new HashMap<>();
    timedOutGoals = new HashMap<>();
    bddCpaNamedRegionManager = pBddCpaNamedRegionManager;
    printLabels = pPrintLabels;
  }

  public Set<Goal> getTestGoals() {
    Set<Goal> result = new HashSet<>();
    for (List<Goal> goalList : mapping.values()) {
      result.addAll(goalList);
    }
    return result;
  }

  public int getNumberOfFeasibleTestGoals() {
    return numberOfFeasibleGoals;
  }

  public int getNumberOfInfeasibleTestGoals() {
    return infeasibleGoals.size();
  }

  public int getNumberOfTimedoutTestGoals() {
    return timedOutGoals.size();
  }

  public boolean hasTimedoutTestGoals() {
    return !timedOutGoals.isEmpty();
  }

  public void addTimedOutGoal(int index, Goal goal, Region presenceCondition) {
    timedOutGoals.put(index, Pair.of(goal, presenceCondition));
  }

  public Map<Integer, Pair<Goal, Region>> getTimedOutGoals() {
    return timedOutGoals;
  }

  public void addInfeasibleGoal(Goal goal, Region presenceCondition) {
    if (presenceCondition != null && infeasibleGoals.containsKey(goal)) {
      Region constraints = infeasibleGoals.get(goal);
      infeasibleGoals.put(goal, bddCpaNamedRegionManager.makeOr(constraints, presenceCondition));
    } else {
      infeasibleGoals.put(goal, presenceCondition);
    }
  }

  public boolean addTestCase(TestCase testcase, Goal goal) {
    if (testSuiteAlreadyContrainsTestCase(testcase, goal)) { return true; }
    numberOfFeasibleGoals++;

    List<Goal> goals = mapping.get(testcase);

    boolean testcaseExisted = true;

    if (goals == null) {
      goals = new LinkedList<>();
      mapping.put(testcase, goals);
      testcaseExisted = false;
    }

    goals.add(goal);

    return testcaseExisted;
  }

  private boolean testSuiteAlreadyContrainsTestCase(TestCase pTestcase, Goal pGoal) {
    for (Entry<TestCase, List<Goal>> entry : mapping.entrySet()) {
      TestCase testCase = entry.getKey();
      if (testCase.equals(pTestcase)) {
        for (Goal goal : entry.getValue()) {
          if (goal.equals(pGoal)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  public Set<TestCase> getTestCases() {
    return mapping.keySet();
  }

  public int getNumberOfTestCases() {
    return getTestCases().size();
  }


  public Map<Goal, Region> getInfeasibleGoals() {
    return infeasibleGoals;
  }

  public List<Goal> getTestGoalsCoveredByTestCase(TestCase testcase) {
    return mapping.get(testcase);
  }

  @Override
  public String toString() {
    StringBuffer str = new StringBuffer();

    for (Map.Entry<TestCase, List<Goal>> entry : mapping.entrySet()) {
      str.append(entry.getKey().toString() + "\n");
      List<CFAEdge> errorPath = entry.getKey().getPath();
      if (errorPath != null) {
        str.append("Errorpath Length: " + entry.getKey().getPath().size() + "\n");
      }

      for (Goal goal : entry.getValue()) {
        str.append("Goal ");
        str.append(getTestGoalLabel(goal));

        Region presenceCondition = goal.getPresenceCondition();
        if (presenceCondition != null) {
          str.append(": " + bddCpaNamedRegionManager.dumpRegion(presenceCondition));
        }
        str.append("\n");
      }

      if (printLabels) {
        str.append("Labels:\n");
        for (CFAEdge edge : entry.getKey().getPath()) {
          CFANode predecessor = edge.getPredecessor();
          if (predecessor instanceof CLabelNode && !((CLabelNode) predecessor).getLabel().isEmpty()) {
            str.append(((CLabelNode) predecessor).getLabel());
            str.append("\n");
          }
        }
      }

      str.append("\n");
    }

    if (!infeasibleGoals.isEmpty()) {
      str.append("infeasible:\n");

      for (Entry<Goal, Region> entry : infeasibleGoals.entrySet()) {
        str.append("Goal ");
        str.append(getTestGoalLabel(entry.getKey()));

        Region presenceCondition = entry.getValue();
        if (presenceCondition != null) {
          str.append(": cannot be covered with PC ");
          str.append(bddCpaNamedRegionManager.dumpRegion(presenceCondition));
        }
        str.append("\n");
      }

      str.append("\n");
    }

    if (!timedOutGoals.isEmpty()) {
      str.append("timed out:\n");

      for (Entry<Integer, Pair<Goal, Region>> entry : timedOutGoals.entrySet()) {
        str.append("Goal ");
        str.append(getTestGoalLabel(entry.getValue().getFirst()));

        Region presenceCondition = entry.getValue().getSecond();
        if (presenceCondition != null) {
          str.append(": timed out for PC ");
          str.append(bddCpaNamedRegionManager.dumpRegion(presenceCondition));
        }
        str.append("\n");
      }

      str.append("\n");
    }

    return str.toString().replace("__SELECTED_FEATURE_", "");
  }

  /**
   * Returns the label of a test goal if there is one; otherwise the goal index will be returned.
   *
   * @param goal
   * @return
   */
  public String getTestGoalLabel(Goal goal) {
    String label = "";

    CFANode predecessor = goal.getCriticalEdge().getPredecessor();
    if (predecessor instanceof CLabelNode && !((CLabelNode) predecessor).getLabel().isEmpty()) {
      label = ((CLabelNode) predecessor).getLabel();
    } else {
      label = new Integer(goal.getIndex()).toString();
    }

    return label;
  }

  /**
   * Summarizes the presence conditions of tests in this testsuite that cover the parameter test goal.
   */
  public Region getGoalCoverage(Goal pGoal) {
    Region totalCoverage = bddCpaNamedRegionManager.makeFalse();
    for (Entry<TestCase, List<Goal>> entry : this.mapping.entrySet()) {
      if (entry.getValue().contains(pGoal)) {
        assert entry.getKey().getPresenceCondition() != null;
        totalCoverage = bddCpaNamedRegionManager.makeOr(totalCoverage, entry.getKey().getPresenceCondition());
      }
    }
    return totalCoverage;
  }

  public boolean isKnownAsInfeasible(Goal goal) {
    return infeasibleGoals.containsKey(goal);
  }

}
