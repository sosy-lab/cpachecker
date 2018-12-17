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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.AutomatonGoal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.GoalCondition;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class TigerVariabilityTest {

  private static final String FASE_C = "test/programs/tiger/simulator/FASE2015.c";
  private static final String MINI_FASE_C = "test/programs/tiger/simulator/mini_FASE2015.c";
  private static final String PRESENCE_CONDITION_TEST_C =
      "test/programs/tiger/simulator/presenceConditionTest.c";

  public static List<Pair<String, Pair<String, List<Interval>>>> miniFaseTS = null;
  public static List<Pair<String, Pair<String, List<Interval>>>> faseTS = null;

  private static final String configFolder = "config";
  private static final String configExtension = ".properties";
  private static final String configFile =
      configFolder + "\\tiger-simulators" + configExtension;
  private static final String configFile_noOmega =
      configFolder + "\\tiger-simulators-noOmega" + configExtension;

  public static String miniFaseFm = "__SELECTED_FEATURE_PLUS";
  public static String faseFm =
      "__SELECTED_FEATURE_FOOBAR_SPL  "
          + " &  (!__SELECTED_FEATURE_FOOBAR_SPL    |  __SELECTED_FEATURE_COMP) "
          + " &  (!__SELECTED_FEATURE_FOOBAR_SPL    |  __SELECTED_FEATURE_OP) "
          + " &  (!__SELECTED_FEATURE_COMP          |  __SELECTED_FEATURE_FOOBAR_SPL) "
          + " &  (!__SELECTED_FEATURE_OP            |  __SELECTED_FEATURE_FOOBAR_SPL) "
          + " &  (!__SELECTED_FEATURE_NOTNEGATIVE   |  __SELECTED_FEATURE_FOOBAR_SPL) "
          + " &  (!__SELECTED_FEATURE_COMP          |  __SELECTED_FEATURE_LE          |  __SELECTED_FEATURE_GR) "
          + " &  (!__SELECTED_FEATURE_LE            |  __SELECTED_FEATURE_COMP)  "
          + " &  (!__SELECTED_FEATURE_GR            |  __SELECTED_FEATURE_COMP)  "
          + " &  (!__SELECTED_FEATURE_LE            |  !__SELECTED_FEATURE_GR)  "
          + " &  (!__SELECTED_FEATURE_OP            |  __SELECTED_FEATURE_PLUS        |  __SELECTED_FEATURE_MINUS)  "
          + " &  (!__SELECTED_FEATURE_PLUS          |  __SELECTED_FEATURE_OP)  "
          + " &  (!__SELECTED_FEATURE_MINUS         |  __SELECTED_FEATURE_OP)  "
          + " &  (!__SELECTED_FEATURE_PLUS          |  !__SELECTED_FEATURE_MINUS)  "
          + " &  (!__SELECTED_FEATURE_NOTNEGATIVE   |  __SELECTED_FEATURE_MINUS)  "
          + " &  (__SELECTED_FEATURE_LE "
          + " |  __SELECTED_FEATURE_PLUS "
          + " |  __SELECTED_FEATURE_NOTNEGATIVE "
          + " |  __SELECTED_FEATURE_GR "
          + " |  __SELECTED_FEATURE_MINUS "
          + " |  TRUE)";

  // ------------------------------- Variability Tests----------------------------------------------

  /**
   * Type: simulator Analysis: predicate Specialties: -
   */
  @Test
  public void simulator_miniFase() throws Exception {
    Map<String, String> prop =
        TigerTestUtil.getConfigurationFromPropertiesFile(
            new File(configFile));
    prop.put("tiger.tiger_with_presenceConditions", "true");
    prop.put("cpa.predicate.merge", "SEP");
    prop.put("tiger.coverageCheck", "Single");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "Goals: G1");

    TestResults results = CPATestRunner.run(prop, MINI_FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);
  }


  /**
   * Type: simulator Analysis: predicate Specialties: multiple test goals
   */
  @Test
  public void simulator_fase() throws Exception {
    Map<String, String> prop =
        TigerTestUtil.getConfigurationFromPropertiesFile(
            new File(configFile));
    prop.put("tiger.tiger_with_presenceConditions", "true");
    prop.put("tiger.coverageCheck", "Single");
    prop.put("cpa.predicate.merge", "SEP");
    prop.put("tiger.limitsPerGoal.time.cpu", "-1");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "Goals: G1, G2, G3, G4, G5, G6, G7");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

  }

  public boolean coversCondition(
      String condition,
      Iterable<String> includedFeatures,
      Iterable<String> excludedFeatures) {
    for (String excluded : excludedFeatures) {
      if (condition.contains(excluded)) {
        return false;
      }
    }
    for (String included : includedFeatures) {
      if (!included.startsWith("!")) {
        if (condition.contains("!" + included)) {
          return false;
        }
      }

      if (!condition.contains(included)) {
        return false;
      }
    }
    return true;
  }

  public boolean hasTestCaseCoveringGoalWithConditions(
      TestSuite<AutomatonGoal> testsuite,
      AutomatonGoal goal,
      Iterable<String> includedFeatures,
      Iterable<String> excludedFeatures) {

    boolean covers = false;
    for (TestCase tc : testsuite.getCoveringTestCases(goal)) {
      covers =
          covers || coversCondition(tc.dumpPresenceCondition(), includedFeatures, excludedFeatures);
    }
    return covers;
  }

  public boolean hasGoalCoveringConditions(
      TestSuite<AutomatonGoal> testsuite,
      Iterable<String> includedFeatures,
      Iterable<String> excludedFeatures) {

    boolean covers = false;
    for (Entry<TestCase, List<GoalCondition<AutomatonGoal>>> entry : testsuite.getMapping()
        .entrySet()) {
      for (GoalCondition goalCondition : entry.getValue()) {
        covers =
            covers
                || coversCondition(
                    testsuite.getGoalPresenceCondition(goalCondition),
                    includedFeatures,
                    excludedFeatures);
      }
    }
    return covers;
  }

  @Test
  public void simulator_PresenceCondition() throws Exception {
    Map<String, String> prop =
        TigerTestUtil.getConfigurationFromPropertiesFile(new File(configFile));
    prop.put("tiger.tiger_with_presenceConditions", "true");
    prop.put("tiger.coverageCheck", "None");
    prop.put("cpa.predicate.merge", "SEP");
    prop.put("tiger.limitsPerGoal.time.cpu", "-1");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "Goals: G1, G2, G3, G4");

    TestResults results = CPATestRunner.run(prop, PRESENCE_CONDITION_TEST_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite<AutomatonGoal> testSuite = (TestSuite) result;

    assertTrue(testSuite.getTestCases().size() == 8);
    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    AutomatonGoal goal1 = testSuite.getGoalByName("G1");
    AutomatonGoal goal2 = testSuite.getGoalByName("G2");
    AutomatonGoal goal3 = testSuite.getGoalByName("G3");
    AutomatonGoal goal4 = testSuite.getGoalByName("G4");

    for (TestCase tc : testSuite.getTestCases()) {
      assertTrue(testSuite.getTestGoalsForTestcase(tc).size() == 1);
    }

    assertTrue(testSuite.getCoveringTestCases(goal1).size() == 2);
    assertTrue(testSuite.getCoveringTestCases(goal2).size() == 2);
    assertTrue(testSuite.getCoveringTestCases(goal3).size() == 2);
    assertTrue(testSuite.getCoveringTestCases(goal4).size() == 2);

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal1,
            Arrays.asList("__SELECTED_FEATURE_X", "__SELECTED_FEATURE_PLUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL", "__SELECTED_FEATURE_Y")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal1,
            Arrays.asList(
                "__SELECTED_FEATURE_X",
                "!__SELECTED_FEATURE_PLUS",
                "__SELECTED_FEATURE_MINUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL", "__SELECTED_FEATURE_Y")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal2,
            Arrays.asList(
                "!__SELECTED_FEATURE_X",
                "__SELECTED_FEATURE_Y",
                "__SELECTED_FEATURE_PLUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL", "__SELECTED_FEATURE_MINUS")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal2,
            Arrays.asList(
                "!__SELECTED_FEATURE_X",
                "__SELECTED_FEATURE_Y",
                "!__SELECTED_FEATURE_PLUS",
                "__SELECTED_FEATURE_MINUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal3,
            Arrays.asList("__SELECTED_FEATURE_X", "__SELECTED_FEATURE_PLUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL", "__SELECTED_FEATURE_MINUS")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal3,
            Arrays.asList(
                "!__SELECTED_FEATURE_X",
                "__SELECTED_FEATURE_Y",
                "__SELECTED_FEATURE_PLUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL", "__SELECTED_FEATURE_MINUS")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal4,
            Arrays.asList(
                "__SELECTED_FEATURE_X",
                "!__SELECTED_FEATURE_PLUS",
                "__SELECTED_FEATURE_MINUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal4,
            Arrays.asList(
                "!__SELECTED_FEATURE_X",
                "!__SELECTED_FEATURE_PLUS",
                "__SELECTED_FEATURE_MINUS",
                "__SELECTED_FEATURE_Y"),
            Arrays.asList("__SELECTED_FEATURE_SPL")));

  }

  @Test
  public void simulator_PresenceCondition_NoOmega() throws Exception {
    Map<String, String> prop =
        TigerTestUtil.getConfigurationFromPropertiesFile(new File(configFile_noOmega));
    prop.put("tiger.tiger_with_presenceConditions", "true");
    prop.put("tiger.coverageCheck", "None");
    prop.put("cpa.predicate.merge", "SEP");
    prop.put("tiger.limitsPerGoal.time.cpu", "-1");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "Goals: G1, G2, G3, G4");

    TestResults results = CPATestRunner.run(prop, PRESENCE_CONDITION_TEST_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite<AutomatonGoal> testSuite = (TestSuite) result;

    assertTrue(testSuite.getTestCases().size() == 6);
    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    AutomatonGoal goal1 = testSuite.getGoalByName("G1");
    AutomatonGoal goal2 = testSuite.getGoalByName("G2");
    AutomatonGoal goal3 = testSuite.getGoalByName("G3");
    AutomatonGoal goal4 = testSuite.getGoalByName("G4");

    for (TestCase tc : testSuite.getTestCases()) {
      assertTrue(testSuite.getTestGoalsForTestcase(tc).size() == 1);
    }

    assertTrue(testSuite.getCoveringTestCases(goal1).size() == 1);
    assertTrue(testSuite.getCoveringTestCases(goal2).size() == 1);
    assertTrue(testSuite.getCoveringTestCases(goal3).size() == 2);
    assertTrue(testSuite.getCoveringTestCases(goal4).size() == 2);

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal1,
            Arrays.asList("__SELECTED_FEATURE_X"),
            Arrays.asList(
                "__SELECTED_FEATURE_SPL",
                "__SELECTED_FEATURE_Y",
                "__SELECTED_FEATURE_PLUS")));


    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal2,
            Arrays
                .asList("!__SELECTED_FEATURE_X", "__SELECTED_FEATURE_Y"),
            Arrays.asList(
                "__SELECTED_FEATURE_SPL",
                "__SELECTED_FEATURE_MINUS",
                "__SELECTED_FEATURE_PLUS")));



    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal3,
            Arrays.asList("__SELECTED_FEATURE_X", "__SELECTED_FEATURE_PLUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL", "__SELECTED_FEATURE_MINUS")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal3,
            Arrays
                .asList("!__SELECTED_FEATURE_X", "__SELECTED_FEATURE_Y", "__SELECTED_FEATURE_PLUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL", "__SELECTED_FEATURE_MINUS")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal4,
            Arrays.asList(
                "__SELECTED_FEATURE_X",
                "!__SELECTED_FEATURE_PLUS",
                "__SELECTED_FEATURE_MINUS"),
            Arrays.asList("__SELECTED_FEATURE_SPL")));

    assertTrue(
        hasTestCaseCoveringGoalWithConditions(
            testSuite,
            goal4,
            Arrays.asList(
                "!__SELECTED_FEATURE_X",
                "!__SELECTED_FEATURE_PLUS",
                "__SELECTED_FEATURE_MINUS",
                "__SELECTED_FEATURE_Y"),
            Arrays.asList("__SELECTED_FEATURE_SPL")));

  }


}
