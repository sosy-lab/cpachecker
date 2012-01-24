/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class BasicBlockNodes2Coverage extends ExperimentalSeries {

  @Test
  public void test_locks_101() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(32, lResult.getNumberOfTestGoals());
    Assert.assertEquals(26, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfTestCases()); // TODO was 7
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_102() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(37, lResult.getNumberOfTestGoals());
    Assert.assertEquals(30, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfTestCases()); // TODO was 8
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_103() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(42, lResult.getNumberOfTestGoals());
    Assert.assertEquals(34, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_104() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(47, lResult.getNumberOfTestGoals());
    Assert.assertEquals(38, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(5, lResult.getNumberOfTestCases()); // TODO was 10
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_105() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(52, lResult.getNumberOfTestGoals());
    Assert.assertEquals(42, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfTestCases()); // TODO was 9
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_106() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(57, lResult.getNumberOfTestGoals());
    Assert.assertEquals(46, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(11, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfTestCases()); // TODO was 9
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_107() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(62, lResult.getNumberOfTestGoals());
    Assert.assertEquals(50, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfTestCases()); // TODO was 12
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_108() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(67, lResult.getNumberOfTestGoals());
    Assert.assertEquals(54, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(13, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfTestCases()); // TODO was 13
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_109() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(72, lResult.getNumberOfTestGoals());
    Assert.assertEquals(58, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(14, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfTestCases()); // TODO was 13
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_110() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(77, lResult.getNumberOfTestGoals());
    Assert.assertEquals(62, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfTestCases()); // TODO was 16
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_111() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_NODES_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(82, lResult.getNumberOfTestGoals());
    Assert.assertEquals(66, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(16, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfTestCases()); // TODO was 16
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
