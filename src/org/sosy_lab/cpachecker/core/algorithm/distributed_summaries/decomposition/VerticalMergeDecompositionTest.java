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

@RunWith(Parameterized.class)
public class VerticalMergeDecompositionTest {

  @Parameters(name = "{0}")
  public static List<Object[]> getParameters() {
    return DssBlockDecompositionTestUtil.getFiles();
  }

  @Parameter public String path;

  private static DssBlockDecomposition createDecomposition(CFA cfa)
      throws InvalidConfigurationException, IOException {
    BlockOperator blockOperator = new BlockOperator();
    Configuration config =
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    config.inject(blockOperator);
    try {
      blockOperator.setCFA(cfa);
    } catch (CPAException e) {
      // if blockOperator.setCFA throws a CPAexception, this is because of an invalid
      // configuration
      throw new InvalidConfigurationException("Initialization of block operator failed", e);
    }

    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);

    return new VerticalMergeDecomposition(
        new LinearBlockNodeDecomposition(isBlockEnd),
        2,
        Comparator.comparing(BlockNodeWithoutGraphInformation::getId));
  }

  @Test
  public void testVerticalMergeDecomposition() throws Exception {

    CFA cfa = TestUtil.buildTestCFA(path);

    DssBlockDecomposition decomposition = createDecomposition(cfa);

    BlockGraph graph = decomposition.decompose(cfa);

    DssBlockDecompositionTestUtil.checkBlockGraph(graph, cfa);
  }
}
