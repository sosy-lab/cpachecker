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
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;


public class TestSuite {
  private Map<TestCase, List<Goal>> mapping;
  private List<Goal> infeasibleGoals;
  private Map<Integer, Goal> timedOutGoals;
  private int numberOfFeasibleGoals = 0;

  public TestSuite() {
    mapping = new HashMap<>();
    infeasibleGoals = new LinkedList<>();
    timedOutGoals = new HashMap<>();
  }

  public Set<Goal> getTestGoals(){
  Set<Goal> result = new HashSet<>();
    for(List<Goal> goalList : mapping.values()){
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

  public void addTimedOutGoal(int index, Goal goal) {
    timedOutGoals.put(index, goal);
  }

  public Map<Integer, Goal> getTimedOutGoals() {
    return timedOutGoals;
  }

  public void addInfeasibleGoal(Goal goal) {
    infeasibleGoals.add(goal);
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
      str.append(entry.getKey().toString());
      str.append("\n");

      for (Goal goal : entry.getValue()) {
        CFANode predecessor = goal.getCriticalEdge().getPredecessor();
        if (predecessor instanceof CLabelNode && !((CLabelNode) predecessor).getLabel().isEmpty()) {
          str.append(((CLabelNode) predecessor).getLabel());
        } else {
          str.append(goal.getIndex());
        }
        str.append(goal.toSkeleton());
        str.append("\n");
      }

      str.append("\n");
    }

    if (!infeasibleGoals.isEmpty()) {
      str.append("infeasible:\n");

      for (Goal goal : infeasibleGoals) {
        CFANode predecessor = goal.getCriticalEdge().getPredecessor();
        if (predecessor instanceof CLabelNode && !((CLabelNode) predecessor).getLabel().isEmpty()) {
          str.append(((CLabelNode) predecessor).getLabel());
        } else {
          str.append(goal.getIndex());
        }
        str.append(goal.toSkeleton());
        str.append("\n");
      }

      str.append("\n");
    }

    if (!timedOutGoals.isEmpty()) {
      str.append("timed out:\n");

      for (Goal goal : timedOutGoals.values()) {
        CFANode predecessor = goal.getCriticalEdge().getPredecessor();
        if (predecessor instanceof CLabelNode && !((CLabelNode) predecessor).getLabel().isEmpty()) {
          str.append(((CLabelNode) predecessor).getLabel());
        } else {
          str.append(goal.getIndex());
        }
        str.append(goal.toSkeleton());
        str.append("\n");
      }

      str.append("\n");
    }

    return str.toString();
  }
}
