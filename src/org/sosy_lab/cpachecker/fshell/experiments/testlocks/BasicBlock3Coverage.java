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

public class BasicBlock3Coverage extends ExperimentalSeries {

  @Test
  public void test_locks_201() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(32768, lResult.getNumberOfTestGoals());
    Assert.assertEquals(13824, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(18944, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(199, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_202() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1369, lResult.getNumberOfTestGoals());
    Assert.assertEquals(813, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(556, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(47, lResult.getNumberOfTestCases()); // TODO was 39
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_203() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1764, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1057, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(707, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(57, lResult.getNumberOfTestCases());  // TODO was 52 (and 55 before)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_204() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(2209, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1333, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(876, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(41, lResult.getNumberOfTestCases()); // TODO was 68
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_205() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(2704, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1641, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1063, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(31, lResult.getNumberOfTestCases()); // TODO was 66
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_206() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(3249, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1981, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1268, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(47, lResult.getNumberOfTestCases()); // TODO was 84
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_207() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(3844, lResult.getNumberOfTestGoals());
    Assert.assertEquals(2353, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1491, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(46, lResult.getNumberOfTestCases()); // TODO was 84 (85)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_208() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(4489, lResult.getNumberOfTestGoals());
    Assert.assertEquals(2757, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1732, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(59, lResult.getNumberOfTestCases()); // TODO was 139 (143)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_209() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(5184, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3193, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1991, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(91, lResult.getNumberOfTestCases()); // TODO was 156
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_210() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(5929, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3661, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2268, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(50, lResult.getNumberOfTestCases()); // TODO was 171 (172)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_211() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(6724, lResult.getNumberOfTestGoals());
    Assert.assertEquals(4161, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2563, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(133, lResult.getNumberOfTestCases()); // TODO was 213 (211)
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
