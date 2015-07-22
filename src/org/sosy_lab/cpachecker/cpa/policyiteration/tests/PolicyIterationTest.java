package org.sosy_lab.cpachecker.cpa.policyiteration.tests;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
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

  private static final String TEST_DIR_PATH = "test/programs/policyiteration/";

  @Test public void stateful_true_assert() throws Exception {
    check("stateful_true_assert.c");
  }

  @Test public void tests_true_assert() throws Exception {
    check("tests_true_assert.c");
  }

  @Test public void loop_bounds_true_assert() throws Exception {
    check("loop_bounds_true_assert.c");
  }

  @Test public void pointer_read_true_assert() throws Exception {
    check("pointers/pointer_read_true_assert.c");
  }

  @Test public void pointer_read_false_assert() throws Exception {
    check("pointers/pointer_read_false_assert.c");
  }

  @Test public void pointer_write_false_assert() throws Exception {
    check("pointers/pointer_write_false_assert.c");
  }

  @Test public void pointer2_true_assert() throws Exception {
    check("pointers/pointer2_true_assert.c");
  }

  @Test public void loop2_true_assert() throws Exception {
    check("loop2_true_assert.c");
  }

  @Test public void simplest_false_assert() throws Exception {
    check("simplest_false_assert.c");
  }

  @Test public void loop_false_assert() throws Exception {
    check("loop_false_assert.c");
  }

  @Test public void double_pointer() throws Exception {
    check("pointers/double_pointer.c");
  }

  @Test public void loop_nested_false_assert() throws Exception {
    check("loop_nested_false_assert.c");
  }

  @Ignore
  @Test public void pointer_past_abstraction_true_assert() throws Exception {
    // todo: requires re-enabling formula slicing.
    check("pointers/pointer_past_abstraction_true_assert.c");
  }

  @Test public void pointer_past_abstraction_false_assert() throws Exception {
    check("pointers/pointer_past_abstraction_false_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateOctagons", "true"));
  }

  @Ignore
  @Test public void pointers_loop_true_assert() throws Exception {
    // todo: requires re-enabling formula slicing.
    check("pointers/pointers_loop_true_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateOctagons", "true"));
  }

  @Test public void octagons_loop_true_assert() throws Exception {
    check("octagons/octagons_loop_true_assert.c",
       ImmutableMap.of("cpa.stator.policy.generateOctagons", "true"));
  }

  @Test public void octagons_loop_false_assert() throws Exception {
    check("octagons/octagons_loop_false_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateOctagons", "true"));
  }

  @Test public void ineqality_true_assert() throws Exception {
    check("inequality_true_assert.c");
  }

  @Test public void initial_true_assert() throws Exception {
    check("initial_true_assert.c");
  }

  @Test public void template_generation_true_assert() throws Exception {
    check("template_generation_true_assert.c");
  }

  @Test public void pointers_change_aliasing_false_assert() throws Exception {
    check("pointers/pointers_change_aliasing_false_assert.c");
  }

  @Test public void fixpoint_true_assert() throws Exception {
    check("fixpoint_true_assert.c");
  }

  @Test public void valdet_prefixing_true_assert() throws Exception {
    check("valdet_prefixing_true_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateOctagons", "true",

            // Enabling two options below make non-prefixing variation of
            // val.det. work.
            "cpa.stator.policy.shortCircuitSyntactic", "false",
            "cpa.stator.policy.checkPolicyInitialCondition", "false"));
  }

  @Test public void array_false_assert() throws Exception {
    check("array_false_assert.c");
  }

  @Test public void classcast_fail_true_assert() throws Exception {
    check("classcast_fail_true_assert.c");
  }

  @Test public void formula_fail_true_assert() throws Exception {
    check("formula_fail_true_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateLowerBound", "false",
                        "cpa.stator.policy.generateFromAsserts", "false",
                        "cpa.stator.policy.pathFocusing", "false"));
  }

  @Test public void unrolling_true_assert() throws Exception {
    check("unrolling_true_assert.c",
        ImmutableMap.of("cpa.loopstack.loopIterationsBeforeAbstraction",
            "2"));
  }

  @Test public void timeout_true_assert() throws Exception {
    check("timeout_true_assert.c");
  }

  @Test public void boolean_true_assert() throws Exception {
    // Use explicit value analysis to track boolean variables.
    check("boolean_true_assert.c",
        ImmutableMap.of("cpa.stator.policy.generateOctagons", "true",
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.loopstack.LoopstackCPA, cpa.value.ValueAnalysisCPA, cpa.policyiteration.PolicyCPA",
            "cpa.stator.policy.joinOnMerge", "false",
            "precision.trackIntAddVariables", "false",
            "precision.trackVariablesBesidesEqAddBool", "false"));
  }

  @Test public void cex_check() throws Exception {
    check("test/programs/benchmarks/loops/terminator_01_false-unreach-call_false-termination.i",
        ImmutableMap.of(
            "analysis.checkCounterexamples", "true",
            "counterexample.checker", "CPACHECKER",
            "counterexample.checker.config",
              "config/cex-checks/predicateAnalysis-as-bitprecise-cex-check.properties"
        ));
  }

  private void check(String filename) throws Exception {
    check(filename, new HashMap<String, String>());
  }

  private void check(String filename, Map<String, String> extra) throws Exception {
    String fullPath;
    if (filename.contains("test/programs/benchmarks")) {
      fullPath = filename;
    } else {
      fullPath = Paths.get(TEST_DIR_PATH, filename).toString();
    }

    TestResults results = CPATestRunner.runAndLogToSTDOUT(
        getProperties(extra), fullPath);
    if (filename.contains("_true_assert") || filename.contains("_true-unreach")) {
      results.assertIsSafe();
    } else if (filename.contains("_false_assert") || filename.contains("_false-unreach")) {
      results.assertIsUnsafe();
    }
  }

  private Map<String, String> getProperties(Map<String, String> extra) {
    Map<String, String> props = new HashMap<>((ImmutableMap.<String, String>builder()
        .put("cpa", "cpa.arg.ARGCPA")
        .put("ARGCPA.cpa", "cpa.composite.CompositeCPA")
        .put("CompositeCPA.cpas",
            Joiner.on(", ").join(ImmutableList.<String>builder()
                .add("cpa.location.LocationCPA")
                .add("cpa.callstack.CallstackCPA")
                .add("cpa.functionpointer.FunctionPointerCPA")
                .add("cpa.loopstack.LoopstackCPA")
                .add("cpa.policyiteration.PolicyCPA")
                .build()
            ))
        )
        .put("cpa.loopstack.loopIterationsBeforeAbstraction", "1")
        .put("solver.z3.requireProofs", "false")
        .put("solver.solver", "Z3")
        .put("specification", "config/specification/default.spc")
        .put("cpa.predicate.ignoreIrrelevantVariables", "true")
        .put("cpa.predicate.maxArrayLength", "3")
        .put("cpa.predicate.defaultArrayLength", "3")
        .put("parser.usePreprocessor", "true")
        .put("cfa.findLiveVariables", "true")
        .put("analysis.traversal.order", "bfs")
        .put("analysis.traversal.useCallstack", "true")
        .put("analysis.traversal.useReversePostorder", "true")

        .put("log.consoleLevel", "INFO")
    .build());
    props.putAll(extra);
    return props;
  }
}
