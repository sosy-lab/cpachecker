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
package org.sosy_lab.cpachecker.fllesh;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.fllesh.experiments.Experiment;

public class MainTest {

  private String[] getParameters(String pQuery, String pSource, String pEntryFunction, boolean pDisablePreprocessing) {
    List<String> lArguments = new LinkedList<String>();
    lArguments.add(pQuery);
    lArguments.add(pSource);
    lArguments.add(pEntryFunction);
    
    String[] lResult;
    
    if (pDisablePreprocessing) {
      lArguments.add("disablecilpreprocessing");
      
      lResult = new String[4];
    }
    else {
      lResult = new String[3];
    }
    
    return lArguments.toArray(lResult);
  }
  
  private static Experiment mExperiment = null;
  
  private static String BASIC_BLOCK_COVERAGE = "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  private static String STATEMENT_COVERAGE = "COVER \"EDGES(ID)*\".NODES(ID).\"EDGES(ID)*\"";
  private static String CONDITION_COVERAGE = "COVER \"EDGES(ID)*\".EDGES(@CONDITIONEDGE).\"EDGES(ID)*\"";
  
  @BeforeClass
  public static void createLogFile() {
    if (mExperiment != null) {
      throw new RuntimeException();
    }
    
    SimpleDateFormat lDateFormat = new SimpleDateFormat("'log.'yyyy-MM-dd'.'HH-mm-ss'.csv'");
    String lFileName = "test" + File.separator + "output" + File.separator + lDateFormat.format(new Date());
    
    mExperiment = new Experiment(lFileName);
  }
  
  @AfterClass
  public static void closeLogFile() {
    mExperiment.close();
    
    mExperiment = null;
  }
  
  @Test
  public void testMain001() throws Exception {
    String[] lArguments = getParameters(
        "COVER \"EDGES(ID)*\".EDGES(@CALL(f)).\"EDGES(ID)*\"",
        "test/programs/simple/functionCall.c",
        "main",
        true
    );

    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("001", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(1, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());    
  }

  @Test
  public void testMain002() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = "COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"";
    lArguments[1] = "test/programs/simple/negate.c";
    lArguments[2] = "negate";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("002", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(1, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
  }

  @Test
  public void testMain003() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = "COVER \"EDGES(ID)*\".{x > 100}.EDGES(@LABEL(L)).\"EDGES(ID)*\"";
    lArguments[1] = "test/programs/simple/negate.c";
    lArguments[2] = "negate";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("003", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(1, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
  }

  @Test
  public void testMain004() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = "COVER \"EDGES(ID)*\".EDGES(ID).\"EDGES(ID)*\"";
    lArguments[1] = "test/programs/fql/conditioncoverage.cil.c";
    lArguments[2] = "foo";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("004", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(20, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(20, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(3, Main.mResult.getNumberOfTestCases());
    
    /*
     * Discussion: Creates a real valued assignment (3.5) to integer variable x!
     * 
     */
  }
  
  @Test
  public void testMain005() throws Exception {
    String[] lArguments = new String[4];

    // This query should be equivalent to statement coverage
    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/conditioncoverage.cil.c";
    lArguments[2] = "foo";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("005", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(18, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(18, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(3, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void testMain006() throws Exception {
    String[] lArguments = new String[3];

    // This query should be equivalent to statement coverage
    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/conditioncoverage.c";
    lArguments[2] = "foo";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("006", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
  }
  
  @Test
  public void testMain007() throws Exception {
    String[] lArguments = new String[3];

    lArguments[0] = "COVER \"EDGES(ID)*\".NODES(@CONDITIONEDGE).\"EDGES(ID)*\"";
    lArguments[1] = "test/programs/fql/conditioncoverage.c";
    lArguments[2] = "foo";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("007", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
  }
  
  @Test
  public void testMain008() throws Exception {
    String[] lArguments = new String[3];

    lArguments[0] = "COVER \"EDGES(ID)*\".NODES(@CONDITIONEDGE).\"EDGES(ID)*\"";
    lArguments[1] = "test/programs/fql/using_random.c";
    lArguments[2] = "foo";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("008", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
  }
  
  @Test
  public void testMain009() throws Exception {
    String[] lArguments = new String[3];

    lArguments[0] = "COVER \"EDGES(ID)*\".NODES(@CONDITIONEDGE).\"EDGES(ID)*\"";
    lArguments[1] = "test/programs/fql/using_random_error.c";
    lArguments[2] = "foo";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("009", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
  }
  
  /** beginning FShell test cases (but cil preprocessed) */
  
  @Test
  public void testMain010() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/minimal.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("010", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(5, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(5, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: ok.
     */
  }
  
  @Test
  public void testMain011() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/variables.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("011", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(6, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: ok.
     */
  }
  
  @Test
  public void testMain012() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/globals.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("012", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(9, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(7, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: ok.
     */
  }
  
  @Test
  public void testMain013() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/boolop-control-flow1.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("013", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(18, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(18, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain014() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/boolop-control-flow2.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("014", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(9, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain015() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/cov-union.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("015", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(21, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(21, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     * TODO: This is a problem when replaying the test input!
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain016() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/undefined-func.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("016", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(-1, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(7, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: Pointer argv is not initialized correctly (and, argv is used in the program)
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain017() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/repeat.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("017", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(16, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void testMain018() throws Exception {
    String[] lArguments = new String[4];

    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/labels.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("018", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(17, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(17, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain019() throws Exception {
    String[] lArguments = new String[4];
    
    lArguments[0] = STATEMENT_COVERAGE;
    lArguments[1] = "test/programs/fql/basic/simple-control-flow.cil.c";
    lArguments[2] = "main";
    lArguments[3] = "disablecilpreprocessing";
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("019", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(17, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(17, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: Not integers, but reals are calculated as test inputs.
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain020() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE, 
                                        "test/programs/fql/test_locks_2.c", 
                                        "main", 
                                        false);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("020", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(44, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(45, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(3, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain021() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE, 
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c", 
                                        "main", 
                                        false);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("021", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(690, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain022() throws Exception {
    String[] lArguments = new String[2];

    lArguments[0] = BASIC_BLOCK_COVERAGE;
    lArguments[1] = "test/programs/simple/functionCall.c";

    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("022", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain023() throws Exception {
    String[] lArguments = getParameters(
        BASIC_BLOCK_COVERAGE, 
        "test/programs/fql/basic/globals.cil.c", 
        "main", 
        false);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("023", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(3, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(2, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: ok. We get one more test goal than FShell 2 does.
     */
  }
  
  @Test
  public void testMain024() throws Exception {
    String[] lArguments = getParameters(
        BASIC_BLOCK_COVERAGE, 
        "test/programs/fql/conditioncoverage.cil.c", 
        "foo", 
        true); // disable CIL preprocessing
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("024", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(6, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(3, Main.mResult.getNumberOfTestCases());
    
    /*
     * Discussion: Creates a real valued assignment (3.5) to integer variable x!
     * 
     * Generates a different amount of test goals: 
     *  
     * E.g., there is no test goal generated for the structure
     * 
     * if (a) {
     * }
     * else {
     * }
     * 
     */
    
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain025() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE, 
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c", 
                                        "main", 
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("025", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(690, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain026() throws Exception {
    String[] lArguments = getParameters(CONDITION_COVERAGE, 
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c",
                                        //"/home/andreas/ase-experimente/kbfiltr.c",
                                        "main", 
                                        //false);
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("026", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(690, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain027() throws Exception {
    String[] lArguments = getParameters("COVER \"EDGES(ID)*\".EDGES(@CONDITIONEDGE).\"EDGES(ID)*\".EDGES(@CONDITIONEDGE).\"EDGES(ID)*\"", 
                                        "test/programs/fql/ntdrivers/kbfiltr.i.cil.c", 
                                        "main", 
                                        false);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("027", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(690, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain028() throws Exception {
    String[] lArguments = getParameters("COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
                                        "test/programs/fql/arrays/infeasible_label.1.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("028", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(-1, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain029() throws Exception {
    String[] lArguments = getParameters("COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
                                        "test/programs/fql/arrays/assignment.1.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("029", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(-1, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain030() throws Exception {
    String[] lArguments = getParameters("COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
                                        "test/programs/fql/arrays/assignment.2.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("030", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(-1, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    Assert.assertTrue(false);
  }
  
  @Test
  public void testMain031() throws Exception {
    String[] lArguments = getParameters(//STATEMENT_COVERAGE,
                                        //CONDITION_COVERAGE,
                                        "COVER \"EDGES(ID)*\".EDGES(@LABEL(L)).\"EDGES(ID)*\"",
                                        //BASIC_BLOCK_COVERAGE, 
                                        //"/home/andreas/ase-experimente/sort1.c", 
                                        //"/home/andreas/ase-experimente/list2.i",
                                        //"/home/andreas/ase-experimente/adapted/list2.c",
                                        "/home/andreas/ase-experimente/modeling/test.c",
                                        "main",
                                        //false);
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("031", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(-1, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, Main.mResult.getNumberOfInfeasibleTestGoals());
    // TODO this is a dummy test case
    Assert.assertEquals(1, Main.mResult.getNumberOfTestCases());
    
    Assert.assertTrue(false);
  }
  
}
