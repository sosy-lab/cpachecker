package org.sosy_lab.cpachecker.cpa.policyiteration.tests;

import java.util.HashMap;
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

  @Test public void stateful_true_assert() throws Exception {
    check("test/programs/policyiteration/stateful_true_assert.c");
  }

  @Test public void tests_true_assert() throws Exception {
    check("test/programs/policyiteration/tests_true_assert.c");
  }

  @Test public void loop_bounds_true_assert() throws Exception {
    check("test/programs/policyiteration/loop_bounds_true_assert.c");
  }

  @Test public void pointer_read_true_assert() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_read_true_assert.c");
  }

  @Test public void pointer_read_false_assert() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_read_false_assert.c");
  }

  @Test public void pointer_write_false_assert() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_write_false_assert.c");
  }

  @Test public void pointer2_true_assert() throws Exception {
    check("test/programs/policyiteration/pointers/pointer2_true_assert.c");
  }

  @Test public void loop2_true_assert() throws Exception {
    check("test/programs/policyiteration/loop2_true_assert.c");
  }

  @Test public void simplest_false_assert() throws Exception {
    check("test/programs/policyiteration/simplest_false_assert.c");
  }

  @Test public void loop_false_assert() throws Exception {
    check("test/programs/policyiteration/loop_false_assert.c");
  }

  @Test public void double_pointer() throws Exception {
    check("test/programs/policyiteration/pointers/double_pointer.c");
  }

  @Test public void loop_nested_false_assert() throws Exception {
    check("test/programs/policyiteration/loop_nested_false_assert.c");
  }

  @Test public void pointer_past_abstraction_true_assert() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_past_abstraction_true_assert.c");
  }

  @Test public void pointer_past_abstraction_false_assert() throws Exception {
    check("test/programs/policyiteration/pointers/pointer_past_abstraction_false_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateOctagons", "true"));
  }

  @Test public void pointers_loop_true_assert() throws Exception {
    check("test/programs/policyiteration/pointers/pointers_loop_true_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateOctagons", "true"));
  }

  @Test public void octagons_loop_true_assert() throws Exception {
    check("test/programs/policyiteration/octagons/octagons_loop_true_assert.c",
       ImmutableMap.of("cpa.stator.policy.generateOctagons", "true"));
  }

  @Test public void octagons_loop_false_assert() throws Exception {
    check("test/programs/policyiteration/octagons/octagons_loop_false_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateOctagons", "true"));
  }

  @Test public void ineqality_true_assert() throws Exception {
    check("test/programs/policyiteration/inequality_true_assert.c");
  }

  @Test public void initial_true_assert() throws Exception {
    check("test/programs/policyiteration/initial_true_assert.c");
  }

  private void check(String filename) throws Exception {
    check(filename, new HashMap<String, String>());
  }

  private void check(String filename, Map<String, String> extra) throws Exception {
    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(extra),
        filename
    );
    if (filename.contains("_true_assert")) {
      results.assertIsSafe();
    } else if (filename.contains("_false_assert") || filename.contains("_false-unreach")) {
      results.assertIsUnsafe();
    }
  }

  private Map<String, String> getProperties(Map<String, String> extra) {
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
        .putAll(extra)
        .put("reachedSet.export", "true")
        .put("cpa.predicate.solver", "Z3")
        .put("log.consoleLevel", "FINE")
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
