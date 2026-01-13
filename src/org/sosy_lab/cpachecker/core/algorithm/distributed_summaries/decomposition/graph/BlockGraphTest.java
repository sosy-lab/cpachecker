// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssDecompositionOptions;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class BlockGraphTest {
  private static final String CONFIGURATION_FILE_GENERATE_CFA = "config/generateCFA.properties";
  private static final String CONFIGURATION_FILE_MERGE_DECOMPOSITION = "config/dss.properties";
  private static final String PROGRAM = "doc/examples/example.c";
  private static final String EXPORT_BLOCKS_JSON_PATH_1 = "block_analysis/blocks1.json";
  private static final String EXPORT_BLOCKS_JSON_PATH_2 = "block_analysis/blocks2.json";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
  /**
   * Tests that {@link BlockGraph}s created from the same {@link CFA} with different starting node
   * IDs can export to the same JSON representation.
   */
  @Test
  public void testCanExportWithNodeIdThatStartsAtNonZero() throws Exception {
    Path tempFolderPath = tempFolder.getRoot().toPath();
    CFA originalCFA = generateCfa(tempFolderPath);
    CFA shiftedCFA = generateCfa(tempFolderPath);

    assumeTrue(originalCFA.nodes() != shiftedCFA.nodes());

    BlockGraph blockGraphFromOriginalCfa = generateBlockGraph(originalCFA, tempFolderPath);
    BlockGraph blockGraphFromShiftedCfa = generateBlockGraph(shiftedCFA, tempFolderPath);

    assumeTrue(blockGraphFromOriginalCfa != blockGraphFromShiftedCfa);

    Path exportPathForOriginalCfa = tempFolderPath.resolve(EXPORT_BLOCKS_JSON_PATH_1);
    Path exportPathForShiftedCfa = tempFolderPath.resolve(EXPORT_BLOCKS_JSON_PATH_2);

    blockGraphFromOriginalCfa.export(exportPathForOriginalCfa, originalCFA);
    blockGraphFromShiftedCfa.export(exportPathForShiftedCfa, shiftedCFA);

    assumeTrue(Files.exists(exportPathForOriginalCfa));
    assumeTrue(Files.exists(exportPathForShiftedCfa));

    assertThat(Files.readString(exportPathForOriginalCfa))
        .isEqualTo(Files.readString(exportPathForShiftedCfa));
  }

  private BlockGraph generateBlockGraph(CFA cfa, Path tempFolderPath) throws Exception {
    Configuration configForMergeDecomposition =
        TestUtil.generateConfig(CONFIGURATION_FILE_MERGE_DECOMPOSITION, tempFolderPath);
    DssDecompositionOptions options = new DssDecompositionOptions(configForMergeDecomposition, cfa);
    DssBlockDecomposition decomposer = options.getConfiguredDecomposition();
    return decomposer.decompose(cfa);
  }

  private CFA generateCfa(Path tempFolderPath) throws Exception {
    Configuration configToGenerateCfa = TestUtil.generateConfig(CONFIGURATION_FILE_GENERATE_CFA, tempFolderPath);
    TestResults result = CPATestRunner.run(configToGenerateCfa, PROGRAM);
    return result.getCheckerResult().getCfa();
  }
}
