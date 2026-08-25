// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class AutomatonTest {
  private static final String CPAS_UNINITVARS =
      "cpa.location.LocationCPA, cpa.uninitvars.UninitializedVariablesCPA";
  private static final String OUTPUT_FILE = "output/AutomatonExport.dot";

  // Specification Tests
  @Test
  public void cyclicInclusionTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_UNINITVARS,
            "specification", "test/config/automata/tmpSpecification.spc",
            "analysis.stopAfterError", "FALSE");

    Path tmpSpc = Path.of("test/config/automata/tmpSpecification.spc");
    String content =
        "#include UninitializedVariablesTestAutomaton.txt \n#include tmpSpecification.spc \n";
    IO.writeFile(tmpSpc, StandardCharsets.US_ASCII, content);
    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/UninitVarsErrors.c");
    results.assertIsSafe();
    assertThat(results.log())
        .contains("test/config/automata/tmpSpecification.spc\" was referenced multiple times.");
    Files.delete(tmpSpc);
  }

  @Test
  public void includeSpecificationTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_UNINITVARS,
            "specification", "test/config/automata/defaultSpecificationForTesting.spc",
            "analysis.stopAfterError", "FALSE");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/UninitVarsErrors.c");
    assertThat(results.log()).contains("Automaton: Uninitialized return value");
    assertThat(results.log()).contains("Automaton: Uninitialized variable used");
    /*
    results = run(prop, "test/programs/simple/PointerAnalysisErrors.c");
    assertThat(results.getLog()).contains("Found a DOUBLE_FREE"));
    assertThat(results.getLog()).contains("Found an INVALID_FREE"));
    assertThat(results.getLog()).contains("Found a POTENTIALLY_UNSAFE_DEREFERENCE"));
    assertThat(results.getLog()).contains("Found a Memory Leak"));
    assertThat(results.getLog()).contains("Found an UNSAFE_DEREFERENCE"));
    */
  }

  @Test
  public void specificationAndNoCompositeTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "cpa", "cpa.location.LocationCPA",
            "specification", "test/config/automata/LockingAutomatonAll.txt");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/modificationExample.c");
    assertThat(results.log())
        .contains("Option specification gave specification automata, but no CompositeCPA was used");
    results.assertIs(Result.NOT_YET_STARTED);
  }

  @Test
  public void modificationTestWithSpecification() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA",
            "specification", "test/config/automata/modifyingAutomaton.txt",
            "cpa.value.threshold", "10");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/modificationExample.c");
    assertThat(results.log()).contains("MODIFIED");
    assertThat(results.log()).contains("Modification successful");
    results.assertIsSafe();
  }

  // Automaton Tests
  @Test
  public void syntaxErrorTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of("specification", "config/predicateAnalysis.properties");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/UninitVarsErrors.c");
    results.assertIs(Result.NOT_YET_STARTED);
    assertThat(results.log()).contains("Illegal character");
  }

  @Test
  public void matchEndOfProgramTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA",
            "specification", "test/config/automata/PrintLastStatementAutomaton.spc",
            "analysis.stopAfterError", "TRUE");

    IntegrationTestResult results = IntegrationTestRunner.run(prop, "test/programs/simple/loop1.c");
    assertThat(results.log()).contains("Last statement is \"return (0);\"");
    assertThat(results.log()).contains("Last statement is \"return (-1);\"");
    results.assertIsSafe();
  }

  @Test
  public void failIfNoAutomatonGiven() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA,"
                    + " cpa.automaton.ControlAutomatonCPA",
            "log.consoleLevel", "INFO",
            "cpa.value.threshold", "10");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/modificationExample.c");
    results.assertIs(Result.NOT_YET_STARTED);
    assertThat(results.log())
        .contains("Explicitly specified automaton CPA needs option cpa.automaton.inputFile!");
  }

  @Test
  public void modificationTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA,"
                    + " cpa.automaton.ControlAutomatonCPA",
            "cpa.automaton.inputFile", "test/config/automata/modifyingAutomaton.txt",
            "cpa.value.threshold", "10");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/modificationExample.c");
    assertThat(results.log()).contains("MODIFIED");
    assertThat(results.log()).contains("Modification successful");
    results.assertIsSafe();
  }

  @Test
  public void modification_in_Observer_throws_Test() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA,"
                    + " cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile", "test/config/automata/modifyingAutomaton.txt",
            "cpa.value.threshold", "10");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/modificationExample.c");
    // check for stack trace
    assertThat(results.log()).contains("Error: Invalid configuration (The transition \"MATCH ");
  }

  @Test
  public void setuidTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile", "test/config/automata/simple_setuid.txt",
            "analysis.stopAfterError", "FALSE");
    Configuration config = TestUtils.configurationForTest().setOptions(prop).build();
    IntegrationTestResult results =
        IntegrationTestRunner.run(config, "test/programs/simple/simple_setuid_test.c", Level.FINER);
    assertThat(results.log()).contains("Systemcall in line 22 with userid 2");
    assertThat(results.log()).contains("going to ErrorState on edge \"system(40);\"");
    results.assertIsUnsafe();
  }

  @Test
  public void uninitVarsTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA,"
                + " cpa.uninitvars.UninitializedVariablesCPA",
            "cpa.automaton.inputFile",
            "test/config/automata/UninitializedVariablesTestAutomaton.txt",
            "cpa.automaton.dotExportFile",
            OUTPUT_FILE,
            "analysis.stopAfterError",
            "FALSE");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/UninitVarsErrors.c");
    assertThat(results.log()).contains("Automaton: Uninitialized return value");
    assertThat(results.log()).contains("Automaton: Uninitialized variable used");
  }

  @Test
  public void locking_correct() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile", "test/config/automata/LockingAutomatonAll.txt",
            "cpa.automaton.dotExportFile", OUTPUT_FILE);

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/locking_correct.c");
    results.assertIsSafe();
  }

  @Test
  public void locking_incorrect() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile", "test/config/automata/LockingAutomatonAll.txt");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/locking_incorrect.c");
    results.assertIsUnsafe();
  }

  @Test
  public void set_variable_correct() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.callstack.CallstackCPA,"
                + " cpa.functionpointer.FunctionPointerCPA, cpa.value.ValueAnalysisCPA,"
                + " cpa.predicate.PredicateCPA, cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile",
            "test/config/automata/set_variable.spc",
            "cpa.automaton.dotExportFile",
            OUTPUT_FILE);

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/set_correct.c");
    results.assertIsSafe();
  }

  @Test
  public void set_variable_incorrect() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.callstack.CallstackCPA,"
                + " cpa.functionpointer.FunctionPointerCPA, cpa.value.ValueAnalysisCPA,"
                + " cpa.predicate.PredicateCPA, cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile",
            "test/config/automata/set_variable.spc",
            "cpa.automaton.dotExportFile",
            OUTPUT_FILE);

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/set_incorrect.c");
    results.assertIsUnsafe();
  }

  @Test
  public void valueAnalysis_observing() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA,"
                    + " cpa.value.ValueAnalysisCPA",
            "cpa.automaton.inputFile",
                "test/config/automata/ExplicitAnalysisObservingAutomaton.txt",
            "cpa.value.threshold", "2000");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/ex2.cil.c");
    assertThat(results.log()).contains("st==3 after Edge st = 3;");
    assertThat(results.log()).contains("st==1 after Edge st = 1;");
    assertThat(results.log()).contains("st==2 after Edge st = 2;");
    assertThat(results.log()).contains("st==4 after Edge st = 4;");
    results.assertIsSafe();
  }

  @Test
  public void functionIdentifying() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile", "test/config/automata/FunctionIdentifyingAutomaton.txt");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/simple/functionCall.c");
    assertThat(results.log()).contains("i'm in Main after Edge int y;");
    assertThat(results.log()).contains("i'm in f after Edge y = (f)()");
    assertThat(results.log()).contains("i'm in f after Edge int x;");
    assertThat(results.log()).contains("i'm in Main after Edge return");
    assertThat(results.log()).contains("i'm in Main after Edge ERROR:");
  }

  @Test
  public void interacting_Automata() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas",
                "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA automatonA,"
                    + " cpa.automaton.ObserverAutomatonCPA automatonB, cpa.value.ValueAnalysisCPA",
            "automatonA.cpa.automaton.inputFile", "test/config/automata/InteractionAutomatonA.txt",
            "automatonB.cpa.automaton.inputFile", "test/config/automata/InteractionAutomatonB.txt",
            "cpa.value.threshold", "2000");

    IntegrationTestResult results = IntegrationTestRunner.run(prop, "test/programs/simple/loop1.c");
    assertThat(results.log()).contains("A: Matched i in line 21 x=2");
    assertThat(results.log()).contains("B: A increased to 2 And i followed ");
    results.assertIsSafe();
  }

  @Test
  public void coversSingleLine() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA",
            "specification", "test/config/automata/testCoversSingleLine.txt",
            "log.consoleLevel", "INFO");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/coverage/test1.c");
    results.assertIs(Result.FALSE);
  }

  @Test
  public void coversEitherLine() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA",
            "specification", "test/config/automata/testCoversEitherLine.txt");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/coverage/test2.c");
    results.assertIs(Result.FALSE);
  }

  @Test
  public void doesNotCoverUnreachableLine() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA",
            "specification", "test/config/automata/testNotCoveringUnreachableLine.txt");

    IntegrationTestResult results =
        IntegrationTestRunner.run(prop, "test/programs/coverage/test2.c");
    results.assertIs(Result.TRUE);
  }
}
