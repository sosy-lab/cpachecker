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
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

/** Integration tests for the POR CPA using the {@code por.properties} configuration. */
public class PORCPATest {

  private static final String TEST_DIR = "test/programs/por/";

  private static Configuration getConfig(Map<String, String> extra)
      throws InvalidConfigurationException, IOException {
    return TestDataTools.configurationForTest()
        .loadFromFile("config/por.properties")
        .setOptions(extra)
        .build();
  }

  private static Configuration getConfig() throws InvalidConfigurationException, IOException {
    return getConfig(ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  // -- safe programs --

  @Test
  public void twoThreadsSafe() throws Exception {
    TestResults results = CPATestRunner.run(getConfig(), TEST_DIR + "two_threads_safe.c");
    results.assertIsSafe();
  }

  @Test
  public void singleThreadSafe() throws Exception {
    TestResults results = CPATestRunner.run(getConfig(), TEST_DIR + "single_thread_safe.c");
    results.assertIsSafe();
  }

  @Test
  public void localVarsOnlySafe() throws Exception {
    TestResults results = CPATestRunner.run(getConfig(), TEST_DIR + "local_vars_only_safe.c");
    results.assertIsSafe();
  }

  // -- unsafe programs --

  @Test
  public void twoThreadsUnsafe() throws Exception {
    TestResults results = CPATestRunner.run(getConfig(), TEST_DIR + "two_threads_unsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void threeThreadsUnsafe() throws Exception {
    TestResults results = CPATestRunner.run(getConfig(), TEST_DIR + "three_threads_unsafe.c");
    results.assertIsUnsafe();
  }

  // -- safe programs with mutex protection --

  @Test
  public void mutexProtectedSafe() throws Exception {
    TestResults results = CPATestRunner.run(getConfig(), TEST_DIR + "mutex_protected_safe.c");
    results.assertIsSafe();
  }

  @Test
  public void c11MutexSafe() throws Exception {
    TestResults results = CPATestRunner.run(getConfig(), TEST_DIR + "c11_mutex_safe.c");
    results.assertIsSafe();
  }

  // -- unsafe programs without proper mutex protection --

  @Test
  public void mutexUnprotectedUnsafe() throws Exception {
    TestResults results = CPATestRunner.run(getConfig(), TEST_DIR + "mutex_unprotected_unsafe.c");
    results.assertIsUnsafe();
  }
}
