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

public class BMCAlgorithmTest {

  private String getProgramPath(String programName) {
    return "test/programs/simple/recursion/" + programName;
  }

  private Configuration getConfiguration() throws IOException, InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromFile(Path.of("config/bmc-rec.properties"))
        .build();
  }

  // Actualy takes around 200 milisenconds, but when running it in isolation in an IDE, the startup
  // time takes around 1.5 seconds, so we give it a bit more time to be safe.
  // The same applies to all functions here.
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

  @Test(timeout = 50000000)
  public void count_local_pointer_wrong() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_pointer_wrong.c"));
    results.assertIsUnsafe();
  }
}
