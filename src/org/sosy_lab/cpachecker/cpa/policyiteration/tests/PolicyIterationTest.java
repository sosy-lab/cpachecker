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

  @Test public void pointer_past_abstraction_true_assert() throws Exception {
    check("pointers/pointer_past_abstraction_true_assert.c", ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,
            "cpa.stator.policy.generateOctagons", "true",
            "cpa.stator.policy.joinOnMerge", "false",
            "cpa.slicing.useCounterexampleBasedSlicing", "true"
        )
    );
  }

  @Test public void pointer_past_abstraction_false_assert() throws Exception {
    check("pointers/pointer_past_abstraction_false_assert.c"
        , ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,
            "cpa.stator.policy.runCongruence", "false",
            "cpa.stator.policy.joinOnMerge", "false",
            "cpa.slicing.useCounterexampleBasedSlicing", "true"
        )
    );
  }

  @Test public void pointers_loop_true_assert() throws Exception {
    check("pointers/pointers_loop_true_assert.c",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,
            "cpa.stator.policy.generateOctagons", "true",
            "cpa.stator.policy.joinOnMerge", "false",
            "cpa.slicing.useSyntacticFormulaSlicing", "true"
        ));
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

  @Test
  @Ignore
  public void debug3() throws Exception {

    check("test/programs/benchmarks/ldv-commit-tester/m0_false-unreach-call_drivers-block-virtio_blk-ko--101_1a--39a1d13.c",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,

//            "cpa.stator.policy.generateOctagons", "false",
            "cpa.slicing.useSyntacticFormulaSlicing", "true",
//            "cpa.slicing.applyANDtransformation", "false",
//            "cpa.slicing.applyORtransformation", "false",


            // todo: does LPI ever have a chance to run value determination
            // in this mode? This is more problematic than we have thought.
            "cpa.slicing.breakSCC", "false",
            "cpa.slicing.joinOnMerge", "true",
            "cpa.stator.policy.runCongruence", "false"
        ));
  }

  @Test
  @Ignore
  public void debug4() throws Exception {
    // note: gives a recursion error without slicing.
    // HM, what could that be.
    // converges to the right answer in 4 minutes.
    // hm, it's kind of late.
    check("test/programs/benchmarks/ldv-linux-3.0/module_get_put-drivers-block-loop.ko_false-unreach-call.cil.out.i.pp.i",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,
            "cpa.slicing.useSyntacticFormulaSlicing", "true"
        ));
  }

  @Test
  @Ignore
  public void debug5() throws Exception {
    // ok finally, we have an error.
    // with syntactic formula slicing: incorrect "true" verdict.
    // without.

    // Works fine with non-syntactic formula slicing (???)
    // (shouldn't that be at least as strong? makes no sense to me).
    check("test/programs/benchmarks/ldv-linux-3.12-rc1/linux-3.12-rc1.tar.xz-144_2a-drivers--input--misc--ims-pcu.ko-entry_point_false-unreach-call.cil.out.c",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,
            "cpa.slicing.useSyntacticFormulaSlicing", "true",
            "cpa.stator.policy.runCongruence", "false"));
  }

  @Test
  @Ignore
  public void debug6() throws Exception {

    // wtf, this is very different to the integration testing results.
    check(
        "test/programs/benchmarks/ldv-linux-3.12-rc1/linux-3.12-rc1.tar.xz-144_2a-drivers--isdn--gigaset--usb_gigaset.ko-entry_point_false-unreach-call.cil.out.c",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING
        )
        );
  }

  @Test
  @Ignore
  public void debug7() throws Exception {
    // wow, apparently a lot of time can be actually spent in AND-LBE
    // transformation.
    // hm this function dies with recursion error now.
    check(
        "test/programs/benchmarks/ldv-validator-v0.6/linux-stable-d47b389-1-32_7a-drivers--media--video--cx88--cx88-blackbird.ko-entry_point_false-unreach-call.cil.out.c",
    ImmutableMap.of(
        "CompositeCPA.cpas", CPAS_W_SLICING
    ));
  }

  @Test
  @Ignore
  public void debug_timeout() throws Exception {
    check("test/programs/benchmarks/loops/eureka_05_true-unreach-call.i",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING
//            "cpa.stator.policy.wideningThreshold", "0"
        ));
  }

  @Test
  @Ignore
  public void debug_timeout2() throws Exception {
    // now this one seems to be giving trouble...
    // ok so i go on an infinite loop where i really shouldn't.
    check("test/programs/benchmarks/loops/veris.c_OpenSER__cases1_stripFullBoth_arr_true-unreach-call.i",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,
            "cpa.stator.policy.wideningThreshold", "0",
            "cpa.stator.policy.runCongruence", "false",
            "cpa.stator.policy.joinOnMerge", "false"
        ));
  }

  @Test
  @Ignore // this is Z3 regression.
  public void debug_fresh_timeout() throws Exception {
    check("test/programs/benchmarks/loops/bubble_sort_true-unreach-call.i");
  }

  @Test
  @Ignore
  public void syntactic_fail() throws Exception {
    check("test/programs/benchmarks/loops/ludcmp_false-unreach-call.i",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,
            "cpa.stator.policy.wideningThreshold", "0",
//            "cpa.stator.policy.runCongruence", "false",
            "cpa.stator.policy.joinOnMerge", "false",
            "cpa.slicing.runSyntacticSlicing", "true",
            "cpa.slicing.runDestructiveSlicing", "false"
//            "cpa.slicing.useCounterexampleBasedWeakening", "true"
        ));
  }

  @Test
  @Ignore
  public void debugSyntacticFail() throws Exception {
    // todo: figure out why the syntactic approach is broken.
    check("test/programs/benchmarks/ldv-linux-3.0/usb_urb-drivers-misc-c2port-core.ko_true-unreach-call.cil.out.i.pp.i",
        ImmutableMap.of(
        "CompositeCPA.cpas", CPAS_W_SLICING,
        "cpa.stator.policy.wideningThreshold", "0",
//        "cpa.stator.policy.runCongruence", "false",
//            "cpa.slicing.joinOnMerge", "false",
            "cpa.slicing.runDestructiveSlicing", "false",
        "cpa.stator.policy.joinOnMerge", "false",
            "cpa.slicing.runSyntacticSlicing", "true"

        ));
  }

  @Test
  public void debugSyntacticFail2() throws Exception {
    check("test/programs/benchmarks/ldv-linux-3.4-simple/32_1_cilled_true-unreach-call_ok_nondet_linux-3.4-32_1-drivers--input--joystick--spaceorb.ko-ldv_main0_sequence_infinite_withcheck_stateful.cil.out.c",
        ImmutableMap.of(
            "CompositeCPA.cpas", CPAS_W_SLICING,
            "cpa.stator.policy.wideningThreshold", "0",
            "cpa.slicing.runSyntacticSlicing", "true"
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

        .put("solver.solver", "z3")
//        .put("solver.solver", "mathsat5")
//        .put("solver.mathsat5.loadOptimathsat5", "true")
        .put("specification", "config/specification/default.spc")
        .put("cpa.predicate.ignoreIrrelevantVariables", "true")
        .put("cpa.predicate.maxArrayLength", "1000")
        .put("cpa.predicate.defaultArrayLength", "3")
        .put("parser.usePreprocessor", "true")
        .put("cfa.findLiveVariables", "true")
        .put("analysis.traversal.order", "bfs")
        .put("analysis.traversal.useCallstack", "true")

        // todo: does it affect the speed?
//        .put("analysis.traversal.useReverseLoopstack", "true")
        .put("analysis.traversal.useReversePostorder", "true")

        .put("log.consoleLevel", "INFO")
    .build());
    props.putAll(extra);
    return props;
  }

  private static final String CPAS_W_SLICING = Joiner.on(", ").join(ImmutableList.<String>builder()
          .add("cpa.location.LocationCPA")
          .add("cpa.callstack.CallstackCPA")
          .add("cpa.functionpointer.FunctionPointerCPA")
          .add("cpa.loopstack.LoopstackCPA")
          .add("cpa.formulaslicing.FormulaSlicingCPA")
          .add("cpa.policyiteration.PolicyCPA")
          .build()
  );

}
