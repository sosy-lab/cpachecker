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
                                        "test/programs/locks/test_locks_5.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_5.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_002() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_6.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_6.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(93, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(86, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(7, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(20, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_003() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_7.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_7.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(106, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(98, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(8, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_004() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_8.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_8.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(119, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(110, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(34, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_005() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_9.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_9.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(132, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(122, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(10, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(28, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_006() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_10.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_10.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(145, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(134, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(11, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(28, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_007() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_11.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_11.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(158, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(146, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(33, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_008() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_12.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_12.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(171, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(158, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(13, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(47, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_009() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_13.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_13.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(184, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(170, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(14, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(55, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_010() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_14.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_14.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(197, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(182, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(15, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(51, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_011() throws Exception {
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/locks/test_locks_15.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_15.c (stmtcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(210, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(194, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(63, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_101() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_5.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_5.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(14, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(14, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(5, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_102() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_6.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_6.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(16, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(16, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_103() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_7.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_7.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(18, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(18, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(5, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_104() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_8.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_8.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(20, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(20, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(18, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_105() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_9.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_9.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(22, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(22, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(9, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_106() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_10.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_10.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(24, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(24, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(8, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_107() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_11.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_11.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(26, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(26, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(10, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_108() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_12.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_12.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(28, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(28, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(22, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_109() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_13.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_13.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(30, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(30, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(28, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_110() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_14.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_14.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(32, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(32, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(22, Main.mResult.getNumberOfTestCases());
  }
  
  @Test
  public void test_locks_111() throws Exception {
    String[] lArguments = getParameters(BASIC_BLOCK_COVERAGE,
                                        "test/programs/locks/test_locks_15.c",
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment("test_locks_15.c (bbcov)", Main.mResult.getTask().getNumberOfTestGoals(), Main.mResult.getNumberOfFeasibleTestGoals(), Main.mResult.getNumberOfInfeasibleTestGoals(), Main.mResult.getNumberOfTestCases(), (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(34, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(34, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(0, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(32, Main.mResult.getNumberOfTestCases());
  }
  
}
