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
package org.sosy_lab.cpachecker.fshell;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fshell.experiments.ExperimentalSeries;

public class MainTest extends ExperimentalSeries {

  @Test
  public void testMain001() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@CALL(f)).\"EDGES(ID)*\"",
        "test/programs/simple/functionCall.c",
        "main",
        true
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain002() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
        "test/programs/simple/negate.c",
        "negate",
        true
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain002b() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
        "test/programs/fql/blastnondet.c",
        "foo",
        true
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain003() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".{x > 100}.EDGES(@LABEL(L)).\"EDGES(ID)*\"",
        "test/programs/simple/negate.c",
        "negate",
        true
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain004() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".EDGES(ID).\"EDGES(ID)*\"",
        "test/programs/fql/conditioncoverage.cil.c",
        "foo",
        true
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(20, lResult.getNumberOfTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(5, lResult.getNumberOfImpreciseTestCases());

    /*
     * Discussion: Creates a real valued assignment (3.5) to integer variable x!
     */
  }

  @Test
  public void testMain005() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/conditioncoverage.cil.c",
        "foo",
        true
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(18, lResult.getNumberOfTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(3, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain006() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/conditioncoverage.c",
        "foo",
        false
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(18, lResult.getNumberOfTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(3, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain007() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".NODES(@CONDITIONEDGE).\"EDGES(ID)*\"",
        "test/programs/fql/conditioncoverage.c",
        "foo",
        false
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(8, lResult.getNumberOfTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(2, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain008() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".NODES(@CONDITIONEDGE).\"EDGES(ID)*\"",
        "test/programs/fql/using_random.c",
        "foo",
        false
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(5, lResult.getNumberOfTestGoals());
    Assert.assertEquals(5, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain009() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\".NODES(@CONDITIONEDGE).\"EDGES(ID)*\"",
        "test/programs/fql/using_random_error.c",
        "foo",
        false
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(3, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  /** beginning FShell test cases (but cil preprocessed) */

  @Test
  public void testMain010() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/minimal.cil.c",
        "main",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(5, lResult.getNumberOfTestGoals());
    Assert.assertEquals(5, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: ok.
     */
  }

  @Test
  public void testMain011() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/variables.cil.c",
        "foo",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(9, lResult.getNumberOfTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: ok.
     */
  }

  @Test
  public void testMain012() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/globals.cil.c",
        "main",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(9, lResult.getNumberOfTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: ok.
     */
  }

  @Test
  public void testMain013() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/boolop-control-flow1.cil.c",
        "main",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(18, lResult.getNumberOfTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(3, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     */
  }

  @Test
  public void testMain014() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/boolop-control-flow2.cil.c",
        "main",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(9, lResult.getNumberOfTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(1, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     */
  }

  @Test
  public void testMain015() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/cov-union.cil.c",
        "main",
        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(26, lResult.getNumberOfTestGoals());
    Assert.assertEquals(23, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfTestCases());
    Assert.assertEquals(3, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     * TODO: This is a problem when replaying the test input!
     */
  }

  @Test
  public void testMain017() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        //"COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
        "test/programs/fql/basic/repeat.cil.c",
        "foo",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(15, lResult.getNumberOfTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain018() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/labels.cil.c",
        "main",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(17, lResult.getNumberOfTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(2, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     */
  }

  @Test
  public void testMain019() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/simple-control-flow.cil.c",
        "main",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(17, lResult.getNumberOfTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(2, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     */
  }

  @Test
  public void testMain020() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/test_locks_2.c",
                                        "main",
                                        false);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(43, lResult.getNumberOfTestGoals());
    Assert.assertEquals(40, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain022() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
        "test/programs/fql/simple/functionCall.c",
        "main",
        false
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(6, lResult.getNumberOfTestGoals());
    Assert.assertEquals(4, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain022b() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
        "test/programs/fql/simple/functionCall2.c",
        "main",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(4, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain023() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
        "test/programs/fql/basic/globals.cil.c",
        "main",
        false);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(4, lResult.getNumberOfTestGoals());
    Assert.assertEquals(3, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: ok. We get one more test goal than FShell 2 does.
     * x is preinitialized to 0!
     */
  }

  @Test
  public void testMain024() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
        "test/programs/fql/conditioncoverage.cil.c",
        "foo",
        true); // disable CIL preprocessing

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(9, lResult.getNumberOfTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfTestCases());
    // TODO resolve imprecise test cases
    Assert.assertEquals(2, lResult.getNumberOfImpreciseTestCases());

    /*
     * Discussion: Creates a real valued assignment (3.5) to integer variable x!
     */
  }

  @Test
  public void testMain031() throws Exception {
    String[] lArguments = Main.getParameters(
        "COVER \"EDGES(ID)*\"",
        "test/programs/simple/functionCall.c",
        "main",
        true
    );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain028() throws Exception {
    String[] lArguments = Main.getParameters("COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
                                        "test/programs/fql/arrays/infeasible_label.1.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void testMain016() throws Exception {
    String[] lArguments = Main.getParameters(
        PredefinedCoverageCriteria.STATEMENT_COVERAGE,
        "test/programs/fql/basic/undefined-func.cil.c",
        "main",
        true
        );

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(-1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: Pointer argv is not initialized correctly (and, argv is used in the program)
     */
    Assert.assertTrue(false);
  }

  @Test
  public void testMain021() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.STATEMENT_COVERAGE,
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c",
                                        "main",
                                        false);

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

  @Test
  public void testMain025() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE,
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

  @Test
  public void testMain026() throws Exception {
    String[] lArguments = Main.getParameters(PredefinedCoverageCriteria.CONDITION_COVERAGE,
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

    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    Assert.assertTrue(false);
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

  @Test
  public void testMain029() throws Exception {
    String[] lArguments = Main.getParameters("COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
                                        "test/programs/fql/arrays/assignment.1.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(0, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: This unit test fails because assignment to arrays are not handled correctly.
     */
  }

  @Test
  public void testMain030() throws Exception {
    String[] lArguments = Main.getParameters("COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
                                        "test/programs/fql/arrays/assignment.2.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);

    Assert.assertEquals(-1, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    Assert.assertTrue(false);
  }

}
