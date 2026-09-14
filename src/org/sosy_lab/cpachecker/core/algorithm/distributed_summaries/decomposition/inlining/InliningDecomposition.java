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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
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
   * <p>Besides the own ID, the successors and predecessors are also adjusted correctly.
   */
  private Iterable<BlockNode> mapBlocksWithCallStack(FunctionSCC scc, CallStack stack) {

    CallStack previousStack = stack.pop();

    Set<String> idsInScc =
        FluentIterable.from(scc.functions())
            .transformAndConcat(f -> f.blockNodes())
            .transform(n -> n.getId())
            .toSet();

    BlockNode callingBlock = stack.getLastCallBlock();

    String callingBlockId = callingBlock != null ? callingBlock.getId() : null;
    String returnBlockId =
        callingBlock != null
            ? getReturnBlockIdForEntryNode(stack.getLastCallScc(), callingBlock)
            : null;

    return FluentIterable.from(scc.functions())
        .transformAndConcat(f -> f.blockNodes())
        .transform(
            node -> {
              Predicate<String> successorFilter = successorFilter(idsInScc, returnBlockId, node);

              Function<String, String> succMapper = successorMapper(scc, node, idsInScc, stack);

              Predicate<String> predecessorFilter =
                  predecessorFilter(node, idsInScc, callingBlockId);

              Function<String, String> predMapper =
                  predecessorMapper(scc, node, idsInScc, callingBlockId, stack, previousStack);

              return copyWithMappedBlockIds(
                  node,
                  stack::toStringWithBlockId,
                  predecessorFilter,
                  predMapper,
                  successorFilter,
                  succMapper);
            });
  }

  /**
   * filters out every successor that contains a return out of this SCC that does not belong to the
   * call that entered it
   */
  private Predicate<String> successorFilter(
      Set<String> idsInScc, String returnBlockId, BlockNode node) {
    boolean isReturn = node.getFinalLocation() instanceof FunctionExitNode;

    Predicate<String> successorFilter =
        s -> idsInScc.contains(s) || !isReturn || (isReturn && s.equals(returnBlockId));
    return successorFilter;
  }

  /**
   * filters out every predecessor that would be a call into this SCC but does not fit the call
   * stack
   */
  private Predicate<String> predecessorFilter(
      BlockNode currNode, Set<String> idsInScc, String callingBlockId) {

    boolean isSccEntry = currNode.getInitialLocation() instanceof FunctionEntryNode;
    boolean isCorrectSccEntry = currNode.getPredecessorIds().contains(callingBlockId);

    Predicate<String> predecessorFilter =
        s ->
            // keep blocks inside SCC
            idsInScc.contains(s)
                // the current call into this SCC
                || (isCorrectSccEntry && s.equals(callingBlockId))
                // not a function entry point -> a return from a
                // function call to outside this SCC
                || !isSccEntry;
    return predecessorFilter;
  }

  /**
   * Maps the IDs of successors to the Ids the copy will have for the specific call stack
   *
   * <p>Adds the stack. If we have a return or a call, the stack is adjusted accordingly.
   *
   * @param scc the scc of the block whose IDs are mapped
   * @param node the block whose IDs are mapped
   * @param idsInScc the ids of all nodes in the scc
   * @param stack the callstack for which we copy the block
   * @return a function that adds the correct stack suffix to the successor ids
   */
  private Function<String, String> successorMapper(
      FunctionSCC scc, BlockNode node, Set<String> idsInScc, CallStack stack) {

    FunctionSCC callingSCC = stack.getLastCallScc();
    var blocksInCallingSCC =
        callingSCC == null
            ? FluentIterable.of()
            : FluentIterable.from(callingSCC.functions())
                .transformAndConcat(f -> f.blockNodes())
                .transform(n -> n.getId());

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

  /**
   * Maps the IDs of predecessors to the IDs the copy will have for the specific call stack
   *
   * <p>Adds the stack. If we have a return or a call, the stack is adjusted accordingly.
   *
   * @param scc the scc of the block whose IDs are mapped
   * @param node the block whose IDs are mapped
   * @param idsInScc the ids of all nodes in the scc
   * @param callingBlockId the id of the block that made the last call
   * @param stack the callstack for which we copy the block
   * @param previousStack the stack of the calling block
   * @return a function that adds the correct stack suffix to the predecessor ids
   */
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
            .filter(
                n -> n.getNodes().contains(callNode) && !n.getFinalLocation().equals(callNode)));
  }

  private String getReturnBlockIdForEntryNode(FunctionSCC callingScc, BlockNode callingBlock) {

    assert callingBlock.getFinalLocation() instanceof FunctionEntryNode;

    CFAEdge returnEdge =
        Iterables.getOnlyElement(
            callingBlock
                .getFinalLocation()
                .getAllEnteringEdges()
                // the call edge of the block
                .filter(e -> callingBlock.getEdges().contains(e))
                .transformAndConcat(e -> e.getPredecessor().getAllLeavingEdges())
                // the corresponding summary edges
                .filter(e -> e instanceof FunctionSummaryEdge)
                // the node after the return
                .transform(e -> e.getSuccessor())
                .transformAndConcat(n -> n.getAllEnteringEdges())
                .filter(e -> e instanceof FunctionReturnEdge),
            null);

    if (returnEdge == null) {
      return null;
    }

    FluentIterable<BlockNode> returnBlock =
        FluentIterable.from(callingScc.functions())
            .transformAndConcat(f -> f.blockNodes())
            .filter(n -> n.getEdges().contains(returnEdge));

    assert returnBlock.size() <= 1;

    if (returnBlock.isEmpty()) {
      return null;
    }

    return returnBlock.get(0).getId();
  }

  private BlockNode copyWithMappedBlockIds(
      BlockNode original,
      Function<String, String> idMapper,
      Predicate<String> predecessorFilter,
      Function<String, String> predecessorMapper,
      Predicate<String> successorFilter,
      Function<String, String> successorMapper) {

    ImmutableSet<String> mappedPredecessors =
        FluentIterable.from(original.getPredecessorIds())
            .filter(predecessorFilter)
            .transform(predecessorMapper)
            .toSet();

    ImmutableSet<String> mappedSuccessors =
        FluentIterable.from(original.getSuccessorIds())
            .filter(successorFilter)
            .transform(successorMapper)
            .toSet();

    return new BlockNode(
        idMapper.apply(original.getId()),
        original.getInitialLocation(),
        original.getFinalLocation(),
        original.getNodes(),
        original.getEdges(),
        mappedPredecessors,
        mappedSuccessors);
  }
}
