/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;


public class PathFormulaManagerImplArraysTest0 extends SolverBasedTest0 {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void testArrayPathSat1() {
    // a[0] = 1;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

  @Test
  public void testArrayPathUnsat1() {
    // a[0] = 1;
    // if (a[0] != 1) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

  @Test
  public void testArrayPathSat2() {
    // a[0] = 1;
    // a[1] = 2;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

  @Test
  public void testArrayPathUnsat2() {
    // a[0] = 1;
    // a[1] = 2;
    // if (a[0] == 2) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

  @Test
  public void testArrayPathUnsat3() {
    // a[0] = 1;
    // a[0] = 2;
    // if (a[0] == 1) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

  @Test
  public void testArrayPathSat3() {
    // a[0] = 1;
    // a[0] = 2;
    // if (a[0] != 1) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

  @Test
  public void testArrayBranchingUnsat1() {
    // a[0] = 0;
    // if (a[1] == 10) {
    //    a[2] = 20;
    //    a[3] = 30;
    // } else {
    //    a[2] = 0;
    //    a[3] = 0;
    // }
    // if (a[2] > 100) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

  @Test
  public void testArrayBranchingUnsat2() {
    // a[0] = 0;
    // if (a[1] == 10) {
    //    a[2] = 20;
    //    a[3] = 30;
    // } else {
    //    a[2] = 0;
    // }
    // if (a[2] > 100) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

  @Test
  public void testArrayBranchingUnsat3() {
    // a[0] = 0;
    // if (a[1] == 10) {
    //    a[2] = 20;
    //    a[3] = 30;
    // }
    // if (a[0] > 10) {
    //  ERROR: ...
    // }
    fail("Not yet implemented");
  }

}
