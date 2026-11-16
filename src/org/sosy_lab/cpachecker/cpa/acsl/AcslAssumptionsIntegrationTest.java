// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/** Checks that the predicate analysis assumes the ACSL predicates of the ACSL CPA. */
public class AcslAssumptionsIntegrationTest {

  private static final String PROGRAM = "test/programs/acsl/assertion_as_assumption.c";

  private static Configuration predicateAnalysis(String pAdditionalCpas) throws Exception {
    return TestUtils.configurationForTest()
        .loadFromFile("config/predicateAnalysis.properties")
        .setOption("parser.collectACSLAnnotations", "true")
        .setOption(
            "CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.callstack.CallstackCPA,"
                + " cpa.functionpointer.FunctionPointerCPA, cpa.predicate.PredicateCPA"
                + pAdditionalCpas)
        .setOption("specification", "config/specification/default.spc")
        .build();
  }

  @Test
  public void predicateAnalysisAssumesAnnotations() throws Exception {
    IntegrationTestRunner.run(predicateAnalysis(", cpa.acsl.ACSLCPA"), PROGRAM).assertIsSafe();
  }

  @Test
  public void predicateAnalysisWithoutAnnotations() throws Exception {
    IntegrationTestRunner.run(predicateAnalysis(""), PROGRAM).assertIsUnsafe();
  }
}
