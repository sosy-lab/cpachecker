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
package org.sosy_lab.cpachecker.cpa.policy;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import com.google.common.collect.ImmutableMap;

/**
 * Integration testing for policy iteration.
 */
public class PolicyCPATest {

  @Test
  public void runPolicyIteration() throws Exception {
    Map<String, String> prop = (ImmutableMap.<String, String>builder()
        .put("cpa", "cpa.arg.ARGCPA")
        .put("ARGCPA.cpa", "cpa.composite.CompositeCPA")
        .put("CompositeCPA.cpas",
            "cpa.location.LocationCPA, " +
            "cpa.callstack.CallstackCPA, cpa.policy.PolicyCPA")
        .put("cpa.predicate.solver", "Z3")
        .put("log.consoleLevel", "FINE")
        .put("specification", "config/specification/default.spc")
        .put("cpa.predicate.solver.useLogger", "true")
    ).build();

    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        prop,
        "test/programs/simple/policyTest.c"
    );

    String log = results.getLog();

    System.out.println(log);

    System.out.println(results.getCheckerResult().getResultString());

    Assert.assertEquals(false, results.isSafe());
  }
}
