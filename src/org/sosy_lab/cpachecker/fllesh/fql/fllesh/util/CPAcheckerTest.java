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

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CPAcheckerTest {
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    org.sosy_lab.cpachecker.core.CPAchecker.logger = null;
  }

  @Test
  public void test_01() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/functionCall.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);

    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }

  @Test
  public void test_02() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/loop1.c");

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);

    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }

  @Test
  public void test_03() throws IOException, InvalidConfigurationException, CPAException {
    ImmutableMap<String, String> lProperties =
      ImmutableMap.of("analysis.programNames", "test/programs/simple/uninitVars.cil.c");

    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */

    Configuration lConfiguration = new Configuration(mPropertiesFile, lProperties);

    LogManager lLogManager = new LogManager(lConfiguration);

    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);

    // TODO: reimplement or reuse?
    //System.out.println(lCPAchecker.runAlgorithm().toString());
  }
}
