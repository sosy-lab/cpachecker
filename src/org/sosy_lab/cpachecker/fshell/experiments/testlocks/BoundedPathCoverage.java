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
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class BoundedPathCoverage extends ExperimentalSeries {

  @Test
  public void test_locks_1_k1() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 1).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(7, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_1_k2() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 2).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(31, lResult.getNumberOfTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(24, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_1_k3() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 3).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(127, lResult.getNumberOfTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(112, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_1_k4() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 4).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(511, lResult.getNumberOfTestGoals());
    Assert.assertEquals(31, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(480, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(31, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_1_k5() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 5).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(2047, lResult.getNumberOfTestGoals());
    Assert.assertEquals(63, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1984, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(63, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_1_k6() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 6).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(8191, lResult.getNumberOfTestGoals());
    Assert.assertEquals(127, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(8064, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(127, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_1_k7() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 7).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    // 611.402s
    Assert.assertEquals(32767, lResult.getNumberOfTestGoals());
    Assert.assertEquals(255, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(32512, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(255, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_1_k8() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 8).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(32767, lResult.getNumberOfTestGoals());
    Assert.assertEquals(255, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(32512, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(255, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
