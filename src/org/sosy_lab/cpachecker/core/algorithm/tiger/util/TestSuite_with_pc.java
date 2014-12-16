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

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal_with_pc;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class TestSuite_with_pc {

  private Map<TestCase_with_pc, List<Goal_with_pc>> mapping;
  private Map<Goal_with_pc, Region> infeasibleGoals;
  private Map<Integer, Pair<Goal_with_pc, Region>> timedOutGoals;
  private int numberOfFeasibleGoals = 0;
  private NamedRegionManager bddCpaNamedRegionManager;

  public TestSuite_with_pc(NamedRegionManager pBddCpaNamedRegionManager) {
    mapping = new HashMap<>();
    infeasibleGoals = new HashMap<>();
    timedOutGoals = new HashMap<>();
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

  public void addTimedOutGoal(int index, Goal_with_pc goal, Region region) {
    timedOutGoals.put(index, Pair.of(goal, region));
  }

  public Map<Integer, Pair<Goal_with_pc, Region>> getTimedOutGoals() {
    return timedOutGoals;
  }

  public Map<Goal_with_pc, Region> getInfeasibleGoals() {
    return infeasibleGoals;
  }

  /** States that the goal is infeasible when enforcing the given constraints
   */
  public void addInfeasibleGoal(Goal_with_pc goal, Region pForConstraints) {
    assert (pForConstraints != null);
    if (infeasibleGoals.containsKey(goal)) {
      Region constraints = infeasibleGoals.get(goal);
      infeasibleGoals.put(goal, bddCpaNamedRegionManager.makeOr(constraints, pForConstraints));
    } else {
      infeasibleGoals.put(goal, pForConstraints);
    }
  }

  public boolean addTestCase(TestCase_with_pc testcase, Goal_with_pc goal) {
    numberOfFeasibleGoals++;
    List<Goal_with_pc> goals = mapping.get(testcase);
    boolean testcaseExisted = true;
    if (goals == null) {
      goals = new LinkedList<>();
      mapping.put(testcase, goals);
      testcaseExisted = false;
    }
    goals.add(goal);
    return testcaseExisted;
  }

  public Set<TestCase_with_pc> getTestCases() {
    return mapping.keySet();
  }

  public int getNumberOfTestCases() {
    return getTestCases().size();
  }

  @Override
  public String toString() {
    StringBuffer str = new StringBuffer();
    for (Entry<TestCase_with_pc, List<Goal_with_pc>> entry : mapping.entrySet()) {
      str.append("Testcase ");
      str.append(entry.getKey().toString());
      str.append(" covers\n");
      for (Goal_with_pc goal : entry.getValue()) {
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
    for (Entry<Goal_with_pc, Region> entry : infeasibleGoals.entrySet()) {
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
      for (Pair<Goal_with_pc, Region> goal : timedOutGoals.values()) {
        //str.append(goal.getIndex());
        str.append(goal.getFirst().toSkeleton());
        str.append("\n");
      }
      str.append("\n");
    }
    return str.toString();
  }
  /**
   * Summarizes the presence conditions of tests in this testsuite that cover the parameter test goal.
   */
  public Region getGoalCoverage(Goal_with_pc pGoal) {
    Region totalCoverage = bddCpaNamedRegionManager.makeFalse();
    for (Entry<TestCase_with_pc, List<Goal_with_pc>> entry : this.mapping.entrySet()) {
      if (entry.getValue().contains(pGoal)) {
        assert entry.getKey().getRegion()!=null;
        totalCoverage = bddCpaNamedRegionManager.makeOr(totalCoverage, entry.getKey().getRegion());
      }
    }
    return totalCoverage;
  }

  public boolean isKnownAsInfeasible(Goal goal) {
    return infeasibleGoals.containsKey(goal);
  }
}
