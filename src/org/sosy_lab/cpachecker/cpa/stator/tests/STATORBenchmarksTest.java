package org.sosy_lab.cpachecker.cpa.stator.tests;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import com.google.common.collect.ImmutableMap;

/**
 * Integration testing for policy iteration.
 */
public class STATORBenchmarksTest {

  @Test public void checkInitialState() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/stateful.c"
    );
    Assert.assertEquals(true, results.isSafe());
  }

  @Test public void checkComplexLoop() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/tests.c"
    );
    Assert.assertEquals(true, results.isSafe());
  }

  @Test public void checkLoopBounds() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/loop_bounds.c"
    );
    Assert.assertEquals(true, results.isSafe());
  }

  @Test public void checkPointerRead() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/pointer_read.c"
    );
    Assert.assertEquals(true, results.isSafe());
  }

  @Test public void checkPointerWrite() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/pointer_write.c"
    );
    Assert.assertEquals(false, results.isSafe());
  }

  @Test public void checkPointerAssignment() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/pointer2.c"
    );

    Assert.assertEquals(true, results.isSafe());
  }

  @Test public void checkLoopBounds2() throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(),
        "test/programs/stator/loop2.c"
    );
    Assert.assertEquals(true, results.isSafe());
  }

  private Map<String, String> getProperties() {
    return (ImmutableMap.<String, String>builder()
        .put("cpa", "cpa.arg.ARGCPA")
        .put("ARGCPA.cpa", "cpa.composite.CompositeCPA")
        .put("CompositeCPA.cpas",
            "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, " +
                "cpa.stator.memory.ExplicitMemoryCPA, " +
                "cpa.stator.policy.PolicyCPA")
        .put("cpa.predicate.solver", "Z3")
        .put("log.consoleLevel", "INFO")
        .put("specification", "config/specification/default.spc")
        .put("cpa.predicate.handlePointerAliasing", "false")
        .put("cpa.predicate.ignoreIrrelevantVariables", "false")
        .put("parser.usePreprocessor", "true")
    ).build();

  }
}
