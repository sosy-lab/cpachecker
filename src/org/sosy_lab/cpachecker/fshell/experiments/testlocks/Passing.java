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

public class Passing extends ExperimentalSeries {

  @Test
  public void test_locks_1() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"",
        "test/programs/fql/locks/test_locks_1_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(11, lResult.getNumberOfTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_2() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_1_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(11, lResult.getNumberOfTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_3() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_5_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(32, lResult.getNumberOfTestGoals());
    Assert.assertEquals(26, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_4() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_10_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(57, lResult.getNumberOfTestGoals());
    Assert.assertEquals(46, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(11, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_5() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_15_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(82, lResult.getNumberOfTestGoals());
    Assert.assertEquals(66, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(16, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_6() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_20_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(107, lResult.getNumberOfTestGoals());
    Assert.assertEquals(86, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(21, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_7() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_1_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(11, lResult.getNumberOfTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_8() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_5_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(32, lResult.getNumberOfTestGoals());
    Assert.assertEquals(26, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_9() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_10_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(57, lResult.getNumberOfTestGoals());
    Assert.assertEquals(46, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(11, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_10() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_15_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(82, lResult.getNumberOfTestGoals());
    Assert.assertEquals(66, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(16, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_11() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\" PASSING EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*.EDGES(@LABEL(L)).EDGES(ID)*",
        "test/programs/fql/locks/test_locks_20_labeled.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(107, lResult.getNumberOfTestGoals());
    Assert.assertEquals(86, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(21, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
