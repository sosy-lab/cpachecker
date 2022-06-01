// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizationInfoWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.ErrorInvariantsAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace.TraceAtom;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class TraceFormulaTest {

  private final Level logLevel = Level.FINEST;

  private enum FLAlgorithm {
    MAXSAT,
    ERRINV
  }

  private enum LogKeys {
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
    Path program = Path.of(test_dir, name);

    return CPATestRunner.run(config, program.toString(), logLevel);
  }

  private Multimap<LogKeys, Object> findFLPatterns(String log, Set<LogKeys> keywords) {
    Multimap<LogKeys, Object> entries = ArrayListMultimap.create();
    Splitter.on("\n")
        .split(log)
        .forEach(
            line -> {
              List<String> result = Splitter.on("=").limit(2).splitToList(line);
              if (result.size() == 2 && LogKeys.containsKey(result.get(0))) {
                LogKeys key = LogKeys.valueOf(result.get(0).toUpperCase());
                String value = result.get(1).replaceAll("\\(.*, " + logLevel + "\\)", "").trim();
                if (keywords.contains(key)) {
                  if (key == LogKeys.TFPRECONDITION) {
                    entries.put(key, value);
                  } else {
                    value = CharMatcher.anyOf("[]").removeFrom(value);
                    Splitter.on(", ")
                        .splitToList(value)
                        .forEach(loc -> entries.put(key, Integer.parseInt(loc)));
                  }
                }
              }
            });
    return entries;
  }

  private void checkIfExpectedValuesMatchResultValues(
      String program,
      FLAlgorithm algorithm,
      Map<String, String> options,
      Map<LogKeys, Object> expected)
      throws Exception {

    TestResults test = runFaultLocalization(program, algorithm, options);
    FaultLocalizationInfoWithTraceFormula faultInfo =
        (FaultLocalizationInfoWithTraceFormula)
            AbstractStates.getTargetStates(test.getCheckerResult().getReached()).stream()
                .filter(state -> ((ARGState) state).getCounterexampleInformation().isPresent())
                .map(state -> ((ARGState) state).getCounterexampleInformation().orElseThrow())
                .filter(cex -> cex instanceof FaultLocalizationInfoWithTraceFormula)
                .findFirst()
                .orElseThrow();

    Multimap<LogKeys, Object> found = findFLPatterns(test.getLog(), expected.keySet());

    List<Integer> lines = new ArrayList<>();
    for (Fault fault : faultInfo.getRankedList()) {
      switch (algorithm) {
        case ERRINV:
          if (!(fault instanceof ErrorInvariantsAlgorithm.Interval)) {
            // Faults produced by ErrorInvariantsAlgorithm always have exactly one member
            TraceAtom traceElement = (TraceAtom) Iterables.getOnlyElement(fault);
            lines.add(traceElement.correspondingEdge().getFileLocation().getStartingLineInOrigin());
          }
          break;

        case MAXSAT:
          for (FaultContribution contribution : fault) {
            TraceAtom traceElement = (TraceAtom) contribution;
            lines.add(traceElement.correspondingEdge().getFileLocation().getStartingLineInOrigin());
          }
          break;

        default:
          throw new AssertionError(algorithm + " is not a valid algorithm.");
      }
    }

    expected.forEach(
        (key, value) -> {
          switch (key) {
            case TFRESULT:
              {
                @SuppressWarnings("unchecked")
                ImmutableList<Integer> expectedLines = (ImmutableList<Integer>) value;
                ImmutableList<Integer> foundLinesLog =
                    transformedImmutableListCopy(found.get(key), val -> (Integer) val);
                assertThat(lines).containsExactlyElementsIn(expectedLines);
                assertThat(foundLinesLog).containsExactlyElementsIn(expectedLines);
                break;
              }

            case TFPOSTCONDITION:
              {
                @SuppressWarnings("unchecked")
                ImmutableList<Integer> expectedLines = (ImmutableList<Integer>) value;
                ImmutableList<Integer> foundLines =
                    transformedImmutableListCopy(found.get(key), val -> (Integer) val);
                assertThat(foundLines).containsExactlyElementsIn(expectedLines);
                break;
              }

            case TFPRECONDITION:
              {
                @SuppressWarnings("unchecked")
                ImmutableList<String> expectedValues = (ImmutableList<String>) value;
                ImmutableList<String> variableValues =
                    found.get(key).stream()
                        .map(Object::toString)
                        .collect(ImmutableList.toImmutableList());
                assertThat(variableValues).containsExactlyElementsIn(expectedValues);
                break;
              }

            default:
              throw new AssertionError("Unknown log keyword: " + key);
          }
        });
  }

  @Test
  public void testCorrectCalculationOfPreAndPostCondition() throws Exception {
    // precondition values
    final ImmutableList<String> preconditionValues =
        ImmutableList.of(
            "__VERIFIER_nondet_int!2@: 4",
            "main::number@3: 4",
            "main::copyForCheck@2: 4",
            "main::test@2: 1",
            "main::i@2: 2",
            "isPrime::n@2: 2",
            "isPrime::i@2: 2",
            "isPrime::__retval__@2: 1",
            "main::__CPAchecker_TMP_0@3: 1",
            "main::test@3: 2",
            "main::number@4: 2",
            "main::i@3: 2",
            "main::i@4: 3");
    // post-condition is on line 47
    final ImmutableList<Integer> postConditionLocation = ImmutableList.of(47);
    checkIfExpectedValuesMatchResultValues(
        "unit_test_pre-post-condition.c",
        FLAlgorithm.ERRINV,
        ImmutableMap.of(),
        ImmutableMap.<LogKeys, Object>builder()
            .put(LogKeys.TFPRECONDITION, preconditionValues)
            .put(LogKeys.TFPOSTCONDITION, postConditionLocation)
            .buildOrThrow());
  }

  @Test
  public void testErrorInvariantsOnFlowSensitiveTrace() throws Exception {
    // Lines that are presumably part of a fault
    List<Integer> faultyLines = ImmutableList.of(17);
    checkIfExpectedValuesMatchResultValues(
        "unit_test_traces.c",
        FLAlgorithm.ERRINV,
        ImmutableMap.of(),
        ImmutableMap.of(LogKeys.TFRESULT, faultyLines));
  }

  @Test
  public void testErrorInvariantsOnDefaultTrace() throws Exception {
    // Lines that are presumably part of a fault
    List<Integer> faultyLines = ImmutableList.of(17);
    checkIfExpectedValuesMatchResultValues(
        "unit_test_traces.c",
        FLAlgorithm.ERRINV,
        ImmutableMap.of("faultLocalization.by_traceformula.errorInvariants.disableFSTF", "true"),
        ImmutableMap.of(LogKeys.TFRESULT, faultyLines));
  }

  @Test
  public void testMaxSatSelectorTrace() throws Exception {
    // Lines that are presumably part of a fault
    List<Integer> faultyLines = ImmutableList.of(17);
    checkIfExpectedValuesMatchResultValues(
        "unit_test_traces.c",
        FLAlgorithm.MAXSAT,
        ImmutableMap.of(),
        ImmutableMap.of(LogKeys.TFRESULT, faultyLines));
  }
}
