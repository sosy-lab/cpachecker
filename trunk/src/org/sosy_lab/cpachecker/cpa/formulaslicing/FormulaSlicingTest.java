package org.sosy_lab.cpachecker.cpa.formulaslicing;


import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.TruthJUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager.WEAKENING_STRATEGY;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class FormulaSlicingTest {

  @Parameters(name="{0}")
  public static Object[] getWeakeningStrategies() {
    return WEAKENING_STRATEGY.values();
  }

  @Parameter
  public WEAKENING_STRATEGY weakeningStrategy;

  private static final String TEST_DIR_PATH = "test/programs/formulaslicing/";

  @Test public void expand_equality_true_assert() throws Exception {
    TruthJUnit.assume().that(weakeningStrategy).isNotEqualTo
        (WEAKENING_STRATEGY.SYNTACTIC);
    check("expand_equality_true_assert.c", ImmutableMap.of(
        "rcnf.expandEquality", "true"
    ));
  }

  @Test public void expand_equality_false_assert() throws Exception {
    check("expand_equality_false_assert.c", ImmutableMap.of(
        "rcnf.expandEquality", "true"
    ));
  }

  @Test public void simplest_true_assert() throws Exception {
    check("simplest_true_assert.c");
  }

  @Test public void simplest_false_assert() throws Exception {
    check("simplest_false_assert.c");
  }

  @Test public void bad_slice_false_assert() throws Exception {
    check("bad_slice_false_assert.c");
  }

  @Test public void slice_with_branches_true_assert() throws Exception {
    check("slice_with_branches_true_assert.c");
  }

  @Test public void slice_with_branches_false_assert() throws Exception {
    check("slice_with_branches_false_assert.c");
  }

  @Test
  public void slicing_nested_true_assert() throws Exception {
    check("slicing_nested_true_assert.c");
  }

  @Test public void slicing_nested_false_assert() throws Exception {
    check("slicing_nested_false_assert.c");
  }

  @Test public void slicing_nested_fail_false_assert() throws Exception {
    check("slicing_nested_fail_false_assert.c");
  }

  private void check(String filename) throws Exception {
    check(filename, new HashMap<>());
  }

  private void check(String filename, Map<String, String> extra) throws Exception {
    String fullPath;
    if (filename.contains("test/programs/benchmarks")) {
      fullPath = filename;
    } else {
      fullPath = Paths.get(TEST_DIR_PATH, filename).toString();
    }

    TestResults results = CPATestRunner.run(getProperties(extra), fullPath);
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
                .add("cpa.formulaslicing.FormulaSlicingCPA")
                .add("cpa.targetreachability.TargetReachabilityCPA")
                .add("cpa.assumptions.storage.AssumptionStorageCPA")
                .build()
            ))
    )
        .put("solver.z3.requireProofs", "false")
        .put("solver.solver", "z3")
        .put("specification", "config/specification/default.spc")
        .put("parser.usePreprocessor", "true")
        .put("analysis.traversal.order", "dfs")
        .put("analysis.traversal.useCallstack", "true")
        .put("analysis.traversal.useLoopstack", "true")
        .put("analysis.traversal.useReversePostorder", "true")
        .put("cpa.predicate.ignoreIrrelevantVariables", "false")
        .put("cpa.loopstack.loopIterationsBeforeAbstraction", "1")
        .put("cfa.findLiveVariables", "true")
        .put("cpa.slicing.weakeningStrategy", weakeningStrategy.toString())
        .build());
    props.putAll(extra);
    return props;
  }
}
