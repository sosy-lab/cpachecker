package org.sosy_lab.cpachecker.cpa.policyiteration.tests;

import java.util.Map;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
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
    check("test/programs/policyiteration/pointers/pointer_read_true_assert.c");
  }

  @Test public void checkPointerReadFail() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_read_false_assert.c");
  }

  @Test public void checkPointerWrite() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_write_false_assert.c");
  }

  @Test public void checkPointerAssignment() throws Exception {
    check("test/programs/policyiteration/pointers/pointer2_true_assert.c");
  }

  @Test public void checkLoopBounds2() throws Exception {
    check("test/programs/policyiteration/loop2_true_assert.c");
  }

  @Test public void checkSimpleFail() throws Exception {
    check("test/programs/policyiteration/simplest_false_assert.c");
  }

  @Test public void checkLoopFail() throws Exception {
    check("test/programs/policyiteration/loop_false_assert.c");
  }

  @Test public void checkDoublePointer() throws Exception {
    check("test/programs/policyiteration/pointers/double_pointer.c");
  }

  @Test public void checkNestedLoopFail() throws Exception {
    check("test/programs/policyiteration/loop_nested_false_assert.c");
  }

  @Test public void checkPointerPastAbstractionTrue() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_past_abstraction_true_assert.c");
  }

  @Test public void checkPointerPastAbstractionFalse() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_past_abstraction_false_assert.c");
  }

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
        .put("cpa", "cpa.composite.CompositeCPA")
        .put("CompositeCPA.cpas",
            Joiner.on(", ").join(ImmutableList.of(
                "cpa.location.LocationCPA",
                "cpa.callstack.CallstackCPA",
//                "cpa.conditions.path.PathConditionsCPA",
//                "cpa.value.ValueAnalysisCPA",
//                "cpa.pointer2.PointerCPA",
                "cpa.policyiteration.PolicyCPA"
            ))
        )
        .put("cpa.predicate.solver", "Z3")
        .put("log.consoleLevel", "INFO")
        .put("specification", "config/specification/default.spc")
        .put("cpa.predicate.ignoreIrrelevantVariables", "false")
        .put("parser.usePreprocessor", "true")
        .put("cfa.findLiveVariables", "true")
//        .put("cpa.conditions.path.assignments.hardThreshold", "1")
//        .put("cpa.conditions.path.assignments.softThreshold", "2")
//        .put("cpa.conditions.path.condition", "AssignmentsInPathCondition")
    ).build();

  }
}
