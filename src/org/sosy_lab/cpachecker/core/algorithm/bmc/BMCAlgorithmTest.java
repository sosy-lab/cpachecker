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
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

// All tests in this class take between 100 - 300 ms, but when running them as standalone in an IDE
// they can take up to 2.5 seconds due to the startup of the JVM. Therefore, we use a slightly
// higher timeout than is absolutely necessary
public class BMCAlgorithmTest {

  private String getProgramPath(String programName) {
    return "test/programs/simple/recursion/" + programName;
  }

  private Configuration getConfiguration() throws IOException, InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromFile(Path.of("config/bmc-rec.properties"))
        .build();
  }

  @Test(timeout = 300000000)
  public void count_local() throws Exception {
    TestResults results = CPATestRunner.run(getConfiguration(), getProgramPath("countup_local.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void count_local_wrong() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_wrong.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void count_local_wrong2() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_wrong2.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_pointer.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer_wrong_simple() throws Exception {
    TestResults results =
        CPATestRunner.run(
            getConfiguration(), getProgramPath("countup_local_pointer_wrong_simple.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer_wrong() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_pointer_wrong.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer_correct_simple() throws Exception {
    TestResults results =
        CPATestRunner.run(
            getConfiguration(), getProgramPath("countup_local_pointer_correct_simple.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void count_local_pointer_correct() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_pointer_correct.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void fib_correct() throws Exception {
    TestResults results = CPATestRunner.run(getConfiguration(), getProgramPath("fib-correct.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void fib_wrong() throws Exception {
    TestResults results = CPATestRunner.run(getConfiguration(), getProgramPath("fib-wrong.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void mutually_recursive_wrong1() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("mutually-recursive-wrong1.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void mutually_recursive_wrong2() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("mutually-recursive-wrong2.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 3000)
  public void mutually_recursive_correct() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("mutually-recursive-correct.c"));
    results.assertIsSafe();
  }
}
