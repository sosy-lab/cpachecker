package org.sosy_lab.cpachecker.cpa.policyiteration.tests;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

import java.nio.file.Paths;
import java.util.Map;

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

  @Test
  public void pointer_past_abstraction_true_assert() throws Exception {
    checkWithSlicing(
        "pointers/pointer_past_abstraction_true_assert.c",
        ImmutableMap.of("cpa.lpi.maxExpressionSize", "2"));
  }

  @Test
  public void pointer_past_abstraction_false_assert() throws Exception {
    checkWithSlicing(
        "pointers/pointer_past_abstraction_false_assert.c", ImmutableMap.of());
  }

  @Test
  public void pointers_loop_true_assert() throws Exception {
    checkWithSlicing(
        "pointers/pointers_loop_true_assert.c",
        ImmutableMap.of(
            "cpa.lpi.maxExpressionSize", "2",
            "cpa.lpi.linearizePolicy", "false"));
  }

  @Test public void octagons_loop_true_assert() throws Exception {
    check("octagons/octagons_loop_true_assert.c",
       ImmutableMap.of("cpa.lpi.maxExpressionSize", "2"));
  }

  @Test public void octagons_loop_false_assert() throws Exception {
    check("octagons/octagons_loop_false_assert.c",
        ImmutableMap.of("cpa.lpi.maxExpressionSize", "2"));
  }

  @Test public void inequality_true_assert() throws Exception {
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
        ImmutableMap.of("cpa.lpi.maxExpressionSize", "2",

            // Enabling two options below make non-prefixing variation of
            // val.det. work.
            "cpa.lpi.shortCircuitSyntactic", "false",
            "cpa.lpi.checkPolicyInitialCondition", "false"));
  }

  @Test public void array_false_assert() throws Exception {
    check("array_false_assert.c");
  }

  @Test public void classcast_fail_true_assert() throws Exception {
    check("classcast_fail_true_assert.c");
  }

  @Test public void formula_fail_true_assert() throws Exception {
    check("formula_fail_true_assert.c",
        ImmutableMap.of("cpa.lpi.allowedCoefficients", "1",
                        "cpa.lpi.abstractionLocations", "all"));
  }

  @Test public void unrolling_true_assert() throws Exception {
    check("unrolling_true_assert.c",
        ImmutableMap.of("cpa.loopstack.loopIterationsBeforeAbstraction", "2"));
  }

  @Test public void timeout_true_assert() throws Exception {
    check("timeout_true_assert.c");
  }

  @Test public void boolean_true_assert() throws Exception {
    // Use explicit value analysis to track boolean variables.
    check("boolean_true_assert.c",
        ImmutableMap.of("cpa.lpi.maxExpressionSize", "2",
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.loopstack.LoopstackCPA, cpa.value.ValueAnalysisCPA, cpa.policyiteration.PolicyCPA",
            "precision.trackIntAddVariables", "false",
            "precision.trackVariablesBesidesEqAddBool", "false"));
  }

  // Testing overflow tracking.
  @Test public void overflow_guards_true_assert() throws Exception {
    checkWithOverflow("overflow/guards_true_assert.c");
  }

  @Test public void overflow_increment_false_assert() throws Exception {
    checkWithOverflow("overflow/increment_false_assert.c");
  }

  @Test public void overflow_simplest_true_assert() throws Exception {
    checkWithOverflow("overflow/simplest_true_assert.c");
  }

  @Test public void increment_in_guard_false_assert() throws Exception {
    checkWithOverflow("overflow/increment_in_guard_false_assert.c");
  }

  @Test public void many_functions_true_assert() throws Exception {
    checkWithBAM("bam/many_functions_true_assert.c");
  }

  @Test public void many_functions_false_assert() throws Exception {
    checkWithBAM("bam/many_functions_false_assert.c");
  }

  @Test public void loop_around_summary_true_assert() throws Exception {
    checkWithBAM("bam/loop_around_summary_true_assert.c");
  }

  @Test public void loop_around_summary_false_assert() throws Exception {
    checkWithBAM("bam/loop_around_summary_false_assert.c");
  }

  private void check(String filename) throws Exception {
    check(filename, ImmutableMap.of());
  }

  private void check(String filename, Map<String, String> extra) throws Exception {
    check(filename, getProperties("policyIteration.properties", extra));
  }

  private void checkWithSlicing(String filename, Map<String, String> extra)
      throws Exception {
    check(filename, getProperties("policyIteration-with-slicing.properties", extra));
  }

  private void checkWithOverflow(String filename) throws Exception {
    check(
        filename,
        getProperties("policyIteration-with-overflow.properties", ImmutableMap.of())
    );
  }

  private void checkWithBAM(String filename) throws Exception {
    check(
        filename,
        getProperties("policyIteration-with-bam.properties", ImmutableMap.of())
    );
  }

  private void check(String filename, Configuration config) throws Exception {
    String fullPath;
    if (filename.contains("test/programs/benchmarks")) {
      fullPath = filename;
    } else {
      fullPath = Paths.get(TEST_DIR_PATH, filename).toString();
    }

    TestResults results = CPATestRunner.run(config, fullPath);
    if (filename.contains("_true_assert") || filename.contains("_true-unreach")) {
      results.assertIsSafe();
    } else if (filename.contains("_false_assert") || filename.contains("_false-unreach")) {
      results.assertIsUnsafe();
    }
  }

  private Configuration getProperties(String configFile, Map<String, String> extra)
      throws InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromResource(PolicyIterationTest.class, configFile)
        .setOptions(extra)
        .build();
  }
}
