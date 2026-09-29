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
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
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

  @Test
  public void stalledViolationPropagationMustNotProduceProof() throws Exception {
    var config =
        TestUtils.configurationForTestWithOutput(tempFolder)
            .loadFromFile("config/distributed-summary-synthesis/dss-coverage-fallback.properties")
            .setOption("specification", "config/specification/sv-comp-reachability.spc")
            .setOption("analysis.machineModel", "Linux64")
            .setOption("distributedSummaries.executorType", "SEQUENTIAL")
            .setOption("output.disable", "true")
            .build();
    IntegrationTestRunner.run(config, "test/programs/dss/partial-replace-stalled-loop.c")
        .assertIs(Result.UNKNOWN);
  }

  @Test
  public void entryInvariantsDoNotTurnUncertainComparisonIntoTrue() throws Exception {
    Path program = tempFolder.getRoot().toPath().resolve("comparison.c");
    Files.writeString(
        program,
        "extern unsigned long __VERIFIER_nondet_ulong(void); "
            + "extern void __VERIFIER_error(void); int main(void) { "
            + "unsigned long long raw = __VERIFIER_nondet_ulong(); "
            + "unsigned char special = ((raw | (0x7ULL << 52)) == -1); "
            + "unsigned char valid = (raw == 0); "
            + "if (!(special || valid)) __VERIFIER_error(); return 0; }");
    var config =
        TestUtils.configurationForTestWithOutput(tempFolder)
            .loadFromFile("config/distributed-summary-synthesis/dss-coverage-fallback.properties")
            .setOption("specification", "config/specification/sv-comp-reachability.spc")
            .setOption("analysis.machineModel", "Linux64")
            .setOption("distributedSummaries.decomposition.largestHorizontalMerge", "1")
            .setOption(
                "distributedSummaries.entryInvariantConfiguration",
                "config/distributed-summary-synthesis/dss-entry-invariants.properties")
            .setOption("distributedSummaries.entryInvariantTimeLimit", "10s")
            .setOption("output.disable", "true")
            .build();
    IntegrationTestRunner.run(config, program.toString()).assertIsUnsafe();
  }

  @Test
  @SuppressWarnings("deprecation") // Inspect inherited options without constructing an analysis.
  public void portfolioStagesPreserveCallerLimitsAndSpecification() throws Exception {
    var caller =
        Configuration.builder()
            .loadFromFile("config/dss.properties")
            .setOption("limits.time.cpu", "7800s")
            .setOption("specification", "caller.spc")
            .build();
    for (String stage : new String[] {"boolean", "cartesian", "fallback"}) {
      var config =
          Configuration.builder()
              .copyFrom(caller)
              .loadFromFile(
                  "config/distributed-summary-synthesis/dss-coverage-" + stage + ".properties")
              .build();
      assertThat(config.getProperty("limits.time.cpu")).isEqualTo("7800s");
      assertThat(config.getProperty("specification")).isEqualTo("caller.spc");
    }
  }

  @Test
  public void restartedAnalysisUsesItsOwnDecomposition() throws Exception {
    Path dir = tempFolder.getRoot().toPath();
    Path program = dir.resolve("branches.c");
    Files.writeString(
        program,
        "extern int choose(void); int main(void) { int x = choose(); "
            + "if (x) x++; else x--; if (x) x++; else x--; return x; }");
    String common =
        "#include "
            + Path.of("config/distributed-summary-synthesis/dss-analysis.properties")
                .toAbsolutePath()
            + "\n";
    Path coarse = dir.resolve("coarse.properties");
    Path fine = dir.resolve("fine.properties");
    Files.writeString(coarse, common);
    Files.writeString(
        fine,
        common
            + "distributedSummaries.decomposition.mergeBranchBoundaries=false\n"
            + "distributedSummaries.decomposition.largestHorizontalMerge=1\n");
    var config =
        TestUtils.configurationForTestWithOutput(tempFolder)
            .setOption(
                "specification",
                Path.of("config/specification/default.spc").toAbsolutePath().toString())
            .setOption("analysis.restartAfterUnknown", "true")
            .setOption("analysis.useLoopStructure", "true")
            .setOption("restartAlgorithm.alwaysRestart", "true")
            .setOption("restartAlgorithm.configFiles", coarse + "," + fine)
            .setOption("output.disable", "true")
            .build();
    var result = IntegrationTestRunner.run(config, program.toString());
    result.assertIsSafe();
    var sizes =
        Pattern.compile("Decomposed CFA in (\\d+) blocks")
            .matcher(result.log())
            .results()
            .map(m -> Integer.parseInt(m.group(1)))
            .toList();
    assertThat(sizes).hasSize(2);
    assertThat(sizes.get(1)).isGreaterThan(sizes.get(0));
  }
}
