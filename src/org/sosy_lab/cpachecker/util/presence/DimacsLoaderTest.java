/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.presence;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

import java.util.Map;


public class DimacsLoaderTest {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void testExternalModelSafe() throws Exception {
    final String safeProgramFile = "test/programs/simple/externalModelTest_SAFE.c";
    TestResults runResult = runProgram(safeProgramFile);
    runResult.assertIsSafe();
  }

  @Test
  public void testExternalModelUnSafe() throws Exception {
    final String safeProgramFile = "test/programs/simple/externalModelTest_UNSAFE.c";
    TestResults runResult = runProgram(safeProgramFile);
    runResult.assertIsUnsafe();
  }

  private TestResults runProgram(final String pProgramFile)
      throws Exception {

    Map<String, String> prop = ImmutableMap.of(
        "cfa.useMultiEdges",                "FALSE",
        "automata.properties.granularity",  "BASENAME",
        "analysis.checkCounterexamples", "FALSE",
        "cpa.predicate.externModelVariablePrefix", ""
      );

    Configuration cfg = TestDataTools.configurationForTest()
        .loadFromFile("config/predicateAnalysis-PredAbsRefiner-ABEl-bitprecise.properties")
        .setOptions(prop)
        .build();

    return CPATestRunner.run(cfg, pProgramFile, false);
  }
}
