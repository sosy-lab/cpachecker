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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class CillyTest {

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
  }

  @Test
  public void test001() throws IOException {
    Cilly lCilly = new Cilly();

    lCilly.cillyfy("test/programs/simple/functionCall.c");
  }

  @Test
  public void test002() throws IOException {
    Cilly lCilly = new Cilly();

    assertFalse(lCilly.isCillyInvariant("test/programs/simple/functionCall.c"));
  }

  @Test
  public void test003() throws IOException {
    Cilly lCilly = new Cilly();

    File lCillyfiedFile = lCilly.cillyfy("test/programs/simple/functionCall.c");

    System.out.println(lCillyfiedFile);

    assertTrue(lCilly.isCillyInvariant(lCillyfiedFile));
  }

  @Test
  public void test004() throws IOException {
    assertEquals("test/programs/simple/functionCall.cil.c", Cilly.getNiceCILName("test/programs/simple/functionCall.c"));
  }

}
