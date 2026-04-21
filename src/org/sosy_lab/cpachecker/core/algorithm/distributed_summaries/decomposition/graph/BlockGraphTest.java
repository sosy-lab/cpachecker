// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssDecompositionOptions;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class BlockGraphTest {
  private static final String CONFIGURATION_FILE_MERGE_DECOMPOSITION = "config/dss.properties";
  private static final String PROGRAM = "doc/examples/example.c";

  /**
   * Tests that {@link BlockGraph}s created from the same {@link CFA} with different starting node
   * IDs can export to the same JSON representation.
   */
  @Test
  public void testCanExportWithNodeIdThatStartsAtNonZero() throws Exception {
    String programText = Files.readString(Path.of(PROGRAM), StandardCharsets.UTF_8);
    CFA originalCFA = TestDataTools.makeCFA(programText);
    CFA shiftedCFA = TestDataTools.makeCFA(programText);

    // If the CFAs have the same nodes, then they were not shifted and this test is not valid
    assertThat(originalCFA.nodes()).isNotEmpty();
    assertThat(originalCFA.nodes()).containsNoneIn(shiftedCFA.nodes());

    BlockGraph blockGraphFromOriginalCfa = generateBlockGraph(originalCFA);
    BlockGraph blockGraphFromShiftedCfa = generateBlockGraph(shiftedCFA);

    var exportedBlockGraphFromOriginalCfa = blockGraphFromOriginalCfa.getExportData(originalCFA);
    var exportedBlockGraphFromShiftedCfa = blockGraphFromShiftedCfa.getExportData(shiftedCFA);

    assertThat(exportedBlockGraphFromOriginalCfa).isEqualTo(exportedBlockGraphFromShiftedCfa);
  }

  private BlockGraph generateBlockGraph(CFA cfa) throws Exception {
    Configuration configForMergeDecomposition =
        TestDataTools.configurationForTest()
            .loadFromFile(CONFIGURATION_FILE_MERGE_DECOMPOSITION)
            .build();
    DssDecompositionOptions options = new DssDecompositionOptions(configForMergeDecomposition, cfa);
    DssBlockDecomposition decomposer = options.getConfiguredDecomposition();
    return decomposer.decompose(cfa);
  }
}
