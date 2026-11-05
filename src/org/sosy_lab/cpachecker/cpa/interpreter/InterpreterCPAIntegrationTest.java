// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interpreter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Checks that the interpreter configurations give the expected verdict for the specification that
 * CPAchecker derives from an SV-COMP property file, i.e., for what {@code --spec <property>.prp}
 * does on the command line.
 */
@RunWith(Parameterized.class)
public class InterpreterCPAIntegrationTest {

  private static final String PROGRAM_DIR = "test/programs/simple/interpreter/";

  /**
   * The property files that CPAchecker ships. The JUnit tests must not use the ones below {@code
   * test/programs/benchmarks/}, which is a submodule that is not always checked out.
   */
  private static final String PROPERTY_DIR = "config/properties/";

  /**
   * One verification task: the configuration that CPAchecker switches to for the given property,
   * the program to analyze, and the verdict that the analysis must report.
   */
  private record Task(
      String config,
      String property,
      String program,
      Result expected,
      ImmutableMap<String, String> options) {

    @Override
    public String toString() {
      return config + " " + program + " = " + expected;
    }
  }

  private static Task task(String pConfig, String pProperty, String pProgram, Result pExpected) {
    return new Task(pConfig, pProperty, pProgram, pExpected, ImmutableMap.of());
  }

  @Parameters(name = "{0}")
  public static ImmutableList<Task> tasks() {
    return ImmutableList.of(
        // The plain value analysis reports a spurious counterexample for the recursive programs,
        // because it loses the values of a caller during a recursive call.
        task("interpreter", "unreach-call", "recursive-factorial-true.c", Result.TRUE),
        task("interpreter", "unreach-call", "recursive-factorial-false.c", Result.FALSE),
        task("interpreter", "unreach-call", "mutual-recursion-true.c", Result.TRUE),
        // The program has more than one execution, so InterpreterCPA is not applicable to it
        // and must not claim any result.
        task("interpreter", "unreach-call", "nondeterministic-input.c", Result.UNKNOWN),
        // Values that a callee writes through a pointer to a local of its caller are kept only by
        // the SMG analysis.
        task("interpreter-smg", "unreach-call", "pointer-to-caller-local-true.c", Result.TRUE),
        // With sampling the programs above that depend on an input are executed for one assignment
        // of the inputs, so a violation is found.
        task("interpreter-sampling", "unreach-call", "nondeterministic-input.c", Result.FALSE),
        task(
            "interpreter-sampling",
            "unreach-call",
            "input-error-in-second-branch-false.c",
            Result.FALSE),
        // Only one of the executions was explored, so TRUE must not be reported even though this
        // execution did not violate the specification.
        task("interpreter-sampling", "unreach-call", "input-safe-unknown.c", Result.UNKNOWN),
        task("interpreter-sampling", "unreach-call", "recursive-factorial-true.c", Result.TRUE),
        task("interpreter--termination", "termination", "terminating-loop-true.c", Result.TRUE),
        // A non-terminating program cannot be shown to be non-terminating by executing it, so the
        // analysis must not report FALSE. It runs into the time limit, hence the small limit here.
        new Task(
            "interpreter--termination",
            "termination",
            "nonterminating.c",
            Result.UNKNOWN,
            ImmutableMap.of("limits.time.cpu", "10s")),
        task("interpreter--overflow", "no-overflow", "terminating-loop-true.c", Result.TRUE),
        task("interpreter--overflow", "no-overflow", "signed-overflow-false.c", Result.FALSE),
        task("interpreter--memorysafety", "valid-memsafety", "heap-recursion-true.i", Result.TRUE),
        task(
            "interpreter--memorysafety",
            "valid-memsafety",
            "heap-out-of-bounds-false.i",
            Result.FALSE),
        task(
            "interpreter--memorycleanup", "valid-memcleanup", "heap-recursion-true.i", Result.TRUE),
        task("interpreter--memorycleanup", "valid-memcleanup", "heap-leak-false.i", Result.FALSE));
  }

  @Parameter public Task task;

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  @Test
  public void testVerdict() throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile("config/" + task.config() + ".properties")
            .setOption("specification", PROPERTY_DIR + task.property() + ".prp")
            // CPAchecker takes the entry function from the property file itself when the property
            // is given on the command line, so it has to be set here.
            .setOption("analysis.entryFunction", "main")
            .setOptions(task.options())
            .build();
    IntegrationTestRunner.run(config, PROGRAM_DIR + task.program()).assertIs(task.expected());
  }
}
