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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestStep.VariableAssignment;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.NFA;
import org.sosy_lab.cpachecker.util.presence.PresenceConditions;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceConditionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TestSuite implements AlgorithmResult {

  private final boolean printLabels;
  private final boolean useTigerAlgorithm_with_pc;
  private long generationStartTime = 0;

  private final Map<TestCase, List<Goal>> mapping;
  private final Map<Goal, List<TestCase>> coveringTestCases;

  private final ImmutableSet<Goal> testGoals;
  private final Set<Goal> feasibleGoals;
  private final Set<Goal> partiallyFeasibleGoals;
  private final Set<Goal> partiallyInfeasibleGoals;
  private final Set<Goal> infeasibleGoals;
  private final Set<Goal> timedOutGoals;
  private final Set<Goal> partiallyTimedOutGoals;

  private final Map<Goal, PresenceCondition> remainingPresenceConditions;
  private final Map<Goal, PresenceCondition> remainingPresenceConditionsBeforeTimeout;
  private final Map<Pair<TestCase, Goal>, PresenceCondition> coveringPresenceConditions;
  private final Map<Goal, PresenceCondition> infeasiblePresenceConditions;
  private final Map<Integer, Pair<Goal, PresenceCondition>> timedOutPresenceCondition;

  private final ImmutableMap<CFAEdge, List<NFA<GuardedEdgeLabel>>> edgeToTgaMapping;

  public TestSuite(Set<Goal> pGoalsToCover, boolean pPrintLabels, boolean pUseTigerAlgorithm_with_pc) {
    mapping = new HashMap<>();
    coveringTestCases = new HashMap<>();
    coveringPresenceConditions = new HashMap<>();
    feasibleGoals = Sets.newLinkedHashSet();
    partiallyFeasibleGoals = Sets.newLinkedHashSet();
    partiallyInfeasibleGoals = Sets.newLinkedHashSet();
    infeasibleGoals = Sets.newLinkedHashSet();
    timedOutGoals = Sets.newLinkedHashSet();
    partiallyTimedOutGoals = Sets.newLinkedHashSet();
    timedOutPresenceCondition = new HashMap<>();
    printLabels = pPrintLabels;
    useTigerAlgorithm_with_pc = pUseTigerAlgorithm_with_pc;
    remainingPresenceConditions = new HashMap<>();
    remainingPresenceConditionsBeforeTimeout = new HashMap<>();
    infeasiblePresenceConditions = new HashMap<>();

    if (isVariabilityAware()) {
      for (Goal goal : pGoalsToCover) {
        remainingPresenceConditions.remove(goal); // null equals 'true'
      }
    }

    testGoals = ImmutableSet.copyOf(pGoalsToCover);
    edgeToTgaMapping = createEdgeToTgaMapping(pGoalsToCover);
  }

  public Set<Goal> getUncoveredTestGoals() {
    return Sets.difference(testGoals, Sets.union(feasibleGoals, infeasibleGoals));
  }

  private ImmutableMap<CFAEdge, List<NFA<GuardedEdgeLabel>>> createEdgeToTgaMapping(Set<Goal> pGoalsToCover) {
    final Map<CFAEdge, List<NFA<GuardedEdgeLabel>>> result = Maps.newHashMap();
    for (Goal goal : pGoalsToCover) {
      NFA<GuardedEdgeLabel> automaton = goal.getAutomaton();
      for (NFA<GuardedEdgeLabel>.Edge edge : automaton.getEdges()) {
        if (edge.getSource().equals(edge.getTarget())) {
          continue;
        }

        GuardedEdgeLabel label = edge.getLabel();
        for (CFAEdge e : label.getEdgeSet()) {
          List<NFA<GuardedEdgeLabel>> tgaSet = result.get(e);

          if (tgaSet == null) {
            tgaSet = new ArrayList<>();
            result.put(e, tgaSet);
          }

          tgaSet.add(automaton);
        }
      }
    }

    return ImmutableMap.copyOf(result);
  }

  private PresenceConditionManager pcm() {
    return PresenceConditions.manager();
  }

  public boolean isVariabilityAware() {
    return useTigerAlgorithm_with_pc;
  }

  public Set<TestCase> getTestCases() {
    return mapping.keySet();
  }

  public List<TestCase> getCoveringTestCases(Goal pGoal) {
    return coveringTestCases.get(pGoal);
  }

  public int getNumberOfTestCases() {
    return getTestCases().size();
  }

  public ImmutableSet<Goal> getGoals() {
    return testGoals;
  }

  public Set<Goal> getFeasibleGoals() {
    return feasibleGoals;
  }

  public int getNumberOfFeasibleTestGoals() {
    return feasibleGoals.size();
  }

  public Set<Goal> getPartiallyFeasibleGoals() {
    return partiallyFeasibleGoals;
  }

  public int getNumberOfPartiallyFeasibleTestGoals() {
    return partiallyFeasibleGoals.size();
  }

  public int getNumberOfInfeasibleTestGoals() {
    return infeasibleGoals.size();
  }

  public int getNumberOfPartiallyInfeasibleTestGoals() {
    return partiallyInfeasibleGoals.size();
  }

  public Map<Integer, Pair<Goal, PresenceCondition>> getTimedOutGoals() {
    return timedOutPresenceCondition;
  }

  public int getNumberOfTimedoutTestGoals() {
    if (useTigerAlgorithm_with_pc) {
      return timedOutPresenceCondition.size();
    } else {
      return timedOutGoals.size();
    }
  }

  public Set<Goal> getPartiallyTimedOutGoals() {
    return partiallyTimedOutGoals;
  }

  public int getNumberOfPartiallyTimedOutTestGoals() {
    return partiallyTimedOutGoals.size();
  }

  public PresenceCondition getRemainingPresenceCondition(Goal pGoal) {
    PresenceCondition pc = remainingPresenceConditions.get(pGoal);
    if (pc == null) {
      return pcm().makeTrue();
    } else {
      return pc;
    }
  }

  private void setRemainingPresenceCondition(Goal pGoal, PresenceCondition presenceCondtion) {
    remainingPresenceConditions.put(pGoal, presenceCondtion);
  }

  public PresenceCondition getInfeasiblePresenceCondition(Goal pGoal) {
    return PresenceConditions.orFalse(infeasiblePresenceConditions.get(pGoal));
  }

  public boolean addTestCase(TestCase testcase, Goal goal, PresenceCondition pPresenceCondition)
      throws InterruptedException {
    if (contains(testcase, goal)) { return true; }

    boolean testcaseExisted = true;

    List<Goal> goalsCovByTC = mapping.get(testcase);
    List<TestCase> covTCs = coveringTestCases.get(goal);

    if (goalsCovByTC == null) {
      goalsCovByTC = new LinkedList<>();
      mapping.put(testcase, goalsCovByTC);
      testcaseExisted = false;
    }

    if (covTCs == null) {
      covTCs = new LinkedList<>();
      coveringTestCases.put(goal, covTCs);
    }

    goalsCovByTC.add(goal);
    covTCs.add(testcase);

    if (pPresenceCondition != null) {
      coveringPresenceConditions.put(Pair.of(testcase, goal), pPresenceCondition);

      setRemainingPresenceCondition(goal,
          pcm().makeAnd(getRemainingPresenceCondition(goal),
              pcm().makeNegation(pPresenceCondition)));
    }

    if (isGoalCovered(goal)) {
      partiallyFeasibleGoals.remove(goal);
      feasibleGoals.add(goal);
    } else {
      partiallyFeasibleGoals.add(goal);
    }

    return testcaseExisted;
  }

  public void addInfeasibleGoal(Goal pGoal, PresenceCondition pPresenceCondition) throws InterruptedException {
    setRemainingPresenceCondition(pGoal, pcm().makeFalse());
    infeasiblePresenceConditions.put(pGoal, pPresenceCondition);

    if (pcm().checkEqualsTrue(getInfeasiblePresenceCondition(pGoal))) {
      infeasibleGoals.add(pGoal);
    } else {
      partiallyInfeasibleGoals.add(pGoal);
    }
  }

  private void addTimedOutGoal(Goal pGoal, PresenceCondition pPresenceCondition) {
    if (isVariabilityAware()) {
      remainingPresenceConditionsBeforeTimeout.put(pGoal, getRemainingPresenceCondition(pGoal));
      setRemainingPresenceCondition(pGoal, pcm().makeFalse());

      timedOutPresenceCondition.put(pGoal.getIndex(), Pair.of(pGoal, pPresenceCondition));
    }

    if (!isVariabilityAware()) {
      timedOutGoals.add(pGoal);
    } else {
      if (partiallyFeasibleGoals.contains(pGoal)) {
        partiallyTimedOutGoals.add(pGoal);
      } else {
        timedOutGoals.add(pGoal);
      }
    }
  }

  public void addTimedOutGoals(Set<Goal> pTestGoalsToBeProcessed) {
    for (Goal goal : pTestGoalsToBeProcessed) {
      addTimedOutGoal(goal, getRemainingPresenceCondition(goal));
    }
  }

  private boolean contains(TestCase pTestcase, Goal pGoal) {
    List<Goal> goals = mapping.get(pTestcase);
    if (goals != null) { return goals.contains(pGoal); }

    return false;
  }

  public List<Goal> getTestGoalsCoveredByTestCase(TestCase testcase) {
    return mapping.get(testcase);
  }

  private long getGenerationStartTime() {
    return generationStartTime;
  }

  public void setGenerationStartTime(long pGenerationStartTime) {
    generationStartTime = pGenerationStartTime;
  }

  /**
   * Returns the label of a test goal if there is one; otherwise the goal index will be returned.
   */
  public String getTestGoalLabel(Goal goal) {
    final String label;

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
    if (pcm() == null) { return null; }

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

  public boolean isGoalCovered(Goal pGoal) throws InterruptedException {
    return pcm().checkEqualsFalse(getRemainingPresenceCondition(pGoal));
  }

  public boolean isGoalPartiallyCovered(Goal pGoal) throws InterruptedException {
    return !isGoalCoveredOrInfeasible(pGoal) && getCoveringTestCases(pGoal) != null
        && !getCoveringTestCases(pGoal).isEmpty();
  }

  public boolean areGoalsCoveredOrInfeasible(Set<Goal> pGoals) throws InterruptedException {
    for (Goal goal : pGoals) {
      if (!(isGoalCovered(goal) || isGoalInfeasible(goal))) { return false; }
    }
    return true;
  }

  public boolean isGoalCoveredOrInfeasible(Goal pGoal) throws InterruptedException {
    return isGoalCovered(pGoal) || isGoalInfeasible(pGoal);
  }

  public Appender dumpRegion(PresenceCondition region) {
    return pcm().dump(region);
  }

  public void prepareForRetryAfterTimeout() {
    for (Goal goal : timedOutGoals) {
      setRemainingPresenceCondition(goal, remainingPresenceConditionsBeforeTimeout.get(goal));
    }

    for (Goal goal : partiallyTimedOutGoals) {
      setRemainingPresenceCondition(goal, remainingPresenceConditionsBeforeTimeout.get(goal));
    }

    timedOutGoals.clear();
    partiallyTimedOutGoals.clear();
    remainingPresenceConditionsBeforeTimeout.clear();
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
        cnt++;
      }

      List<CFAEdge> errorPath = testcase.getErrorPath();
      if (testcase.getGenerationTime() != -1) {
        str.append(
            "Generation Time: " + (testcase.getGenerationTime() - getGenerationStartTime()) + "\n");
      }
      if (errorPath != null) {
        str.append("Errorpath Length: " + testcase.getErrorPath().size() + "\n");
      }

      List<Goal> goals = mapping.get(testcase);
      for (Goal goal : goals) {
        str.append("Goal ");
        str.append(getTestGoalLabel(goal));

        PresenceCondition presenceCondition =
            coveringPresenceConditions.get(Pair.of(testcase, goal));
        if (presenceCondition != null) {
          str.append(": " + presenceCondition.toString().replace(" & TRUE", ""));
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
          if (predecessor instanceof CLabelNode
              && !((CLabelNode) predecessor).getLabel().isEmpty()) {
            str.append(((CLabelNode) predecessor).getLabel());
            str.append(" ");
          }
        }
        str.append("\n");
      }

      str.append("\n");
    }

    if (!infeasibleGoals.isEmpty() || !partiallyInfeasibleGoals.isEmpty()) {
      str.append("infeasible:\n");

      Set<Goal> infeasibleOrPartiallyInfeasibleGoals = new HashSet<>();
      infeasibleOrPartiallyInfeasibleGoals.addAll(infeasibleGoals);
      infeasibleOrPartiallyInfeasibleGoals.addAll(partiallyInfeasibleGoals);

      for (Goal entry : infeasibleOrPartiallyInfeasibleGoals) {
        str.append("Goal ");
        str.append(getTestGoalLabel(entry));

        PresenceCondition presenceCondition = infeasiblePresenceConditions.get(entry);
        if (presenceCondition != null) {
          str.append(": cannot be covered with PC ");
          str.append(presenceCondition.toString().replace(" & TRUE", ""));
        }
        str.append("\n");
      }

      str.append("\n");
    }

    if (useTigerAlgorithm_with_pc && !timedOutPresenceCondition.isEmpty()) {
      str.append("timed out:\n");

      for (Entry<Integer, Pair<Goal, PresenceCondition>> entry : timedOutPresenceCondition
          .entrySet()) {
        str.append("Goal ");
        str.append(getTestGoalLabel(entry.getValue().getFirst()));

        PresenceCondition presenceCondition = entry.getValue().getSecond();
        if (presenceCondition != null) {
          str.append(": timed out for PC ");
          str.append(presenceCondition);
        }
        str.append("\n");
      }

      str.append("\n");
    }

    if (!timedOutGoals.isEmpty()) {
      str.append("timed out:\n");

      for (Goal goal : timedOutGoals) {
        str.append("Goal ");
        str.append(getTestGoalLabel(goal));
        str.append("\n");
      }

      str.append("\n");
    }

    return str.toString().replace("__SELECTED_FEATURE_", "");
  }

  public long getTotalNumberOfGoals() {
    return testGoals.size();
  }

  public List<NFA<GuardedEdgeLabel>> getTGAForEdge(CFAEdge pLCFAEdge) {
    return edgeToTgaMapping.get(pLCFAEdge);
  }
}
