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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithmConfiguration;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class TestSuite implements AlgorithmResult {

  public class GoalCondition {
    public GoalCondition(
        Goal pGoal,
        Region pSimplifiedPresenceCondition,
        NamedRegionManager pRegionManager) {
      goal = pGoal;
      simplifiedPresenceCondition = pSimplifiedPresenceCondition;
      regionManager = pRegionManager;
    }

    private NamedRegionManager regionManager;
    public Goal goal;
    public Region simplifiedPresenceCondition;

    @Override
    public String toString() {
      return goal.toString()
          + " : "
          + regionManager.dumpRegion(simplifiedPresenceCondition).toString();
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof GoalCondition)) {
        return false;
      }
      GoalCondition otherGC = (GoalCondition) other;
      if (this.goal != otherGC.goal) {
        return false;
      }
      if (this.simplifiedPresenceCondition != otherGC.simplifiedPresenceCondition) {
        return false;
      }
      return true;
    }

  }

  private Map<TestCase, List<GoalCondition>> mapping;
  private Map<Goal, Region> infeasibleGoals;
  private Map<Integer, Pair<Goal, Region>> timedOutGoals;
  private NamedRegionManager bddCpaNamedRegionManager;
  private Map<Goal, List<TestCase>> coveringTestCases;
  private List<Goal> includedTestGoals;
  private TestSuiteData testSuiteData;
  private int numberOfFeasibleGoals = 0;
  private TigerAlgorithmConfiguration tigerConfig;

  // new
  private Map<org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal, Region> remainingPresenceConditions;
  boolean useTigerAlgorithm_with_pc;

  public TestSuite(
      NamedRegionManager pBddCpaNamedRegionManager,
      List<Goal> includedTestGoals,
      boolean pUseTigerAlgorithm_with_pc,
      TigerAlgorithmConfiguration pTigerConfig) {
    mapping = new LinkedHashMap<>();
    infeasibleGoals = new HashMap<>();
    timedOutGoals = new HashMap<>();
    useTigerAlgorithm_with_pc = pUseTigerAlgorithm_with_pc;
    bddCpaNamedRegionManager = pBddCpaNamedRegionManager;
    coveringTestCases = new LinkedHashMap<>();
    this.includedTestGoals = Lists.newLinkedList();
    this.includedTestGoals.addAll(includedTestGoals);
    remainingPresenceConditions = new HashMap<>();
    testSuiteData = null;
    tigerConfig = pTigerConfig;

  }

  public String getGoalPresenceCondition(GoalCondition gc) {
    if (gc == null || gc.simplifiedPresenceCondition == null || bddCpaNamedRegionManager == null) {
      return "";
    }
    return bddCpaNamedRegionManager.dumpRegion(gc.simplifiedPresenceCondition).toString();
  }

  public Set<Goal> getTestGoals() {
    Set<Goal> result = new HashSet<>();
    for (List<GoalCondition> goalList : mapping.values()) {
      for (GoalCondition goalcondition : goalList) {
        result.add(goalcondition.goal);
      }
    }
    return result;
  }

  public Set<Goal> getTestGoalsForTestcase(TestCase testcase){
    Set<Goal> result = new HashSet<>();
    for(GoalCondition goalCond: mapping.get(testcase)) {
      result.add(goalCond.goal);
    }
    return result;
  }

  public int getNumberOfFeasibleTestGoals() {
    return coveringTestCases.keySet().size();
  }

  public Map<TestCase, List<GoalCondition>> getMapping() {
    return mapping;
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
    if (presenceCondition != null
        && infeasibleGoals.containsKey(goal)
        && useTigerAlgorithm_with_pc) {
      infeasibleGoals.put(
          goal,
          bddCpaNamedRegionManager
              .makeOr(infeasibleGoals.get(goal), presenceCondition));
    } else {
      infeasibleGoals.put(goal, presenceCondition);
    }
  }

  private void addCoveredPresenceCondition(Goal pGoal, Region pPresenceCondition) {

    setRemainingPresenceCondition(
        pGoal,
        bddCpaNamedRegionManager.makeAnd(
            getRemainingPresenceCondition(pGoal, bddCpaNamedRegionManager),
            bddCpaNamedRegionManager.makeNot(pPresenceCondition)));
  }

  public boolean addTestCase(
      TestCase testcase,
      Goal goal,
      Region pPresenceCondition,
      Region pSimplifiedPresenceCondition) {
    if (testSuiteAlreadyContainsTestCase(testcase, goal)) {
      return true;
    }
    if (!isGoalPariallyCovered(goal)) {
      numberOfFeasibleGoals++;
    }

    List<GoalCondition> goals = mapping.get(testcase);
    List<TestCase> testcases = coveringTestCases.get(goal);

    if (testcases == null) {
      testcases = new LinkedList<>();
      coveringTestCases.put(goal, testcases);
    }

    boolean testcaseExisted = true;

    if (goals == null) {
      goals = new LinkedList<>();
      mapping.put(testcase, goals);
      testcaseExisted = false;
    }
    goals.add(new GoalCondition(goal, pSimplifiedPresenceCondition, bddCpaNamedRegionManager));
    testcases.add(testcase);

    if (useTigerAlgorithm_with_pc) {

      addCoveredPresenceCondition(goal, pSimplifiedPresenceCondition);

    }
    return testcaseExisted;
  }

  public void setRemainingPresenceCondition(Goal pGoal, Region presenceCondtion) {
    remainingPresenceConditions.put(pGoal, presenceCondtion);
  }

  private boolean isGoalPariallyCovered(Goal pGoal) {
    if (useTigerAlgorithm_with_pc) {
      if (remainingPresenceConditions.get(pGoal) != null
          && remainingPresenceConditions.get(pGoal).isFalse()) {
        return true;
      }
    }

    List<TestCase> testCases = coveringTestCases.get(pGoal);
    return (testCases != null && testCases.size() > 0);

  }

  public void updateTestcaseToGoalMapping(TestCase testcase, Goal goal, Region pSimplifiedPresenceCondition) {
    List<GoalCondition> goals = mapping.get(testcase);
    GoalCondition goalCond =
        new GoalCondition(goal, pSimplifiedPresenceCondition, bddCpaNamedRegionManager);
    if (!goals.contains(goalCond)) {
      goals.add(goalCond);
    }

    // if (useTigerAlgorithm_with_pc) {
    // remainingPresenceConditions.put(goal, bddCpaNamedRegionManager.makeTrue());
    // }

    List<TestCase> testcases = coveringTestCases.get(goal);

    if (testcases == null) {
      testcases = new LinkedList<>();
      coveringTestCases.put(goal, testcases);
      mapping.put(testcase, goals);
      if (useTigerAlgorithm_with_pc) {
        addCoveredPresenceCondition(goal, pSimplifiedPresenceCondition);
      }

    }
    testcases.add(testcase);
  }

  public List<Goal> getIncludedTestGoals() {
    return includedTestGoals;
  }

  public Goal getGoalByName(String name) {
    for (Goal goal : includedTestGoals) {
      if (goal.getName().equals(name)) {
        return goal;
      }
    }
    return null;
  }

  public void setIncludedTestGoals(List<Goal> pIncludedTestGoals) {
    includedTestGoals.clear();
    includedTestGoals.addAll(pIncludedTestGoals);
  }


  public boolean testSuiteAlreadyContainsTestCase(TestCase pTestcase, Goal pGoal) {


    for (TestCase containingTestcase : this.getTestCases()) {
      if (pTestcase.isEquivalent(containingTestcase) && mapping.get(containingTestcase).contains(pGoal)) {
        return true;
      }
    }

    return false;
    /*
     * Entry<TestCase, List<Goal>> entry : mapping.entrySet()
     *
     *
     * pTestcase.get
     *
     *
     * // TODO make a real comparison and not just a string compare String testcaseString =
     * "Testcase " + pTestcase.toString() + " covers"; String testgoalString = "Goal "; CFANode
     * predecessor = pGoal.getCriticalEdge().getPredecessor(); if (predecessor instanceof CLabelNode
     * && !((CLabelNode) predecessor).getLabel().isEmpty()) { testgoalString += ((CLabelNode)
     * predecessor).getLabel(); } else { testgoalString += pGoal.getIndex(); } testgoalString += " "
     * + pGoal.toSkeleton() + (pGoal.getPresenceCondition() != null ? " with targetPC " +
     * bddCpaNamedRegionManager .dumpRegion(pGoal .getPresenceCondition()) : "");
     *
     * for (Entry<TestCase, List<Goal>> entry : mapping.entrySet()) { String testcaseStringCmp =
     * "Testcase " + entry.getKey().toString() + " covers"; if
     * (testcaseString.equals(testcaseStringCmp)) { for (Goal goal : entry.getValue()) { String
     * testgoalStringCmp = "Goal "; CFANode predecessorCmp =
     * goal.getCriticalEdge().getPredecessor(); if (predecessorCmp instanceof CLabelNode &&
     * !((CLabelNode) predecessorCmp).getLabel().isEmpty()) { testgoalStringCmp += ((CLabelNode)
     * predecessorCmp).getLabel(); } else { testgoalStringCmp += goal.getIndex(); }
     * testgoalStringCmp += " " + goal.toSkeleton() + (goal.getPresenceCondition() != null ?
     * " with targetPC " + bddCpaNamedRegionManager .dumpRegion(goal .getPresenceCondition()) : "");
     * if (testgoalString.equals(testgoalStringCmp)) { return true; } } } else { continue; } }
     * return false;
     */
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

  // public List<Goal> getTestGoalsCoveredByTestCase(TestCase testcase) {
  // return mapping.get(testcase);
  // }

  private String simplePresenceCondition(Region presenceCondition) {
    if (presenceCondition == null) {
      return "";
    }
    return bddCpaNamedRegionManager.dumpRegion(presenceCondition)
    .toString()
        .replace(tigerConfig.getFeatureVariablePrefix(), "")
        .replace(" & TRUE", "");

  }

  private void assembleTestSuiteData() {
    testSuiteData = new TestSuiteData();
    testSuiteData.setNumberOfTestCases(mapping.entrySet().size());

    List<TestCaseData> testCaseDatas = Lists.newLinkedList();

    for (Map.Entry<TestCase, List<GoalCondition>> entry : mapping.entrySet()) {
      TestCase testCase = entry.getKey();
      TestCaseData testCaseData = new TestCaseData();
      testCaseData.setId(testCase.getId());
      if(testCase.getPresenceCondition() != null) {
        testCaseData.setPresenceCondition(simplePresenceCondition(testCase.getPresenceCondition()));
      }

      Map<String, String> inputs = Maps.newLinkedHashMap();
      Map<String, BigInteger> in = testCase.getInputs();
      for (String key : in.keySet()) {
        inputs.put(key, in.get(key).toString());

      }
      testCaseData.setInputs(inputs);

      Map<String, String> outputs = Maps.newLinkedHashMap();
      Map<String, BigInteger> out = testCase.getOutputs();
      for (String key : out.keySet()) {
        outputs.put(key, out.get(key).toString());
      }
      testCaseData.setOutputs(outputs);

      List<String> coveredGoals = Lists.newLinkedList();
      for (GoalCondition goalCondition : entry.getValue()) {
        String goalString =
            goalCondition.goal.getIndex()
                + "@("
                + getTestGoalLabel(goalCondition.goal)
                + ") : "
                + simplePresenceCondition(goalCondition.simplifiedPresenceCondition);
        coveredGoals.add(goalString);
      }
      testCaseData.setCoveredGoals(coveredGoals);
      testCaseData.setCoveredLabels(testCase.calculateCoveredLabels());
      testCaseData.setErrorPathLength(testCase.getErrorPath().size());

      testCaseDatas.add(testCaseData);
    }

    testSuiteData.setTestCases(testCaseDatas);

    List<String> infeasibleGoalStrings = Lists.newLinkedList();
    if (!infeasibleGoals.isEmpty()) {
      for (Entry<Goal, Region> entry : infeasibleGoals.entrySet()) {
        infeasibleGoalStrings.add(getTestGoalLabel(entry.getKey()));
      }
    }

    testSuiteData.setInfeasibleGoals(infeasibleGoalStrings);

    List<String> timedoutGoalStrings = Lists.newLinkedList();
    if (!timedOutGoals.isEmpty()) {
      for (Entry<Integer, Pair<Goal, Region>> entry : timedOutGoals.entrySet()) {
        timedoutGoalStrings.add(getTestGoalLabel(entry.getValue().getFirst()));

      }
    }

    testSuiteData.setTimedOutGoals(timedoutGoalStrings);
  }

  @Override
  public String toString() {
    /*
     * StringBuffer str = new StringBuffer();
     * str.append("Number of Testcases: ").append(mapping.entrySet().size()).append("\n\n"); for
     * (Map.Entry<TestCase, List<Goal>> entry : mapping.entrySet()) { List<CFAEdge> errorPath =
     * entry.getKey().getErrorPath();
     *
     * str.append(entry.getKey().toString() + "\n\n");
     *
     * str.append("\tCovered goals {\n"); for (Goal goal : entry.getValue()) {
     * str.append("\t\t").append(goal.getIndex()).append("@");
     * str.append("(").append(getTestGoalLabel(goal)).append(")");
     *
     * Region presenceCondition = goal.getPresenceCondition(); if (presenceCondition != null) {
     * str.append(": " + bddCpaNamedRegionManager.dumpRegion(
     * (org.sosy_lab.cpachecker.util.predicates.regions.Region) presenceCondition)); }
     * str.append("\n"); } str.append("\t}\n\n");
     *
     * str.append("\tCovered labels {\n"); List<String> labels =
     * entry.getKey().calculateCoveredLabels(); str.append("\t\t"); for (String label : labels) {
     * str.append(label).append(", "); } str.delete(str.length() - 2, str.length());
     * str.append("\n"); str.append("\t}\n"); str.append("\n");
     *
     * if (errorPath != null) { str.append("\tErrorpath Length: " +
     * entry.getKey().getErrorPath().size() + "\n"); } str.append("\n\n"); }
     *
     * if (!infeasibleGoals.isEmpty()) { str.append("infeasible:\n");
     *
     * for (Entry<Goal, Region> entry : infeasibleGoals.entrySet()) { str.append("Goal ");
     * str.append(getTestGoalLabel(entry.getKey()));
     *
     * Region presenceCondition = entry.getValue(); if (presenceCondition != null) {
     * str.append(": cannot be covered with PC "); //
     * str.append(bddCpaNamedRegionManager.dumpRegion(presenceCondition)); } str.append("\n"); }
     *
     * str.append("\n"); }
     *
     * if (!timedOutGoals.isEmpty()) { str.append("timed out:\n");
     *
     * for (Entry<Integer, Pair<Goal, Region>> entry : timedOutGoals.entrySet()) {
     * str.append("Goal "); str.append(getTestGoalLabel(entry.getValue().getFirst()));
     *
     * Region presenceCondition = entry.getValue().getSecond(); if (presenceCondition != null) {
     * str.append(": timed out for PC "); str.append( bddCpaNamedRegionManager.dumpRegion(
     * (org.sosy_lab.cpachecker.util.predicates.regions.Region) presenceCondition)); }
     * str.append("\n"); }
     *
     * str.append("\n"); }
     */
    if (testSuiteData == null) {
      assembleTestSuiteData();
    }

    return testSuiteData.toString();
  }

  public String toJsonString() throws JsonGenerationException, JsonMappingException, IOException {
    if (testSuiteData == null) {
      assembleTestSuiteData();
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
    return mapper.writeValueAsString(testSuiteData);
  }

  /**
   * Returns the label of a test goal if there is one; otherwise the goal index will be returned.
   *
   * @param goal
   * @return
   */
  @SuppressWarnings("javadoc")
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
   * Summarizes the presence conditions of tests in this testsuite that cover the parameter test
   * goal.
   */
  // public Region getGoalCoverage(Goal pGoal) {
  // Region totalCoverage = bddCpaNamedRegionManager.makeFalse();
  // for (Entry<TestCase, List<Goal>> entry : this.mapping.entrySet()) {
  // if (entry.getValue().contains(pGoal)) {
  // assert entry.getKey().getPresenceCondition() != null;
  // // totalCoverage = bddCpaNamedRegionManager.makeOr(totalCoverage,
  // // entry.getKey().getPresenceCondition());
  // }
  // }
  // return totalCoverage;
  // }

  public boolean isInfeasible(Goal goal) {
    return infeasibleGoals.containsKey(goal);
  }

  public boolean isGoalTimedOut(Goal goal) {
    for (Entry<Integer, Pair<Goal, Region>> entry : timedOutGoals.entrySet()) {
      if (entry.getValue().getFirst().equals(goal)) {
        return true;
      }
    }
    return false;
  }

  public boolean isGoalCovered(Goal pGoal) {
    if (useTigerAlgorithm_with_pc) {
      if (remainingPresenceConditions.get(pGoal) != null) {
        return remainingPresenceConditions.get(pGoal).isFalse();
      } else {
        return false;
      }
    } else {
    List<TestCase> testCases = coveringTestCases.get(pGoal);
    return (testCases != null && testCases.size() > 0);
    }

  }

  public List<TestCase> getCoveringTestCases(Goal goal) {
    return coveringTestCases.get(goal);
  }

  public boolean isVariabilityAware() {
    return useTigerAlgorithm_with_pc;
  }

  public Region
      getRemainingPresenceCondition(Goal pGoal, NamedRegionManager pBddCpaNamedRegionManager) {
    if (!remainingPresenceConditions.containsKey(pGoal)) {
      return pBddCpaNamedRegionManager.makeTrue();
    }
    return remainingPresenceConditions.get(pGoal);
  }

  public boolean areGoalsCoveredOrInfeasible(LinkedList<Goal> pGoalsToCover) {
    for (Goal goal : pGoalsToCover) {
      if (!isGoalCovered(goal) || isInfeasible(goal)) {
        return false;
      }
    }
    return true;
  }

  public String getPresenceConditionOfGoalInTestcase(Goal goal, TestCase testCase) {
    if (bddCpaNamedRegionManager == null || goal == null || testCase == null || mapping == null) {
      return "";
    }

    List<GoalCondition> goals = mapping.get(testCase);
    if (goals == null) {
      return "";
    }
    GoalCondition goalCondition = null;
    for (GoalCondition gc : goals) {
      if (gc.goal == goal) {
        goalCondition = gc;
        break;
      }
    }
    return goalCondition == null
        ? ""
        : bddCpaNamedRegionManager.dumpRegion(goalCondition.simplifiedPresenceCondition).toString();
  }
}
