package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager.WEAKENING_STRATEGY;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

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
    assume().that(weakeningStrategy).isNotEqualTo(WEAKENING_STRATEGY.SYNTACTIC);
    check(
        "expand_equality_true_assert.c",
        ImmutableMap.of(
            "rcnf.expandEquality", "true",
            // Program is unsafe if overflows are considered
            "cpa.predicate.encodeBitvectorAs", "integer"));
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

  @Ignore(
      "With the SYNTACTIC weakening strategy, the analysis is not precise enough and raises a false alarm for this task.")
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
    check(filename, ImmutableMap.of());
  }

  private void check(String filename, Map<String, String> extra) throws Exception {
    String fullPath = Paths.get(TEST_DIR_PATH, filename).toString();

    TestResults results = CPATestRunner.run(getProperties(extra), fullPath);
    if (filename.contains("_true_assert") || filename.contains("_true-unreach")) {
      results.assertIsSafe();
    } else if (filename.contains("_false_assert") || filename.contains("_false-unreach")) {
      results.assertIsUnsafe();
    }
  }

  private Configuration getProperties(Map<String, String> extra)
      throws InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromResource(FormulaSlicingTest.class, "formula-slicing.properties")
        .setOption("cpa.slicing.weakeningStrategy", weakeningStrategy.toString())
        .setOptions(extra)
        .build();
  }
}
