/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * This class provides strategies for iterating through a CFA
 * (a set of {@link CFANode}s connected by {@link CFAEdge}s).
 * Strategies differ for example in the direction (forwards/backwards),
 * and whether summary edges are recognized.
 *
 * Instances of this class are always immutable, thread-safe and may be re-used.
 * Thus, care must be taken when calling methods of this class which return a
 * CFATraversal instance. This never mutate the instance on which they are called!
 *
 * Right code:
 * <code>
 * CFATraversal traversal = CFATraversal.allEdgesForward();
 * traversal = traversal.backwards();
 * traversal.traverse(...);
 * </code>
 *
 * Wrong code:
 * <code>
 * CFATraversal traversal = CFATraversal.allEdgesForward();
 * traversal.backwards(); // WRONG!!! Has no effect!
 * traversal.traverse(...);
 * </code>
 *
 * For traversing the CFA, a {@link CFAVisitor} needs to be given.
 * Several default implementations are available.
 *
 * Important: The instances of this class do not track a set of already visited
 * nodes. Thus a visitor may be called several times for a single node.
 * If the visitor never specifies to stop the traversal and the CFA contains loops,
 * this will produce an infinite loop!
 * It is strongly recommended to use the {@link NodeCollectingCFAVisitor} to
 * prevent this and visit each node only once (wrap your own visitor in it).
 */
public class CFATraversal {

  private static final Function<CFANode, Iterable<CFAEdge>> FORWARD_EDGE_SUPPLIER =
      CFAUtils::allLeavingEdges;

  private static final Function<CFANode, Iterable<CFAEdge>> BACKWARD_EDGE_SUPPLIER =
      CFAUtils::allEnteringEdges;

  // function providing the outgoing edges for a CFANode
  private final Function<CFANode, Iterable<CFAEdge>> edgeSupplier;

  // function providing the successor node of an edge
  private final Function<CFAEdge, CFANode> successorSupplier;

  // predicate for whether an edge should be ignored
  private final Predicate<CFAEdge> ignoreEdge;

  protected CFATraversal(Function<CFANode, Iterable<CFAEdge>> pEdgeSupplier,
      Function<CFAEdge, CFANode> pSuccessorSupplier, Predicate<CFAEdge> pIgnoreEdge) {
    edgeSupplier = pEdgeSupplier;
    successorSupplier = pSuccessorSupplier;
    ignoreEdge = pIgnoreEdge;
  }

  /**
   * Returns a default instance of this class, which iterates forward through
   * the CFA, visiting all nodes and edges in a DFS-like strategy.
   */
  public static CFATraversal dfs() {
    return new CFATraversal(
        FORWARD_EDGE_SUPPLIER, CFAEdge::getSuccessor, Predicates.<CFAEdge>alwaysFalse());
  }

  /**
   * Returns a new instance of this class which behaves exactly like the current
   * instance, except its traversal direction is reversed (e.g., going backwards
   * instead of going forwards).
   */
  public CFATraversal backwards() {
    if (edgeSupplier == FORWARD_EDGE_SUPPLIER) {
      return new CFATraversal(BACKWARD_EDGE_SUPPLIER, CFAEdge::getPredecessor, ignoreEdge);
    } else if (edgeSupplier == BACKWARD_EDGE_SUPPLIER) {
      return new CFATraversal(FORWARD_EDGE_SUPPLIER, CFAEdge::getSuccessor, ignoreEdge);
    } else {
      throw new AssertionError();
    }
  }

  /**
   * Returns a new instance of this class which behaves exactly like the current
   * instance, except it ignores summary edges ({@link FunctionSummaryEdge}s).
   * It will not call the visitor for them, and it will not follow this edge
   * during traversing.
   */
  public CFATraversal ignoreSummaryEdges() {
    return new CFATraversal(edgeSupplier,
        successorSupplier,
        Predicates.<CFAEdge>or(ignoreEdge,
            Predicates.instanceOf(FunctionSummaryEdge.class)));
  }

  /**
   * Returns a new instance of this class which behaves exactly like the current
   * instance, except it ignores function call and return edges.
   * It will not call the visitor for them, and it will not follow this edge
   * during traversing. Thus it will always stay inside the current function.
   */
  @SuppressWarnings("unchecked")
  public CFATraversal ignoreFunctionCalls() {
    return new CFATraversal(edgeSupplier,
        successorSupplier,
        Predicates.<CFAEdge>or(
            ignoreEdge,
            Predicates.instanceOf(FunctionCallEdge.class),
            Predicates.instanceOf(FunctionReturnEdge.class)
            ));
  }

  /**
   * Traverse through the CFA according to the strategy represented by the
   * current instance, starting at a given node and passing each
   * encountered node and edge to a given visitor.
   * @param startingNode The starting node.
   * @param visitor The visitor to notify.
   */
  public void traverse(final CFANode startingNode, final CFATraversal.CFAVisitor visitor) {

    Deque<CFANode> toProcess = new ArrayDeque<>();

    toProcess.addLast(startingNode);

    while (!toProcess.isEmpty()) {
      CFANode n = toProcess.removeLast();

      CFATraversal.TraversalProcess result = visitor.visitNode(n);
      if (result == TraversalProcess.ABORT) {
        return;
      }

      if (result != TraversalProcess.SKIP) {
        for (CFAEdge edge : edgeSupplier.apply(n)) {
          if (ignoreEdge.apply(edge)) {
            continue;
          }

          result = visitor.visitEdge(edge);
          if (result == TraversalProcess.ABORT) {
            return;
          }

          if (result != TraversalProcess.SKIP) {
            toProcess.addLast(successorSupplier.apply(edge));
          }
        }
      }
    }
    return;
  }

  /**
   * Traverse through the CFA according to the strategy represented by the
   * current instance, starting at a given node and passing each
   * encountered node and edge to a given visitor.
   *
   * Each node will be visited only once.
   * This method does the same as wrapping the given visitor in a
   * {@link NodeCollectingCFAVisitor} and calling {@link #traverse(CFANode, CFAVisitor)}.
   *
   * @param startingNode The starting node.
   * @param visitor The visitor to notify.
   */
  public void traverseOnce(final CFANode startingNode, final CFATraversal.CFAVisitor visitor) {
    traverse(startingNode, new NodeCollectingCFAVisitor(visitor));
  }

  /**
   * Traverse through the CFA according to the strategy represented by the
   * current instance, starting at a given node and collecting all encountered nodes.
   * @param startingNode The starting node.
   * @return A modifiable reference to the set of visited nodes.
   */
  public Set<CFANode> collectNodesReachableFrom(final CFANode startingNode) {
    NodeCollectingCFAVisitor visitor = new NodeCollectingCFAVisitor();
    this.traverse(startingNode, visitor);
    return visitor.getVisitedNodes();
  }

  /**
   * Traverse through the CFA according to the strategy represented by the
   * current instance, starting at a given node and collecting all encountered nodes
   * up to a given end node
   * @param startingNode The starting node.
   * @param endingNode The ending node
   * @return A modifiable reference to the set of visited nodes.
   */
  public Set<CFANode> collectNodesReachableFromTo(final CFANode startingNode,
                                                  final CFANode endingNode) {
    NodeCollectingCFAVisitor visitor = new NodeCollectingCFAVisitor();
    visitor.stopVisitingNode = endingNode;
    this.traverse(startingNode, visitor);
    return visitor.getVisitedNodes();
  }

  // --- Useful visitor implementations ---

  /**
   * An implementation of {@link CFAVisitor} which does two things:
   * - It keeps a set of all visited nodes, and provides this set after the traversal process.
   * - It prevents the traversal process from visiting a node twice.
   *
   * Because of the last point it is suggested to always use this visitor.
   *
   * Instances of this visitor may be re-used.
   * In this case, the following uses will re-use the set of visited nodes from
   * the first time (i.e., a node visited in the first traversal will not be
   * visited in the second traversal)
   */
  public final static class NodeCollectingCFAVisitor extends ForwardingCFAVisitor {

    private final Set<CFANode> visitedNodes = new HashSet<>();

    /**
     * A Node where the visitor should stop calling the visit method of its
     * super class. This is used by {@link CFATraversal#collectNodesReachableFromTo(CFANode, CFANode)}.
     */
    private CFANode stopVisitingNode = null;

    /**
     * Creates a new instance which delegates calls to another visitor, but
     * never calls that visitor twice for the same node.
     * @param pDelegate The visitor to delegate to.
     */
    public NodeCollectingCFAVisitor(CFAVisitor pDelegate) {
      super(pDelegate);
    }

    /**
     * Convenience constructor for cases when you only need the functionality
     * of this visitor and no other visitor.
     */
    public NodeCollectingCFAVisitor() {
      super(DefaultCFAVisitor.INSTANCE);
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      if (visitedNodes.add(pNode) && !(stopVisitingNode != null && stopVisitingNode == pNode)) {
        return super.visitNode(pNode);
      }

      return TraversalProcess.SKIP;
    }

    /**
     * Get the set of nodes this visitor has seen so far.
     * This set may be modified by the caller. Nodes put in this set will not be
     * visited by this visitor, nodes removed from this set may be visited again.
     *
     * This method may be called even before the first time this visitor is used.
     * The returned set may not be modified during a traversal process.
     *
     * @return A modifiable reference to the set of visited nodes.
     */
    public Set<CFANode> getVisitedNodes() {
      return visitedNodes;
    }
  }

  /**
   * An implementation of {@link CFAVisitor} which keeps track of all visited
   * edges.
   */
  public final static class EdgeCollectingCFAVisitor extends ForwardingCFAVisitor {

    private final List<CFAEdge> visitedEdges = new ArrayList<>();

    /**
     * Creates a new instance which delegates calls to another visitor.
     * @param pDelegate The visitor to delegate to.
     */
    public EdgeCollectingCFAVisitor(CFAVisitor pDelegate) {
      super(pDelegate);
    }

    /**
     * Convenience constructor for cases when you only need the functionality
     * of this visitor and a {@link NodeCollectingCFAVisitor}.
     */
    public EdgeCollectingCFAVisitor() {
      super(new NodeCollectingCFAVisitor());
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      visitedEdges.add(pEdge);
      return super.visitEdge(pEdge);
    }

    /**
     * Get the list of edges this visitor has seen so far in chronological order.
     * The list may contain edges twice, if they were visited several times.
     * Note that this is not the case if the {@link NodeCollectingCFAVisitor}
     * is used.
     *
     * The returned list may be modified, but not during the traversal process.
     * This will have no effect on the visitor, aside from the results of future
     * calls to this method.
     *
     * @return A reference to the set of visited nodes.
     */
    public List<CFAEdge> getVisitedEdges() {
      return visitedEdges;
    }
  }

  /**
   * An implementation of {@link CFAVisitor} which delegates to several other
   * visitors.
   * All visitors will be called in the same order for each edge and node.
   * If one visitor returns ABORT, the other visitors will still be called,
   * and ABORT is returned.
   * If one visitor returns SKIP, the other visitors will still be called,
   * and SKIP is returned if none of them returned ABORT.
   * Otherwise CONTINUE is returned.
   */
  public static class CompositeCFAVisitor implements CFAVisitor {

    private final ImmutableList<CFAVisitor> visitors;

    public CompositeCFAVisitor(Iterable<CFAVisitor> pVisitors) {
      visitors = ImmutableList.copyOf(pVisitors);
    }

    public CompositeCFAVisitor(CFAVisitor... pVisitors) {
      visitors = ImmutableList.copyOf(pVisitors);
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      TraversalProcess totalResult = TraversalProcess.CONTINUE;

      for (CFAVisitor visitor : visitors) {
        TraversalProcess result = visitor.visitEdge(pEdge);

        if (result == TraversalProcess.ABORT) {
          totalResult = TraversalProcess.ABORT;
        } else if (result == TraversalProcess.SKIP && totalResult != TraversalProcess.ABORT) {
          totalResult = TraversalProcess.SKIP;
        }
      }
      return totalResult;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      TraversalProcess totalResult = TraversalProcess.CONTINUE;

      for (CFAVisitor visitor : visitors) {
        TraversalProcess result = visitor.visitNode(pNode);

        if (result == TraversalProcess.ABORT) {
          totalResult = TraversalProcess.ABORT;
        } else if (result == TraversalProcess.SKIP && totalResult != TraversalProcess.ABORT) {
          totalResult = TraversalProcess.SKIP;
        }
      }
      return totalResult;
    }
  }


  // --- Types and classes for implementors of CFAVisitors

  /**
   * Interface for CFA traversal visitors used by {@link CFATraversal#traverse(CFANode, CFAVisitor)}.
   *
   * If any of these method throws an exception, the traversal process is
   * immediately aborted and the exception is passed to the caller.
   *
   * @see CFATraversal
   */
  public static interface CFAVisitor {

    /**
     * Called for each edge the traversal process encounters.
     * @param edge The current CFAEdge.
     * @return A value of {@link TraversalProcess} to steer the traversal process.
     */
    TraversalProcess visitEdge(CFAEdge edge);

    /**
     * Called for each node the traversal process encounters.
     * @param node The current CFANode.
     * @return A value of {@link TraversalProcess} to steer the traversal process.
     */
    TraversalProcess visitNode(CFANode node);
  }

  /**
   * An enum for possible actions a visitor can tell the traversal strategy to
   * do next.
   */
  public static enum TraversalProcess {
   /**
     * Continue normally.
     */
    CONTINUE,

    /**
     * Skip following the currently handled node or edge (i.e., the successors won't be visited).
     */
    SKIP,

    /**
     * Completely abort the traversal process (forgetting all nodes and edges which are still to be visited).
     */
    ABORT
  }

  /**
   * A default implementation of {@link CFAVisitor} which does nothing and
   * always returns {@link TraversalProcess#CONTINUE}.
   */
  public static class DefaultCFAVisitor implements CFAVisitor {

    private static final CFAVisitor INSTANCE = new DefaultCFAVisitor();

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      return TraversalProcess.CONTINUE;
    }
  }

  /**
   * A default implementation of {@link CFAVisitor} which forwards everything to
   * another visitor.
   */
  public abstract static class ForwardingCFAVisitor implements CFAVisitor {

    protected final CFAVisitor delegate;

    protected ForwardingCFAVisitor(CFAVisitor pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      return delegate.visitEdge(pEdge);
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      return delegate.visitNode(pNode);
    }
  }
}