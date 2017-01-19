/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.policyiteration.tests;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

/**
 * Testing summary generation support for LPI.
 */
public class LPISummariesTest {

  private static final String TEST_DIR_PATH = "test/programs/policyiteration/recursive/";

  @Test
  public void fibonacci_true_assert() throws Exception {
    checkWithSummaries("fibonacci_true_assert.c");
  }

  @Test public void fibonacci_false_assert() throws Exception {
    checkWithSummaries("fibonacci_false_assert.c");
  }

  @Test public void call_in_loop_true_assert() throws Exception {
    checkWithSummaries("call_in_loop_true_assert.c");
  }

  @Test public void call_in_loop_false_assert() throws Exception {
    checkWithSummaries("call_in_loop_false_assert.c");
  }

  @Test public void recursion_counter_true_assert() throws Exception {
    checkWithSummaries("recursion_counter_true_assert.c");
  }

  @Test public void recursion_counter_false_assert() throws Exception {
    checkWithSummaries("recursion_counter_false_assert.c");
  }

  @Test public void check_in_func_false_assert() throws Exception {
    checkWithSummaries("check_in_func_false_assert.c");
  }

  @Test public void check_in_func_true_assert() throws Exception {
    checkWithSummaries("check_in_func_true_assert.c");
  }

  @Test public void conditionals_false_assert() throws Exception {
    checkWithSummaries("conditionals_false_assert.c");
  }

  @Test public void conditionals_true_assert() throws Exception {
    checkWithSummaries("conditionals_true_assert.c");
  }

  @Test public void counting_false_assert() throws Exception {
    checkWithSummaries("counting_false_assert.c");
  }

  @Test public void counting_true_assert() throws Exception {
    checkWithSummaries("counting_true_assert.c");
  }

  @Test
  public void globals_modified_false_assert() throws Exception {
    checkWithSummaries("globals_modified_false_assert.c");
  }

  @Test public void globals_modified_true_assert() throws Exception {
    checkWithSummaries("globals_modified_true_assert.c");
  }

  @Test public void mutual_recursive_false_assert() throws Exception {
    checkWithSummaries("mutual_recursive_false_assert.c");
  }

  @Test public void mutual_recursive_true_assert() throws Exception {
    checkWithSummaries("mutual_recursive_true_assert.c");
  }

  @Test public void nobasecase_true_assert() throws Exception {
    checkWithSummaries("nobasecase_true_assert.c");
  }

  @Test public void recursive_sum_false_assert() throws Exception {
    checkWithSummaries("recursive_sum_false_assert.c");
  }

  @Test public void recursive_sum_true_assert() throws Exception {
    checkWithSummaries("recursive_sum_true_assert.c");
  }

  @Test public void simple_false_assert() throws Exception {
    checkWithSummaries("simple_false_assert.c");
  }

  @Test public void simple_true_assert() throws Exception {
    checkWithSummaries("simple_true_assert.c");
  }

  @Test public void param_renamed_false_assert() throws Exception {
    checkWithSummaries("param_renamed_false_assert.c");
  }

  @Test public void param_renamed_true_assert() throws Exception {
    checkWithSummaries("param_renamed_true_assert.c");
  }

  private void checkWithSummaries(String filename) throws Exception {
    check(filename,
        getProperties("policyIteration-with-summaries.properties",
            ImmutableMap.of()));
  }

  private void check(String filename, Configuration config) throws Exception {
    // todo: avoid logic duplication with IntervalSummariesTest and PolicyIterationTest.
    String fullPath;
    if (filename.contains("test/programs/benchmarks")) {
      fullPath = filename;
    } else {
      fullPath = Paths.get(TEST_DIR_PATH, filename).toString();
    }

    TestResults results = CPATestRunner.run(config, fullPath);
    if (filename.contains("_true_assert") || filename.contains("_true-unreach")) {
      results.assertIsSafe();
    } else if (filename.contains("_false_assert") || filename.contains("_false-unreach")) {
      results.assertIsUnsafe();
    }
  }

  private Configuration getProperties(String configFile, Map<String, String> extra)
      throws InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromResource(PolicyIterationTest.class, configFile)
        .setOptions(extra)
        .build();
  }
}
