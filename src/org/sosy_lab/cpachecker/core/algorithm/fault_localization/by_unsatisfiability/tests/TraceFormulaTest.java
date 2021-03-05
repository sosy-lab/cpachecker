// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.tests;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class TraceFormulaTest {

  private final Level logLevel = Level.FINEST;

  enum FLAlgorithm {
    MAXSAT,
    ERRINV
  }

  enum LogKeys {
    TFRESULT,
    TFPRECONDITION,
    TFPOSTCONDITION;

    public static boolean containsKey(String keyString) {
      for (LogKeys key : values()) {
        if (key.toString().toLowerCase().equals(keyString)) {
          return true;
        }
      }
      return false;
    }
  }

  private TestResults runFaultLocalization(
      String name, FLAlgorithm algorithm, Map<String, String> additionalOptions) throws Exception {
    final Configuration config =
        TestDataTools.configurationForTest()
            .loadFromResource(
                TraceFormulaTest.class, "predicateAnalysisWithFaultLocalization.properties")
            .setOption("faultLocalization.by_traceformula.type", algorithm.name())
            .setOptions(additionalOptions)
            .build();

    String test_dir = "test/programs/fault_localization/";
    Path program = Paths.get(test_dir, name);

    return CPATestRunner.run(config, program.toString(), logLevel);
  }

  private Map<LogKeys, String> findFLPatterns(String log, Set<LogKeys> keywords) {
    Map<LogKeys, String> entries = new HashMap<>();
    Splitter.on("\n")
        .split(log)
        .forEach(
            line -> {
              List<String> result = Splitter.on("=").limit(2).splitToList(line);
              if (result.size() == 2 && LogKeys.containsKey(result.get(0))) {
                LogKeys key = LogKeys.valueOf(result.get(0).toUpperCase());
                String value = result.get(1).replaceAll("\\(.*, " + logLevel + "\\)", "").trim();
                if (keywords.contains(key)) {
                  entries.merge(key, value, (val1, val2) -> val1 + ", " + val2);
                }
              }
            });
    return entries;
  }

  private void checkIfExpectedValuesMatchResultValues(
      String program,
      FLAlgorithm algorithm,
      Map<String, String> options,
      Map<LogKeys, String> expected)
      throws Exception {
    TestResults test = runFaultLocalization(program, algorithm, options);
    Map<LogKeys, String> found = findFLPatterns(test.getLog(), expected.keySet());
    expected.forEach(
        (key, value) -> {
          String compare = found.get(key);
          assertThat(compare).isEqualTo(value);
        });
  }

  @Test
  public void testCorrectCalculationOfPreAndPostCondition() throws Exception {
    // precondition values
    final String preconditionValues = "4, 4, 4, 1, 2, 2, 2, 1, 1, 2, 2, 2, 3";
    // post-condition is on line 47
    final String postConditionLocation = "line 47";
    checkIfExpectedValuesMatchResultValues(
        "unit_test_pre-post-condition.c",
        FLAlgorithm.ERRINV,
        ImmutableMap.of(),
        ImmutableMap.<LogKeys, String>builder()
            .put(LogKeys.TFPRECONDITION, preconditionValues)
            .put(LogKeys.TFPOSTCONDITION, postConditionLocation)
            .build());
  }

  @Test
  public void testErrorInvariantsOnFlowSensitiveTrace() throws Exception {
    // Selector indices that are part of a fault
    final String resultSelectors = "[13, 17]";
    checkIfExpectedValuesMatchResultValues(
        "unit_test_traces.c",
        FLAlgorithm.ERRINV,
        ImmutableMap.of(),
        ImmutableMap.<LogKeys, String>builder().put(LogKeys.TFRESULT, resultSelectors).build());
  }

  @Test
  public void testErrorInvariantsOnDefaultTrace() throws Exception {
    // Selector indices that are part of a fault
    final String resultSelector = "[17]";
    checkIfExpectedValuesMatchResultValues(
        "unit_test_traces.c",
        FLAlgorithm.ERRINV,
        ImmutableMap.of("faultLocalization.by_traceformula.errorInvariants.disableFSTF", "true"),
        ImmutableMap.<LogKeys, String>builder().put(LogKeys.TFRESULT, resultSelector).build());
  }

  @Test
  public void testMaxSatSelectorTrace() throws Exception {
    // Selector indices that are part of a fault
    final String resultSelectors = "[12, 13, 14, 17]";
    checkIfExpectedValuesMatchResultValues(
        "unit_test_traces.c",
        FLAlgorithm.MAXSAT,
        ImmutableMap.of(),
        ImmutableMap.<LogKeys, String>builder().put(LogKeys.TFRESULT, resultSelectors).build());
  }
}
