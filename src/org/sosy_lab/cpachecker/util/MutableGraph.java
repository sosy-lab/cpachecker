// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a mutable graph by wrapping nodes and edges of an existing (and possibly immutable)
 * graph.
 *
 * <p>Nodes from the existing graph are wrapped in {@link MutableGraph.Node} objects. Edges from the
 * existing graph are wrapped in {@link MutableGraph.Edge} objects. All connections between nodes
 * and edges are stored in the mutable graph nodes and edges and can be modified. The connections of
 * the wrapped nodes and edges are not changed by the mutable graph and may differ from the mutable
 * graph node and edge connections.
 *
 * @param <N> the wrapped node type
 * @param <E> the wrapped edge type
 */
public abstract class MutableGraph<N, E> {

  /**
   * Returns the mutable graph node that wraps the specified node.
   *
   * @param pWrappedNode the wrapped node to get a mutable graph node for
   * @return If this mutable graph contains a node that wraps the specified node, an {@link
   *     Optional} containing the mutable graph node is returned. Otherwise, if the mutable graph
   *     does not contain such a node, {@code Optional.empty()} is returned.
   */
  public abstract Optional<Node<N, E>> getNode(N pWrappedNode);

  public Node<N, E> wrapNode(N pWrappedNode) {

    Objects.requireNonNull(pWrappedNode, "pWrappedNode must not be null");

    return new Node<>(pWrappedNode);
  }

  public Edge<N, E> wrapEdge(E pWrappedEdge) {

    Objects.requireNonNull(pWrappedEdge, "pWrappedEdge must not be null");

    return new Edge<>(pWrappedEdge);
  }

  public Edge<N, E> wrapEdge(E pWrappedEdge, Node<N, E> pPredecessor, Node<N, E> pSuccessor) {

    Objects.requireNonNull(pWrappedEdge, "pWrappedEdge must not be null");
    Objects.requireNonNull(pPredecessor, "pPredecessor must not be null");
    Objects.requireNonNull(pSuccessor, "pSuccessor must not be null");

    Edge<N, E> edge = wrapEdge(pWrappedEdge);
    attachLeaving(pPredecessor, edge);
    attachEntering(pSuccessor, edge);

    return edge;
  }

  public void attachEntering(Node<N, E> pNode, Edge<N, E> pEdge) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEdge, "pEdge must not be null");
    Preconditions.checkArgument(
        pEdge.successor == null, "Cannot attach entering edge that already has a successor");

    pEdge.successor = pNode;
    pNode.enteringEdges.add(pEdge);
  }

  public boolean detachEntering(Node<N, E> pNode, Edge<N, E> pEdge) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEdge, "pEdge must not be null");
    Preconditions.checkArgument(
        pEdge.successor != null, "Cannot detach entering edge that has no successor");

    boolean contained = pNode.enteringEdges.remove(pEdge);

    if (contained) {
      pEdge.successor = null;
    }

    return contained;
  }

  public void attachLeaving(Node<N, E> pNode, Edge<N, E> pEdge) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEdge, "pEdge must not be null");
    Preconditions.checkArgument(
        pEdge.predecessor == null, "Cannot attach leaving edge that already has a predecessor");

    pEdge.predecessor = pNode;
    pNode.leavingEdges.add(pEdge);
  }

  public boolean detachLeaving(Node<N, E> pNode, Edge<N, E> pEdge) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEdge, "pEdge must not be null");
    Preconditions.checkArgument(
        pEdge.predecessor != null, "Cannot detach leaving edge that has no predecessor");

    boolean contained = pNode.leavingEdges.remove(pEdge);

    if (contained) {
      pEdge.predecessor = null;
    }

    return contained;
  }

  /**
   *
   *
   * <pre>{@code
   * Before:
   * --- a ---> [this] --- b ---->
   *
   * After:
   * --- a ---> [pPredecessor] --- pEnteringEdge ---> [this] --- b ---->
   *
   * }</pre>
   */
  public void insertPredecessor(
      Node<N, E> pNode, Edge<N, E> pEnteringEdge, Node<N, E> pPredecessor) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEnteringEdge, "pEnteringEdge must not be null");
    Objects.requireNonNull(pPredecessor, "pPredecessor must not be null");

    for (Iterator<Edge<N, E>> iterator = pNode.newEnteringIterator(); iterator.hasNext(); ) {
      Edge<N, E> enteringEdge = iterator.next();
      iterator.remove();
      attachEntering(pPredecessor, enteringEdge);
    }

    attachEntering(pNode, pEnteringEdge);
    attachLeaving(pPredecessor, pEnteringEdge);
  }

  /**
   *
   *
   * <pre>{@code
   * Before:
   * --- a ---> [this] --- b ---->
   *
   * After:
   * --- a ---> [this] --- pLeavingEdge ---> [pSuccessor] --- b ---->
   *
   * }</pre>
   */
  public void insertSuccessor(Node<N, E> pNode, Edge<N, E> pLeavingEdge, Node<N, E> pSuccessor) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pLeavingEdge, "pLeavingEdge must not be null");
    Objects.requireNonNull(pSuccessor, "pSuccessor must not be null");

    for (Iterator<Edge<N, E>> iterator = pNode.newLeavingIterator(); iterator.hasNext(); ) {
      Edge<N, E> leavingEdge = iterator.next();
      iterator.remove();
      attachLeaving(pSuccessor, leavingEdge);
    }

    attachEntering(pSuccessor, pLeavingEdge);
    attachLeaving(pNode, pLeavingEdge);
  }

  public Iterable<Edge<N, E>> iterateEntering(Node<N, E> pNode) {

    Objects.requireNonNull(pNode, "pNode must not be null");

    return pNode::newEnteringIterator;
  }

  public Iterable<Edge<N, E>> iterateLeaving(Node<N, E> pNode) {

    Objects.requireNonNull(pNode, "pNode must not be null");

    return pNode::newLeavingIterator;
  }

  public void detachBoth(Edge<N, E> pEdge) {

    Objects.requireNonNull(pEdge, "pEdge must not be null");

    if (pEdge.predecessor != null) {
      detachLeaving(pEdge.predecessor, pEdge);
    }

    if (pEdge.successor != null) {
      detachEntering(pEdge.successor, pEdge);
    }
  }

  public static final class Node<N, E> {

    private final N wrappedNode;
    private final List<Edge<N, E>> enteringEdges;
    private final List<Edge<N, E>> leavingEdges;

    private Node(N pWrappedNode) {
      wrappedNode = pWrappedNode;
      enteringEdges = new ArrayList<>();
      leavingEdges = new ArrayList<>();
    }

    public N getWrappedNode() {
      return wrappedNode;
    }

    private Iterator<Edge<N, E>> newEnteringIterator() {
      return new Iterator<>() {

        private Iterator<Edge<N, E>> delegate = enteringEdges.iterator();
        private Edge<N, E> current = null;

        @Override
        public boolean hasNext() {
          return delegate.hasNext();
        }

        @Override
        public Edge<N, E> next() {
          return current = delegate.next();
        }

        @Override
        public void remove() {
          delegate.remove();
          current.successor = null;
        }
      };
    }

    private Iterator<Edge<N, E>> newLeavingIterator() {
      return new Iterator<>() {

        private Iterator<Edge<N, E>> delegate = leavingEdges.iterator();
        private Edge<N, E> current = null;

        @Override
        public boolean hasNext() {
          return delegate.hasNext();
        }

        @Override
        public Edge<N, E> next() {
          return current = delegate.next();
        }

        @Override
        public void remove() {
          delegate.remove();
          current.predecessor = null;
        }
      };
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[wrappedNode=%s, enteringEdges=%s, leavingEdges=%s]",
          getClass(),
          wrappedNode,
          enteringEdges,
          leavingEdges);
    }
  }

  public static final class Edge<N, E> {

    private final E wrappedEdge;
    private @Nullable Node<N, E> predecessor;
    private @Nullable Node<N, E> successor;

    private Edge(E pWrappedEdge) {
      wrappedEdge = pWrappedEdge;
      predecessor = null;
      successor = null;
    }

    public E getWrappedEdge() {
      return wrappedEdge;
    }

    public Optional<Node<N, E>> getPredecessor() {
      return Optional.ofNullable(predecessor);
    }

    public Node<N, E> getPredecessorOrElseThrow() {
      return getPredecessor()
          .orElseThrow(() -> new IllegalStateException("Missing edge predecessor: " + this));
    }

    public Optional<Node<N, E>> getSuccessor() {
      return Optional.ofNullable(successor);
    }

    public Node<N, E> getSuccessorOrElseThrow() {
      return getSuccessor()
          .orElseThrow(() -> new IllegalStateException("Missing edge successor: " + this));
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[wrappedEdge=%s, predecessor=%s, successor=%s]",
          getClass(),
          wrappedEdge,
          predecessor,
          successor);
    }
  }
}
