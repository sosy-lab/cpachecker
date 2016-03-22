/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;

import junit.framework.AssertionFailedError;

public class TigerTestHelper {

  public static Map<String, String> getConfigurationFromPropertiesFile(
      File propertiesFile) {
    Map<String, String> configuration = new HashMap<>();

    try (BufferedReader reader = new BufferedReader(new FileReader(propertiesFile))) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.startsWith("# ") || line.isEmpty()) {
          continue;
        }

        if (line.startsWith("#include")) {
          String[] pair = line.split(" ");
          if (pair.length != 2) {
            continue;
          } else {
            pair[0] = pair[0].trim();
            pair[1] = pair[1].trim();
          }

          configuration.putAll(getConfigurationFromPropertiesFile(
              new File(propertiesFile.getPath().substring(0,
                  propertiesFile.getPath().lastIndexOf(File.separator)) + "/" + pair[1])));
        }

        String[] pair = line.split("=");
        if (pair.length != 2) {
          continue;
        } else {
          pair[0] = pair[0].trim();
          pair[1] = pair[1].trim();
        }

        if (pair[0].equals("tiger.algorithmConfigurationFile")) {
          pair[1] = "config/" + pair[1];
        }

        configuration.put(pair[0], pair[1]);
      }
      reader.close();
    } catch (IOException e) {
      return null;
    }

    return configuration;
  }

  public static boolean validTestCases(TestSuite pSuite,
      List<Pair<String, Pair<String, List<Interval>>>> pMiniExampleTS,
      String pFeatureModel)
      throws InvalidConfigurationException, SolverException, InterruptedException {

    SolverHelper helper = new SolverHelper();
    BooleanFormulaManagerView bfm = helper.getFormulaManager().getBooleanFormulaManager();
    BooleanFormula fm = helper.parseFormula(pFeatureModel);

    for (Goal goal : pSuite.getGoals()) {
      Pair<String, Pair<String, List<Interval>>> expectedGoal =
          getGoalFromExpectedTestSuite(goal, pMiniExampleTS);
      if (expectedGoal == null) { throw new AssertionFailedError(
          "Expected result for goal " + goal.getName() + " is missing!"); }
      BooleanFormula expectedPC = helper.parseFormula(expectedGoal.getSecond().getFirst());

      if (pSuite.isGoalCovered(goal)) {
        // Goal is (partially) feasible
        BooleanFormula goalPC = null;
        if (pSuite.isVariabilityAware()) {
          String regionString = pSuite.dumpRegion(pSuite.getGoalCoverage(goal)).toString();
          goalPC = helper.parseFormula(regionString);
        } else {
          goalPC = bfm.makeBoolean(true);
        }

        if (fm != null) {
          expectedPC = appendFeatureModel(helper, fm, expectedPC);
          goalPC = appendFeatureModel(helper, fm, goalPC);
        }

        if (!helper.equivalent(expectedPC,
            goalPC)) { throw new AssertionFailedError("Feasible presence condition of "
                + goal.getName() + " does not match with expected feasible presence condition."); }

        List<TestCase> testCases = pSuite.getCoveringTestCases(goal);
        for (TestCase testCase : testCases) {
          if (!checkInputs(testCase.getInputs(),
              expectedGoal.getSecond().getSecond(), testCase)) { throw new AssertionFailedError(
                  "Inputs for testing " + goal.getName() + " do not match with expected inputs."); }
        }
      }

      if (pSuite.isGoalInfeasible(goal)) {
        // Goal is (partially) infeasible
        BooleanFormula goalPC = null;
        if (pSuite.isVariabilityAware()) {
          expectedPC = bfm.not(expectedPC);
          goalPC = helper.parseFormula(
              pSuite.dumpRegion(pSuite.getInfeasiblePresenceCondition(goal)).toString());
        } else {
          goalPC = bfm.makeBoolean(false);
        }

        if (fm != null) {
          expectedPC = appendFeatureModel(helper, fm, expectedPC);
          goalPC = appendFeatureModel(helper, fm, goalPC);
        }

        if (!helper.equivalent(expectedPC, goalPC)) { throw new AssertionFailedError(
            "Infeasible presence condition of " + goal.getName()
                + " does not match with expected infeasible presence condition."); }
      }
    }

    return true;
  }

  private static boolean checkInputs(List<BigInteger> pInputs,
      List<Interval> pList, TestCase testCase) {
    if (pInputs.size() != pList.size()) { return false; }

    for (int i = 0; i < pInputs.size(); i++) {
      Interval expectedInput = pList.get(i);
      if (!expectedInput.compare(pInputs.get(i), testCase)) {
        return false;
      }
    }

    return true;
  }

  private static BooleanFormula appendFeatureModel(SolverHelper helper, BooleanFormula fm,
      BooleanFormula expectedPresenceCondition) {
    expectedPresenceCondition =
        helper.getFormulaManager().getBooleanFormulaManager().and(expectedPresenceCondition,
            fm);
    return expectedPresenceCondition;
  }

  private static Pair<String, Pair<String, List<Interval>>> getGoalFromExpectedTestSuite(
      Goal pGoal,
      List<Pair<String, Pair<String, List<Interval>>>> pMiniExampleTS) {
    for (Pair<String, Pair<String, List<Interval>>> pair : pMiniExampleTS) {
      if (pGoal.getName().equals(pair.getFirst())) { return pair; }
    }
    return null;
  }

}
