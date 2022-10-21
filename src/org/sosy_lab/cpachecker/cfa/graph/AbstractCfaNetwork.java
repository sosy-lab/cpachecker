// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
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

          private Iterator<CFAEdge> outEdges = Collections.emptyIterator();

          @Override
          protected @Nullable CFAEdge computeNext() {

            while (!outEdges.hasNext()) {
              if (nodeIterator.hasNext()) {
                CFANode node = nodeIterator.next();
                outEdges = outEdges(node).iterator();
              } else {
                return endOfData();
              }
            }

            return outEdges.next();
          }
        };
      }
    };
  }

  // network properties

  @Override
  public boolean isDirected() {
    return true;
  }

  @Override
  public boolean allowsParallelEdges() {
    return false;
  }

  @Override
  public boolean allowsSelfLoops() {
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
    CFAEdge functionCallEdge = Iterables.getOnlyElement(nonSummaryOutEdges);
    checkState(
        functionCallEdge instanceof FunctionCallEdge,
        "Function call edge expected: %s",
        functionCallEdge);
    CFANode functionEntryNode = successor(functionCallEdge);
    checkState(
        functionEntryNode instanceof FunctionEntryNode,
        "Function entry node expected: %s",
        functionEntryNode);

    return (FunctionEntryNode) functionEntryNode;
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

    CFANode predecessor = predecessor(pFunctionCallEdge);
    Set<CFAEdge> nonSuperOutEdges = withoutSuperEdges().outEdges(predecessor);
    checkState(nonSuperOutEdges.size() == 1, "Single non-super out-edge expected: %s", predecessor);
    CFAEdge functionSummaryEdge = Iterables.getOnlyElement(nonSuperOutEdges);
    checkState(
        functionSummaryEdge instanceof FunctionSummaryEdge,
        "Function summary edge expected: %s",
        functionSummaryEdge);

    return (FunctionSummaryEdge) functionSummaryEdge;
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge) {

    CFANode successor = successor(pFunctionReturnEdge);
    Set<CFAEdge> nonSuperInEdges = withoutSuperEdges().inEdges(successor);
    checkState(nonSuperInEdges.size() == 1, "Single non-super in-edge expected: %s", successor);
    CFAEdge functionSummaryEdge = Iterables.getOnlyElement(nonSuperInEdges);
    checkState(
        functionSummaryEdge instanceof FunctionSummaryEdge,
        "Function summary edge expected: %s",
        functionSummaryEdge);

    return (FunctionSummaryEdge) functionSummaryEdge;
  }
}
