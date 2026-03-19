// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

/**
 * Integration tests for the POR CPA using the {@code por.properties} configuration.
 */
@RunWith(Parameterized.class)
public class PORCPATest {

  private static final String TEST_DIR = "test/programs/por/";

  private static Configuration getConfig(String config, Map<String, String> extra)
      throws InvalidConfigurationException, IOException {
    return TestDataTools.configurationForTest()
        .loadFromFile("config/por-pred.properties")
        .setOptions(extra)
        .build();
  }

  private static Configuration getConfig(String config)
      throws InvalidConfigurationException, IOException {
    return getConfig(config, ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  private static List<String> getConfigs() {
    return List.of(
        "config/por-pred.properties",
        "config/por-pred-aa.properties"
    );
  }

  private static List<Pair<String, Boolean>> getTestCases() {
    return List.of(
        Pair.of("two_threads_safe.c", true),
        Pair.of("two_threads_join_safe.c", true),
        Pair.of("single_thread_safe.c", true),
        Pair.of("local_vars_only_safe.c", true),
        Pair.of("two_threads_unsafe.c", false),
        Pair.of("three_threads_unsafe.c", false),
        Pair.of("mutex_protected_safe.c", true),
        Pair.of("c11_mutex_safe.c", true),
        Pair.of("mutex_unprotected_unsafe.c", false),
        Pair.of("atomic_increment_safe.c", true),
        Pair.of("atomic_swap_safe.c", true),
        Pair.of("atomic_split_unsafe.c", false)
    );
  }

  @Parameters(name = "{0} [{1}]")
  public static Object[][] testData() {
    List<String> configs = getConfigs();
    List<Pair<String, Boolean>> testCases = getTestCases();

    Object[][] data = new Object[configs.size() * testCases.size()][3];
    int index = 0;
    for (String config : configs) {
      for (Pair<String, Boolean> testCase : testCases) {
        data[index][0] = testCase.getFirst(); // fileName
        data[index][1] = config; // configuration
        data[index][2] = testCase.getSecond(); // expectedSafe
        index++;
      }
    }
    return data;
  }

  @Parameter(0)
  public String fileName;

  @Parameter(1)
  public String configuration;

  @Parameter(2)
  public boolean expectedSafe;

  @Test
  public void testPor() throws Exception {
    var config = getConfig(configuration);
    TestResults results = CPATestRunner.run(config, TEST_DIR + fileName);
    if (expectedSafe) {
      results.assertIsSafe();
    } else {
      results.assertIsUnsafe();
    }
  }
}
