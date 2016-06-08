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

import com.google.common.collect.Sets;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.InfeasibilityPropagation.Prediction;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestStep.VariableAssignment;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TestSuite extends AlgorithmResult {

  private int numberOfFeasibleGoals = 0;
  private boolean printLabels;
  boolean useTigerAlgorithm_with_pc;
  private long generationStartTime = 0;

  private Map<TestCase, List<Goal>> mapping;
  private Map<Goal, List<TestCase>> coveringTestCases;
  private Map<Pair<TestCase, Goal>, PresenceCondition> coveringPresenceConditions;
  private Set<Goal> testGoals;
  private Set<Goal> infeasibleGoals;
  private Map<Integer, Pair<Goal, PresenceCondition>> timedOutGoals;
  private Map<Goal, PresenceCondition> remainingPresenceConditions;
  private Map<Goal, PresenceCondition> infeasiblePresenceConditions;

  public TestSuite(boolean pPrintLabels, boolean pUseTigerAlgorithm_with_pc) {
    mapping = new HashMap<>();
    coveringTestCases = new HashMap<>();
    coveringPresenceConditions = new HashMap<>();
    testGoals = Sets.newLinkedHashSet();
    infeasibleGoals = Sets.newLinkedHashSet();
    timedOutGoals = new HashMap<>();
    printLabels = pPrintLabels;
    useTigerAlgorithm_with_pc = pUseTigerAlgorithm_with_pc;
    remainingPresenceConditions = new HashMap<>();
    infeasiblePresenceConditions = new HashMap<>();
  }

  private PresenceConditionManager pcm() {
    return PresenceConditions.manager();
  }

  public Set<Goal> getGoals() {
    return testGoals;
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

  public void addTimedOutGoal(Goal goal, PresenceCondition presenceCondition) {
    timedOutGoals.put(goal.getIndex(), Pair.of(goal, presenceCondition));
  }

  public Map<Integer, Pair<Goal, PresenceCondition>> getTimedOutGoals() {
    return timedOutGoals;
  }

  public PresenceCondition getRemainingPresenceCondition(Goal pGoal) {
    return PresenceConditions.orTrue(remainingPresenceConditions.get(pGoal));
  }

  public void setRemainingPresenceCondition(Goal pGoal, PresenceCondition presenceCondtion) {
    remainingPresenceConditions.put(pGoal, presenceCondtion);
  }

  public PresenceCondition getInfeasiblePresenceCondition(Goal pGoal) {
    return PresenceConditions.orFalse(infeasiblePresenceConditions.get(pGoal));
  }

  public void setInfeasiblePresenceCondition(Goal pGoal, PresenceCondition presenceCondtion) {
    infeasiblePresenceConditions.put(pGoal, presenceCondtion);
  }

  public List<TestCase> getCoveringTestCases(Goal pGoal) {
    return coveringTestCases.get(pGoal);
  }

  public boolean isGoalCoveredByTestCase(Goal pGoal, TestCase pTestCase) {
    return coveringTestCases.get(pGoal).contains(pTestCase);
  }

  public void addInfeasibleGoal(Goal goal, PresenceCondition presenceCondition, Prediction[] pGoalPrediction) {
    if (useTigerAlgorithm_with_pc) {
      if (presenceCondition != null && infeasibleGoals.contains(goal)) {
        setInfeasiblePresenceCondition(goal,
            pcm().makeOr(infeasiblePresenceConditions.get(goal), presenceCondition));
      } else {
        setInfeasiblePresenceCondition(goal, presenceCondition);
      }
    }

    if (!infeasibleGoals.contains(goal)) {
      infeasibleGoals.add(goal);
    }

    if (pGoalPrediction != null) {
      pGoalPrediction[goal.getIndex() - 1] = Prediction.INFEASIBLE;
    }
  }

  public boolean addTestCase(TestCase testcase, Goal goal, PresenceCondition pPresenceCondition) {
    if (testSuiteAlreadyContrainsTestCase(testcase, goal)) { return true; }
    if (!isGoalPariallyCovered(goal)) {
      numberOfFeasibleGoals++;
    }

    List<Goal> goals = mapping.get(testcase);
    List<TestCase> testcases = coveringTestCases.get(goal);

    boolean testcaseExisted = true;

    if (goals == null) {
      goals = new LinkedList<>();
      mapping.put(testcase, goals);
      testcaseExisted = false;
    }

    if (testcases == null) {
      testcases = new LinkedList<>();
      coveringTestCases.put(goal, testcases);
    }

    goals.add(goal);
    testcases.add(testcase);

    if (useTigerAlgorithm_with_pc) {
      coveringPresenceConditions.put(Pair.of(testcase, goal), pPresenceCondition);

      setRemainingPresenceCondition(goal,
          pcm().makeAnd(getRemainingPresenceCondition(goal),
              pcm().makeNegation(pPresenceCondition)));
    }

    return testcaseExisted;
  }

  private boolean isGoalPariallyCovered(Goal pGoal) {
    if (useTigerAlgorithm_with_pc) {
      if (pcm().checkEqualsFalse(remainingPresenceConditions.get(pGoal))) {
        return true;
      }
    }

    List<TestCase> testCases = coveringTestCases.get(pGoal);
    return (testCases != null && testCases.size() > 0);
  }

  private boolean testSuiteAlreadyContrainsTestCase(TestCase pTestcase, Goal pGoal) {
    List<Goal> goals = mapping.get(pTestcase);
    if (goals != null) { return goals.contains(pGoal); }

    return false;
  }

  public Set<TestCase> getTestCases() {
    return mapping.keySet();
  }

  public int getNumberOfTestCases() {
    return getTestCases().size();
  }


  public Set<Goal> getInfeasibleGoals() {
    return infeasibleGoals;
  }

  public List<Goal> getTestGoalsCoveredByTestCase(TestCase testcase) {
    return mapping.get(testcase);
  }

  public long getGenerationStartTime() {
    return generationStartTime;
  }

  public void setGenerationStartTime(long pGenerationStartTime) {
    generationStartTime = pGenerationStartTime;
  }

  @Override
  public String toString() {
    StringBuffer str = new StringBuffer();

    List<TestCase> testcases = new ArrayList<>(mapping.keySet());
    Collections.sort(testcases, new Comparator<TestCase>() {

      @Override
      public int compare(TestCase pTestCase1, TestCase pTestCase2) {
        if (pTestCase1.getGenerationTime() > pTestCase2.getGenerationTime()) {
          return 1;
        } else if (pTestCase1.getGenerationTime() < pTestCase2.getGenerationTime()) { return -1; }
        return 0;
      }
    });

    for (TestCase testcase : testcases) {
      str.append(testcase.toString() + "\n");

      List<TestStep> testSteps = testcase.getTestSteps();
      int cnt = 0;
      str.append("Test Steps:\n");
      for (TestStep testStep : testSteps) {
        str.append("  Test Step " + cnt + "\n");
        for (VariableAssignment assignment : testStep.getAssignments()) {
          str.append("    " + assignment + "\n");
        }
      }

      List<CFAEdge> errorPath = testcase.getErrorPath();
      if (testcase.getGenerationTime() != -1) {
        str.append("Generation Time: " + (testcase.getGenerationTime() - getGenerationStartTime()) + "\n");
      }
      if (errorPath != null) {
        str.append("Errorpath Length: " + testcase.getErrorPath().size() + "\n");
      }

      List<Goal> goals = mapping.get(testcase);
      for (Goal goal : goals) {
        str.append("Goal ");
        str.append(getTestGoalLabel(goal));

        PresenceCondition presenceCondition = coveringPresenceConditions.get(Pair.of(testcase, goal));
        if (presenceCondition != null) {
          str.append(": " + pcm().dump(presenceCondition).toString().replace("@0", "")
              .replace(" & TRUE", ""));
        }
        str.append("\n");
      }

      if (printLabels) {
        str.append("Labels: ");
        for (CFAEdge edge : testcase.getErrorPath()) {
          if (edge == null) {
            continue;
          }
          CFANode predecessor = edge.getPredecessor();
          if (predecessor instanceof CLabelNode && !((CLabelNode) predecessor).getLabel().isEmpty()) {
            str.append(((CLabelNode) predecessor).getLabel());
            str.append(" ");
          }
        }
        str.append("\n");
      }

      str.append("\n");
    }

    if (!infeasibleGoals.isEmpty()) {
      str.append("infeasible:\n");

      for (Goal entry : infeasibleGoals) {
        str.append("Goal ");
        str.append(getTestGoalLabel(entry));

        PresenceCondition presenceCondition = infeasiblePresenceConditions.get(entry);
        if (presenceCondition != null) {
          str.append(": cannot be covered with PC ");
          str.append(pcm().dump(presenceCondition).toString().replace("@0", "")
              .replace(" & TRUE", ""));
        }
        str.append("\n");
      }

      str.append("\n");
    }

    if (!timedOutGoals.isEmpty()) {
      str.append("timed out:\n");

      for (Entry<Integer, Pair<Goal, PresenceCondition>> entry : timedOutGoals.entrySet()) {
        str.append("Goal ");
        str.append(getTestGoalLabel(entry.getValue().getFirst()));

        PresenceCondition presenceCondition = entry.getValue().getSecond();
        if (presenceCondition != null) {
          str.append(": timed out for PC ");
          str.append(pcm().dump(presenceCondition));
        }
        str.append("\n");
      }

      str.append("\n");
    }

    return str.toString().replace("__SELECTED_FEATURE_", "");
  }

  public Pair<String, Integer> parseVariableName(String name) {
    String variableName;
    if (name.contains("::")) {
      variableName = name.substring(name.indexOf("::") + 2, name.indexOf("@"));
    } else {
      variableName = name.substring(0, name.indexOf("@"));
    }
    int ssaIndex = new Integer(name.substring(name.indexOf("@") + 1));

    return Pair.of(variableName, ssaIndex);
  }

  /**
   * Returns the label of a test goal if there is one; otherwise the goal index will be returned.
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
  public PresenceCondition getGoalCoverage(Goal pGoal) {
    if (pcm() == null) {
      return null;
    }

    PresenceCondition totalCoverage = pcm().makeFalse();
    for (Entry<TestCase, List<Goal>> entry : this.mapping.entrySet()) {
      if (entry.getValue().contains(pGoal)) {
        assert entry.getKey().getPresenceCondition() != null;
        totalCoverage = pcm().makeOr(totalCoverage, entry.getKey().getPresenceCondition());
      }
    }
    return totalCoverage;
  }

  public boolean isGoalInfeasible(Goal goal) {
    return infeasibleGoals.contains(goal);
  }

  public void addGoals(Collection<Goal> pGoals) {
    if (useTigerAlgorithm_with_pc) {
      for (Goal goal : pGoals) {
        remainingPresenceConditions.put(goal, pcm().makeTrue());
      }
    }
    testGoals.addAll(pGoals);
  }

  public boolean isGoalCovered(Goal pGoal) {
    if (useTigerAlgorithm_with_pc) {
      return pcm().checkEqualsFalse(remainingPresenceConditions.get(pGoal));
    } else {
      List<TestCase> testCases = coveringTestCases.get(pGoal);
      return (testCases != null && testCases.size() > 0);
    }
  }

  public boolean areGoalsCovered(Set<Goal> pTestGoalsToBeProcessed) {
    for (Goal goal : pTestGoalsToBeProcessed) {
      if (!isGoalCovered(goal)) {
        return false;
      }
    }

    return true;
  }

  public boolean areGoalsInfeasible(Set<Goal> pTestGoalsToBeProcessed) {
    for (Goal goal : pTestGoalsToBeProcessed) {
      if (!isGoalInfeasible(goal)) {
        return false;
      }
    }
    return true;
  }

  public void addTimedOutGoals(Set<Goal> pTestGoalsToBeProcessed) {
    for (Goal goal : pTestGoalsToBeProcessed) {
      addTimedOutGoal(goal, remainingPresenceConditions.get(goal));
    }
  }

  public boolean areGoalsCoveredOrInfeasible(Set<Goal> pGoals) {
    for (Goal goal : pGoals) {
      if (!(isGoalCovered(goal) || isGoalInfeasible(goal))) {
        return false;
      }
    }
    return true;
  }

  public boolean isGoalCoveredOrInfeasible(Goal pGoal) {
    return isGoalCovered(pGoal) || isGoalInfeasible(pGoal);
  }

  public boolean isGoalTimedout(Goal pGoal) {
    return timedOutGoals.containsKey(pGoal);
  }

  public int size() {
    return getTestCases().size();
  }

  public Appender dumpRegion(PresenceCondition region) {
    return pcm().dump(region);
  }

  public boolean isVariabilityAware() {
    return useTigerAlgorithm_with_pc;
  }

}
