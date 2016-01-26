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
import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sosy_lab.cpachecker.core.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;
import org.sosy_lab.cpachecker.util.test.TestRunStatisticsParser;

import com.google.common.truth.Truth;


public class TigerTest {

  @Rule
  public Timeout globalTimeout = Timeout.seconds(20); // 10 seconds max per method tested

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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/mini_example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 0);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/mini_example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(1);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(0);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/mini_example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 0);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/mini_example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 0);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/mini_example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 0);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/mini_example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 0);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/mini_example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 0);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 4);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 4);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    Map<String,     String> prop = TigerTestHelper.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("cpa.arg.dumpAfterIteration", "false");
    prop.put("cpa.predicate.targetStateSatCheck", "false");
    prop.put("tiger.numberOfTestGoalsPerRun", "-1");
    prop.put("tiger.usePowerset", "true");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);

    TestSuite testSuite = (TestSuite) result;

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());
    stat.assertThatNumber("Number of feasible test goals").isEqualTo(testSuite.getNumberOfFeasibleTestGoals());

    Truth.assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(1);
    Truth.assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(4);
    Truth.assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 4);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 4);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/products/example.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 4);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/mini_FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/mini_FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/mini_FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/mini_FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/mini_FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/mini_FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.usePowerset", "false");
    prop.put("tiger.useAutomataCrossProduct", "false");
    prop.put("tiger.checkCoverage", "true");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 7);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
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
    prop.put("tiger.fqlQuery", "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, "test/programs/tiger/simulator/FASE2015.c");
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertNotNull(result);
    assertTrue(result instanceof TestSuite);

    TestSuite testSuite = (TestSuite) result;

    Truth.assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    Truth.assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    Truth.assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);
  }

}
