// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.MergeBlockNodesDecomposition.FunctionCallMerging;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.InliningDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

@RunWith(Parameterized.class)
public class MergeBlockNodesDecompositionTest {

  @Parameters(name = "{0}")
  public static List<Object[]> getParameters() {
    return DecompositionTestBase.getFiles();
  }

  @Parameter public String path;

  private static DssBlockDecomposition createDecomposition(CFA cfa)
      throws InvalidConfigurationException, IOException {
    Predicate<CFANode> isBlockEnd = DssTestUtils.createBlockOperator(cfa);

    return new MergeBlockNodesDecomposition(
        new LinearBlockNodeDecomposition(isBlockEnd),
        2,
        -1,
        Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
        false,
        true);
  }

  @Test
  public void testMergeNodeDecomposition() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile(path);

    DssBlockDecomposition decomposition = createDecomposition(cfa);

    BlockGraph graph = decomposition.decompose(cfa);

    DecompositionTestBase.checkBlockGraph(graph, cfa);
  }

  /**
   * The inlining decomposition shares the nodes of a function between its calls. The analysis of a
   * block stops at its final location, so no merged block may leave its final location, unless it
   * also starts there.
   */
  @Test
  public void testInliningMergeDecompositionStopsOnlyAtTheEnd() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile(path);

    DssBlockDecomposition decomposition =
        new MergeBlockNodesDecomposition(
            new InliningDecomposition(
                new LinearBlockNodeDecomposition(DssTestUtils.createBlockOperator(cfa))),
            2,
            -1,
            Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
            false,
            true);

    for (BlockNode block : decomposition.decompose(cfa).getNodes()) {
      if (block.getInitialLocation().equals(block.getFinalLocation())) {
        continue;
      }
      Truth.assertWithMessage("Block %s leaves its final location", block.getId())
          .that(
              block.getEdges().stream()
                  .filter(e -> e.getPredecessor().equals(block.getFinalLocation())))
          .isEmpty();
    }
  }

  /**
   * With {@link FunctionCallMerging#UNIQUE_RETURNS}, no block returns from the function it starts
   * in towards two call sites, also after merging branch boundaries.
   */
  @Test
  public void testUniqueReturnsAfterInliningAndBranchBoundaryMerging() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile(path);

    DssBlockDecomposition decomposition =
        new BranchBoundaryMergeDecomposition(
            new MergeBlockNodesDecomposition(
                new InliningDecomposition(
                    new LinearBlockNodeDecomposition(DssTestUtils.createBlockOperator(cfa))),
                2,
                -1,
                Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
                false,
                FunctionCallMerging.UNIQUE_RETURNS),
            FunctionCallMerging.UNIQUE_RETURNS);

    for (BlockNode block : decomposition.decompose(cfa).getNodes()) {
      Truth.assertWithMessage("Block %s returns to several call sites", block.getId())
          .that(
              MergeBlockNodesDecomposition.returnsToSeveralCallSites(
                  block.getInitialLocation(), block.getFinalLocation(), ImmutableList.of(block)))
          .isFalse();
    }
  }
}
