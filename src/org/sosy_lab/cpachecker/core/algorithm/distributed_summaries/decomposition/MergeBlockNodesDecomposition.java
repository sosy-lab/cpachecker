// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public class MergeBlockNodesDecomposition implements DssBlockDecomposition {

  private final DssBlockDecomposition decomposer;
  private final long targetNumber;
  private final Comparator<BlockNode> sort;
  private final HorizontalMergeDecomposition horizontalMerger;
  private final VerticalMergeDecomposition verticalMerger;

  private final boolean allowSingleBlockDecomposition;

  public MergeBlockNodesDecomposition(
      DssBlockDecomposition pDecomposition,
      long pTargetNumber,
      int pHorizontalMergeLimit,
      Comparator<BlockNode> pSort,
      boolean pAllowSingleBlockDecomposition,
      boolean pMergeFunctionCalls) {
    this(
        pDecomposition,
        pTargetNumber,
        pHorizontalMergeLimit,
        pSort,
        pAllowSingleBlockDecomposition,
        FunctionCallMerging.of(pMergeFunctionCalls));
  }

  public MergeBlockNodesDecomposition(
      DssBlockDecomposition pDecomposition,
      long pTargetNumber,
      int pHorizontalMergeLimit,
      Comparator<BlockNode> pSort,
      boolean pAllowSingleBlockDecomposition,
      FunctionCallMerging pFunctionCallMerging) {
    horizontalMerger =
        new HorizontalMergeDecomposition(
            pDecomposition, pTargetNumber, pHorizontalMergeLimit, pSort, pFunctionCallMerging);
    verticalMerger =
        new VerticalMergeDecomposition(pDecomposition, pTargetNumber, pSort, pFunctionCallMerging);
    decomposer = pDecomposition;
    targetNumber = pTargetNumber;
    sort = pSort;
    allowSingleBlockDecomposition = pAllowSingleBlockDecomposition;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    if (targetNumber <= 1 && allowSingleBlockDecomposition) {
      return new SingleBlockDecomposition().decompose(cfa);
    }
    Collection<BlockNode> nodes = decomposer.decompose(cfa).getNodes();

    while (nodes.size() > targetNumber) {
      int sizeBefore = nodes.size();
      nodes = sorted(horizontalMerger.mergeHorizontally(nodes));
      if (nodes.size() <= targetNumber) {
        break;
      }
      nodes = sorted(verticalMerger.mergeVertically(nodes));
      if (sizeBefore == nodes.size()) {
        break;
      }
    }
    return new BlockGraph(ImmutableSet.copyOf(nodes));
  }

  /** Which blocks with calls or returns of the same function merging may create. */
  public enum FunctionCallMerging {
    /** Merging ignores calls and returns. */
    ALWAYS,
    /**
     * Merging creates no block that can leave the function it starts in towards two different call
     * sites, see {@link #returnsToSeveralCallSites}. An analysis whose conditions do not carry a
     * calling context needs this: from the start of such a block, it could return from one call to
     * the call site of another.
     */
    UNIQUE_RETURNS,
    /** Merging creates no block with two calls or two returns of the same function. */
    NEVER;

    static FunctionCallMerging of(boolean pMergeFunctionCalls) {
      return pMergeFunctionCalls ? ALWAYS : NEVER;
    }
  }

  /**
   * Whether merging may create a block with the given initial and final location from the given
   * blocks.
   */
  static boolean mayMerge(
      FunctionCallMerging pMerging,
      CFANode pInitialLocation,
      CFANode pFinalLocation,
      Collection<BlockNode> pBlocks) {
    return switch (pMerging) {
      case ALWAYS -> true;
      case UNIQUE_RETURNS -> !returnsToSeveralCallSites(pInitialLocation, pFinalLocation, pBlocks);
      case NEVER -> !containCallsOrReturnsOfSameFunction(pBlocks);
    };
  }

  /**
   * Whether a block made of the given blocks can return to two different call sites from a function
   * it did not call itself. The paths of the block start at its initial location and end at its
   * final location. A return edge continues a path that entered the function through a call edge of
   * the block only towards that call. Returns out of the function the block starts in, and out of
   * the functions these returns lead to, are not determined by the block: these are compared level
   * by level, so the block returns to several call sites if two paths take different return edges
   * at the same level.
   */
  public static boolean returnsToSeveralCallSites(
      CFANode pInitialLocation, CFANode pFinalLocation, Collection<BlockNode> pBlocks) {
    Set<CFAEdge> edges =
        FluentIterable.from(pBlocks).transformAndConcat(BlockNode::getEdges).toSet();
    Map<Integer, FunctionSummaryEdge> returnPerLevel = new HashMap<>();
    // a point is a location, the calls entered in the block that are still open, and the level
    record Point(CFANode location, ImmutableList<FunctionSummaryEdge> calls, int level) {}
    Set<Point> reached = new HashSet<>();
    Deque<Point> waitlist = new ArrayDeque<>();
    Point start = new Point(pInitialLocation, ImmutableList.of(), 0);
    waitlist.push(start);
    reached.add(start);
    while (!waitlist.isEmpty()) {
      Point point = waitlist.pop();
      if (point != start && point.location().equals(pFinalLocation)) {
        continue;
      }
      for (CFAEdge edge : point.location().getLeavingEdges()) {
        if (!edges.contains(edge)) {
          continue;
        }
        ImmutableList<FunctionSummaryEdge> calls = point.calls();
        int level = point.level();
        if (edge instanceof FunctionCallEdge call) {
          if (calls.size() >= MAX_OPEN_CALLS) {
            // only recursion opens that many calls, and then the context is ambiguous anyway
            return true;
          }
          calls =
              ImmutableList.<FunctionSummaryEdge>builder()
                  .addAll(calls)
                  .add(call.getSummaryEdge())
                  .build();
        } else if (edge instanceof FunctionReturnEdge returnEdge) {
          if (calls.isEmpty()) {
            FunctionSummaryEdge previous =
                returnPerLevel.putIfAbsent(level, returnEdge.getSummaryEdge());
            if (previous != null && !previous.equals(returnEdge.getSummaryEdge())) {
              return true;
            }
            level++;
          } else if (calls.getLast().equals(returnEdge.getSummaryEdge())) {
            calls = calls.subList(0, calls.size() - 1);
          } else {
            continue;
          }
        }
        Point successor = new Point(edge.getSuccessor(), calls, level);
        if (reached.add(successor)) {
          waitlist.push(successor);
        }
      }
    }
    return false;
  }

  private static final int MAX_OPEN_CALLS = 100;

  public static boolean containCallsOrReturnsOfSameFunction(Iterable<BlockNode> toMerge) {

    return FluentIterable.from(toMerge)
        .transformAndConcat(b -> b.getEdges())
        .filter(e -> e instanceof FunctionCallEdge || e instanceof FunctionReturnEdge)
        .transform(e -> (e instanceof FunctionCallEdge) ? e.getSuccessor() : e.getPredecessor())
        .toMultiset()
        .entrySet()
        .stream()
        .anyMatch(entry -> entry.getCount() > 1);
  }

  /**
   * Whether a block made of the given blocks would pass its final location before its end. The
   * analysis of a block stops at the final location, so such a block would never explore the paths
   * that continue from there, e.g., the second call of a function whose entry is the final location
   * and whose first call lies in the block, too. A block whose initial and final location coincide
   * passes the final location only at its start.
   */
  public static boolean passesFinalLocationEarly(
      CFANode pInitialLocation, CFANode pFinalLocation, Iterable<BlockNode> pBlocks) {
    return !pInitialLocation.equals(pFinalLocation)
        && FluentIterable.from(pBlocks)
            .transformAndConcat(BlockNode::getEdges)
            .anyMatch(e -> e.getPredecessor().equals(pFinalLocation));
  }

  private Collection<BlockNode> sorted(Collection<BlockNode> pSort) {
    if (sort == null) {
      return pSort;
    }
    return ImmutableList.sortedCopyOf(sort, pSort);
  }
}
