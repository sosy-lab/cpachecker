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

public class SSHSimplified {

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
  public void ssh_001() throws Exception {
    String lCFile = "s3_clnt_1_BUG.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_002() throws Exception {
    String lCFile = "s3_clnt_1.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_003() throws Exception {
    String lCFile = "s3_clnt_2_BUG.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_004() throws Exception {
    String lCFile = "s3_clnt_2.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_005() throws Exception {
    String lCFile = "s3_clnt_3_BUG.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_006() throws Exception {
    String lCFile = "s3_clnt_3.cil_org.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_007() throws Exception {
    String lCFile = "s3_clnt_3.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_008() throws Exception {
    String lCFile = "s3_clnt_4_BUG.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_009() throws Exception {
    String lCFile = "s3_clnt_4.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_010() throws Exception {
    String lCFile = "s3_srvr_1_BUG.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_011() throws Exception {
    String lCFile = "s3_srvr_1.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_012() throws Exception {
    String lCFile = "s3_srvr_2_BUG.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_013() throws Exception {
    String lCFile = "s3_srvr_2.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_014() throws Exception {
    String lCFile = "s3_srvr_3.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_015() throws Exception {
    String lCFile = "s3_srvr_4.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_016() throws Exception {
    String lCFile = "s3_srvr_6.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_017() throws Exception {
    String lCFile = "s3_srvr_7.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void ssh_018() throws Exception {
    String lCFile = "s3_srvr_8.cil.c";
    
    String[] lArguments = getParameters(STATEMENT_COVERAGE,
                                        "test/programs/ssh-simplified/" + lCFile,
                                        "main",
                                        true);
    
    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    mExperiment.addExperiment(lCFile + " (stmtcov)", Main.mResult, (lEndTime - lStartTime)/1000.0);
    
    Assert.assertEquals(80, Main.mResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(74, Main.mResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(6, Main.mResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(12, Main.mResult.getNumberOfTestCases());
    Assert.assertEquals(0, Main.mResult.getNumberOfImpreciseTestCases());
  }
  
}
