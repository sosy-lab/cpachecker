// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Map.Entry;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssBlockDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.FunctionSCCGraph.FunctionSCC;

public class InliningDecomposition implements DssBlockDecomposition {

  private DssBlockDecomposition childDecomposition;

  public InliningDecomposition(DssBlockDecomposition pChildDecomposition) {
    childDecomposition = pChildDecomposition;
  }

  @Override
  public BlockGraph decompose(CFA pCfa) throws InterruptedException {

    BlockGraph childGraph = childDecomposition.decompose(pCfa);
    FunctionGraph functionGraph = FunctionGraph.from(childGraph);
    FunctionSCCGraph sccGraph = FunctionSCCGraph.from(functionGraph);

    Multimap<FunctionSCC, CallStack> callStacks = sccGraph.findCallStacks();

    ImmutableSet<BlockNode> nodes = copyNodesWithCallStacks(callStacks);

    BlockGraph inlined = new BlockGraph(nodes);

    return inlined;
  }

  private ImmutableSet<@NonNull BlockNode> copyNodesWithCallStacks(
      Multimap<FunctionSCC, CallStack> pCallStacks) {
    ImmutableSet.Builder<BlockNode> blocks = ImmutableSet.builder();

    for (Entry<FunctionSCC, CallStack> entry : pCallStacks.entries()) {
      blocks.addAll(mapBlocksWithCallStack(entry.getKey(), entry.getValue()));
    }

    return blocks.build();
  }

  /**
   * Copy all nodes in this SCC, with the IDs adjusted to the call stack.
   *
   * <p>Besides the own ID, the successors and predecessors are also mapped and filtered.
   */
  private Iterable<BlockNode> mapBlocksWithCallStack(FunctionSCC scc, CallStack stack) {

    CallStack previousStack = stack.pop();

    Set<String> idsInScc =
        FluentIterable.from(scc.functions())
            .transformAndConcat(f -> f.blockNodes())
            .transform(n -> n.getId())
            .toSet();

    BlockNode callingBlock = stack.getLastCallBlock();

    String callingBlockId = callingBlock != null ? stack.getLastCallBlock().getId() : null;
    String returnBlockId =
        callingBlock != null
            ? getReturnBlockForEntryNode(stack.getLastCallScc(), callingBlock).getId()
            : null;

    return FluentIterable.from(scc.functions())
        .transformAndConcat(f -> f.blockNodes())
        .transform(
            node -> {
              FunctionSCC callingSCC = stack.getLastCallScc();
              var blocksInCallingSCC =
                  callingSCC == null
                      ? FluentIterable.of()
                      : FluentIterable.from(callingSCC.functions())
                          .transformAndConcat(f -> f.blockNodes())
                          .transform(n -> n.getId());

              boolean isReturn = node.getFinalLocation() instanceof FunctionExitNode;

              // if this is the block that returns to after the call into
              // this scc, we need to only keep the correct return block
              Predicate<String> successorFilter =
                  s ->
                      idsInScc.contains(s) // keep blocks inside SCC
                          || !isReturn
                          || (isReturn && s.equals(returnBlockId));

              Function<String, String> succMapper =
                  successorMapper(scc, node, idsInScc, stack, blocksInCallingSCC);

              boolean isSccEntry = node.getInitialLocation() instanceof FunctionEntryNode;
              boolean isCorrectSccEntry = node.getPredecessorIds().contains(callingBlockId);

              // filter out blocks that originate from a different call into this SCC
              Predicate<String> predecessorFilter =
                  s ->
                      idsInScc.contains(s) // keep blocks inside SCC
                          || (isCorrectSccEntry
                              && s.equals(callingBlockId)) // the current call into this SCC
                          || !isSccEntry; // not a function entry point -> a return from a
              // function call to outside this SCC

              Function<String, String> predMapper =
                  predecessorMapper(scc, node, idsInScc, callingBlockId, stack, previousStack);

              return node.withMappedIds(
                  stack::toStringWithBlockId,
                  predecessorFilter,
                  predMapper,
                  successorFilter,
                  succMapper);
            });
  }

  private Function<String, String> successorMapper(
      FunctionSCC scc,
      BlockNode node,
      Set<String> idsInScc,
      CallStack stack,
      FluentIterable<?> blocksInCallingSCC) {
    return s -> {
      if (idsInScc.contains(s)) {
        return stack.toStringWithBlockId(s);
      } else {
        if (blocksInCallingSCC.contains(s)) {
          // The block is in the SCC that called into the current SCC
          // -> This is a return from this SCC -> successor has less stack
          return stack.pop().toStringWithBlockId(s);
        }
        // This is a call to outside this SCC -> successor has additional stack
        return stack.addCall(scc, node).toStringWithBlockId(s);
      }
    };
  }

  private Function<String, String> predecessorMapper(
      FunctionSCC scc,
      BlockNode node,
      Set<String> idsInScc,
      String callingBlockId,
      CallStack stack,
      CallStack previousStack) {
    return s -> {
      if (idsInScc.contains(s)) {
        return stack.toStringWithBlockId(s);
      } else {
        if (s.equals(callingBlockId)) {
          return previousStack.toStringWithBlockId(s);
        } else {
          // This is a return from a call to outside this SCC, everything else should
          // be filtered out.
          // find the block that started this call, because we need it for the stack
          // of this predecessor
          CFANode exitNode = node.getInitialLocation();
          BlockNode callBlock = getCallBlockForExitNode(scc, node, exitNode);

          return stack.addCall(scc, callBlock).toStringWithBlockId(s);
        }
      }
    };
  }

  private BlockNode getCallBlockForExitNode(FunctionSCC scc, BlockNode node, CFANode exitNode) {
    assert exitNode instanceof FunctionExitNode;
    CFANode callNode =
        Iterables.getOnlyElement(
            exitNode
                .getAllLeavingEdges()
                // all return edges in this block
                .filter(e -> node.getEdges().contains(e))
                .transformAndConcat(e -> e.getSuccessor().getAllEnteringEdges())
                // the corresponding summary edges
                .filter(e -> e instanceof CFunctionSummaryEdge)
                .transform(e -> e.getPredecessor()));

    return Iterables.getOnlyElement(
        FluentIterable.from(scc.functions())
            .transformAndConcat(f -> f.blockNodes())
            .filter(n -> n.getNodes().contains(callNode)));
  }

  private BlockNode getReturnBlockForEntryNode(FunctionSCC callingScc, BlockNode callingBlock) {

    assert callingBlock.getFinalLocation() instanceof FunctionEntryNode;

    CFANode returnNode =
        Iterables.getOnlyElement(
            callingBlock
                .getFinalLocation()
                .getAllEnteringEdges()
                // the call edge of the block
                .filter(e -> callingBlock.getEdges().contains(e))
                .transformAndConcat(e -> e.getPredecessor().getAllLeavingEdges())
                // the corresponding summary edges
                .filter(e -> e instanceof CFunctionSummaryEdge)
                .transform(e -> e.getSuccessor()));

    return Iterables.getOnlyElement(
        FluentIterable.from(callingScc.functions())
            .transformAndConcat(f -> f.blockNodes())
            .filter(
                n ->
                    n.getNodes().contains(returnNode)
                        && !n.getInitialLocation().equals(returnNode)));
  }
}
