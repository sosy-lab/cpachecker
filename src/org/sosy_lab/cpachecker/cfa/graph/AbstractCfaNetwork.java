// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.graph.AbstractNetwork;
import com.google.common.graph.ElementOrder;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * This class provides a skeletal implementation of {@link CfaNetwork}.
 *
 * <p>If this class is extended, only a small number of methods need to be implemented. These
 * methods fully define a {@link CfaNetwork}.
 */
abstract class AbstractCfaNetwork extends AbstractNetwork<CFANode, CFAEdge> implements CfaNetwork {

  // network-level accessors

  @Override
  public Set<CFAEdge> edges() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new AbstractIterator<>() {

          private final Iterator<CFANode> nodeIterator = nodes().iterator();
          private Iterator<CFAEdge> currentNodeOutEdgeIterator = Collections.emptyIterator();

          @Override
          protected @Nullable CFAEdge computeNext() {
            while (!currentNodeOutEdgeIterator.hasNext()) {
              if (nodeIterator.hasNext()) {
                CFANode currentNode = nodeIterator.next();
                currentNodeOutEdgeIterator = outEdges(currentNode).iterator();
              } else {
                return endOfData();
              }
            }

            return currentNodeOutEdgeIterator.next();
          }
        };
      }
    };
  }

  // network properties

  @Override
  public final boolean isDirected() {
    return true;
  }

  @Override
  public final boolean allowsParallelEdges() {
    return false;
  }

  @Override
  public final boolean allowsSelfLoops() {
    return true;
  }

  @Override
  public ElementOrder<CFANode> nodeOrder() {
    return ElementOrder.unordered();
  }

  @Override
  public ElementOrder<CFAEdge> edgeOrder() {
    return ElementOrder.unordered();
  }

  // element-level accessors

  @Override
  public Set<CFANode> adjacentNodes(CFANode pNode) {
    return Collections.unmodifiableSet(Sets.union(predecessors(pNode), successors(pNode)));
  }

  @Override
  public Set<CFANode> predecessors(CFANode pNode) {
    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Iterators.transform(inEdges(pNode).iterator(), AbstractCfaNetwork.this::predecessor);
      }

      @Override
      public int size() {
        return inDegree(pNode);
      }
    };
  }

  @Override
  public Set<CFANode> successors(CFANode pNode) {
    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Iterators.transform(outEdges(pNode).iterator(), AbstractCfaNetwork.this::successor);
      }

      @Override
      public int size() {
        return outDegree(pNode);
      }
    };
  }

  @Override
  public Set<CFAEdge> incidentEdges(CFANode pNode) {
    return Collections.unmodifiableSet(Sets.union(inEdges(pNode), outEdges(pNode)));
  }

  // `CfaNetwork` specific

  @Override
  public Set<FunctionEntryNode> entryNodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<FunctionEntryNode> iterator() {
        Set<CFANode> entryNodeSet = Sets.filter(nodes(), node -> node instanceof FunctionEntryNode);
        return Iterators.transform(entryNodeSet.iterator(), node -> (FunctionEntryNode) node);
      }
    };
  }

  @Override
  public CFANode predecessor(CFAEdge pEdge) {
    return incidentNodes(pEdge).source();
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return incidentNodes(pEdge).target();
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionSummaryEdge pFunctionSummaryEdge) {
    CFANode predecessor = predecessor(pFunctionSummaryEdge);
    Set<CFAEdge> nonSummaryOutEdges = withoutSummaryEdges().outEdges(predecessor);
    checkState(
        nonSummaryOutEdges.size() == 1, "Single non-summary out-edge expected: %s", predecessor);
    CFAEdge nonSummaryOutEdge = Iterables.getOnlyElement(nonSummaryOutEdges);
    CFANode node = successor(nonSummaryOutEdge);
    // skip blank edges if necessary
    while (!(node instanceof FunctionEntryNode)
        && outDegree(node) == 1
        && Iterables.getOnlyElement(outEdges(node)).getEdgeType() == CFAEdgeType.BlankEdge) {
      node = Iterables.getOnlyElement(successors(node));
    }
    if (node instanceof FunctionEntryNode functionEntryNode) {
      return functionEntryNode;
    } else {
      throw new IllegalStateException(
          "Cannot determine function entry node for " + pFunctionSummaryEdge);
    }
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    Set<CFANode> waitlisted = new HashSet<>(ImmutableList.of(pFunctionEntryNode));
    Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

    while (!waitlist.isEmpty()) {

      CFANode node = waitlist.remove();

      if (node instanceof FunctionExitNode) {
        return Optional.of((FunctionExitNode) node);
      }

      for (CFAEdge outEdge : outEdges(node)) {
        // we don't want to leave the current function
        if (!(outEdge instanceof FunctionCallEdge)) {
          CFANode successor = successor(outEdge);
          if (waitlisted.add(successor)) {
            waitlist.add(successor);
          }
        }
      }
    }

    return Optional.empty();
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionCallEdge pFunctionCallEdge) {
    CFANode node = predecessor(pFunctionCallEdge);
    // skip blank edges if necessary
    while (outDegree(node) == 1
        && inDegree(node) == 1
        && Iterables.getOnlyElement(inEdges(node)).getEdgeType() == CFAEdgeType.BlankEdge) {
      node = Iterables.getOnlyElement(predecessors(node));
    }
    for (CFAEdge edge : outEdges(node)) {
      if (edge instanceof FunctionSummaryEdge functionSummaryEdge) {
        return functionSummaryEdge;
      }
    }
    throw new IllegalStateException(
        "Cannot determine function summary edge for " + pFunctionCallEdge);
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge) {
    CFANode node = successor(pFunctionReturnEdge);
    // skip blank edges if necessary
    while (inDegree(node) == 1
        && outDegree(node) == 1
        && Iterables.getOnlyElement(outEdges(node)).getEdgeType() == CFAEdgeType.BlankEdge) {
      node = Iterables.getOnlyElement(successors(node));
    }
    for (CFAEdge edge : inEdges(node)) {
      if (edge instanceof FunctionSummaryEdge functionSummaryEdge) {
        return functionSummaryEdge;
      }
    }
    throw new IllegalStateException(
        "Unable to determine function summary edge for " + pFunctionReturnEdge);
  }
}
