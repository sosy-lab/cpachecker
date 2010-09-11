/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.experiments.testlocks;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fllesh.FlleShResult;
import org.sosy_lab.cpachecker.fllesh.Main;
import org.sosy_lab.cpachecker.fllesh.experiments.ExperimentalSeries;

public class StatementCoverage extends ExperimentalSeries {
  
  @Test
  public void test_locks_001() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(83, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(77, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_002() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(97, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(90, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(7, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_003() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(111, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(103, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(8, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_004() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(125, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(116, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_005() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(139, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(129, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(10, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_006() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(153, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(142, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(11, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_007() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(167, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(155, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_008() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(181, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(168, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(13, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(13, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_009() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(195, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(181, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(14, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(13, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_010() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(209, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(194, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(15, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(16, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_011() throws Exception {
    String[] lArguments = Main.getParameters(Main.STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(223, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(207, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(16, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(16, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
}
