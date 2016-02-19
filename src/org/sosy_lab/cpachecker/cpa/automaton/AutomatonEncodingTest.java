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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.IOException;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;
import org.sosy_lab.cpachecker.util.test.TestRunStatisticsParser;

import com.google.common.collect.ImmutableMap;

public class AutomatonEncodingTest {

  @Ignore
  @Test
  public void testEncodingOfLdvRule100_Safe() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_100_1a.spc";
    final String programFile = "test/config/automata/encode/true_100_1a_1.c";

    TestResults results = runWithAutomataEncoding(specFile, programFile);

    results.assertIsSafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());
  }

  @Test
  public void testEncodingOfLdvRule8_Safe() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_08_1a_encode.spc";
    final String programFile = "test/config/automata/encode/true_08_1a_2.c";

    TestResults results = runWithAutomataEncoding(specFile, programFile);

    results.assertIsSafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());
  }

  @Test
  public void testEncodingOfLdvRule118_Safe() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/ldv_118_test.c";

    TestResults results = runWithAutomataEncoding(specFile, programFile);

    results.assertIsSafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Number of times merged").isAtLeast(2);
    stat.assertThatNumber("Number of refinements").isAtMost(3);
  }

  @Test
  public void testEncodingOfLdvRule118_Unsafe() throws Exception {
    final String specFile = "test/config/automata/encode/LDV_118_1a_encode.spc";
    final String programFile = "test/config/automata/encode/ldv_118_test_false.c";

    TestResults results = runWithAutomataEncoding(specFile, programFile);

    results.assertIsUnsafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());

    stat.assertThatNumber("Number of times merged").isAtLeast(0);
    stat.assertThatNumber("Number of successful refinements").isAtMost(3);
    stat.assertThatNumber("Max states per location").isAtMost(2);
  }

  private TestResults runWithAutomataEncoding(final String specFile, final String programFile)
      throws InvalidConfigurationException, IOException, Exception {

    Map<String, String> prop = ImmutableMap.of(
        "specification",                    specFile,
        "cpa.predicate.ignoreIrrelevantVariables", "TRUE",
        "cfa.useMultiEdges",                "FALSE",
        "automata.properties.granularity",  "BASENAME",
        "analysis.checkCounterexamples", "FALSE"
      );

    Configuration cfg = TestDataTools.configurationForTest()
        .loadFromFile("config/predicateAnalysis-PredAbsRefiner-ABEl-bitprecise.properties")
        .setOptions(prop)
        .build();

    TestResults results = CPATestRunner.run(cfg, programFile, false);

    return results;
  }

  @Test
  public void testAssumeOnNamedArgument_Safe() throws Exception {
    final String specFile = "test/config/automata/encode/SPEC_sqrt.spc";
    final String programFile = "test/config/automata/encode/SPEC_sqrt_true.c";

    TestResults results = runWithAutomataEncoding(specFile, programFile);

    results.assertIsSafe();

    TestRunStatisticsParser stat = new TestRunStatisticsParser();
    results.getCheckerResult().printStatistics(stat.getPrintStream());
  }


}
