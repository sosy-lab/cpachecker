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
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
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
  public void singleRunConfigurationsPreserveCallerLimitsAndSpecification() throws Exception {
    for (String name : new String[] {"dss", "dss-plain"}) {
      Configuration config =
          Configuration.builder()
              .loadFromFile("config/" + name + ".properties")
              .setOption("limits.time.cpu", "7800s")
              .setOption("specification", "caller.spc")
              .build();
      Properties options = new Properties();
      options.load(new StringReader(config.asPropertiesString()));
      assertThat(options.getProperty("limits.time.cpu")).isEqualTo("7800s");
      assertThat(options.getProperty("specification")).isEqualTo("caller.spc");
      assertThat(options.getProperty("analysis.algorithm.distributedSummarySynthesis"))
          .isEqualTo("true");
      assertThat(options.getProperty("analysis.restartAfterUnknown", "false")).isEqualTo("false");
      assertThat(options.getProperty("restartAlgorithm.configFiles")).isNull();
      assertThat(options.getProperty("limits.time.wall")).isNull();
      boolean optimized = name.equals("dss");
      DssAnalysisOptions analysisOptions = new DssAnalysisOptions(config);
      assertThat(analysisOptions.sharePrecision()).isEqualTo(optimized);
      assertThat(analysisOptions.combineStates()).isEqualTo(optimized);
      assertThat(analysisOptions.cacheViolationConditions()).isEqualTo(optimized);
      assertThat(analysisOptions.compressMessages()).isEqualTo(optimized);
      assertThat(analysisOptions.abstractAtBlockEntry()).isEqualTo(optimized);
      assertThat(analysisOptions.combineViolationConditionsByHash()).isEqualTo(optimized);
      Properties worker = new Properties();
      worker.load(
          new StringReader(
              Configuration.builder()
                  .loadFromFile(analysisOptions.getForwardConfiguration())
                  .build()
                  .asPropertiesString()));
      assertThat(worker.getProperty("CompositeCPA.cpas"))
          .contains(optimized ? ".DssLocationCPA," : "cpa.location.LocationCPA,");
      for (String option :
          new String[] {
            "dss.graphViolationConditions",
            "dss.cpa.predicate.projectViolationConditions",
            "dss.cpa.predicate.projectNestedDisjunctions",
            "dss.cpa.predicate.generalizeViolationConditions",
            "dss.cpa.predicate.generalizeOverPreconditionPredicates",
          }) {
        assertWithMessage("%s in %s", option, name)
            .that(worker.getProperty(option))
            .isEqualTo(Boolean.toString(optimized));
      }
    }
  }

  @Test
  public void violationUpdatesAreExploredBeforeReplacement() throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile("config/dss.properties")
            .setOption("analysis.machineModel", "Linux64")
            .setOption("specification", "config/specification/sv-comp-reachability.spc")
            .setOption("distributedSummaries.executorType", "SEQUENTIAL")
            .setOption("distributedSummaries.decomposition.mergeBranchBoundaries", "false")
            .setOption("distributedSummaries.decomposition.largestHorizontalMerge", "1")
            .setOption("limits.time.wall", "60s")
            .build();
    // The sequential executor gives a reproducible message order. Batching violation updates
    // used to terminate this unsafe loop with TRUE; every update must get its own exploration.
    IntegrationTestRunner.run(
            config, "test/programs/simple/block_analysis/inlined_mutex_cycle_unsafe.c")
        .assertIsUnsafe();
  }

  @Test
  public void restartedAnalysisUsesItsOwnDecomposition() throws Exception {
    Path dir = tempFolder.getRoot().toPath();
    Path program = dir.resolve("branches.c");
    Files.writeString(
        program,
        "extern int choose(void); int main(void) { int x = choose(); "
            + "if (x) x++; else x--; if (x) x++; else x--; return x; }");
    String common = "#include " + Path.of("config/dss.properties").toAbsolutePath() + "\n";
    Path coarse = dir.resolve("coarse.properties");
    Path fine = dir.resolve("fine.properties");
    Files.writeString(coarse, common);
    Files.writeString(
        fine,
        common
            + "distributedSummaries.decomposition.mergeBranchBoundaries=false\n"
            + "distributedSummaries.decomposition.largestHorizontalMerge=1\n");
    Configuration config =
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
    IntegrationTestResult result = IntegrationTestRunner.run(config, program.toString());
    result.assertIsSafe();
    List<Integer> sizes =
        Pattern.compile("Decomposed CFA in (\\d+) blocks")
            .matcher(result.log())
            .results()
            .map(m -> Integer.parseInt(m.group(1)))
            .toList();
    assertThat(sizes).hasSize(2);
    assertThat(sizes.get(1)).isGreaterThan(sizes.getFirst());
  }
}
