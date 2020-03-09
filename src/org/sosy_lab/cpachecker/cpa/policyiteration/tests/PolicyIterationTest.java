package org.sosy_lab.cpachecker.cpa.policyiteration.tests;

import static org.junit.Assume.assumeNoException;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.CPATestRunner.ExpectedVerdict;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

/**
 * Integration testing for policy iteration.
 */
public class PolicyIterationTest {

  private static final String TEST_DIR_PATH = "test/programs/policyiteration/";

  @Test public void stateful_true_assert() throws Exception {
    check("stateful.c", ExpectedVerdict.TRUE);
  }

  @Test public void tests_true_assert() throws Exception {
    check("tests.c", ExpectedVerdict.TRUE);
  }

  @Test public void loop_bounds_true_assert() throws Exception {
    check("loop_bounds.c", ExpectedVerdict.TRUE);
  }

  @Test public void pointer_read_true_assert() throws Exception {
    check("pointers/pointer_read-1.c", ExpectedVerdict.TRUE);
  }

  @Test public void pointer_read_false_assert() throws Exception {
    check("pointers/pointer_read-2.c", ExpectedVerdict.FALSE);
  }

  @Test public void pointer_write_false_assert() throws Exception {
    check("pointers/pointer_write.c", ExpectedVerdict.FALSE);
  }

  @Test public void pointer2_true_assert() throws Exception {
    check("pointers/pointer2.c", ExpectedVerdict.TRUE);
  }

  @Test public void loop2_true_assert() throws Exception {
    check("loop2.c", ExpectedVerdict.TRUE);
  }

  @Test public void simplest_false_assert() throws Exception {
    check("simplest-1.c", ExpectedVerdict.FALSE);
  }

  @Test public void loop_false_assert() throws Exception {
    check("loop.c", ExpectedVerdict.FALSE);
  }

  @Test public void double_pointer() throws Exception {
    check("pointers/double_pointer.c", ExpectedVerdict.NONE);
  }

  @Test public void loop_nested_false_assert() throws Exception {
    check("loop_nested-2.c", ExpectedVerdict.FALSE);
  }

  @Test
  public void pointer_past_abstraction_true_assert() throws Exception {
    checkWithSlicing(
        "pointers/pointer_past_abstraction-2.c",
        ImmutableMap.of("cpa.lpi.maxExpressionSize", "2"),
        ExpectedVerdict.TRUE);
  }

  @Test
  public void pointer_past_abstraction_false_assert() throws Exception {
    checkWithSlicing(
        "pointers/pointer_past_abstraction-1.c", ImmutableMap.of(), ExpectedVerdict.FALSE);
  }

  @Test
  public void pointers_loop_true_assert() throws Exception {
    checkWithSlicing(
        "pointers/pointers_loop-2.c",
        ImmutableMap.of(
            "cpa.lpi.maxExpressionSize", "2",
            "cpa.lpi.linearizePolicy", "false"),
        ExpectedVerdict.TRUE);
  }

  @Test public void octagons_loop_true_assert() throws Exception {
    check("octagons/octagons_loop-1.c",
       ImmutableMap.of("cpa.lpi.maxExpressionSize", "2"),
       ExpectedVerdict.TRUE);
  }

  @Test public void octagons_loop_false_assert() throws Exception {
    check("octagons/octagons_loop-2.c",
        ImmutableMap.of("cpa.lpi.maxExpressionSize", "2"),
        ExpectedVerdict.FALSE);
  }

  @Test public void inequality_true_assert() throws Exception {
    check("inequality.c", ExpectedVerdict.TRUE);
  }

  @Test public void initial_true_assert() throws Exception {
    check("initial.c", ExpectedVerdict.TRUE);
  }

  @Test public void template_generation_true_assert() throws Exception {
    check("template_generation.c", ExpectedVerdict.TRUE);
  }

  @Test public void pointers_change_aliasing_false_assert() throws Exception {
    check("pointers/pointers_change_aliasing.c", ExpectedVerdict.FALSE);
  }

  @Test public void fixpoint_true_assert() throws Exception {
    check("fixpoint.c", ExpectedVerdict.TRUE);
  }

  @Test public void valdet_prefixing_true_assert() throws Exception {
    check("valdet_prefixing.c",
        ImmutableMap.of("cpa.lpi.maxExpressionSize", "2",

            // Enabling two options below make non-prefixing variation of
            // val.det. work.
            "cpa.lpi.shortCircuitSyntactic", "false",
            "cpa.lpi.checkPolicyInitialCondition", "false"),
        ExpectedVerdict.TRUE);
  }

  @Test public void array_false_assert() throws Exception {
    check("array.c", ExpectedVerdict.FALSE);
  }

  @Test public void classcast_fail_true_assert() throws Exception {
    check("classcast_fail.c", ExpectedVerdict.TRUE);
  }

  @Test public void formula_fail_true_assert() throws Exception {
    check("formula_fail.c",
        ImmutableMap.of("cpa.lpi.allowedCoefficients", "1",
                        "cpa.lpi.abstractionLocations", "all"),
        ExpectedVerdict.TRUE);
  }

  @Test public void unrolling_true_assert() throws Exception {
    check("unrolling.c",
        ImmutableMap.of("cpa.loopbound.loopIterationsBeforeAbstraction", "2"),
        ExpectedVerdict.TRUE);
  }

  @Test public void timeout_true_assert() throws Exception {
    check("timeout.c",
        ExpectedVerdict.TRUE);
  }

  @Test public void boolean_true_assert() throws Exception {
    // Use explicit value analysis to track boolean variables.
    check("boolean.c",
        ImmutableMap.of("cpa.lpi.maxExpressionSize", "2",
            "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, cpa.functionpointer.FunctionPointerCPA, cpa.loopbound.LoopBoundCPA, cpa.value.ValueAnalysisCPA, cpa.policyiteration.PolicyCPA",
            "cpa.loopbound.trackStack", "true",
            "precision.trackIntAddVariables", "false",
            "precision.trackVariablesBesidesEqAddBool", "false"),
        ExpectedVerdict.TRUE);
  }

  // Testing overflow tracking.
  @Test public void overflow_guards_true_assert() throws Exception {
    checkWithOverflow("overflow/guards.c", ExpectedVerdict.TRUE);
  }

  @Test public void overflow_increment_false_assert() throws Exception {
    checkWithOverflow("overflow/increment.c",
        ExpectedVerdict.FALSE);
  }

  @Test public void overflow_simplest_true_assert() throws Exception {
    checkWithOverflow("overflow/simplest.c",
        ExpectedVerdict.TRUE);
  }

  @Test public void increment_in_guard_false_assert() throws Exception {
    checkWithOverflow("overflow/increment_in_guard.c",
        ExpectedVerdict.FALSE);
  }

  @Test public void many_functions_true_assert() throws Exception {
    checkWithBAM("bam/many_functions-1.c",
        ExpectedVerdict.TRUE);
  }

  @Test public void many_functions_false_assert() throws Exception {
    checkWithBAM("bam/many_functions-2.c",
        ExpectedVerdict.FALSE);
  }

  @Test public void loop_around_summary_true_assert() throws Exception {
    checkWithBAM("bam/loop_around_summary-1.c",
        ExpectedVerdict.TRUE);
  }

  @Test public void loop_around_summary_false_assert() throws Exception {
    checkWithBAM("bam/loop_around_summary-2.c",
        ExpectedVerdict.FALSE);
  }

  private void check(String filename, ExpectedVerdict pExpected) throws Exception {
    check(filename, ImmutableMap.of(), pExpected);
  }

  private void check(String filename, Map<String, String> extra, ExpectedVerdict pExpected) throws Exception {
    check(filename, getProperties("policyIteration.properties", extra), pExpected);
  }

  private void checkWithSlicing(String filename, Map<String, String> extra, ExpectedVerdict pExpected)
      throws Exception {
    check(filename, getProperties("policyIteration-with-slicing.properties", extra), pExpected);
  }

  private void checkWithOverflow(String filename, ExpectedVerdict pExpected) throws Exception {
    check(
        filename,
        getProperties("policyIteration-with-overflow.properties", ImmutableMap.of()),
        pExpected);
  }

  private void checkWithBAM(String filename, ExpectedVerdict pExpected) throws Exception {
    check(
        filename,
        getProperties("policyIteration-with-bam.properties", ImmutableMap.of()),
        pExpected);
  }

  private void check(String filename, Configuration config, ExpectedVerdict pExpected) throws Exception {
    String fullPath = Paths.get(TEST_DIR_PATH, filename).toString();

    TestResults results;
    try {
      results = CPATestRunner.run(config, fullPath);
    } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
      assumeNoException("missing binary dependency for old apron binary", e);
      throw new AssertionError(e);
    }
    if (pExpected == ExpectedVerdict.TRUE) {
      results.assertIsSafe();
    } else if (pExpected == ExpectedVerdict.FALSE) {
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
