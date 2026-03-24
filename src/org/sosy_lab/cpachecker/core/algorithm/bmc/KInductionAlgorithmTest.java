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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

// All tests in this class take between 100 - 300 ms, but when running them as standalone in an IDE
// they can take up to 2.5 seconds due to the startup of the JVM. Therefore, we use a slightly
// higher timeout than is absolutely necessary
//
// The only exception to this is the BMC tests for comparison, which take 2 seconds, to
// guarantee that we did not just execute the base case of the induction and then timeout, but
// actually execute the inductive step as well
public class KInductionAlgorithmTest {

  private String getProgramPath(String programName) {
    return "test/programs/simple/recursion/" + programName;
  }

  private Configuration getConfiguration() throws IOException, InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromFile(Path.of("config/kInduction-plain-rec.properties"))
        .build();
  }

  private Configuration getBmcConfiguration() throws IOException, InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromFile(Path.of("config/bmc-rec.properties"))
        .setOption("limits.time.cpu", "4s")
        .build();
  }

  @Test(timeout = 3000)
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
  public void count_inductive() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_inductive.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void count_inductive_wrong() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_inductive_wrong.c"));
    results.assertIsUnsafe();
  }

  @Test(timeout = 5000)
  public void count_inductive_bmc_timeout() throws Exception {
    // Here to avoid errors in the task which make it trivial to solve with BMC, to guarantee that
    // the inductive step is actually executed and not just the base case
    TestResults results =
        CPATestRunner.run(getBmcConfiguration(), getProgramPath("countup_inductive.c"));
    results.assertIs(Result.UNKNOWN);
  }

  @Test(timeout = 3000)
  public void count_even_inductive() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_even_inductive.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void id_simple() throws Exception {
    TestResults results = CPATestRunner.run(getConfiguration(), getProgramPath("id_simple.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 3000)
  public void id_simple_wrong() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("id_simple_wrong.c"));
    results.assertIsUnsafe();
  }
}
