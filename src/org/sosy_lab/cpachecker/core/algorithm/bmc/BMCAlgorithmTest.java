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
    return "test/programs/recursion/" + programName;
  }

  private Configuration getConfiguration() throws IOException, InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromFile(Path.of("config/bmc-rec.properties"))
        .build();
  }

  @Test(timeout = 90000)
  public void count_local() throws Exception {
    TestResults results = CPATestRunner.run(getConfiguration(), getProgramPath("countup_local.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 90000)
  public void count_local_wrong() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_wrong.c"));
    results.assertIsSafe();
  }

  @Test(timeout = 90000)
  public void count_local_wrong2() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_wrong2.c"));
    results.assertIsSafe();
  }
}
