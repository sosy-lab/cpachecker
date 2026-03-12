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

  @Test(timeout = 3000)
  public void count_local_pointer_wrong() throws Exception {
    TestResults results =
        CPATestRunner.run(getConfiguration(), getProgramPath("countup_local_pointer_wrong.c"));
    results.assertIsUnsafe();
  }

  /**
   * TODOs currently:
   *
   * <p>- Fix the entry and exit of functions in the pointer target set by only doing it after the
   * edge handling and also doing the update during the assingment which assigns the left-hand side
   * to the right-hand side of the function call.
   *
   * <p>- Test what happens if the same recursive function is called multiple times, since the bases
   * would be equal due to the index, but should contain different values. Should be no problem
   * since the memory regions should always be written to before being read from.
   *
   * <p>-Refactor the code such that bases get a proper name with a callstack record instead of the
   * current handling (sepparate MR)
   *
   * <p>- Add more tests, e.g., with multiple recursive functions, with recursion and loops, with
   * more complex data structures, etc.
   *
   * <p>- check if it is usefull, necessary to propagate the callstack information accross the
   * PointerTarget structures which require the current name
   *
   * <p>- check if this works with the lazy version of aliasing only when necessary and not always.
   * If not, ideally make it work by assigning the correct value when the aliasing occurs using the
   * `addValueImportConstraints` function to create them. In this case eventually I do not need to
   * delete the variable from the SSA index.
   */
}
