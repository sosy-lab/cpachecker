// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.util.test.TestUtils.configurationForTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;

/**
 * Integration tests for the ordering-consistency CPA using the {@code
 * orderingConsistency.properties} configuration.
 */
@RunWith(Parameterized.class)
public class OrderingConsistencyTest {

  private static final String TEST_DIR = "test/programs/por/";
  private static final String CONFIG_FILE = "config/orderingConsistency.properties";

  private static Configuration getConfig(
      String encoding, String initialBound, String step, String finalBound)
      throws InvalidConfigurationException, IOException {
    return configurationForTest()
        .loadFromFile(CONFIG_FILE)
        .setOption("parser.usePreprocessor", "true")
        .setOption("oc.encoding", encoding)
        .setOption("oc.initialLoopBound", initialBound)
        .setOption("oc.loopBoundStep", step)
        .setOption("oc.finalLoopBound", finalBound)
        .build();
  }

  private static List<Pair<String, Result>> getTestCases() {
    return List.of(
        Pair.of("single_thread_safe.c", Result.TRUE),
        Pair.of("local_vars_only_safe.c", Result.TRUE),
        Pair.of("two_threads_safe.c", Result.TRUE),
        Pair.of("two_threads_join_safe.c", Result.TRUE),
        Pair.of("mutex_protected_safe.c", Result.TRUE),
        Pair.of("c11_mutex_safe.c", Result.TRUE),
        Pair.of("atomic_increment_safe.c", Result.TRUE),
        Pair.of("atomic_swap_safe.c", Result.TRUE),
        Pair.of("pthread_exit_safe.c", Result.TRUE),
        Pair.of("oc_branch_create.c", Result.TRUE),
        Pair.of("oc_loop_safe.c", Result.TRUE),
        Pair.of("oc_array_safe.c", Result.TRUE),
        Pair.of("two_threads_unsafe.c", Result.FALSE),
        Pair.of("three_threads_unsafe.c", Result.FALSE),
        Pair.of("mutex_unprotected_unsafe.c", Result.FALSE),
        Pair.of("atomic_split_unsafe.c", Result.FALSE),
        Pair.of("function.c", Result.FALSE),
        Pair.of("singleton.i", Result.FALSE),
        Pair.of("oc_array_index_unsafe.c", Result.FALSE),
        Pair.of("oc_heap_struct_unsafe.c", Result.FALSE));
  }

  private static List<String> getUnknownTestCases() {
    return List.of("bigshot_s.i", "28-race_reach_83-list2_racing1.i");
  }

  // suite-wide default bounds: a finite final bound so programs with unbounded
  // loops terminate with UNKNOWN instead of deepening forever
  private static final String DEFAULT_INITIAL_BOUND = "5";
  private static final String DEFAULT_STEP = "5";
  private static final String DEFAULT_FINAL_BOUND = "15";

  @Parameters(name = "{0} [{1}]")
  public static List<Object[]> testData() {
    List<Object[]> data = new ArrayList<>();
    for (String encoding : List.of("REFINEMENT", "CLOCKS")) {
      for (Pair<String, Result> testCase : getTestCases()) {
        data.add(
            new Object[] {
              testCase.getFirst(),
              encoding,
              DEFAULT_INITIAL_BOUND,
              DEFAULT_STEP,
              DEFAULT_FINAL_BOUND,
              testCase.getSecond()
            });
      }
    }
    // REFINEMENT is too slow on this one; CLOCKS reaches the violation at round 3
    // with the suite default bounds
    data.add(
        new Object[] {
          "fib_unsafe-5.i",
          "CLOCKS",
          DEFAULT_INITIAL_BOUND,
          DEFAULT_STEP,
          DEFAULT_FINAL_BOUND,
          Result.FALSE
        });
    // safe up to the bound, but the infinite loop is always truncated: the default
    // final bound of 15 is reached without proving the unwinding assertion
    data.add(
        new Object[] {
          "need-learning.i",
          "REFINEMENT",
          DEFAULT_INITIAL_BOUND,
          DEFAULT_STEP,
          DEFAULT_FINAL_BOUND,
          null
        });
    data.add(
        new Object[] {
          "need-learning.i",
          "CLOCKS",
          DEFAULT_INITIAL_BOUND,
          DEFAULT_STEP,
          DEFAULT_FINAL_BOUND,
          null
        });
    for (String fileName : getUnknownTestCases()) {
      data.add(
          new Object[] {
            fileName, "REFINEMENT", DEFAULT_INITIAL_BOUND, DEFAULT_STEP, DEFAULT_FINAL_BOUND, null
          });
    }
    return data;
  }

  @Parameter(0)
  public String fileName;

  @Parameter(1)
  public String encoding;

  @Parameter(2)
  public String initialBound;

  @Parameter(3)
  public String step;

  @Parameter(4)
  public String finalBound;

  @Parameter(5)
  public Result expectedResult;

  @Test
  public void testOrderingConsistency() throws Exception {
    Configuration config = getConfig(encoding, initialBound, step, finalBound);
    IntegrationTestResult results = IntegrationTestRunner.run(config, TEST_DIR + fileName);
    if (expectedResult == null) {
      assertThat(results.cpaCheckerResult().getResult()).isNoneOf(Result.TRUE, Result.FALSE);
    } else {
      results.assertIs(expectedResult);
    }
  }
}
