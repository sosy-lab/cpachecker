/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class AutomatonTest {
  private static final String CPAS_UNINITVARS = "cpa.location.LocationCPA, cpa.uninitvars.UninitializedVariablesCPA";
  private static final String OUTPUT_FILE = "output/AutomatonExport.dot";

  // Specification Tests
  @Test
  public void cyclicInclusionTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",       CPAS_UNINITVARS,
        "specification",           "test/config/automata/tmpSpecification.spc",
        "analysis.stopAfterError", "FALSE"
      );

      Path tmpSpc = Paths.get("test/config/automata/tmpSpecification.spc");
      String content = "#include UninitializedVariablesTestAutomaton.txt \n" +
      "#include tmpSpecification.spc \n";
      MoreFiles.writeFile(tmpSpc, StandardCharsets.US_ASCII, content);
      TestResults results = CPATestRunner.run(prop, "test/programs/simple/UninitVarsErrors.c");
      results.assertIsSafe();
      assertThat(results.getLog()).contains("test/config/automata/tmpSpecification.spc\" was referenced multiple times.");
      Files.delete(tmpSpc);
  }
  @Test
  public void includeSpecificationTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",        CPAS_UNINITVARS,
        "specification",            "test/config/automata/defaultSpecificationForTesting.spc",
        "analysis.stopAfterError", "FALSE"
      );

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/UninitVarsErrors.c");
      assertThat(results.getLog()).contains("Automaton: Uninitialized return value");
      assertThat(results.getLog()).contains("Automaton: Uninitialized variable used");
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
    Map<String, String> prop = ImmutableMap.of(
        "cpa",              "cpa.location.LocationCPA",
        "specification",    "test/config/automata/LockingAutomatonAll.txt");

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/modificationExample.c");
      assertThat(results.getLog()).contains("Option specification gave specification automata, but no CompositeCPA was used");
      results.assertIs(Result.NOT_YET_STARTED);
  }
  @Test
  public void modificationTestWithSpecification() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",   "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA",
        "specification",       "test/config/automata/modifyingAutomaton.txt",
        "cpa.value.threshold", "10");

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/modificationExample.c");
      assertThat(results.getLog()).contains("MODIFIED");
      assertThat(results.getLog()).contains("Modification successful");
      results.assertIsSafe();
  }

  //Automaton Tests
  @Test
  public void syntaxErrorTest() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of("specification", "config/predicateAnalysis.properties");

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/UninitVarsErrors.c");
      results.assertIs(Result.NOT_YET_STARTED);
      assertThat(results.getLog()).contains("Illegal character");
  }

  @Test
  public void matchEndOfProgramTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",       "cpa.location.LocationCPA",
        "specification",           "test/config/automata/PrintLastStatementAutomaton.spc",
        "analysis.stopAfterError", "TRUE"
      );

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/loop1.c");
      assertThat(results.getLog()).contains("Last statement is \"return (0);\"");
      assertThat(results.getLog()).contains("Last statement is \"return (-1);\"");
      results.assertIsSafe();
  }
  @Test
  public void failIfNoAutomatonGiven() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",   "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA, cpa.automaton.ControlAutomatonCPA",
        "log.consoleLevel",    "INFO",
        "cpa.value.threshold", "10");

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/modificationExample.c");
      results.assertIs(Result.NOT_YET_STARTED);
      assertThat(results.getLog()).contains("Explicitly specified automaton CPA needs option cpa.automaton.inputFile!");
  }

  @Test
  public void modificationTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",       "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA, cpa.automaton.ControlAutomatonCPA",
        "cpa.automaton.inputFile", "test/config/automata/modifyingAutomaton.txt",
        "cpa.value.threshold",     "10");

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/modificationExample.c");
      assertThat(results.getLog()).contains("MODIFIED");
      assertThat(results.getLog()).contains("Modification successful");
      results.assertIsSafe();
  }

  @Test
  public void modification_in_Observer_throws_Test() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",       "cpa.location.LocationCPA, cpa.value.ValueAnalysisCPA, cpa.automaton.ObserverAutomatonCPA",
        "cpa.automaton.inputFile", "test/config/automata/modifyingAutomaton.txt",
        "cpa.value.threshold",     "10"
      );

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/modificationExample.c");
      // check for stack trace
      assertThat(results.getLog()).contains("Error: Invalid configuration (The transition \"MATCH ");
  }

  @Test
  public void setuidTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",       "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
        "cpa.automaton.inputFile", "test/config/automata/simple_setuid.txt",
        "analysis.stopAfterError", "FALSE"
      );


      TestResults results = CPATestRunner.run(prop, "test/programs/simple/simple_setuid_test.c");
      assertThat(results.getLog()).contains("Systemcall in line 14 with userid 2");
      assertThat(results.getLog()).contains("going to ErrorState on edge \"system(40);\"");
      results.assertIsUnsafe();
  }
  @Test
  public void uninitVarsTest() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",           "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA, cpa.uninitvars.UninitializedVariablesCPA",
        "cpa.automaton.inputFile",     "test/config/automata/UninitializedVariablesTestAutomaton.txt",
        "cpa.automaton.dotExportFile", OUTPUT_FILE,
        "analysis.stopAfterError",     "FALSE"
      );

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/UninitVarsErrors.c");
      assertThat(results.getLog()).contains("Automaton: Uninitialized return value");
      assertThat(results.getLog()).contains("Automaton: Uninitialized variable used");
  }

  @Test
  public void locking_correct() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",           "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
        "cpa.automaton.inputFile",     "test/config/automata/LockingAutomatonAll.txt",
        "cpa.automaton.dotExportFile", OUTPUT_FILE
      );

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/locking_correct.c");
      results.assertIsSafe();
  }

  @Test
  public void locking_incorrect() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile", "test/config/automata/LockingAutomatonAll.txt");

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/locking_incorrect.c");
      results.assertIsUnsafe();
  }

  @Test
  public void valueAnalysis_observing() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",       "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA, cpa.value.ValueAnalysisCPA",
        "cpa.automaton.inputFile", "test/config/automata/ExplicitAnalysisObservingAutomaton.txt",
        "cpa.value.threshold",     "2000"
      );

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/ex2.cil.c");
      assertThat(results.getLog()).contains("st==3 after Edge st = 3;");
      assertThat(results.getLog()).contains("st==1 after Edge st = 1;");
      assertThat(results.getLog()).contains("st==2 after Edge st = 2;");
      assertThat(results.getLog()).contains("st==4 after Edge st = 4;");
      results.assertIsSafe();
  }

  @Test
  public void functionIdentifying() throws Exception {
    Map<String, String> prop =
        ImmutableMap.of(
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA",
            "cpa.automaton.inputFile", "test/config/automata/FunctionIdentifyingAutomaton.txt");

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/functionCall.c");
      assertThat(results.getLog()).contains("i'm in Main after Edge int y;");
      assertThat(results.getLog()).contains("i'm in f after Edge y = f()");
      assertThat(results.getLog()).contains("i'm in f after Edge int x;");
      assertThat(results.getLog()).contains("i'm in Main after Edge return");
      assertThat(results.getLog()).contains("i'm in Main after Edge ERROR:");
  }

  @Test
  public void interacting_Automata() throws Exception {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",                  "cpa.location.LocationCPA, cpa.automaton.ObserverAutomatonCPA automatonA, cpa.automaton.ObserverAutomatonCPA automatonB, cpa.value.ValueAnalysisCPA",
        "automatonA.cpa.automaton.inputFile", "test/config/automata/InteractionAutomatonA.txt",
        "automatonB.cpa.automaton.inputFile", "test/config/automata/InteractionAutomatonB.txt",
        "cpa.value.threshold" ,               "2000"
      );

      TestResults results = CPATestRunner.run(prop, "test/programs/simple/loop1.c");
      assertThat(results.getLog()).contains("A: Matched i in line 13 x=2");
      assertThat(results.getLog()).contains("B: A increased to 2 And i followed ");
      results.assertIsSafe();
  }
}
