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

import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class TigerVariabilityTest {

  private static final String FASE_C = "test/programs/tiger/simulator/FASE2015.c";
  private static final String MINI_FASE_C = "test/programs/tiger/simulator/mini_FASE2015.c";

  public static List<Pair<String, Pair<String, List<Interval>>>> miniFaseTS = null;
  public static List<Pair<String, Pair<String, List<Interval>>>> faseTS = null;

  private static final String configFolder = "config";
  private static final String configExtension = ".properties";
  private static final String configFile =
      configFolder + "\\tiger-simulators-value" + configExtension;

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
    prop.put("tiger.checkCoverage", "true");
    prop.put("cpa.predicate.merge", "SEP");
    prop.put("tiger.limitsPerGoal.time.cpu", "-1");
    prop.put("tiger.allCoveredGoalsPerTestCase", "false");
    prop.put(
        "tiger.fqlQuery",
        "COVER \"EDGES(ID)*\".(EDGES(@LABEL(G1))+EDGES(@LABEL(G2))+EDGES(@LABEL(G3))+EDGES(@LABEL(G4))+EDGES(@LABEL(G5))+EDGES(@LABEL(G6))+EDGES(@LABEL(G7))).\"EDGES(ID)*\"");

    TestResults results = CPATestRunner.run(prop, FASE_C);
    AlgorithmResult result = results.getCheckerResult().getAlgorithmResult();

    assertThat(result).isInstanceOf(TestSuite.class);
    TestSuite testSuite = (TestSuite) result;

    assertThat(testSuite.getNumberOfFeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfInfeasibleTestGoals()).isEqualTo(7);
    assertThat(testSuite.getNumberOfTimedoutTestGoals()).isEqualTo(0);

  }


}
