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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;


public class TestSuite {
  private Map<TestCase, List<Goal>> mapping;
  private Map<Goal, Region> infeasibleGoals;
  private NamedRegionManager bddCpaNamedRegionManager;
  private int numberOfFeasibleGoals = 0;
  private Map<Integer, Goal> timedOutGoals;

  public TestSuite(NamedRegionManager pBddCpaNamedRegionManager) {
    mapping = new HashMap<>();
    infeasibleGoals = new HashMap<>();
    bddCpaNamedRegionManager = pBddCpaNamedRegionManager;
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

  public void addTimedOutGoal(int index, Goal goal) {
    timedOutGoals.put(index, goal);
  }

  public Map<Integer, Goal> getTimedOutGoals() {
    return timedOutGoals;
  }

  public boolean isInfeasible(Goal goal) {
    return infeasibleGoals.containsKey(goal);
  }

  public Map<TestCase, List<Goal>> getMapping() {
    return mapping;
  }

  public Map<Goal, Region> getInfeasibleGoals() {
    return infeasibleGoals;
  }

  /** States that the goal is infeasible when enforcing the given constraints
   */
  public void addInfeasibleGoal(Goal goal, Region pForConstraints) {
    if (infeasibleGoals.containsKey(goal)) {
      Region constraints = infeasibleGoals.get(goal);
      infeasibleGoals.put(goal, bddCpaNamedRegionManager.makeOr(constraints, pForConstraints));
    } else {
      infeasibleGoals.put(goal, pForConstraints);
    }
  }

  public boolean addTestCase(TestCase testcase, Goal goal) {
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

  public Set<TestCase> getTestCases() {
    return mapping.keySet();
  }

  public int getNumberOfTestCases() {
    return getTestCases().size();
  }

  @Override
  public String toString() {
    StringBuffer str = new StringBuffer();

    for (Map.Entry<TestCase, List<Goal>> entry : mapping.entrySet()) {
      str.append("Testcase ");
      str.append(entry.getKey().toString());
      str.append(" covers\n");

      for (Goal goal : entry.getValue()) {
        str.append("Goal ");
        str.append(goal.getIndex());
        str.append(" ");
        str.append(goal.toSkeleton());
        str.append(" with targetPC ");
        str.append(bddCpaNamedRegionManager.dumpRegion(goal.getPresenceCondition()));
        str.append("\n");
      }

      str.append("\n");
    }

    str.append("infeasible:\n");

    for (Entry<Goal, Region> entry : infeasibleGoals.entrySet()) {
      str.append("Goal ");
      str.append(entry.getKey().getIndex());
      str.append(" ");
      str.append(entry.getKey().toSkeleton());
      str.append(" with targetPC ");
      str.append(bddCpaNamedRegionManager.dumpRegion(entry.getKey().getPresenceCondition()));
      str.append("\n\tcannot be covered with PC ");
      str.append(bddCpaNamedRegionManager.dumpRegion(entry.getValue()));
      str.append("\n");
    }

    str.append("\n");

    if (!timedOutGoals.isEmpty()) {
      str.append("timed out:\n");

      for (Goal goal : timedOutGoals.values()) {
        //str.append(goal.getIndex());
        str.append(goal.toSkeleton());
        str.append("\n");
      }

      str.append("\n");
    }

    return str.toString();
  }
}
