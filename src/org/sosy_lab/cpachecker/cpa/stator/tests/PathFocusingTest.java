package org.sosy_lab.cpachecker.cpa.stator.tests;

import java.util.Map;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import com.google.common.collect.ImmutableMap;

/**
 * Integration testing for path focusing.
 */
public class PathFocusingTest {

  @Test public void checkInitialState() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/stateful.c"
    );
    results.assertIsSafe();
  }

  @Test public void checkComplexLoop() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/tests.c"
    );
    results.assertIsSafe();
  }

  @Test public void checkLoopBounds() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/loop_bounds.c"
    );
    results.assertIsSafe();
  }

  @Test public void checkSimplest() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/simplest.c"
    );
    results.assertIsSafe();
  }


  @Test public void checkLoopBounds2() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/loop2.c"
    );
    results.assertIsSafe();
  }

  private Map<String, String> getProperties() {
    return (ImmutableMap.<String, String>builder()
        .put("cpa", "cpa.arg.ARGCPA")
        .put("ARGCPA.cpa", "cpa.composite.CompositeCPA")
        .put("CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, " +
                "cpa.stator.policy.PolicyCPAPathFocusing")
        .put("cpa.predicate.solver", "Z3")
        .put("log.consoleLevel", "FINE")
        .put("specification", "config/specification/default.spc")
        .put("cpa.predicate.handlePointerAliasing", "false")
        .put("cpa.predicate.ignoreIrrelevantVariables", "false")
        .put("parser.usePreprocessor", "true")
        .put("reachedSet.export", "true")
    ).build();

  }

//  @Test public void checkPointerRead() throws Exception {
//    TestResults results = CPATestRunner.runAndLogToSTDOUT(
//        getProperties(),
//        "test/programs/stator/pointer_read.c"
//    );
//    results.assertIsSafe();
//  }
//
//  @Test public void checkPointerWrite() throws Exception {
//    TestResults results = CPATestRunner.runAndLogToSTDOUT(
//        getProperties(),
//        "test/programs/stator/pointer_write.c"
//    );
//    results.assertIsUnsafe();
//  }
//
//  @Test public void checkPointerAssignment() throws Exception {
//    TestResults results = CPATestRunner.runAndLogToSTDOUT(
//        getProperties(),
//        "test/programs/stator/pointer2.c"
//    );
//
//    results.assertIsSafe();
//  }
}
