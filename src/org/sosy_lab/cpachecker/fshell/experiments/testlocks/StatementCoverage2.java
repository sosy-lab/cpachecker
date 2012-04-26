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

public class StatementCoverage2 extends ExperimentalSeries {

  @Test
  public void test_locks_001() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(6724, lResult.getNumberOfTestGoals());
    Assert.assertEquals(4637, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2087, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(31, lResult.getNumberOfTestCases()); // TODO was 7
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_002() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(9216, lResult.getNumberOfTestGoals());
    Assert.assertEquals(6420, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2796, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(48, lResult.getNumberOfTestCases()); // TODO was 8
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_003() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(12100, lResult.getNumberOfTestGoals());
    Assert.assertEquals(8493, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(3607, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(41, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_004() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(15376, lResult.getNumberOfTestGoals());
    Assert.assertEquals(10856, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(4520, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(30, lResult.getNumberOfTestCases()); // TODO was 10
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_005() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(19044, lResult.getNumberOfTestGoals());
    Assert.assertEquals(13509, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(5535, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(33, lResult.getNumberOfTestCases()); // TODO was 9
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_006() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(23104, lResult.getNumberOfTestGoals());
    Assert.assertEquals(16452, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6652, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(39, lResult.getNumberOfTestCases()); // TODO was 9
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_007() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(27556, lResult.getNumberOfTestGoals());
    Assert.assertEquals(19685, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(7871, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(45, lResult.getNumberOfTestCases()); // TODO was 12
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_008() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(32400, lResult.getNumberOfTestGoals());
    Assert.assertEquals(23208, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(9192, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(89, lResult.getNumberOfTestCases()); // TODO was 13
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_009() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(37636, lResult.getNumberOfTestGoals());
    Assert.assertEquals(27021, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(10615, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(70, lResult.getNumberOfTestCases()); // TODO was 13
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_010() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(43264, lResult.getNumberOfTestGoals());
    Assert.assertEquals(31124, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(12140, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(75, lResult.getNumberOfTestCases()); // TODO was 16
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_011() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(49284, lResult.getNumberOfTestGoals());
    Assert.assertEquals(35517, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(13767, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(95, lResult.getNumberOfTestCases()); // TODO was 16
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
