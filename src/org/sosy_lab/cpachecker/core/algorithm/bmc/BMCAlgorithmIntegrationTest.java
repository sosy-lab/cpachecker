// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class BMCAlgorithmIntegrationTest {

  private String getProgramPath(String programName) {
    return "test/programs/simple/recursion/" + programName;
  }

  private Configuration getConfiguration() throws IOException, InvalidConfigurationException {
    return TestUtils.configurationForTest()
        .loadFromFile(Path.of("config/bmc-rec.properties"))
        .build();
  }

  @Test(timeout = 3000)
  public void count_local() throws Exception {
    IntegrationTestResult results = IntegrationTestRunner.run(getConfiguration(), getProgramPath("countup_local.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void count_local_wrong() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfiguration(), getProgramPath("countup_local_wrong.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void count_local_wrong2() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfiguration(), getProgramPath("countup_local_wrong2.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfiguration(), getProgramPath("countup_local_pointer.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer_wrong_simple() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(
            getConfiguration(), getProgramPath("countup_local_pointer_wrong_simple.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer_wrong() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfiguration(), getProgramPath("countup_local_pointer_wrong.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer_correct_simple() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(
            getConfiguration(), getProgramPath("countup_local_pointer_correct_simple.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer_correct() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfiguration(), getProgramPath("countup_local_pointer_correct.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void fib_correct() throws Exception {
    IntegrationTestResult results = IntegrationTestRunner.run(getConfiguration(), getProgramPath("fib-correct.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void fib_wrong() throws Exception {
    IntegrationTestResult results = IntegrationTestRunner.run(getConfiguration(), getProgramPath("fib-wrong.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void mutually_recursive_wrong1() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfiguration(), getProgramPath("mutually-recursive-wrong1.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void mutually_recursive_wrong2() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfiguration(), getProgramPath("mutually-recursive-wrong2.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void mutually_recursive_correct() throws Exception {
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfiguration(), getProgramPath("mutually-recursive-correct.c"));
    results.assertIsSafe();
  }
}
