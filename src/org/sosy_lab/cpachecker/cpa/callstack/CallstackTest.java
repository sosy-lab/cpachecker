// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.truth.Truth.assert_;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.io.TempFile.DeleteOnCloseFile;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class CallstackTest {

  /**
   * Test that CallstackCPA prevents coverage of two states inside a function if the paths of these
   * two states entered the current function separately.
   */
  @Test
  public void testCallstackPreventsUndesiredCoverage() throws Exception {
    List<String> program =
        ImmutableList.of(
            "extern int __VERIFIER_nondet_int();",
            "int global;",
            "",
            "void init() {",
            // create two ARG paths
            "  global = __VERIFIER_nondet_int() ? 1 : 2;",
            "}",
            "",
            "void f() {",
            // Set global variable to constant value such that one path can now cover the other.
            // CallstackCPA should prevent coverage inside f because we entered f on two paths.
            "  global = 3;",
            "}",
            "",
            "void main() {",
            "  init();",
            "  f();",
            "}");
    try (DeleteOnCloseFile programFile =
        TempFile.builder().prefix("test").suffix(".c").createDeleteOnClose()) {
      Files.write(programFile.toPath(), program);

      Configuration config =
          TestDataTools.configurationForTest()
              //          .setOption("cpa.arg.keepCoveredStatesInReached", "true")
              .setOption("cpa", "cpa.arg.ARGCPA")
              .setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA")
              .setOption(
                  "CompositeCPA.cpas",
                  "cpa.location.LocationCPA, cpa.callstack.CallstackCPA,"
                      + " cpa.value.ValueAnalysisCPA")
              .build();

      TestResults result = CPATestRunner.run(config, programFile.toPath().toString());
      result.assertIsSafe();

      FluentIterable<ARGState> argStates =
          from(result.getCheckerResult().getReached()).filter(ARGState.class);
      assert_()
          .withMessage("unexpected merged")
          .that(argStates.filter(s -> s.getParents().size() > 1))
          .isEmpty();

      List<ARGState> coveredStates =
          argStates.transformAndConcat(s -> s.getCoveredByThis()).toList();
      assert_().withMessage("exactly one covered state expected").that(coveredStates).hasSize(1);
      CFANode coverageLocation = AbstractStates.extractLocation(coveredStates.get(0));
      assert_()
          .withMessage("expected coverage only in main")
          .that(coverageLocation.getFunctionName())
          .isEqualTo("main");
      assert_()
          .withMessage("expected coverage right after return from f")
          .that(coverageLocation.getEnteringSummaryEdge())
          .isNotNull();
    }
  }
}
