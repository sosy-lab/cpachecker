/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.experiments.testlocks;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fshell.FShell3Result;
import org.sosy_lab.cpachecker.fshell.Main;
import org.sosy_lab.cpachecker.fshell.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class BasicBlock2Coverage extends ExperimentalSeries {

  @Test
  public void test_locks_200() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_1.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(121, lResult.getNumberOfTestGoals());
    Assert.assertEquals(73, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(48, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_200_2() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_2.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(289, lResult.getNumberOfTestGoals());
    Assert.assertEquals(157, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(132, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_201() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1024, lResult.getNumberOfTestGoals());
    Assert.assertEquals(601, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(423, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(30, lResult.getNumberOfTestCases()); // TODO was 32
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_202() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1369, lResult.getNumberOfTestGoals());
    Assert.assertEquals(813, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(556, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(49, lResult.getNumberOfTestCases()); // TODO was 47, 39
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_203() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1764, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1057, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(707, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(37, lResult.getNumberOfTestCases());  // TODO was 57, 52 (and 55 before)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_204() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(2209, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1333, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(876, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(27, lResult.getNumberOfTestCases()); // TODO was 41, 68
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_205() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(2704, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1641, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1063, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(34, lResult.getNumberOfTestCases()); // TODO was 31, 66
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_206() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(3249, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1981, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1268, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(38, lResult.getNumberOfTestCases()); // TODO was 34, 47, 84
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_207() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(3844, lResult.getNumberOfTestGoals());
    Assert.assertEquals(2353, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1491, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(43, lResult.getNumberOfTestCases()); // TODO was 44, 46, 84 (85)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_208() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(4489, lResult.getNumberOfTestGoals());
    Assert.assertEquals(2757, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1732, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(96, lResult.getNumberOfTestCases()); // TODO was 59, 139 (143)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_209() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(5184, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3193, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1991, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfTestCases()); // TODO was 76, 91, 156
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_210() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(5929, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3661, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2268, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(77, lResult.getNumberOfTestCases()); // TODO was 76, 50, 171 (172)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_211() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(6724, lResult.getNumberOfTestGoals());
    Assert.assertEquals(4161, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2563, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(83, lResult.getNumberOfTestCases()); // TODO was 81, 133, 213 (211)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
