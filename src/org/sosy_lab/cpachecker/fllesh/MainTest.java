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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MainTest {

  private static Experiment mExperiment = null;
  
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
    String[] lArguments = new String[2];

    lArguments[0] = "COVER \"EDGES(ID)*\".EDGES(@CALL(f)).\"EDGES(ID)*\"";
    lArguments[1] = "test/programs/simple/functionCall.c";

    long lStartTime = System.currentTimeMillis();
    
    Main.main(lArguments);
    
    long lEndTime = System.currentTimeMillis();
    
    // TODO give correct values
    mExperiment.addExperiment("001", 0, 0, 0, 0, (lEndTime - lStartTime)/1000.0);
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
    
    // TODO give correct values
    mExperiment.addExperiment("002", 0, 0, 0, 0, (lEndTime - lStartTime)/1000.0);
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
    
    // TODO give correct values
    mExperiment.addExperiment("003", 0, 0, 0, 0, (lEndTime - lStartTime)/1000.0);
  }
  
}
