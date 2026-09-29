// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.InliningDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class DssBlockCallstackAnalysisTest {
  private static Map<BlockNode, CallstackState> infer(BlockGraph graph, CFA cfa) throws Exception {
    return DssBlockCallstackAnalysis.compute(
        graph, cfa, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());
  }

  private static CFA twoCalls() throws Exception {
    return TestCfaUtils.makeCfaFromString("int x; void f() { x++; } int main() { f(); f(); }");
  }

  @Test
  public void sharedCalleeRetainsUnknownEntryContext() throws Exception {
    CFA cfa = twoCalls();
    BlockGraph graph = new LinearBlockNodeDecomposition(node -> true).decompose(cfa);
    var known = infer(graph, cfa);
    var calleeBlocks =
        graph.getNodes().stream()
            .filter(b -> b.getInitialLocation().equals(cfa.getFunctionHead("f")))
            .toList();
    assertThat(calleeBlocks).isNotEmpty();
    for (BlockNode block : calleeBlocks) {
      assertThat(known).doesNotContainKey(block);
    }
  }

  @Test
  public void inliningDistinguishesTheTwoCallSites() throws Exception {
    CFA cfa = twoCalls();
    BlockGraph graph =
        new InliningDecomposition(new LinearBlockNodeDecomposition(node -> true)).decompose(cfa);
    var known = infer(graph, cfa);
    var calleeBlocks =
        graph.getNodes().stream()
            .filter(b -> b.getInitialLocation().equals(cfa.getFunctionHead("f")))
            .toList();
    assertThat(calleeBlocks).hasSize(2);
    ImmutableSet.Builder<CFANode> callers = ImmutableSet.builder();
    for (BlockNode block : calleeBlocks) {
      assertThat(known).containsKey(block);
      CallstackState stack = known.get(block);
      assertThat(stack.getCurrentFunction()).isEqualTo("f");
      assertThat(stack.getPreviousState().getCurrentFunction()).isEqualTo("main");
      callers.add(stack.getCallNode());
      BlockNode contextual = block.withKnownEntryCallstack(stack);
      assertThat(block.getKnownEntryCallstack()).isEmpty();
      assertThat(contextual.getKnownEntryCallstack()).hasValue(stack);
      assertThat(contextual.getEdges()).isEqualTo(block.getEdges());
    }
    assertThat(callers.build()).hasSize(2);
  }

  @Test
  public void entryEqualToExitStillExecutesOneLoopIteration() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("int main() {}");
    CFANode entry = cfa.getMainFunction();
    CFANode head = new CFANode(entry.getFunction());
    var enter = new BlankEdge("", FileLocation.DUMMY, entry, head, "enter");
    var iterate = new BlankEdge("", FileLocation.DUMMY, head, head, "iterate");
    entry.addLeavingEdge(enter);
    head.addEnteringEdge(enter);
    head.addLeavingEdge(iterate);
    head.addEnteringEdge(iterate);
    BlockNode root =
        new BlockNode(
            "root",
            entry,
            head,
            ImmutableSet.of(entry, head),
            ImmutableSet.of(enter),
            ImmutableSet.of(),
            ImmutableSet.of("loop"));
    BlockNode loop =
        new BlockNode(
            "loop",
            head,
            head,
            ImmutableSet.of(head),
            ImmutableSet.of(iterate),
            ImmutableSet.of("root", "loop"),
            ImmutableSet.of("loop"));
    BlockGraph graph = new BlockGraph(ImmutableSet.of(root, loop));
    assertThat(infer(graph, cfa).keySet()).containsExactly(root, loop);
  }

  @Test
  public void incompleteExplorationPublishesNoContext() throws Exception {
    CFA cfa = twoCalls();
    BlockGraph graph = new LinearBlockNodeDecomposition(node -> true).decompose(cfa);
    assertThat(
            DssBlockCallstackAnalysis.compute(
                graph, cfa, LogManager.createTestLogManager(), ShutdownNotifier.createDummy(), 2))
        .isEmpty();
  }

  @Test
  public void recursiveCallGraphFallsBackWithoutPublishingPartialContexts() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("void f() { f(); } int main() { f(); }");
    BlockGraph graph = new LinearBlockNodeDecomposition(node -> true).decompose(cfa);
    assertThat(infer(graph, cfa)).isEmpty();
  }
}
