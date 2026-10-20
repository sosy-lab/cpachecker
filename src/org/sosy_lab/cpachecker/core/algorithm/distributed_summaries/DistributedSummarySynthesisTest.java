// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DistributedSummarySynthesisTest {

  private static final String CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH =
      "config/generateBlockGraph.properties";
  private static final String PROGRAM = "doc/examples/example.c";
  private static final String BLOCKS_JSON_PATH = "output/block_analysis/blocks.json";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  // discard printed statistics; we only care about generation
  @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
  private final PrintStream statisticsStream =
      new PrintStream(ByteStreams.nullOutputStream(), true, Charset.defaultCharset());

  @Test
  public void testBlockDecompositionExportsJson() throws Exception {
    Path tempFolderPath = tempFolder.getRoot().toPath();
    Configuration config =
        TestUtils.configurationForTestWithOutput(tempFolder)
            .loadFromFile(CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH)
            .setOption("language", Language.C.name())
            .build();
    File expectedBlocksJson = tempFolderPath.resolve(BLOCKS_JSON_PATH).toFile();

    IntegrationTestResult result = IntegrationTestRunner.run(config, PROGRAM);
    result.cpaCheckerResult().printStatistics(statisticsStream);
    result.cpaCheckerResult().writeOutputFiles();

    result.assertIs(Result.DONE);
    assertWithMessage(
            "Expected block graph JSON at path '%s', but does not exist", BLOCKS_JSON_PATH)
        .that(expectedBlocksJson.exists())
        .isTrue();
    assertWithMessage("Block graph JSON '%s' is empty file", BLOCKS_JSON_PATH)
        .that(Files.readString(expectedBlocksJson.toPath(), StandardCharsets.UTF_8))
        .isNotEmpty();
  }

  private void assertBothBlockDomains(String pBody, boolean pSafe) throws Exception {
    Path source = Files.createTempFile(tempFolder.getRoot().toPath(), "merging-", ".c");
    Files.writeString(
        source,
        "extern int __VERIFIER_nondet_int(void);\n"
            + "extern void __VERIFIER_error(void);\n"
            + pBody,
        StandardCharsets.UTF_8);
    for (String domain : new String[] {"IDENTITY", "VALUE"}) {
      Path forward = Files.createTempFile(tempFolder.getRoot().toPath(), "forward-", ".properties");
      Files.writeString(
          forward,
          "#include "
              + Path.of("config/distributed-summary-synthesis/dss-block-analysis.properties")
                  .toAbsolutePath()
              + "\ncpa.block.domain="
              + domain
              + "\nsolver.solver=SMTINTERPOL\ncpa.predicate.encodeBitvectorAs=INTEGER\n"
              + "cpa.predicate.encodeFloatAs=RATIONAL\n",
          StandardCharsets.UTF_8);
      Configuration config =
          TestUtils.configurationForTest()
              .loadFromFile("config/dss.properties")
              .setOption("specification", "config/specification/sv-comp-reachability.spc")
              .setOption("distributedSummaries.executorType", "DSS")
              .setOption("distributedSummaries.worker.forwardConfiguration", forward.toString())
              .setOption("solver.solver", "SMTINTERPOL")
              .setOption("cpa.predicate.encodeBitvectorAs", "INTEGER")
              .setOption("cpa.predicate.encodeFloatAs", "RATIONAL")
              .build();
      IntegrationTestResult result = IntegrationTestRunner.run(config, source.toString());
      if (pSafe) {
        result.assertIsSafe();
      } else {
        result.assertIsUnsafe();
        assertThat(result.log()).doesNotContain("Violation path for witness was not correct");
        assertThat(
                result.cpaCheckerResult().getReached().stream()
                    .filter(ARGState.class::isInstance)
                    .map(ARGState.class::cast)
                    .filter(ARGState::isTarget)
                    .anyMatch(s -> s.getCounterexampleInformation().isPresent()))
            .isTrue();
      }
    }
  }

  @Test
  public void testMergingPreservesSafeAndUnsafeDiamondVerdicts() throws Exception {
    assertBothBlockDomains(
        "int main() { int x = __VERIFIER_nondet_int() ? 1 : 2; "
            + "if (x < 1 || x > 2) __VERIFIER_error(); }",
        true);
    assertBothBlockDomains(
        "int main() { int x = __VERIFIER_nondet_int() ? 1 : 2; "
            + "if (x == 2) __VERIFIER_error(); }",
        false);
  }

  @Test
  public void testMergingPreservesCallsiteAndLoopBehaviour() throws Exception {
    assertBothBlockDomains(
        "int f(int x) { return x + 1; } int main() { "
            + "if (f(1) != 2 || f(2) != 3) __VERIFIER_error(); }",
        true);
    assertBothBlockDomains(
        "int main() { int x = 0; while (x < 3) x++; " + "if (x == 3) __VERIFIER_error(); }", false);
  }

  @Test
  public void testStaticRefinementHandlesSharedRootChildren() throws Exception {
    assertBothBlockDomains(
        Files.readString(
            Path.of("test/programs/simple/block_analysis/product-lines_simple-10.c"),
            StandardCharsets.UTF_8),
        true);
  }
}
