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
package org.sosy_lab.cpachecker.cpa.summary.test;


import com.google.common.collect.ImmutableMap;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class IntervalSummariesTest {

  private static final String TEST_DIR_PATH = "test/programs/summaries/intervals/";

  @Test public void simple_true_assert() throws Exception {
    check("simple_true_assert.c");
  }

  @Test public void simple_false_assert() throws Exception {
    check("simple_false_assert.c");
  }

  @Test public void recursive_simple_true_assert() throws Exception {
    check("recursive_simple_true_assert.c");
  }

  @Test public void recursive_simple_false_assert() throws Exception {
    check("recursive_simple_false_assert.c");
  }

  @Test public void recursive_sum_true_assert() throws Exception {
    check("recursive_sum_true_assert.c");
  }

  @Test public void recursive_sum_false_assert() throws Exception {
    check("recursive_sum_false_assert.c");
  }

  @Test public void nobasecase_true_assert() throws Exception {
    check("nobasecase_true_assert.c");
  }

  private void check(String filename) throws Exception {
    check(
        filename,
        getProperties("intervalSummaries.properties", ImmutableMap.of())
    );
  }

  private void check(String filename, Configuration config) throws Exception {

    // todo: avoid code duplication w/ PolicyTest,
    // refactoring here might be useful.
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
        .loadFromResource(IntervalSummariesTest.class, configFile)
        .setOptions(extra)
        .build();
  }

}
