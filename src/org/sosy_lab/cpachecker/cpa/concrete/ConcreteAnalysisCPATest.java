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
package org.sosy_lab.cpachecker.cpa.concrete;

import org.junit.Test;

public class ConcreteAnalysisCPATest {

  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/concreteAnalysis.properties";

  @Test
  public void test_01() {
    String[] lArguments = new String[3];

    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/functionCall.c";

    org.sosy_lab.cpachecker.cmdline.CPAMain.main(lArguments);
  }

  @Test
  public void test_02() {
    String[] lArguments = new String[3];

    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/loop1.c";

    org.sosy_lab.cpachecker.cmdline.CPAMain.main(lArguments);
  }

  @Test
  public void test_03() {
    String[] lArguments = new String[3];

    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/programs/simple/uninitVars.cil.c";

    org.sosy_lab.cpachecker.cmdline.CPAMain.main(lArguments);
  }

/*  @Test
  public void testConcreteAnalysisCPA() {
    String[] lArguments = new String[3];

    lArguments[0] = "-config";
    lArguments[1] = "test/config/concreteAnalysis.properties";
    lArguments[2] = "test/programs/simple/functionCall.c";

    org.sosy_lab.cpachecker.cmdline.CPAMain.main(lArguments);

    fail("Not yet implemented");
  }

  @Test
  public void testGetAbstractDomain() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetMergeOperator() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetStopOperator() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetTransferRelation() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetInitialElement() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetInitialPrecision() {
    fail("Not yet implemented");
  }

  @Test
  public void testGetPrecisionAdjustment() {
    fail("Not yet implemented");
  }
*/
}
