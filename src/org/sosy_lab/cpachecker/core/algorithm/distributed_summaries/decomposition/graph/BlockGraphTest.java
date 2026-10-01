// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssDecompositionOptions;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class BlockGraphTest {
  private static final String CONFIGURATION_FILE_MERGE_DECOMPOSITION =
      "config/distributed-summary-synthesis/dss-base.properties";
  private static final String PROGRAM = "doc/examples/example.c";

  /**
   * Tests that {@link BlockGraph}s created from the same {@link CFA} with different starting node
   * IDs can export to the same JSON representation.
   */
  @Test
  public void testCanExportWithNodeIdThatStartsAtNonZero() throws Exception {
    CFA originalCFA = TestCfaUtils.makeCfaFromFile(PROGRAM);
    CFA shiftedCFA = TestCfaUtils.makeCfaFromFile(PROGRAM);

    // If the CFAs have the same nodes, then they were not shifted and this test is not valid
    assertThat(originalCFA.nodes()).isNotEmpty();
    assertThat(originalCFA.nodes()).containsNoneIn(shiftedCFA.nodes());

    BlockGraph blockGraphFromOriginalCfa = generateBlockGraph(originalCFA);
    BlockGraph blockGraphFromShiftedCfa = generateBlockGraph(shiftedCFA);

    Map<String, Map<String, Object>> exportedBlockGraphFromOriginalCfa =
        blockGraphFromOriginalCfa.getExportData(originalCFA);
    Map<String, Map<String, Object>> exportedBlockGraphFromShiftedCfa =
        blockGraphFromShiftedCfa.getExportData(shiftedCFA);

    assertThat(exportedBlockGraphFromOriginalCfa).isEqualTo(exportedBlockGraphFromShiftedCfa);
  }

  @Test
  public void loopBlocksLoseOnlyTheirSelfEdge() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile("test/programs/simple/block_analysis/for.c");
    BlockGraph graph = generateBlockGraph(cfa, "config/dss.properties");
    // without the option, no block iterates itself and everything else stays as it was
    assertThat(graph.getNodes().stream().noneMatch(BlockNode::iteratesItself)).isTrue();
    BlockGraph adjusted = graph.withLoopBlocksIteratedInternally();

    assertThat(adjusted.getRoot().getId()).isEqualTo(graph.getRoot().getId());
    int loops = 0;
    for (BlockNode node : adjusted.getNodes()) {
      BlockNode original =
          graph.getNodes().stream().filter(n -> n.getId().equals(node.getId())).findFirst().get();
      if (original.getSuccessorIds().contains(original.getId())
          && original.getPredecessorIds().size() > 1) {
        loops++;
        assertThat(node.iteratesItself()).isTrue();
        assertThat(node.getSuccessorIds()).doesNotContain(node.getId());
        assertThat(node.getPredecessorIds()).doesNotContain(node.getId());
        assertThat(node.getPredecessorIds()).isNotEmpty();
      } else {
        assertThat(node.iteratesItself()).isFalse();
        assertThat(node.getSuccessorIds()).isEqualTo(original.getSuccessorIds());
        assertThat(node.getPredecessorIds()).isEqualTo(original.getPredecessorIds());
      }
    }
    assertThat(loops).isGreaterThan(0);
  }

  private BlockGraph generateBlockGraph(CFA cfa) throws Exception {
    return generateBlockGraph(cfa, CONFIGURATION_FILE_MERGE_DECOMPOSITION);
  }

  private BlockGraph generateBlockGraph(CFA cfa, String pConfigurationFile) throws Exception {
    Configuration configForMergeDecomposition =
        TestUtils.configurationForTest().loadFromFile(pConfigurationFile).build();
    DssDecompositionOptions options = new DssDecompositionOptions(configForMergeDecomposition, cfa);
    DssBlockDecomposition decomposer = options.getConfiguredDecomposition();
    return decomposer.decompose(cfa);
  }
}
