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
package org.sosy_lab.cpachecker.fllesh.experiments;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.fllesh.Main;

public class TestLocks {

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
  
  private static String STATEMENT_COVERAGE = "COVER \"EDGES(ID)*\".NODES(ID).\"EDGES(ID)*\"";
  private static String BASIC_BLOCK_COVERAGE = "COVER \"EDGES(ID)*\".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  private static String BASIC_BLOCK_2_COVERAGE = BASIC_BLOCK_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  //private static String BASIC_BLOCK_3_COVERAGE = BASIC_BLOCK_2_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  //private static String BASIC_BLOCK_4_COVERAGE = BASIC_BLOCK_3_COVERAGE + ".EDGES(@BASICBLOCKENTRY).\"EDGES(ID)*\"";
  
  @BeforeClass
  public static void createLogFile() {
    if (mExperiment != null) {
      throw new RuntimeException();
    }
    
    SimpleDateFormat lDateFormat = new SimpleDateFormat("'log.test_locks.'yyyy-MM-dd'.'HH-mm-ss'.csv'");
    String lFileName = "test" + File.separator + "output" + File.separator + lDateFormat.format(new Date());
    
    mExperiment = new Experiment(lFileName);
  }
  
  @AfterClass
  public static void closeLogFile() {
    mExperiment.close();
    
    mExperiment = null;
  }
  
  @Test
  public void test_locks_001() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_5.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(83, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(77, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(7, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_002() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_6.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(97, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(90, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(7, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_003() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_7.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(111, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(103, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(8, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_004() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_8.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(125, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(116, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(10, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_005() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_9.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(139, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(129, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(10, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_006() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_10.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(153, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(142, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(11, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_007() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_11.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(167, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(155, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_008() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_12.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(181, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(168, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(13, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(13, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_009() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_13.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(195, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(181, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(14, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(13, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_010() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_14.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(209, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(194, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(15, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_011() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_15.c (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(223, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(207, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_101() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_5.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(32, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(26, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(7, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_102() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_6.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(37, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(30, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(7, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_103() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_7.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(42, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(34, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(8, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_104() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_8.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(47, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(38, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(10, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_105() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_9.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(52, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(42, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(10, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_106() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_10.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(57, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(46, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(11, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_107() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_11.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(62, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(50, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_108() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_12.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(67, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(54, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(13, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(13, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_109() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_13.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(72, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(58, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(14, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(13, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_110() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_14.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(77, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(62, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(15, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_111() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_15.c (bbcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(82, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(66, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }

  @Test
  public void test_locks_201() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_5.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(1024, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(601, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(423, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(32, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_202() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_6.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(1369, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(813, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(556, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(39, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_203() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_7.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(1764, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1057, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(707, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(52, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_204() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_8.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(2209, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1333, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(876, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(68, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_205() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_9.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(2704, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1641, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1063, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(66, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_206() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_10.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(3249, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1981, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1268, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(84, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_207() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_11.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(3844, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(2353, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1491, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(84, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_208() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_12.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(4489, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(2757, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1732, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(139, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_209() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_13.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(5184, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(3193, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1991, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(156, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_210() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_14.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(5929, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(3661, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2268, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(171, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_211() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_15.c (bb2cov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(6724, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(4161, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2563, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(213, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }

}
