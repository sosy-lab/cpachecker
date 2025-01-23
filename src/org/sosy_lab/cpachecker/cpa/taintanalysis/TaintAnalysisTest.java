package org.sosy_lab.cpachecker.cpa.taintanalysis;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;
import java.util.logging.Level;
import java.nio.file.Path;

public class TaintAnalysisTest {

  private TestResults parseProgram(String pProgramName) throws Exception {
    final Configuration config =
        TestDataTools.configurationForTest()
            .setOption("--config", "config/predicateAnalysis--taintAnalysis.properties")
            .build();

    String testDir = "test/programs/taint_analysis/";
    Path programPath = Path.of(testDir, pProgramName);

    return CPATestRunner.run(config, programPath.toString(), Level.FINEST);
  }

  @Test
  public void testExampleNotTaintedSafe() throws Exception {
    TestResults results = parseProgram("exampleNotTaintedSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleNotTaintedUnsafe() throws Exception {
    TestResults results = parseProgram("exampleNotTaintedUnsafe.c");
    results.assertIsUnsafe();
  }

  @Test
  public void testExampleTaintedSafe() throws Exception {
    TestResults results = parseProgram("exampleTaintedSafe.c");
    results.assertIsSafe();
  }

  @Test
  public void testExampleTaintedUnsafe() throws Exception {
    TestResults results = parseProgram("exampleTaintedUnsafe.c");
    results.assertIsUnsafe();
  }
}
