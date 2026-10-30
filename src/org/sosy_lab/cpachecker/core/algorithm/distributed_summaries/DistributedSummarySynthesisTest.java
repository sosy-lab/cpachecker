// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DistributedSummarySynthesisTest {

  private static final String CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH =
      "config/generateBlockGraph.properties";
  private static final String CONFIGURATION_FILE_DSS = "config/dss.properties";
  private static final String SPECIFICATION_REACHABILITY =
      "config/specification/sv-comp-reachability.spc";
  private static final String PROGRAM = "doc/examples/example.c";
  private static final String PROGRAM_UNSAFE_CYCLE =
      "test/programs/simple/block_analysis/inlined_mutex_cycle_unsafe.c";
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

  /**
   * Batching violation updates used to terminate this unsafe loop with TRUE; every update must get
   * its own exploration. The sequential executor and the unmerged decomposition make the message
   * order reproducible.
   */
  @Test
  public void violationUpdatesAreExploredBeforeReplacement() throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile(CONFIGURATION_FILE_DSS)
            .setOption("analysis.machineModel", MachineModel.LINUX64.name())
            .setOption("specification", SPECIFICATION_REACHABILITY)
            .setOption("distributedSummaries.executorType", "SEQUENTIAL")
            .setOption("distributedSummaries.decomposition.mergeBranchBoundaries", "false")
            .setOption("distributedSummaries.decomposition.largestHorizontalMerge", "1")
            // fail instead of hanging if the analysis stops converging
            .setOption("limits.time.wall", "60s")
            .build();

    IntegrationTestRunner.run(config, PROGRAM_UNSAFE_CYCLE).assertIsUnsafe();
  }
}
