// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

@RunWith(Parameterized.class)
public class HorizontalMergeDecompositionTest {

  @Parameters(name = "{0}")
  public static List<Object[]> getParameters() {
    return DecompositionTestBase.getFiles();
  }

  @Parameter public String path;

  private static DssBlockDecomposition createDecomposition(CFA cfa, int mergeLimit)
      throws InvalidConfigurationException, IOException {
    Predicate<CFANode> isBlockEnd = DssTestUtils.createBlockOperator(cfa);

    return new HorizontalMergeDecomposition(
        new LinearBlockNodeDecomposition(isBlockEnd),
        2,
        mergeLimit,
        Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
        true);
  }

  @Test
  public void testHorizontalMergeDecompositionUnlimited() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile(path);

    DssBlockDecomposition decomposition =
        createDecomposition(cfa, HorizontalMergeDecomposition.NO_MERGE_LIMIT);

    BlockGraph graph = decomposition.decompose(cfa);

    DecompositionTestBase.checkBlockGraph(graph, cfa);
  }

  @Test
  public void testHorizontalMergeDecompositionLimited() throws Exception {

    CFA cfa = TestCfaUtils.makeCfaFromFile(path);

    DssBlockDecomposition decomposition = createDecomposition(cfa, 5);

    BlockGraph graph = decomposition.decompose(cfa);

    DecompositionTestBase.checkBlockGraph(graph, cfa);
  }
}
