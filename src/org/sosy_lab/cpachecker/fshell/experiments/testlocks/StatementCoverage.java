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

public class StatementCoverage extends ExperimentalSeries {

  @Test
  public void test_locks_001() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(82, lResult.getNumberOfTestGoals());
    Assert.assertEquals(76, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(5, lResult.getNumberOfTestCases()); // TODO was 6, 7
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_002() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(96, lResult.getNumberOfTestGoals());
    Assert.assertEquals(89, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfTestCases()); // TODO was 7, 8
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_003() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(110, lResult.getNumberOfTestGoals());
    Assert.assertEquals(102, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfTestCases()); // TODO was 9
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_004() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(124, lResult.getNumberOfTestGoals());
    Assert.assertEquals(115, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfTestCases()); // TODO was 5, 10
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_005() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(138, lResult.getNumberOfTestGoals());
    Assert.assertEquals(128, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfTestCases()); // TODO was 10, 9
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_006() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(152, lResult.getNumberOfTestGoals());
    Assert.assertEquals(141, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(11, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfTestCases()); // TODO was 7, 10, 9
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_007() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(166, lResult.getNumberOfTestGoals());
    Assert.assertEquals(154, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfTestCases()); // TODO was 12
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_008() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(180, lResult.getNumberOfTestGoals());
    Assert.assertEquals(167, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(13, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfTestCases()); // TODO was 13
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_009() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(194, lResult.getNumberOfTestGoals());
    Assert.assertEquals(180, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(14, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(5, lResult.getNumberOfTestCases()); // TODO was 6, 13
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_010() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(208, lResult.getNumberOfTestGoals());
    Assert.assertEquals(193, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfTestCases()); // TODO was 7, 4, 16
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_011() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(222, lResult.getNumberOfTestGoals());
    Assert.assertEquals(206, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(16, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfTestCases()); // TODO was 16
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
