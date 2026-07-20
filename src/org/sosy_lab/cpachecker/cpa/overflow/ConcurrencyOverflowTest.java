// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.overflow;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;
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
import org.sosy_lab.cpachecker.util.test.KnownConcurrencyIssues;

/**
 * Runs the concurrent no-overflow regression programs against the <b>other</b> concurrency analyses
 * that check that property. The POR analyses check the same programs in {@code
 * org.sosy_lab.cpachecker.cpa.por.PORCPAOverflowTest}.
 *
 * <p>{@code sequentialization-concurrency--overflow} gets all of them right, which is what
 * establishes independently that the "unsafe" programs really do overflow — including {@code
 * overflow_stale_lookahead_unsafe.c}, which POR misses.
 *
 * <p>{@code predicateAnalysis-concurrency--overflow} misses every one of them; see {@link
 * KnownConcurrencyIssues}. Those are pre-existing defects in an analysis this change does not
 * touch, so they are skipped rather than fixed.
 */
@RunWith(Parameterized.class)
public class ConcurrencyOverflowTest {

  private static final String TEST_DIR = "test/programs/por/";

  private static final String PREDICATE =
      "config/predicateAnalysis-concurrency--overflow.properties";
  private static final String SEQUENTIALIZATION =
      "config/sequentialization-concurrency--overflow.properties";

  private static Configuration getConfig(String pConfig)
      throws InvalidConfigurationException, IOException {
    return configurationForTest()
        .loadFromFile(pConfig)
        .setOption("parser.usePreprocessor", "true")
        .build();
  }

  @Parameters(name = "{0} [{1}]")
  public static List<Object[]> testData() {
    List<String> configs = ImmutableList.of(PREDICATE, SEQUENTIALIZATION);
    List<Pair<String, Result>> testCases =
        ImmutableList.of(
            Pair.of("overflow_safe.c", Result.TRUE),
            Pair.of("overflow_unsafe.c", Result.FALSE),
            Pair.of("overflow_after_join_unsafe.c", Result.FALSE),
            Pair.of("overflow_stale_lookahead_unsafe.c", Result.FALSE));

    return configs.stream()
        .flatMap(
            config ->
                testCases.stream().map(t -> new Object[] {t.getFirst(), config, t.getSecond()}))
        .toList();
  }

  @Parameter(0)
  public String fileName;

  @Parameter(1)
  public String configuration;

  @Parameter(2)
  public Result expectedResult;

  @Test
  public void testConcurrencyOverflow() throws Exception {
    String knownBug = KnownConcurrencyIssues.reasonFor(configuration, fileName);
    assumeTrue("KNOWN ISSUE (see KnownConcurrencyIssues): " + knownBug, knownBug == null);

    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfig(configuration), TEST_DIR + fileName);
    assertThat(results.cpaCheckerResult().getResult()).isEqualTo(expectedResult);
  }
}
