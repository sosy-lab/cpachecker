// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

@RunWith(Parameterized.class)
public class AtomicOperationTest {
  private static final String TEST_DIR = "test/programs/atomic/";

  @Parameters(name = "{0}")
  public static Collection<Object[]> getTestCases() {
    return List.of(
        new Object[]{"compoundassignment_atomic.c", true},
        new Object[]{"compoundassignment_nonatomic.c", false},
        new Object[]{"postincrement_atomic.c", true},
        new Object[]{"postincrement_nonatomic.c", false},
        new Object[]{"preincrement_atomic.c", true},
        new Object[]{"preincrement_nonatomic.c", false}
    );
  }

  @Parameter(0)
  public String fileName;

  @Parameter(1)
  public boolean expectedSafe;

  @Test
  public void testAtomic() throws Exception {
    var config = TestDataTools.configurationForTest()
        .loadFromFile("config/predicateAnalysis-concurrency.properties")
        .build();
    TestResults results = CPATestRunner.run(config, TEST_DIR + fileName);
    if (expectedSafe) {
      results.assertIsSafe();
    } else {
      results.assertIsUnsafe();
    }
  }

}
