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
package org.sosy_lab.cpachecker.fshell.experiments.ntdrivers;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fshell.FShell3Result;
import org.sosy_lab.cpachecker.fshell.Main;
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class KBFilterTest extends ExperimentalSeries {

  @Test
  public void testMain021() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(690, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    Assert.assertTrue(false);
  }

/*  @Test
  public void testMain025() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(690, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  */
    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    /*Assert.assertTrue(false);
  }

  @Test
  public void testMain026() throws Exception {
    String[] lArguments = Main.getParameters(Main.CONDITION_COVERAGE,
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c",
                                        //"/home/andreas/ase-experimente/kbfiltr.c",
                                        "main",
                                        //false);
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(690, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
    */
    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    /*Assert.assertTrue(false);
  }

  @Test
  public void testMain027() throws Exception {
    String[] lArguments = Main.getParameters("COVER \"EDGES(ID)*\".EDGES(@CONDITIONEDGE).\"EDGES(ID)*\".EDGES(@CONDITIONEDGE).\"EDGES(ID)*\"",
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c",
                                        "main",
                                        false);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(690, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    Assert.assertTrue(false);
  }
  */
}
