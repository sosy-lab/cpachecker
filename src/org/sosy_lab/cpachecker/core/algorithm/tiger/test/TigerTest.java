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

import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.test.ExpectedGoalProperties.Comparators;
import org.sosy_lab.cpachecker.core.algorithm.tiger.test.ExpectedGoalProperties.GoalPropertyType;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class TigerTest {

  private static final String EXAMPLE_C = "test/programs/tiger/simulator/example.c";

  private static final String EXAMPLE_V1_C = "test/programs/tiger/simulator/example_v1.c";

  private static final String EMAIL_SIMULATOR_C = "test/programs/tiger/simulator/email_simulator.c";

  private static List<ExpectedGoalProperties> exampleGoalProperties;

  private static List<ExpectedGoalProperties> exampleGoalProperties_v1;

  @BeforeClass
  public static void initializeExpectedGoalProperties() {

    //example.c
    exampleGoalProperties = Lists.newLinkedList();
    ExpectedGoalProperties g1 = new ExpectedGoalProperties("G1", true);
    g1.addRelativeValueProperty(g1.new RelativeVariableProperty("relevant: x", "relevant: y",
        Comparators.LT, GoalPropertyType.INPUT));
    exampleGoalProperties.add(g1);
    ExpectedGoalProperties g2 = new ExpectedGoalProperties("G2", true);
    g2.addRelativeValueProperty(g2.new RelativeVariableProperty("relevant: x", "relevant: y",
        Comparators.LT, GoalPropertyType.INPUT));
    exampleGoalProperties.add(g2);

    //example_v1.c
    exampleGoalProperties_v1 = Lists.newLinkedList();
    g1 = new ExpectedGoalProperties("G1", true);
    g1.addRelativeValueProperty(g1.new RelativeVariableProperty("relevant: x", "relevant: y",
        Comparators.LT, GoalPropertyType.INPUT));
    exampleGoalProperties_v1.add(g1);
    g2 = new ExpectedGoalProperties("G2", false);
    exampleGoalProperties_v1.add(g2);
    ExpectedGoalProperties g3 = new ExpectedGoalProperties("G3", true);
    g3.addRelativeValueProperty(g3.new RelativeVariableProperty("relevant: x", "relevant: y",
        Comparators.GT, GoalPropertyType.INPUT));
    exampleGoalProperties_v1.add(g3);

  }

  @Test
  public void testExample() throws Exception {
    Map<String, String> prop = TigerTestUtil.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("tiger.inputInterface", "x,y,z");
    prop.put("tiger.outputInterface", "tmp");
    prop.put("tiger.fqlQuery",
        "COVER (\"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G2))).\"EDGES(ID)*\")");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfTestCases() == 1);
    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 2);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 0);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
    for (ExpectedGoalProperties exp : exampleGoalProperties) {
      assertTrue(exp.checkProperties(testSuite));
    }
  }

  @Test
  public void testExample_v1() throws Exception {
    Map<String, String> prop = TigerTestUtil.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("tiger.inputInterface", "x,y,z");
    prop.put("tiger.outputInterface", "tmp");
    prop.put("tiger.fqlQuery",
        "COVER (\"EDGES(ID)*\".(EDGES(@LABEL(G1))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G2))).\"EDGES(ID)*\")+(\"EDGES(ID)*\".(EDGES(@LABEL(G3))).\"EDGES(ID)*\")");

    TestResults results = CPATestRunner.run(prop, EXAMPLE_V1_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfTestCases() == 2);
    assertTrue(testSuite.getNumberOfFeasibleTestGoals() == 2);
    assertTrue(testSuite.getNumberOfInfeasibleTestGoals() == 1);
    assertTrue(testSuite.getNumberOfTimedoutTestGoals() == 0);
    for (ExpectedGoalProperties exp : exampleGoalProperties_v1) {
      assertTrue(exp.checkProperties(testSuite));
    }
  }

  @Test
  public void testEmailSimulatorForTimeout() throws Exception {
    Map<String, String> prop = TigerTestUtil.getConfigurationFromPropertiesFile(
        new File("config/tiger-variants.properties"));
    prop.put("tiger.fqlQuery",
        "COVER (\"EDGES(ID)*\".(EDGES(@LABEL(feat_AutoResp))).\"EDGES(ID)*\")");

    TestResults results = CPATestRunner.run(prop, EMAIL_SIMULATOR_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertTrue(testSuite.getNumberOfTimedoutTestGoals() > 0);
  }
}
