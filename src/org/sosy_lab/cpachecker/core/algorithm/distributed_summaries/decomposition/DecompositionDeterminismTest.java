// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.truth.Truth;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.InliningDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/** Decomposing the same CFA twice must assign exactly the same block ids. */
@RunWith(Parameterized.class)
public class DecompositionDeterminismTest {

  @Parameters(name = "{0}")
  public static List<Object[]> getParameters() {
    return DecompositionTestBase.getFiles();
  }

  @Parameter public String path;

  @Test
  public void testMergeDecompositionAssignsSameIds() throws Exception {
    assertSameIdsWhenDecomposedTwice(false);
  }

  @Test
  public void testInliningDecompositionAssignsSameIds() throws Exception {
    assertSameIdsWhenDecomposedTwice(true);
  }

  private void assertSameIdsWhenDecomposedTwice(boolean pInline) throws Exception {
    CFA firstCfa = TestUtil.buildTestCFA(path);
    List<String> first = fingerprint(createDecomposition(firstCfa, pInline).decompose(firstCfa));

    Objects.hash(new Object(), new Object(), new Object());

    CFA secondCfa = TestUtil.buildTestCFA(path);
    List<String> second = fingerprint(createDecomposition(secondCfa, pInline).decompose(secondCfa));

    Truth.assertWithMessage(
            "Decomposing the same program twice must assign the same id to the same block")
        .that(second)
        .containsExactlyElementsIn(first)
        .inOrder();
  }

  /**
   * Maps every block to its id, keyed and ordered by the block content instead of by the id, so
   * that two decompositions of the same program differ iff they assign different ids.
   *
   * <p>Node numbers are shifted by the smallest one, because they are handed out by a global
   * counter and therefore differ between two CFAs built from the same program.
   */
  private static List<String> fingerprint(BlockGraph pGraph) {
    int minNodeNumber =
        pGraph.getNodes().stream()
            .flatMap(b -> b.getNodes().stream())
            .mapToInt(CFANode::getNodeNumber)
            .min()
            .orElseThrow();

    List<String> lines = new ArrayList<>();
    for (BlockNode block : pGraph.getNodes()) {
      List<Integer> nodeNumbers = new ArrayList<>();
      block.getNodes().forEach(n -> nodeNumbers.add(n.getNodeNumber() - minNodeNumber));
      nodeNumbers.sort(Comparator.naturalOrder());
      lines.add(nodeNumbers + " -> " + block.getId());
    }
    lines.sort(Comparator.naturalOrder());
    return lines;
  }

  private static DssBlockDecomposition createDecomposition(CFA cfa, boolean pInline)
      throws InvalidConfigurationException, IOException {
    BlockOperator blockOperator = new BlockOperator();
    Configuration config =
        TestUtils.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    config.inject(blockOperator);
    try {
      blockOperator.setCFA(cfa);
    } catch (CPAException e) {
      // if blockOperator.setCFA throws a CPAexception, this is because of an invalid configuration
      throw new InvalidConfigurationException("Initialization of block operator failed", e);
    }

    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);

    DssBlockDecomposition child = new LinearBlockNodeDecomposition(isBlockEnd);
    if (pInline) {
      child = new InliningDecomposition(child);
    }

    return new MergeBlockNodesDecomposition(
        child, 2, -1, Comparator.comparing(BlockNodeWithoutGraphInformation::getId), false, true);
  }
}
