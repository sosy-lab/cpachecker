package org.sosy_lab.cpachecker.cpa.policyiteration.tests;

import java.util.Map;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import com.google.common.collect.ImmutableMap;

/**
 * Integration testing for policy iteration.
 */
public class PolicyIterationTest {

  @Test public void checkInitialState() throws Exception {
    check("test/programs/policyiteration/stateful_true_assert.c");
  }

  @Test public void checkComplexLoop() throws Exception {
    check("test/programs/policyiteration/tests_true_assert.c");
  }

  @Test public void checkLoopBounds() throws Exception {
    check("test/programs/policyiteration/loop_bounds_true_assert.c");
  }

  @Test public void checkPointerRead() throws Exception {
    check("test/programs/policyiteration/pointer_read_true_assert.c");
  }

  @Test public void checkPointerWrite() throws Exception {
    check("test/programs/policyiteration/pointer_write_false_assert.c");
  }

// Commented out for performance.
//  @Test public void checkPointerAssignment() throws Exception {
//    check("test/programs/policyiteration/pointer2_true_assert.c");
//  }
//
//  @Test public void checkLoopBounds2() throws Exception {
//    check("test/programs/policyiteration/loop2_true_assert.c");
//  }
//
//  @Test public void checkSimpleFail() throws Exception {
//    check("test/programs/policyiteration/simplest_false_assert.c");
//  }
//
//  @Test public void checkLoopFail() throws Exception {
//    check("test/programs/policyiteration/loop_false_assert.c");
//  }
//
//  @Test public void checkDoublePointer() throws Exception {
//    check("test/programs/policyiteration/double_pointer.c");
//  }
//
//  @Test public void checkNestedLoopFail() throws Exception {
//    check("test/programs/policyiteration/loop_nested_false_assert.c");
//  }

  private void check(String filename) throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        filename
    );
    if (filename.contains("_true_assert")) {
      results.assertIsSafe();
    } else if (filename.contains("_false_assert")) {
      results.assertIsUnsafe();
    }

  }

  private Map<String, String> getProperties() {
    return (ImmutableMap.<String, String>builder()
        .put("cpa", "cpa.arg.ARGCPA")
        .put("ARGCPA.cpa", "cpa.composite.CompositeCPA")
        .put("CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, " +
                "cpa.policyiteration.PolicyCPA")
        .put("cpa.predicate.solver", "Z3")
        .put("log.consoleLevel", "INFO")
        .put("specification", "config/specification/default.spc")
        .put("cpa.predicate.ignoreIrrelevantVariables", "false")
        .put("parser.usePreprocessor", "true")
        .put("cfa.findLiveVariables", "true")
    ).build();

  }
}
