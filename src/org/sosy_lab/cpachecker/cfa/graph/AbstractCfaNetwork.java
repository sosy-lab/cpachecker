// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.graph.AbstractNetwork;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.Traverser;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

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
        return Iterators.unmodifiableIterator(
            Iterables.concat(Iterables.transform(nodes(), node -> outEdges(node))).iterator());
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
    return ElementOrder.stable();
  }

  @Override
  public ElementOrder<CFAEdge> edgeOrder() {
    return ElementOrder.stable();
  }

  // element-level accessors

  @Override
  public Set<CFANode> adjacentNodes(CFANode pNode) {
    return Sets.union(predecessors(pNode), successors(pNode));
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
    return Sets.union(inEdges(pNode), outEdges(pNode));
  }

  // `CfaNetwork` specific

  @Override
  public Set<FunctionEntryNode> entryNodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<FunctionEntryNode> iterator() {
        return Iterators.filter(nodes().iterator(), FunctionEntryNode.class);
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
  public FunctionEntryNode functionEntryNode(FunctionExitNode pFunctionExitNode) {
    return FluentIterable.from(entryNodes())
        .filter(entryNode -> entryNode.getFunction().equals(pFunctionExitNode.getFunction()))
        .first()
        .toJavaUtil()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Function exit node doesn't have a corresponding function entry node: "
                        + pFunctionExitNode));
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    // Try finding the function exit node using intra-function graph traversal to avoid iterating
    // over all nodes.
    Iterable<CFANode> functionNodes =
        Traverser.forGraph(withoutSuperEdges()).depthFirstPostOrder(pFunctionEntryNode);
    Optional<FunctionExitNode> result =
        FluentIterable.from(functionNodes).filter(FunctionExitNode.class).first().toJavaUtil();
    if (result.isEmpty()) {
      // We weren't able to find the function exit node using graph traversal, so we have to check
      // all nodes. Note that the function entry node might not have a corresponding function entry
      // node at all.
      result =
          FluentIterable.from(nodes())
              .filter(FunctionExitNode.class)
              .filter(exitNode -> exitNode.getFunction().equals(pFunctionEntryNode.getFunction()))
              .first()
              .toJavaUtil();
    }
    return result;
  }
}
