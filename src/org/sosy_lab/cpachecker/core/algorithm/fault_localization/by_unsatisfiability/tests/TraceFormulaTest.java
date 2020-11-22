// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.tests;

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
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.BooleanFormulaParser;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.FormulaNode;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class TraceFormulaTest {

  private final Level logLevel = Level.FINEST;

  enum FLAlgorithm {
    MAXSAT, ERRINV
  }

  enum LogKeys {
    TFTRACE,
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
            .loadFromResource(TraceFormulaTest.class, "predicateAnalysisWithFaultLocalization.properties")
            .setOption("faultLocalization.by_traceformula.type", algorithm.name())
            .setOptions(additionalOptions)
            .build();

    String test_dir = "test/programs/fault_localization/";
    Path program = Paths.get(test_dir, name);

    return CPATestRunner.run(config, program.toString(), logLevel);
  }

  private Map<LogKeys, Object> findFLPatterns(String log, Set<LogKeys> keywords) {
    Map<LogKeys, Object> entries = new HashMap<>();
    Splitter.on("\n")
        .split(log)
        .forEach(
            line -> {
              List<String> result = Splitter.on("=").limit(2).splitToList(line);
              if (result.size() == 2 && LogKeys.containsKey(result.get(0))) {
                LogKeys key = LogKeys.valueOf(result.get(0).toUpperCase());
                String value =
                    result.get(1).replace(" (TraceFormula.<init>, " + logLevel + ")", "");
                if (keywords.contains(key)) {
                  switch (key) {
                    case TFTRACE:
                    case TFPRECONDITION:
                    case TFPOSTCONDITION:
                      entries.put(key, BooleanFormulaParser.parse(value));
                      break;
                    default:
                      throw new AssertionError("Unknown key: " + key);
                  }
                }
              }
            });
    return entries;
  }

  private void test0(
      String program,
      FLAlgorithm algorithm,
      Map<String, String> options,
      Map<LogKeys, Object> expected)
      throws Exception {
    TestResults test = runFaultLocalization(program, algorithm, options);
    Map<LogKeys, Object> found = findFLPatterns(test.getLog(), expected.keySet());
    expected.forEach(
        (key, value) -> {
          Object compare = found.get(key);
          switch (key) {
            case TFTRACE:
            case TFPRECONDITION:
            case TFPOSTCONDITION:
              assertNodesEquivalent((FormulaNode) value, (FormulaNode) compare);
              break;
            default:
              throw new AssertionError("Unknown key: " + key);
          }
        });
  }

  private void assertNodesEquivalent(FormulaNode node1, FormulaNode node2) {
    // equivalent is not the same as equal since "a and b" is equivalent to "b and a" but not equal
    // (Object#equal)
    if (!node1.equivalent(node2)) {
      throw new AssertionError("Node " + node1 + " is not equivalent to " + node2);
    }
  }

  @Test
  public void testCorrectConditions() throws Exception {
    // test if calculating post condition influences the precondition
    Map<LogKeys, Object> expected = new HashMap<>();
    expected.put(LogKeys.TFPRECONDITION, BooleanFormulaParser.parse("(`=_<BitVec, 32, >` __VERIFIER_nondet_int!2@ 4_32)"));
    expected.put(LogKeys.TFPOSTCONDITION, BooleanFormulaParser.parse("(`=_<BitVec, 32, >` main::copyForCheck@2 main::test@3)"));

    test0("primefactors.c",
            FLAlgorithm.ERRINV,
            ImmutableMap.of(),
            expected);
  }

  @Test
  public void testFlowSensitive() throws Exception {
    // test if calculating post condition influences the precondition
    Map<LogKeys, Object> expected = new HashMap<>();
    expected.put(LogKeys.TFTRACE,
            BooleanFormulaParser.parse(
                    "(`and` (`and` (`=_<BitVec, 32, >` __VERIFIER_nondet_int!2@ main::x@2) "
                            + "(`=_<BitVec, 32, >` main::y@2 __VERIFIER_nondet_int!3@)) "
                            + "(`or` (`bvslt_32` 0_32 main::y@2) (`=_<BitVec, 32, >` main::x@3 0_32)))"));

    test0(
        "unit_test.c",
        FLAlgorithm.ERRINV,
        ImmutableMap.of(),
        expected);
  }

  @Test
  public void testDefaultTrace() throws Exception {
    // test if calculating post condition influences the precondition
    Map<LogKeys, Object> expected = new HashMap<>();
    expected.put(LogKeys.TFTRACE,
            BooleanFormulaParser.parse(
                    "(`and` (`and` (`and` (`=_<BitVec, 32, >` __VERIFIER_nondet_int!2@ main::x@2) "
                            + "(`=_<BitVec, 32, >` main::y@2 __VERIFIER_nondet_int!3@)) "
                            + "(`not` (`bvslt_32` 0_32 main::y@2))) "
                            + "(`=_<BitVec, 32, >` main::x@3 0_32))\n"));
    test0(
        "unit_test.c",
        FLAlgorithm.ERRINV,
        ImmutableMap.of("faultLocalization.by_traceformula.errorInvariants.disableFSTF", "true"),
        expected);
  }

  @Test
  public void testSelectorTrace() throws Exception {
    // test if calculating post condition influences the precondition
    Map<LogKeys, Object> expected = new HashMap<>();
    expected.put(LogKeys.TFTRACE,
            BooleanFormulaParser.parse(
                    "(`and` (`and` (`and` "
                            + "(`or` (`=_<BitVec, 32, >` __VERIFIER_nondet_int!2@ main::x@2) (`not` S0)) "
                            + "(`or` (`=_<BitVec, 32, >` main::y@2 __VERIFIER_nondet_int!3@) (`not` S1))) "
                            + "(`or` (`not` (`bvslt_32` 0_32 main::y@2)) (`not` S2))) "
                            + "(`or` (`=_<BitVec, 32, >` main::x@3 0_32) (`not` S3)))"));

    test0(
        "unit_test.c",
        FLAlgorithm.MAXSAT,
        ImmutableMap.of(),
        expected);
  }
}
