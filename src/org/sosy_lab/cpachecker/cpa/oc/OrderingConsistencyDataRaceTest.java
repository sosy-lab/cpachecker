// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static org.sosy_lab.cpachecker.util.test.TestUtils.configurationForTest;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
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
 * Integration tests for the ordering-consistency CPA checking the <b>data-race</b> property, as
 * opposed to {@link OrderingConsistencyTest}, which checks reachability.
 *
 * <p>The two properties are genuinely independent, and this class is the only automated coverage of
 * the race side: the race-pair machinery (atomic accesses, read/write locks, critical sections) is
 * otherwise exercised only by ad-hoc benchmark sweeps.
 *
 * <p>The property must be selected by a <b>{@code .prp} property file</b>. The OC algorithm derives
 * its target from {@code Specification#getProperties()}, which an automaton {@code .spc} file does
 * not contribute to — passing {@code specification/datarace.spc} instead would silently fall back
 * to checking reachability and report a meaningless TRUE.
 */
@RunWith(Parameterized.class)
public class OrderingConsistencyDataRaceTest {

  private static final String TEST_DIR = "test/programs/por/";
  private static final String CONFIG_FILE = "config/orderingConsistency.properties";
  private static final String PROPERTY_FILE = "config/properties/no-data-race.prp";

  // The data-race check requires the CLOCKS encoding; REFINEMENT is rejected at configuration time.
  private static final String ENCODING = "CLOCKS";

  private static Configuration getConfig() throws InvalidConfigurationException, IOException {
    return configurationForTest()
        .loadFromFile(CONFIG_FILE)
        .setOption("parser.usePreprocessor", "true")
        .setOption("specification", PROPERTY_FILE)
        // The property file names an entry function, which CPAchecker only infers automatically
        // when the file is passed as --spec on the command line; setting it via the `specification`
        // option requires stating the entry function explicitly to match.
        .setOption("analysis.entryFunction", "main")
        .setOption("oc.encoding", ENCODING)
        .build();
  }

  @Parameters(name = "{0}")
  public static List<Object[]> testData() {
    List<Pair<String, Result>> testCases =
        ImmutableList.of(
            // Concurrent writes through an `_Atomic float *`: the pointee is atomic, so these are
            // not a race. Covers atomicity resolved through a dereference, and a non-integer
            // atomic.
            Pair.of("atomic_float_ptr_safe.c", Result.TRUE),
            // The same declaration, but writing the POINTER itself, which is *not* atomic — a real
            // race, and the trap a name-based atomicity check would fall into.
            Pair.of("atomic_float_ptr_unsafe.c", Result.FALSE),
            // Baselines.
            Pair.of("local_vars_only_safe.c", Result.TRUE),
            Pair.of("atomic_increment_safe.c", Result.TRUE),
            Pair.of("two_threads_unsafe.c", Result.FALSE),
            // Race-unsafe despite its name, and correctly so: the critical sections order the two
            // threads, but main reads y without holding the mutex and without joining either
            // thread, so that read races with thread1's write. (It is reach-error-safe, which is
            // what the name refers to and what OrderingConsistencyTest checks.) Kept precisely
            // because it shows the two properties are independent.
            Pair.of("mutex_protected_safe.c", Result.FALSE),
            // Thread pool created and joined through a symbolic array index (tids[i] in a loop),
            // race-free: exercises per-instance base identity for a local handle array. Its unsafe
            // sibling is a genuine race through the same path and must stay FALSE.
            Pair.of("loop_handle_datarace_safe.c", Result.TRUE),
            Pair.of("loop_handle_datarace_unsafe.c", Result.FALSE));

    return testCases.stream()
        .map(testCase -> new Object[] {testCase.getFirst(), testCase.getSecond()})
        .toList();
  }

  @Parameter(0)
  public String fileName;

  @Parameter(1)
  public Result expectedResult;

  @Test
  public void testDataRace() throws Exception {
    IntegrationTestResult results = IntegrationTestRunner.run(getConfig(), TEST_DIR + fileName);
    results.assertIs(expectedResult);
  }
}
