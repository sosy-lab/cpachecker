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
import java.io.IOException;
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
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithmConfiguration;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class TestSuite<T extends Goal> implements AlgorithmResult {

  private Map<TestCase, List<T>> mapping;
  private Map<T, Region> infeasibleGoals;
  private Map<Integer, Pair<T, Region>> timedOutGoals;
  private BDDUtils bddUtils;
  private Map<T, List<TestCase>> coveringTestCases;
  private List<T> includedTestGoals;
  private TestSuiteData testSuiteData;
  private int numberOfFeasibleGoals = 0;
  private TigerAlgorithmConfiguration tigerConfig;
  private Map<T, Region> remainingPresenceConditions;

  public TestSuite(
      BDDUtils pBddUtils,
      List<T> includedTestGoals,
      TigerAlgorithmConfiguration pTigerConfig) {
    mapping = new LinkedHashMap<>();
    infeasibleGoals = new HashMap<>();
    timedOutGoals = new HashMap<>();
    bddUtils = pBddUtils;
    coveringTestCases = new LinkedHashMap<>();
    this.includedTestGoals = Lists.newLinkedList();
    this.includedTestGoals.addAll(includedTestGoals);
    testSuiteData = null;
    tigerConfig = pTigerConfig;

    remainingPresenceConditions = new HashMap<>();
    for (T goal : includedTestGoals) {
      setRemainingPresenceCondition(goal, bddUtils.makeTrue());
    }
  }

  public Set<T> getTestGoals() {
    Set<T> result = new HashSet<>();
    for (List<T> goalList : mapping.values()) {
      for (T goal : goalList) {
        result.add(goal);
      }
    }
    return result;
  }

  public Set<T> getTestGoalsForTestcase(TestCase testcase) {
    Set<T> result = new HashSet<>();
    for (T goal : mapping.get(testcase)) {
      result.add(goal);
    }
    return result;
  }

  public int getNumberOfFeasibleTestGoals() {
    return coveringTestCases.keySet().size();
  }

  public Map<TestCase, List<T>> getMapping() {
    return mapping;
  }

  public int getNumberOfFeasibleGoals() {
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

  public void addTimedOutGoal(int index, T goal, Region presenceCondition) {
    timedOutGoals.put(index, Pair.of(goal, presenceCondition));
  }

  public Map<Integer, Pair<T, Region>> getTimedOutGoals() {
    return timedOutGoals;
  }

  public void addInfeasibleGoal(T goal, Region presenceCondition) {
    if (presenceCondition != null && infeasibleGoals.containsKey(goal)) {
      presenceCondition = bddUtils.makeOr(infeasibleGoals.get(goal), presenceCondition);
    }

    infeasibleGoals.put(goal, presenceCondition);
  }

  private void addCoveredPresenceCondition(T pGoal, Region pPresenceCondition) {
    Region remainingPresenceCondition = bddUtils.makeAnd(
        getRemainingPresenceCondition(pGoal),
        bddUtils.makeNot(pPresenceCondition));
    setRemainingPresenceCondition(pGoal, remainingPresenceCondition);
  }

  public boolean addTestCase(
      TestCase testcase,
      T goal) {
    if (!isGoalPariallyCovered(goal)) {
      numberOfFeasibleGoals++;
    }

    List<T> goals = mapping.get(testcase);
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
    goals.add(goal);
    testcases.add(testcase);
    addCoveredPresenceCondition(goal, testcase.getPresenceCondition());

    return testcaseExisted;
  }

  public void setRemainingPresenceCondition(T pGoal, Region presenceCondtion) {
    remainingPresenceConditions.put(pGoal, presenceCondtion);
  }

  private boolean isGoalPariallyCovered(T pGoal) {
    if (bddUtils.isVariabilityAware()) {
      if (remainingPresenceConditions.get(pGoal) != null
          && remainingPresenceConditions.get(pGoal).isFalse()) {
        return true;
      }
    }
    return false;
  }

  public void
      updateTestcaseToGoalMapping(TestCase testcase, T goal) {
    List<T> goals = mapping.get(testcase);
    if (!goals.contains(goal)) {
      goals.add(goal);
    }

    // if (useTigerAlgorithm_with_pc) {
    // remainingPresenceConditions.put(goal, bddCpaNamedRegionManager.makeTrue());
    // }

    List<TestCase> testcases = coveringTestCases.get(goal);

    if (testcases == null) {
      testcases = new LinkedList<>();
      coveringTestCases.put(goal, testcases);
      mapping.put(testcase, goals);
    }
    if (bddUtils.isVariabilityAware()) {
      addCoveredPresenceCondition(goal, testcase.getPresenceCondition());
    }

    testcases.add(testcase);
  }

  public List<T> getIncludedTestGoals() {
    return includedTestGoals;
  }

  public T getGoalByName(String name) {
    for (T goal : includedTestGoals) {
      if (goal.getName().equals(name)) {
        return goal;
      }
    }
    return null;
  }

  public void setIncludedTestGoals(List<T> pIncludedTestGoals) {
    includedTestGoals.clear();
    includedTestGoals.addAll(pIncludedTestGoals);
  }


  public Set<TestCase> getTestCases() {
    return mapping.keySet();
  }

  public int getNumberOfTestCases() {
    return getTestCases().size();
  }

  public Map<T, Region> getInfeasibleGoals() {
    return infeasibleGoals;
  }

  // public List<Goal> getTestGoalsCoveredByTestCase(TestCase testcase) {
  // return mapping.get(testcase);
  // }
  private String simplePresenceCondition(Region presenceCondition) {
    if (presenceCondition == null) {
      return "";
    }
    String pc = bddUtils.dumpRegion(presenceCondition).replace(" & TRUE", "");
    if(tigerConfig.shouldRemoveFeatureVariablePrefix()) {
      pc = pc.replace(tigerConfig.getFeatureVariablePrefix(), "");
    }
    return pc;
  }

  private void assembleTestSuiteData() {
    testSuiteData = new TestSuiteData();
    testSuiteData.setNumberOfTestCases(mapping.entrySet().size());

    List<TestCaseData> testCaseDatas = Lists.newLinkedList();

    for (Map.Entry<TestCase, List<T>> entry : mapping.entrySet()) {
      TestCase testCase = entry.getKey();
      TestCaseData testCaseData = new TestCaseData();
      testCaseData.setId(testCase.getId());
      if(testCase.getPresenceCondition() != null) {
        String pc = testCase.dumpPresenceCondition();
        if (tigerConfig.shouldRemoveFeatureVariablePrefix()) {
          pc = pc.replace(tigerConfig.getFeatureVariablePrefix(), "");
        }

        testCaseData.setPresenceCondition(pc);
      }

      List<TestCaseVariable> inputs = testCase.getInputs();

      testCaseData.setInputs(inputs);

      List<TestCaseVariable> outputs = testCase.getOutputs();
      testCaseData.setOutputs(outputs);

      List<String> coveredGoals = Lists.newLinkedList();
      for (T goal : entry.getValue()) {
        String goalString =
            goal.getIndex()
                + "@("
                + goal.getName()
                + ")";
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
      for (Entry<T, Region> entry : infeasibleGoals.entrySet()) {
        infeasibleGoalStrings.add(entry.getKey().getName());
      }
    }

    testSuiteData.setInfeasibleGoals(infeasibleGoalStrings);

    List<String> timedoutGoalStrings = Lists.newLinkedList();
    if (!timedOutGoals.isEmpty()) {
      for (Entry<Integer, Pair<T, Region>> entry : timedOutGoals.entrySet()) {
        timedoutGoalStrings.add(entry.getValue().getFirst().getName());

      }
    }

    testSuiteData.setTimedOutGoals(timedoutGoalStrings);
  }

  @Override
  public String toString() {
    assembleTestSuiteData();
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

  public boolean isInfeasible(T goal) {
    return infeasibleGoals.containsKey(goal);
  }

  public boolean isGoalTimedOut(T goal) {
    for (Entry<Integer, Pair<T, Region>> entry : timedOutGoals.entrySet()) {
      if (entry.getValue().getFirst().equals(goal)) {
        return true;
      }
    }
    return false;
  }

  public boolean isGoalCovered(T pGoal) {
    if (bddUtils.isVariabilityAware()) {
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

  public List<TestCase> getCoveringTestCases(T goal) {
    return coveringTestCases.get(goal);
  }

  public Region getRemainingPresenceCondition(T pGoal) {
    if (!remainingPresenceConditions.containsKey(pGoal)) {
      return bddUtils.makeTrue();
    }

    return remainingPresenceConditions.get(pGoal);
  }

  public boolean areGoalsCoveredOrInfeasible(LinkedList<T> pGoalsToCover) {
    for (T goal : pGoalsToCover) {
      if (!isGoalCovered(goal) || isInfeasible(goal)) {
        return false;
      }
    }
    return true;
  }


  public BDDUtils getBddUtils() {
    return bddUtils;
  }

  // gleiches goal, gleiche ein und ausgaben

  private boolean dominates(TestCase tc1, TestCase tc2) {
    for (T goal : mapping.get(tc2)) {
      if (!mapping.get(tc1).contains(goal)) {
        return false;
      }
    }
    return true;
  }

  private TestCase combine(boolean compareInputOutout, TestCase tc1, TestCase tc2) {
    if (compareInputOutout) {
      if (!tc1.getInputs().equals(tc2.getInputs())) {
        return null;
      }
      if (!tc1.getOutputs().equals(tc2.getOutputs())) {
        return null;
      }
    }
    if(dominates(tc1, tc2)) {
      Region newCondition = bddUtils.makeOr(tc1.getPresenceCondition(), tc2.getPresenceCondition());
      if(bddUtils.dumpRegion(newCondition).contains("||")) {
        return null;
      }
      return new TestCase(tc1.getId(), tc1.getInputs(), tc1.getOutputs(), tc1.getPath(), tc1.getErrorPath(), newCondition, bddUtils);
    }

    if(dominates(tc2, tc1)) {
      Region newCondition = bddUtils.makeOr(tc1.getPresenceCondition(), tc2.getPresenceCondition());
      if(bddUtils.dumpRegion(newCondition).contains("||")) {
        return null;
      }
      return new TestCase(tc2.getId(), tc2.getInputs(), tc2.getOutputs(), tc2.getPath(), tc2.getErrorPath(), newCondition, bddUtils);
    }
    return null;
  }

  private Set<TestCase> combineTestCases(boolean compareInputOutput) {
    Set<TestCase> testcases = new HashSet<>(mapping.keySet());
    Set<TestCase> copy = new HashSet<>(testcases);
    do {
      testcases.clear();
      testcases.addAll(copy);
      for (TestCase tc1 : mapping.keySet()) {
        for (TestCase tc2 : mapping.keySet()) {
          if (tc1 != tc2) {
          TestCase combined = combine(compareInputOutput, tc1, tc2);
          if (combined != null) {
            copy.remove(tc1);
            copy.remove(tc2);
            copy.add(combined);
          }
          }
        }
      }
    } while (copy.size() != testcases.size());
    return copy;
  }

  public Set<TestCase> getShrinkedTestCases() {
    return combineTestCases(true);
  }

  public Set<TestCase> getShrinkedTestCasesIgnoreInputOutput() {
    return combineTestCases(false);
  }
}
