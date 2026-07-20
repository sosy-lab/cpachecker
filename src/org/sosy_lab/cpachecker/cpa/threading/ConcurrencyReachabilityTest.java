// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

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
 * Runs the concurrency regression programs written for the POR and ordering-consistency analyses
 * (see {@code org.sosy_lab.cpachecker.cpa.por.PORCPATest} / {@code
 * org.sosy_lab.cpachecker.cpa.oc.OrderingConsistencyTest}) against <b>every other</b> concurrency
 * analysis that checks reachability, so that the same programs are pinned down across all of them.
 *
 * <p>The programs live under {@code test/programs/por/} because that is where they were introduced;
 * they are ordinary concurrent C programs and are not POR-specific.
 *
 * <p>Where an analysis is merely <i>incomplete</i> it is expected to say so (UNKNOWN) — that is
 * sound and is asserted as such. Where an analysis produces a <b>wrong</b> verdict, the case is
 * listed in {@link KnownConcurrencyIssues} and skipped, with the reason recorded: those are
 * pre-existing defects in analyses this change does not touch, and fixing them is out of scope.
 */
@RunWith(Parameterized.class)
public class ConcurrencyReachabilityTest {

  private static final String TEST_DIR = "test/programs/por/";
  private static final String REACHABILITY_SPEC = "config/specification/sv-comp-reachability.spc";

  private static final String PREDICATE = "config/predicateAnalysis-concurrency.properties";
  private static final String VALUE = "config/valueAnalysis-concurrency.properties";
  private static final String BMC = "config/bmc-concurrency.properties";
  private static final String BDD = "config/bddAnalysis-concurrency.properties";
  private static final String SEQUENTIALIZATION = "config/sequentialization-concurrency.properties";

  /**
   * Thread handles that are not plain variables — {@code pthread_create(&t[0], ...)} and {@code
   * pthread_join(t[i], ...)}. Every analysis here is built on {@link ThreadingCPA}, or on
   * sequentialization, and all of them reject such a handle outright ("Unrecognized C code
   * (unsupported thread assignment): t[0]") and answer UNKNOWN. That is sound, and it is exactly
   * the restriction the POR and OC analyses lifted — they verify these same programs — so pinning
   * the UNKNOWN here documents the capability difference rather than a bug.
   */
  private static final ImmutableSet<String> UNSUPPORTED_THREAD_HANDLE =
      ImmutableSet.of(
          "array_handle_safe.c",
          "array_handle_unsafe.c",
          "loop_handle_safe.c",
          "loop_handle_unsafe.c");

  /**
   * bddAnalysis-concurrency cannot model a pointer or a float, so it can prove neither safety nor
   * the violation on these programs. It honestly answers UNKNOWN rather than a wrong verdict: its
   * counterexample check rejects the (spurious or genuine) error path as infeasible but cannot
   * remove it from the ARG, so the analysis gives up rather than report TRUE or FALSE.
   */
  private static final ImmutableSet<String> BDD_INCOMPLETE =
      ImmutableSet.of("pointer_write_safe.c", "atomic_float_safe.c", "atomic_float_unsafe.c");

  private static Configuration getConfig(String pConfig)
      throws InvalidConfigurationException, IOException {
    return configurationForTest()
        .loadFromFile(pConfig)
        .setOption("parser.usePreprocessor", "true")
        .setOption("specification", REACHABILITY_SPEC)
        .build();
  }

  @Parameters(name = "{0} [{1}]")
  public static List<Object[]> testData() {
    List<String> configs = List.of(PREDICATE, VALUE, BMC, BDD, SEQUENTIALIZATION);
    List<Pair<String, Result>> testCases =
        List.of(
            Pair.of("array_handle_safe.c", Result.TRUE),
            Pair.of("array_handle_unsafe.c", Result.FALSE),
            Pair.of("loop_handle_safe.c", Result.TRUE),
            Pair.of("loop_handle_unsafe.c", Result.FALSE),
            Pair.of("atomic_float_safe.c", Result.TRUE),
            Pair.of("atomic_float_unsafe.c", Result.FALSE),
            Pair.of("pointer_write_safe.c", Result.TRUE));

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
  public void testConcurrencyReachability() throws Exception {
    String knownBug = KnownConcurrencyIssues.reasonFor(configuration, fileName);
    assumeTrue("KNOWN ISSUE (see KnownConcurrencyIssues): " + knownBug, knownBug == null);

    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfig(configuration), TEST_DIR + fileName);
    Result actual = results.cpaCheckerResult().getResult();

    if (UNSUPPORTED_THREAD_HANDLE.contains(fileName)
        || (configuration.equals(BDD) && BDD_INCOMPLETE.contains(fileName))) {
      assertThat(actual).isEqualTo(Result.UNKNOWN);
    } else {
      assertThat(actual).isEqualTo(expectedResult);
    }
  }
}
