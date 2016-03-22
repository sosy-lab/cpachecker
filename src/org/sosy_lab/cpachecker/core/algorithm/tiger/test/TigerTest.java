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
package org.sosy_lab.cpachecker.core.algorithm.tiger.test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.test.Interval.Comparators;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;
import org.sosy_lab.cpachecker.util.test.TestRunStatisticsParser;


public class TigerTest {

//  @Rule
//  public Timeout globalTimeout = Timeout.seconds(20); // 20 seconds max per method tested

  private static final String FASE_C = "test/programs/tiger/simulator/FASE2015.c";
  private static final String MINI_FASE_C = "test/programs/tiger/simulator/mini_FASE2015.c";
  private static final String EXAMPLE_C = "test/programs/tiger/products/example.c";
  private static final String MINI_EXAMPLE_C = "test/programs/tiger/products/mini_example.c";

  public static List<Pair<String, Pair<String, List<Interval>>>> miniExampleTS = null;
  public static List<Pair<String, Pair<String, List<Interval>>>> exampleTS = null;
  public static List<Pair<String, Pair<String, List<Interval>>>> miniFaseTS = null;
  public static List<Pair<String, Pair<String, List<Interval>>>> faseTS = null;

  public static String miniFaseFm = "__SELECTED_FEATURE_PLUS";
  public static String faseFm = "__SELECTED_FEATURE_FOOBAR_SPL  " +
                                " &  (!__SELECTED_FEATURE_FOOBAR_SPL    |  __SELECTED_FEATURE_COMP) "
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

  @BeforeClass
  public static void initializeExpectedTestSuites() {
    miniExampleTS = new ArrayList<>();
    List<Interval> miniExampleIntervals = new ArrayList<>();
    miniExampleIntervals.add(new Interval(null, null, new BigInteger("10"), Comparators.LE, -1, null));
    miniExampleTS.add(Pair.of("G1", Pair.of("true", miniExampleIntervals)));


    exampleTS = new ArrayList<>();
    List<Interval> exampleIntervalsG1 = new ArrayList<>();
    exampleIntervalsG1.add(new Interval(null, null, null, null, 1, Comparators.LE)); // x
    exampleIntervalsG1.add(new Interval(null, null, null, null, 0, Comparators.GE)); // y
    exampleIntervalsG1.add(new Interval(null, null, null, null, -1, null)); // z
    exampleTS.add(Pair.of("G1", Pair.of("true", exampleIntervalsG1)));

    List<Interval> exampleIntervalsG2 = new ArrayList<>();
    exampleTS.add(Pair.of("G2", Pair.of("false", exampleIntervalsG2))); // infeasible

    List<Interval> exampleIntervalsG3 = new ArrayList<>();
    exampleIntervalsG3.add(new Interval(null, null, null, null, 1, Comparators.GE)); // x
    exampleIntervalsG3.add(new Interval(null, null, null, null, 0, Comparators.LE)); // y
    exampleIntervalsG3.add(new Interval(null, null, null, null, -1, null)); // z
    exampleTS.add(Pair.of("G3", Pair.of("true", exampleIntervalsG3)));

    List<Interval> exampleIntervalsG4 = new ArrayList<>();
    exampleIntervalsG4.add(new Interval(null, null, null, null, -1, null)); // x
    exampleIntervalsG4.add(new Interval(null, null, null, null, -1, null)); // y
    exampleIntervalsG4.add(new Interval(new BigInteger("10"), Comparators.GT, null, null, -1, null)); // z
    exampleTS.add(Pair.of("G4", Pair.of("true", exampleIntervalsG4)));

    List<Interval> exampleIntervalsG5 = new ArrayList<>();
    exampleIntervalsG5.add(new Interval(null, null, null, null, -1, null)); // x
    exampleIntervalsG5.add(new Interval(null, null, null, null, -1, null)); // y
    exampleIntervalsG5.add(new Interval(new BigInteger("10"), Comparators.GT, null, null, -1, null)); // z
    exampleTS.add(Pair.of("G5", Pair.of("true", exampleIntervalsG5)));


    miniFaseTS = new ArrayList<>();
    List<Interval> miniFaseIntervals = new ArrayList<>();
    miniFaseIntervals.add(new Interval(null, null, new BigInteger("10"), Comparators.LE, -1, null));
    miniFaseTS.add(Pair.of("G1", Pair.of("true", miniFaseIntervals)));

    faseTS = new ArrayList<>();
    List<Interval> faseIntervalsG1 = new ArrayList<>();
    faseIntervalsG1.add(new Interval(null, null, null, null, -1, null)); // x
    faseIntervalsG1.add(new Interval(null, null, null, null, -1, null)); // y
    faseIntervalsG1.add(new Interval(null, null, null, null, -1, null)); // z
    faseTS.add(Pair.of("G1", Pair.of("__SELECTED_FEATURE_PLUS", faseIntervalsG1)));

    List<Interval> faseIntervalsG2 = new ArrayList<>();
    faseIntervalsG2.add(new Interval(null, null, null, null, -1, null)); // x
    faseIntervalsG2.add(new Interval(null, null, null, null, -1, null)); // y
    faseIntervalsG2.add(new Interval(null, null, null, null, -1, null)); // z
    faseTS.add(Pair.of("G2", Pair.of("__SELECTED_FEATURE_MINUS", faseIntervalsG2)));

    List<Interval> faseIntervalsG3 = new ArrayList<>();
    faseIntervalsG3.add(new Interval(null, null, null, null, -1, null)); // x
    faseIntervalsG3.add(new Interval(null, null, null, null, -1, null)); // y
    faseIntervalsG3.add(new Interval(null, null, null, null, -1, null)); // z
    faseTS.add(Pair.of("G3", Pair.of("__SELECTED_FEATURE_MINUS & __SELECTED_FEATURE_NOTNEGATIVE", faseIntervalsG3)));

    List<Interval> faseIntervalsG4 = new ArrayList<>();
    faseIntervalsG4.add(new Interval(null, null, null, null, 1, Comparators.LE)); // x
    faseIntervalsG4.add(new Interval(null, null, null, null, 0, Comparators.GE)); // y
    faseIntervalsG4.add(new Interval(null, null, null, null, -1, null)); // z
    faseTS.add(Pair.of("G4", Pair.of("__SELECTED_FEATURE_GR", faseIntervalsG4)));

    List<Interval> faseIntervalsG5 = new ArrayList<>();
    faseIntervalsG5.add(new Interval(null, null, null, null, 1, Comparators.GT)); // x
    faseIntervalsG5.add(new Interval(null, null, null, null, 0, Comparators.LT)); // y
    faseIntervalsG5.add(new Interval(null, null, null, null, -1, null)); // z
    faseTS.add(Pair.of("G5", Pair.of("__SELECTED_FEATURE_GR", faseIntervalsG5)));

    List<Interval> faseIntervalsG6 = new ArrayList<>();
    faseIntervalsG6.add(new Interval(null, null, null, null, 1, Comparators.GE)); // x
    faseIntervalsG6.add(new Interval(null, null, null, null, 0, Comparators.LE)); // y
    faseIntervalsG6.add(new Interval(null, null, null, null, -1, null)); // z
    faseTS.add(Pair.of("G6", Pair.of("__SELECTED_FEATURE_LE", faseIntervalsG6)));

    List<Interval> faseIntervalsG7 = new ArrayList<>();
    faseIntervalsG7.add(new Interval(null, null, null, null, 1, Comparators.LE)); // x
    faseIntervalsG7.add(new Interval(null, null, null, null, 0, Comparators.GE)); // y
    faseIntervalsG7.add(new Interval(null, null, null, null, -1, null)); // z
    faseTS.add(Pair.of("G7", Pair.of("__SELECTED_FEATURE_LE", faseIntervalsG7)));
  }

  /*
   * Test various configurations - variants
   */

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: -
   */
  @Test
  public void variants_miniExample() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 0);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);

    assertTrue(testSuite.getGoals().size() == miniExampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniExampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: uses cross product for test goal representation
   */
  @Test
  @Ignore
  public void variants_crossProduct_miniExample() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "true");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniExampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniExampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: uses power set for test goals representation
   */
  @Test
  public void variants_powerset_miniExample() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniExampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniExampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    value + predicate
   * Specialties: -
   */
  @Test
  public void variantsValue_miniExample() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniExampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniExampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    value + predicate
   * Specialties: uses cross product for test goal representation
   */
  @Test
  @Ignore
  public void variantsValue_crossProduct_miniExample() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "true");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniExampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniExampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    value + predicate
   * Specialties: uses power set for test goals representation
   */
  @Test
  public void variantsValue_powerset_miniExample() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniExampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniExampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: target state sat check
   */
  @Test
  public void variants_miniExample_tagetStateSatCheck() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "true");
    prop.put("tiger.limitsPerGoal.time.cpu", "-1");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniExampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniExampleTS, null));
  }

  /*
   * Test various configurations - variants
   */

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: multiple test goals
   */
  @Test
  public void variants_example() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == exampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, exampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: uses cross product for test goal representation
   *              multiple test goals
   */
  @Test
  @Ignore
  public void variants_crossProduct_example() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "true");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == exampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, exampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: uses power set for test goals representation
   *              multiple test goals
   */
  @Test
  public void variants_powerset_example()
      throws Exception {

    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());
    stat.assertThatNumber("Number of feasible test goals")
        .isEqualTo(testSuite.getNumberOfFeasibleTestGoals());

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == exampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, exampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    value + predicate
   * Specialties: multiple test goals
   */
  @Test
  public void variantsValue_example() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == exampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, exampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    value + predicate
   * Specialties: uses cross product for test goal representation
   *              multiple test goals
   */
  @Test
  @Ignore
  public void variantsValue_crossProduct_example() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "true");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == exampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, exampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    value + predicate
   * Specialties: uses power set for test goals representation
   *              multiple test goals
   */
  @Test
  public void variantsValue_powerset_example() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == exampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, exampleTS, null));
  }

  /*
   * Test various configurations - variability aware
   */

  /**
   * Type:        simulator
   * Analysis:    predicate
   * Specialties: -
   */
  @Test
  public void simulator_miniFase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniFaseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniFaseTS, miniFaseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    predicate
   * Specialties: uses cross product for test goal representation
   */
  @Test
  @Ignore
  public void simulator_crossProduct_miniFase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "true");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniFaseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniFaseTS, miniFaseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    predicate
   * Specialties: uses power set for test goals representation
   */
  @Test
  public void simulator_powerset_miniFase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniFaseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniFaseTS, miniFaseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    value + predicate
   * Specialties: -
   */
  @Test
  public void simulatorValue_miniFase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniFaseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniFaseTS, miniFaseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    value + predicate
   * Specialties: uses cross product for test goal representation
   */
  @Test
  @Ignore
  public void simulatorValue_crossProduct_miniFase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "true");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, MINI_FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniFaseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniFaseTS, miniFaseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    value + predicate
   * Specialties: uses power set for test goals representation
   */
  @Test
  public void simulatorValue_powerset_miniFase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\"");

    TestResults results =
        CPATestRunner.run(prop, MINI_FASE_C, true);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == miniFaseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, miniFaseTS, miniFaseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    predicate
   * Specialties: multiple test goals
   */
  @Test
  public void simulator_fase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.limitsPerGoal.time.cpu", "-1");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }

  @Test
  public void simulator_fase_powerset() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }


  @Test
  public void simulator_fase_powerset_noomega() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware-powerset-noOmega.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }



  @Test
  public void simulator_fase_one_parallel() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    predicate
   * Specialties: uses cross product for test goal representation
   */
  @Test
  @Ignore
  public void simulator_crossProduct_fase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "true");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    predicate
   * Specialties: uses power set for test goals representation
   */
  @Test
  public void simulator_powerset_fase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    value + predicate
   * Specialties: -
   */
  @Test
  public void simulatorValue_fase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.limitsPerGoal.time.cpu", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    value + predicate
   * Specialties: uses cross product for test goal representation
   */
  @Test
  @Ignore
  public void simulatorValue_crossProduct_fase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "true");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }

  /**
   * Type:        simulator
   * Analysis:    value + predicate
   * Specialties: uses power set for test goals representation
   */
  @Test
  public void simulatorValue_powerset_fase() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variabilityAware-value.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == faseTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, faseTS, faseFm));
  }

  /*
   * Test various configurations - variants
   */

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: multiple test goals
   */
  @Test
  public void variants_example_noUseOfOmegaLabel() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants-powerset.properties"));
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.useOmegaLabel", "false");
    prop.put("cpa.automaton.splitOnTargetStatesToInactive", "true");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == exampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, exampleTS, null));
  }

  /**
   * Type:        variant
   * Analysis:    predicate
   * Specialties: multiple test goals
   */
  @Test
  public void variants_example_useOfOmegaLabelAndDoNotSplitOnTargetStates() throws Exception {
    Map<String, String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants-powerset.properties"));
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.useOmegaLabel", "true");
    prop.put("cpa.automaton.splitOnTargetStatesToInactive", "true");
    prop.put("tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

    assertTrue(testSuite.getGoals().size() == exampleTS.size());
    assertTrue(TigerTestHelper.validTestCases(testSuite, exampleTS, null));
  }

}
