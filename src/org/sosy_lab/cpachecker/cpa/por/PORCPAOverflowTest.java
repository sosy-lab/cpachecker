// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static org.junit.Assume.assumeTrue;
import static org.sosy_lab.cpachecker.util.test.TestUtils.configurationForTest;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.KnownConcurrencyIssues;

/**
 * Integration tests for the POR CPA checking the <b>no-overflow</b> property, as opposed to {@link
 * PORCPATest}, which checks reachability.
 *
 * <p>This is the only automated coverage of POR + {@link
 * org.sosy_lab.cpachecker.cpa.overflow.OverflowCPA}, and it exists because that combination hid two
 * separate silent wrong-TRUE bugs, each of which made a real overflow simply disappear:
 *
 * <ul>
 *   <li>{@code OverflowTransferRelation} returned <b>no successors at all</b> at a node with no
 *       leaving edges (it built its result inside the loop over those edges). A thread's last edge
 *       ends at its function exit node, so the thread's final state was killed, the thread never
 *       reached its exit, {@code canJoin} was never satisfied, and every {@code pthread_join} stayed
 *       disabled forever — silently making everything after a join unreachable. Guarded by {@code
 *       overflow_after_join_unsafe.c}.
 *   <li>POR applies synthetic bookkeeping edges of its own (the handle write at a create, the
 *       handle-equality assume at a join). Feeding an already-<i>violating</i> state through one of
 *       them returned an empty collection — OverflowCPA reports a violation precisely by producing
 *       no successors — which POR then read as "infeasible branch" and dropped the violation. See
 *       {@code PORTransferRelation#applyBookkeepingEdge}. Guarded by {@code overflow_unsafe.c},
 *       whose second {@code pthread_create} is what destroyed the flagged state.
 * </ul>
 */
@RunWith(Parameterized.class)
public class PORCPAOverflowTest {

  private static final String TEST_DIR = "test/programs/por/";

  private static Configuration getConfig(String config)
      throws InvalidConfigurationException, IOException {
    return configurationForTest()
        .loadFromFile(config)
        .setOptions(ImmutableMap.of("parser.usePreprocessor", "true"))
        .build();
  }

  @Parameters(name = "{0} [{1}]")
  public static List<Object[]> testData() {
    List<String> configs =
        List.of("config/por-value-overflow.properties", "config/por-pred-overflow.properties");
    List<Pair<String, Boolean>> testCases =
        List.of(
            Pair.of("overflow_safe.c", true),
            Pair.of("overflow_unsafe.c", false),
            Pair.of("overflow_after_join_unsafe.c", false),
            Pair.of("overflow_stale_lookahead_unsafe.c", false));

    return configs.stream()
        .flatMap(
            config ->
                testCases.stream()
                    .map(
                        testCase ->
                            new Object[] {testCase.getFirst(), config, testCase.getSecond()}))
        .toList();
  }

  @Parameter(0)
  public String fileName;

  @Parameter(1)
  public String configuration;

  @Parameter(2)
  public boolean expectedSafe;

  @Test
  public void testPorOverflow() throws Exception {
    String knownBug = KnownConcurrencyIssues.reasonFor(configuration, fileName);
    assumeTrue("KNOWN ISSUE (see KnownConcurrencyIssues): " + knownBug, knownBug == null);
    IntegrationTestResult results =
        IntegrationTestRunner.run(getConfig(configuration), TEST_DIR + fileName);
    if (expectedSafe) {
      results.assertIsSafe();
    } else {
      results.assertIsUnsafe();
    }
  }
}
