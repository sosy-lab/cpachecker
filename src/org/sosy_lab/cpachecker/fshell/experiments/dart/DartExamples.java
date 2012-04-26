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
package org.sosy_lab.cpachecker.fshell.experiments.dart;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fshell.FShell3Result;
import org.sosy_lab.cpachecker.fshell.Main;
import org.sosy_lab.cpachecker.fshell.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class DartExamples extends ExperimentalSeries {

  @Test
  public void test_page2_bb() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/DART/page2.c",
                                        "h",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(5, lResult.getNumberOfTestGoals());
    Assert.assertEquals(5, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_page5_1_bb() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/DART/page5-1.c",
                                        "f",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(4, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_page5_2_bb() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/DART/page5-2.c",
                                        "bar",
                                        true);

    FShell3Result lResult = execute(lArguments);

    // TODO we do not support data structures yet

    Assert.assertEquals(4, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_page5_3_bb() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/DART/page5-3.c",
                                        "foobar",
                                        true);

    FShell3Result lResult = execute(lArguments);

    // TODO nonlinearity seams to be a problem

    Assert.assertEquals(8, lResult.getNumberOfTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_page6_bb() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/DART/page6.c",
                                        "ac_controller",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(13, lResult.getNumberOfTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
