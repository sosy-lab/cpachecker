/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.callstack;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.truth.Truth.assert_;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.MoreFiles.DeleteOnCloseFile;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

import java.nio.file.Files;
import java.util.List;

public class CallstackTest {

  /**
   * Test that CallstackCPA prevents coverage of two states inside a function
   * if the paths of these two states entered the current function separately.
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
    try (DeleteOnCloseFile programFile = MoreFiles.createTempFile("test", ".c")) {
      Files.write(programFile.toPath(), program);

      Configuration config =
          TestDataTools.configurationForTest()
              //          .setOption("cpa.arg.keepCoveredStatesInReached", "true")
              .setOption("cpa", "cpa.arg.ARGCPA")
              .setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA")
              .setOption(
                  "CompositeCPA.cpas",
                  "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.value.ValueAnalysisCPA")
              .build();

      TestResults result = CPATestRunner.run(config, programFile.toPath().toString());
      result.assertIsSafe();

      FluentIterable<ARGState> argStates =
          from(result.getCheckerResult().getReached()).filter(ARGState.class);
      assert_()
          .withFailureMessage("unexpected merged")
          .that(argStates.filter(s -> s.getParents().size() > 1))
          .isEmpty();

      List<ARGState> coveredStates =
          argStates.transformAndConcat(s -> s.getCoveredByThis()).toList();
      assert_()
          .withFailureMessage("exactly one covered state expected")
          .that(coveredStates)
          .hasSize(1);
      CFANode coverageLocation = AbstractStates.extractLocation(coveredStates.get(0));
      assert_()
          .withFailureMessage("expected coverage only in main")
          .that(coverageLocation.getFunctionName())
          .isEqualTo("main");
      assert_()
          .withFailureMessage("expected coverage right after return from f")
          .that(coverageLocation.getEnteringSummaryEdge())
          .isNotNull();
    }
  }
}
