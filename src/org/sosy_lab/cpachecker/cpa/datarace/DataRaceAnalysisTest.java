// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.sosy_lab.cpachecker.util.test.TestUtils.configurationForTest;

import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.cpachecker.util.test.KnownConcurrencyIssues;

/**
 * Runs the data-race regression programs against {@code dataRaceAnalysis} (the {@link DataRaceCPA}
 * -based analysis), the other analysis in the tree that checks the no-data-race property. The
 * ordering-consistency analysis checks the same programs in {@code
 * org.sosy_lab.cpachecker.cpa.oc.OrderingConsistencyDataRaceTest}.
 *
 * <p>As there, the property must be selected by a {@code .prp} property file: an automaton {@code
 * .spc} contributes no {@link org.sosy_lab.cpachecker.core.specification.Property}, and setting the
 * property file via the {@code specification} option (rather than {@code --spec}) additionally
 * requires naming the entry function explicitly.
 */
@RunWith(Parameterized.class)
public class DataRaceAnalysisTest {

  private static final String TEST_DIR = "test/programs/por/";
  private static final String CONFIG = "config/dataRaceAnalysis.properties";
  private static final String PROPERTY_FILE = "config/properties/no-data-race.prp";

  /**
   * Sound but incomplete: the analysis honestly answers UNKNOWN rather than claiming the program is
   * race-free. (Ordering consistency proves this one TRUE.)
   */
  private static final ImmutableSet<String> INCOMPLETE = ImmutableSet.of("atomic_float_ptr_safe.c");

  private static Configuration getConfig() throws InvalidConfigurationException, IOException {
    return configurationForTest()
        .loadFromFile(CONFIG)
        .setOption("parser.usePreprocessor", "true")
        .setOption("specification", PROPERTY_FILE)
        .setOption("analysis.entryFunction", "main")
        .build();
  }

  @Parameters(name = "{0}")
  public static List<Object[]> testData() {
    List<Pair<String, Result>> testCases =
        List.of(
            // Concurrent writes THROUGH an `_Atomic float *`: the pointee is atomic, no race.
            Pair.of("atomic_float_ptr_safe.c", Result.TRUE),
            // The same declaration, but writing the (non-atomic) POINTER itself: a real race.
            Pair.of("atomic_float_ptr_unsafe.c", Result.FALSE),
            // Baselines.
            Pair.of("local_vars_only_safe.c", Result.TRUE),
            Pair.of("atomic_increment_safe.c", Result.TRUE),
            Pair.of("two_threads_unsafe.c", Result.FALSE),
            // Race-unsafe despite the name: main reads y without the mutex and without joining.
            Pair.of("mutex_protected_safe.c", Result.FALSE));

    return testCases.stream().map(t -> new Object[] {t.getFirst(), t.getSecond()}).toList();
  }

  @Parameter(0)
  public String fileName;

  @Parameter(1)
  public Result expectedResult;

  @Test
  public void testDataRace() throws Exception {
    String knownBug = KnownConcurrencyIssues.reasonFor(CONFIG, fileName);
    assumeTrue("KNOWN ISSUE (see KnownConcurrencyIssues): " + knownBug, knownBug == null);

    IntegrationTestResult results = IntegrationTestRunner.run(getConfig(), TEST_DIR + fileName);
    Result actual = results.cpaCheckerResult().getResult();

    if (INCOMPLETE.contains(fileName)) {
      assertThat(actual).isEqualTo(Result.UNKNOWN);
    } else {
      assertThat(actual).isEqualTo(expectedResult);
    }
  }
}
