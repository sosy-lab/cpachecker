// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DecompositionTestBase;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.HorizontalMergeDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.MergeBlockNodesDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

@RunWith(Parameterized.class)
public class InliningDecompositionTest {

  @Parameters(name = "{0}")
  public static List<Object[]> getParameters() {
    return DecompositionTestBase.getFiles();
  }

  @Parameter public String path;

  @Test
  public void test() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(path);

    DssBlockDecomposition decomposition = createDecomposition(cfa);

    BlockGraph graph = decomposition.decompose(cfa);

    // TODO the assumptions for the normal decompositions no longer  hold -> find sensible checks?
    DecompositionTestBase.checkFinalLocationReachedOnlyAtEnd(graph);
  }

  @Test
  public void testMergedInliningDecomposition() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromFile(path);

    BlockGraph graph = createMergedDecomposition(cfa).decompose(cfa);

    DecompositionTestBase.checkFinalLocationReachedOnlyAtEnd(graph);
  }

  private static DssBlockDecomposition createDecomposition(CFA cfa)
      throws InvalidConfigurationException, IOException {
    Predicate<CFANode> isBlockEnd = DssTestUtils.createBlockOperator(cfa);

    return new InliningDecomposition(new LinearBlockNodeDecomposition(isBlockEnd));
  }

  /** The decomposition that {@code decompositionType=INLINING_DECOMPOSITION} creates. */
  private static DssBlockDecomposition createMergedDecomposition(CFA cfa)
      throws InvalidConfigurationException, IOException {
    return new MergeBlockNodesDecomposition(
        createDecomposition(cfa),
        2,
        HorizontalMergeDecomposition.NO_MERGE_LIMIT,
        Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
        false,
        true);
  }
}
