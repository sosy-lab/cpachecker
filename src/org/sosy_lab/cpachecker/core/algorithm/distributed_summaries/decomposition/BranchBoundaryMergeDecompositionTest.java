// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssTestUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class BranchBoundaryMergeDecompositionTest {
  private static Set<List<CFAEdge>> prefixes(BlockGraph graph) {
    Set<List<CFAEdge>> result = new LinkedHashSet<>();
    collect(graph, graph.getRoot(), graph.getRoot().getInitialLocation(), false, ImmutableList.of(), result);
    return result;
  }

  private static void collect(
      BlockGraph graph,
      BlockNode block,
      CFANode location,
      boolean entered,
      List<CFAEdge> prefix,
      Set<List<CFAEdge>> result) {
    result.add(prefix);
    if (prefix.size() == 24) {
      return;
    }
    if (location == block.getFinalLocation() && (entered || block.getEdges().isEmpty())) {
      for (BlockNode next : graph.getSuccessorsOf(block)) {
        collect(graph, next, next.getInitialLocation(), false, prefix, result);
      }
      return;
    }
    for (CFAEdge edge : location.getLeavingEdges()) {
      if (block.getEdges().contains(edge)) {
        List<CFAEdge> next = new ArrayList<>(prefix);
        next.add(edge);
        collect(graph, block, edge.getSuccessor(), true, next, result);
      }
    }
  }

  private static void compare(String body) throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromString("extern int choose(void); int main(void) { " + body + " }");
    LinearBlockNodeDecomposition linear =
        new LinearBlockNodeDecomposition(DssTestUtils.createBlockOperator(cfa));
    BlockGraph old =
        new MergeBlockNodesDecomposition(linear, 2, -1, null, false, true).decompose(cfa);
    BlockGraph merged = new BranchBoundaryMergeDecomposition(unused -> old).decompose(cfa);
    merged.checkConsistency(ShutdownNotifier.createDummy());
    assertThat(prefixes(merged)).containsExactlyElementsIn(prefixes(old));
    for (BlockNode block : merged.getNodes()) {
      for (CFANode head : cfa.getLoopStructure().orElseThrow().getAllLoopHeads()) {
        if (block.getNodes().contains(head)) {
          assertThat(head == block.getInitialLocation() || head == block.getFinalLocation())
              .isTrue();
        }
      }
    }
  }

  @Test
  public void sharedPrefixesAndSuffixesKeepAllPaths() throws Exception {
    compare("int x; if (choose()) x = 1; else x = 2; if (choose()) return x; else return x + 1;");
  }

  @Test
  public void loopWithMultipleExitsKeepsIterationBoundaries() throws Exception {
    compare(
        "int x = 0; while (choose()) { if (choose()) break; x++; if (choose()) return x; } return"
            + " 0;");
  }

  @Test
  public void sameEntryAndExitStillExecutesAnIteration() throws Exception {
    compare("int x = 0; while (x < 3) x++; return x;");
  }

  @Test
  public void manyIndependentBoundariesAreMergedInBatches() throws Exception {
    CFA cfa =
        TestCfaUtils.makeCfaFromString(
            "extern int choose(void); int main(void) { int x = 0; "
                + "if (choose()) x++; else x--; ".repeat(1100)
                + "return x; }");
    LinearBlockNodeDecomposition linear =
        new LinearBlockNodeDecomposition(DssTestUtils.createBlockOperator(cfa));
    BlockGraph merged = new BranchBoundaryMergeDecomposition(linear).decompose(cfa);
    merged.checkConsistency(ShutdownNotifier.createDummy());
    assertThat(merged.getNodes().size()).isAtMost(3);
  }
}
