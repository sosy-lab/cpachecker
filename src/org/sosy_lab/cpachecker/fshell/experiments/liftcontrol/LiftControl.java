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
package org.sosy_lab.cpachecker.fshell.experiments.liftcontrol;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fshell.FShell3Result;
import org.sosy_lab.cpachecker.fshell.Main;
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class LiftControl extends ExperimentalSeries {

  @Test
  public void test_lift_bb() throws Exception {
    String[] lArguments = Main.getParameters(
        Main.BASIC_BLOCK_COVERAGE,
        "test/programs/fql/lift_control/lift.cil.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(245, lResult.getNumberOfTestGoals());
    Assert.assertEquals(45, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(187, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(13, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_lift_bb2() throws Exception {
    String[] lArguments = Main.getParameters(
        Main.BASIC_BLOCK_2_COVERAGE,
        "test/programs/fql/lift_control/lift.cil.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(60025, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfTestCases());
    Assert.assertEquals(-1, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_lift_bb3() throws Exception {
    String[] lArguments = Main.getParameters(
        Main.BASIC_BLOCK_3_COVERAGE,
        "test/programs/fql/lift_control/lift.cil.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(14706125, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfTestCases());
    Assert.assertEquals(-1, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_lift_path1() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".PATHS(ID, 1).\"EDGES(ID)*\"",
        "test/programs/fql/lift_control/lift.cil.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(245, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfTestCases());
    Assert.assertEquals(-1, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_lift_2_bb() throws Exception {
    String[] lArguments = Main.getParameters(
        Main.BASIC_BLOCK_COVERAGE,
        "test/programs/fql/lift_control/lift_no_dbgCnt.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(245, lResult.getNumberOfTestGoals());
    Assert.assertEquals(57, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(188, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_lift_3_bb() throws Exception {
    String[] lArguments = Main.getParameters(
        Main.BASIC_BLOCK_COVERAGE,
        "test/programs/fql/lift_control/lift_no_dbgCnt_modified_ioinit.c",
        "main", true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(245, lResult.getNumberOfTestGoals());
    Assert.assertEquals(74, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(171, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
