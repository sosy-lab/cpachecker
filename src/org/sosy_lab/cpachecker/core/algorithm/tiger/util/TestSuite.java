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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
//import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.AutomatonGoal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.CFAGoal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class TestSuite<T extends Goal> implements AlgorithmResult {

  private Map<TestCase, List<T>> mapping;
  private Map<T, Region> infeasibleGoals;
  private Map<Integer, Pair<T, Region>> timedOutGoals;
  private BDDUtils bddUtils;
  private Map<T, List<TestCase>> coveringTestCases;
  private Set<T> includedTestGoals;
  private TestSuiteData testSuiteData;
  private String removePrefixString;
  private Map<T, Region> remainingPresenceConditions;
  private Set<T> allGoals;

  private static TestSuite<CFAGoal> cFAGoalTS;
  // private static TestSuite<AutomatonGoal> automatonGoalTS;


  private static boolean sameTestGoals(Set<?> p1, Set<?> p2) {
    if (p1 == null && p2 == null) {
      return true;
    }

    if (p1 == null || p2 == null) {
      return false;
    }

    return p1.containsAll(p2) && p2.containsAll(p1);
  }

  public static TestSuite<CFAGoal> getCFAGoalTS(
      BDDUtils pBddUtils,
      Set<CFAGoal> includedTestGoals,
      String premovePrefixString,
      Set<CFAGoal> pAllGOals) {
    if (cFAGoalTS != null) {
      if (sameTestGoals(cFAGoalTS.includedTestGoals, includedTestGoals)
          && premovePrefixString.equals(cFAGoalTS.removePrefixString)) {
        return cFAGoalTS;
      }
    }
    cFAGoalTS = new TestSuite<>(pBddUtils, includedTestGoals, premovePrefixString, pAllGOals);
    return cFAGoalTS;
  }

  public static TestSuite<CFAGoal> getCFAGoalTSOrNull() {
    return cFAGoalTS;
  }

  // public static TestSuite<AutomatonGoal> getAutomatonGoalTS(
  // BDDUtils pBddUtils,
  // Set<AutomatonGoal> includedTestGoals,
  // String premovePrefixString,
  // Set<AutomatonGoal> pAllGoals) {
  // if (automatonGoalTS != null) {
  // if (automatonGoalTS.getBddUtils() == pBddUtils
  // && sameTestGoals(automatonGoalTS.includedTestGoals, includedTestGoals)
  // && premovePrefixString.equals(automatonGoalTS.removePrefixString)) {
  // return automatonGoalTS;
  // }
  // }
  // automatonGoalTS = new TestSuite<>(pBddUtils, includedTestGoals, premovePrefixString,
  // pAllGoals);
  // return automatonGoalTS;
  // }

  private TestSuite(
      BDDUtils pBddUtils,
      Set<T> includedTestGoals,
      String premovePrefixString,
      Set<T> pAllGoals) {
    mapping = new LinkedHashMap<>();
    infeasibleGoals = new HashMap<>();
    timedOutGoals = new HashMap<>();
    bddUtils = pBddUtils;
    coveringTestCases = new LinkedHashMap<>();
    this.includedTestGoals = new HashSet<>();
    this.includedTestGoals.addAll(includedTestGoals);
    testSuiteData = null;
    removePrefixString = premovePrefixString;
    allGoals = pAllGoals;
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
    return getTestGoals().size();
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

  private boolean samevars(List<TestCaseVariable> varList1, List<TestCaseVariable> varList2) {
    if (varList1.size() != varList2.size()) {
      return false;
    }

    for (int i = 0; i < varList1.size(); i++) {
      if (!varList1.get(i).equals(varList2.get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean sameInputOutputs(TestCase tc1, TestCase tc2) {
    if (!samevars(tc1.getInputs(), tc2.getInputs())) {
      return false;
    }
    if (!samevars(tc1.getOutputs(), tc2.getOutputs())) {
      return false;
    }
    return true;
  }

  private boolean samePC(TestCase tc1, TestCase tc2) {
    if (tc1.getPresenceCondition() == null && tc2.getPresenceCondition() == null) {
      return true;
    }
    if (tc1.getPresenceCondition() == null || tc2.getPresenceCondition() == null) {
      return false;
    }
    return tc1.getPresenceCondition().equals(tc2.getPresenceCondition());

  }

  private TestCase getSameTCInTS(TestCase testCase) {
    for (TestCase tc : mapping.keySet()) {
      if (sameInputOutputs(tc, testCase) && samePC(tc, testCase)) {
        return tc;
      }
    }
    return null;
  }


  public boolean addTestCase(
      TestCase testcase,
      T goal) {

    if (mapping.keySet().size() > 0) {
      int maxID = mapping.keySet().stream().max(Comparator.comparing(TestCase::getId)).get().getId();
      if (testcase.getId() <= maxID) {
        throw new RuntimeException();
      }
    }

    // TODO dont add same tc again
    TestCase tc = getSameTCInTS(testcase);
    if (tc != null) {
      // larger path size => more explored (cannot be different path due to the same inputs)
      if (tc.getPath() == null || testcase.getPath().size() > tc.getPath().size()) {
        tc.setPath(testcase.getPath());
      }

      testcase = tc;
    }
    List<T> goals = mapping.get(testcase);
    List<TestCase> testcases = coveringTestCases.get(goal);

    if (testcases == null) {
      testcases = new ArrayList<>();
      coveringTestCases.put(goal, testcases);
    }

    boolean testcaseExisted = true;

    if (goals == null) {
      goals = new ArrayList<>();
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
      testcases = new ArrayList<>();
      coveringTestCases.put(goal, testcases);
      mapping.put(testcase, goals);
    }
    if (bddUtils.isVariabilityAware()) {
      addCoveredPresenceCondition(goal, testcase.getPresenceCondition());
    }

    testcases.add(testcase);
  }

  public Set<T> getIncludedTestGoals() {
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
  public Set<TestCase> getTestCases() {
    return mapping.keySet();
  }

  public int getNumberOfTestCases() {
    return getTestCases().size();
  }

  public Map<T, Region> getInfeasibleGoals() {
    return infeasibleGoals;
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
        if (removePrefixString.isEmpty()) {
          pc = pc.replace(removePrefixString, "");
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
      testCaseData.setErrorPathLength(testCase.getPath().size());

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

  public boolean areGoalsCoveredOrInfeasible(List<T> pGoalsToCover) {
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
      return new TestCase(
          tc1.getId(),
          tc1.getInputs(),
          tc1.getOutputs(),
          tc1.getPath(),
          newCondition,
          bddUtils);
    }

    if(dominates(tc2, tc1)) {
      Region newCondition = bddUtils.makeOr(tc1.getPresenceCondition(), tc2.getPresenceCondition());
      if(bddUtils.dumpRegion(newCondition).contains("||")) {
        return null;
      }
      return new TestCase(
          tc2.getId(),
          tc2.getInputs(),
          tc2.getOutputs(),
          tc2.getPath(),
          newCondition,
          bddUtils);
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

  public Set<T> getUncoveredGoals() {
    Set<T> uncovered = new HashSet<>(allGoals);
    uncovered.removeAll(coveringTestCases.keySet());
    return uncovered;
  }
}
