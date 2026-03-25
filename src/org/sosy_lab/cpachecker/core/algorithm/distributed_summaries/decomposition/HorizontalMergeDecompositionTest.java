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
import java.util.function.Predicate;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class HorizontalMergeDecompositionTest {

  private static DssBlockDecomposition createDecomposition(CFA cfa)
      throws InvalidConfigurationException, IOException {
    BlockOperator blockOperator = new BlockOperator();
    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromFile(TestUtil.DSS_CONFIGURATION_FILE)
            .build();
    config.inject(blockOperator);
    try {
      blockOperator.setCFA(cfa);
    } catch (CPAException e) {
      // if blockOperator.setCFA throws a CPAexception, this is because of an invalid
      // configuration
      throw new InvalidConfigurationException("Initialization of block operator failed", e);
    }

    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);

    return new HorizontalMergeDecomposition(
        new LinearBlockNodeDecomposition(isBlockEnd),
        2,
        Comparator.comparing(BlockNodeWithoutGraphInformation::getId));
  }

  @Test
  public void testSimple() throws Exception {

    CFA cfa =
        TestUtil.buildTestCFA(
            DssBlockDecompositionTestUtil.PROGRAMM_PATH_SIMPLE);

    DssBlockDecomposition decomposition = createDecomposition(cfa);

    BlockGraph graph = decomposition.decompose(cfa);

    DssBlockDecompositionTestUtil.checkBlockGraph(graph, cfa);
  }

  @Test
  public void testLarge() throws Exception {

    CFA cfa =
        TestUtil.buildTestCFA(
            DssBlockDecompositionTestUtil.PROGRAMM_PATH_LARGE);

    DssBlockDecomposition decomposition = createDecomposition(cfa);

    BlockGraph graph = decomposition.decompose(cfa);

    DssBlockDecompositionTestUtil.checkBlockGraph(graph, cfa);
  }
}
