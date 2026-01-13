// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class ImportDecompositionTest {

  private static final String CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH =
      "config/generateBlockGraph.properties";
  private static final String CONFIGURATION_FILE_GENERATE_CFA = "config/generateCFA.properties";
  private static final String PROGRAM = "doc/examples/example.c";
  private static final String BLOCKS_JSON_PATH = "block_analysis/blocks.json";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  /**
   * Tests that {@link ImportDecomposition} can decompose a {@link CFA} to a {@link BlockGraph} when
   * the {@link CFA} starts with a non-zero node ID.
   */
  @Test
  public void testCanDecomposeCfaWithNodeIdThatStartsAtNonZero() throws Exception {
    Path tempFolderPath = tempFolder.getRoot().toPath();
    Configuration configToGenerateBlockGraph =
        TestUtil.generateConfig(CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH, tempFolderPath);
    TestResults runWithBlockGraph = CPATestRunner.run(configToGenerateBlockGraph, PROGRAM);
    CFA originalCFA = runWithBlockGraph.getCheckerResult().getCfa();

    // runWithBlockGraph should have generated the blocks json
    Path expectedBlocksJson = tempFolderPath.resolve(BLOCKS_JSON_PATH);
    assumeTrue(expectedBlocksJson.toFile().exists());

    Configuration configToGenerateCfa = TestUtil.generateConfig(CONFIGURATION_FILE_GENERATE_CFA, tempFolderPath);
    TestResults runWithShiftedCfa = CPATestRunner.run(configToGenerateCfa, PROGRAM);
    CFA shiftedCFA = runWithShiftedCfa.getCheckerResult().getCfa();

    assumeTrue(originalCFA.nodes() != shiftedCFA.nodes());

    ImportDecomposition decomposition = new ImportDecomposition(expectedBlocksJson);
    BlockGraph blockGraphWithOriginalCFA = decomposition.decompose(originalCFA);
    BlockGraph blockGraphWithShiftedCFA = decomposition.decompose(shiftedCFA);

    assertThat(blockGraphWithShiftedCFA).isEqualTo(blockGraphWithOriginalCFA);
  }
}
