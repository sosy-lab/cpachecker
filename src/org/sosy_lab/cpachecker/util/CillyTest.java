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
package org.sosy_lab.cpachecker.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

public class CillyTest {

  private static LogManager logger;

  @BeforeClass
  public static void setup() throws IOException, InvalidConfigurationException {
    Configuration config = Configuration.defaultConfiguration();
    logger = new LogManager(config);
  }

  @Test
  public void test001() throws IOException {
    Cilly lCilly = new Cilly(logger);

    lCilly.cillyfy("test/programs/simple/functionCall.c");
  }

  @Test
  public void test002() throws IOException {
    Cilly lCilly = new Cilly(logger);

    assertFalse(lCilly.isCillyInvariant("test/programs/simple/functionCall.c"));
  }

  @Test
  public void test003() throws IOException {
    Cilly lCilly = new Cilly(logger);

    File lCillyfiedFile = lCilly.cillyfy("test/programs/simple/functionCall.c");

    System.out.println(lCillyfiedFile);

    assertTrue(lCilly.isCillyInvariant(lCillyfiedFile));
  }

  @Test
  public void test004() throws IOException {
    assertEquals("test/programs/simple/functionCall.cil.c", Cilly.getNiceCILName("test/programs/simple/functionCall.c"));
  }

}
