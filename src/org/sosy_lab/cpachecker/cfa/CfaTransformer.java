// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public abstract class CfaTransformer<N extends CfaNodeTransformer, E extends CfaEdgeTransformer> {

  public abstract Optional<Node> get(CFANode pOriginalCfaNode);

  public abstract CFA createCfa(N pNodeTransformer, E pEdgeTransformer);

  public static final class Node {

    private final CFANode originalCfaNode;
    private final List<Edge> enteringEdges;
    private final List<Edge> leavingEdges;

    private Node(CFANode pOriginalCfaNode) {

      originalCfaNode = pOriginalCfaNode;

      enteringEdges = new ArrayList<>();
      leavingEdges = new ArrayList<>();
    }

    public static Node forOriginal(CFANode pOriginalCfaNode) {

      Objects.requireNonNull(pOriginalCfaNode, "pOriginalCfaNode must not be null");

      return new Node(pOriginalCfaNode);
    }

    public CFANode getOriginalCfaNode() {
      return originalCfaNode;
    }

    public void attachEntering(Edge pEdge) {

      Objects.requireNonNull(pEdge, "pEdge must not be null");
      Preconditions.checkArgument(
          !pEdge.getSuccessor().isPresent(),
          "Cannot attach entering edge that already has a successor");

      pEdge.setSuccessor(this);
      enteringEdges.add(pEdge);
    }

    public boolean detachEntering(Edge pEdge) {

      Objects.requireNonNull(pEdge, "pEdge must not be null");
      Preconditions.checkArgument(
          !pEdge.getSuccessor().isEmpty(), "Cannot detach entering edge that has no successor");

      boolean contained = enteringEdges.remove(pEdge);

      if (contained) {
        pEdge.setSuccessor(null);
      }

      return contained;
    }

    public void attachLeaving(Edge pEdge) {

      Objects.requireNonNull(pEdge, "pEdge must not be null");
      Preconditions.checkArgument(
          !pEdge.getPredecessor().isPresent(),
          "Cannot attach leaving edge that already has a predecessor");

      pEdge.setPredecessor(this);
      leavingEdges.add(pEdge);
    }

    public boolean detachLeaving(Edge pEdge) {

      Objects.requireNonNull(pEdge, "pEdge must not be null");
      Preconditions.checkArgument(
          !pEdge.getPredecessor().isEmpty(), "Cannot detach leaving edge that has no predecessor");

      boolean contained = leavingEdges.remove(pEdge);

      if (contained) {
        pEdge.setPredecessor(null);
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
     * --- a ---> [pNewNode] --- pEdge ---> [this] --- b ---->
     *
     * }</pre>
     */
    public void insertPredecessor(Edge pEdge, Node pNewNode) {

      for (Iterator<CfaTransformer.Edge> iterator = newEnteringIterator(); iterator.hasNext(); ) {
        CfaTransformer.Edge enteringEdge = iterator.next();
        iterator.remove();
        pNewNode.attachEntering(enteringEdge);
      }

      attachEntering(pEdge);
      pNewNode.attachLeaving(pEdge);
    }

    /**
     *
     *
     * <pre>{@code
     * Before:
     * --- a ---> [this] --- b ---->
     *
     * After:
     * --- a ---> [this] --- pEdge ---> [pNewNode] --- b ---->
     *
     * }</pre>
     */
    public void insertSuccessor(Edge pEdge, Node pNewNode) {

      for (Iterator<CfaTransformer.Edge> iterator = newLeavingIterator(); iterator.hasNext(); ) {
        CfaTransformer.Edge leavingEdge = iterator.next();
        iterator.remove();
        pNewNode.attachLeaving(leavingEdge);
      }

      pNewNode.attachEntering(pEdge);
      attachLeaving(pEdge);
    }

    private Iterator<Edge> newEnteringIterator() {
      return new Iterator<>() {

        private Iterator<Edge> delegate = enteringEdges.iterator();
        private Edge current = null;

        @Override
        public boolean hasNext() {
          return delegate.hasNext();
        }

        @Override
        public Edge next() {
          return current = delegate.next();
        }

        @Override
        public void remove() {
          delegate.remove();
          current.setSuccessor(null);
        }
      };
    }

    public Iterable<Edge> iterateEntering() {
      return this::newEnteringIterator;
    }

    private Iterator<Edge> newLeavingIterator() {
      return new Iterator<>() {

        private Iterator<Edge> delegate = leavingEdges.iterator();
        private Edge current = null;

        @Override
        public boolean hasNext() {
          return delegate.hasNext();
        }

        @Override
        public Edge next() {
          return current = delegate.next();
        }

        @Override
        public void remove() {
          delegate.remove();
          current.setPredecessor(null);
        }
      };
    }

    public Iterable<Edge> iterateLeaving() {
      return this::newLeavingIterator;
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[originalCfaNode=%s, enteringEdges=%s, leavingEdges=%s]",
          getClass(),
          originalCfaNode,
          enteringEdges,
          leavingEdges);
    }
  }

  public static final class Edge {

    private final CFAEdge originalCfaEdge;
    private @Nullable Node predecessor;
    private @Nullable Node successor;

    private Edge(CFAEdge pOriginalCfaEdge) {
      originalCfaEdge = pOriginalCfaEdge;
      predecessor = null;
      successor = null;
    }

    public static Edge forOriginal(CFAEdge pOriginalCfaEdge) {

      Objects.requireNonNull(pOriginalCfaEdge, "pOriginalCfaEdge must not be null");

      return new Edge(pOriginalCfaEdge);
    }

    public CFAEdge getOriginalCfaEdge() {
      return originalCfaEdge;
    }

    public Optional<Node> getPredecessor() {
      return Optional.ofNullable(predecessor);
    }

    Node getPredecessorOrElseThrow() {
      return getPredecessor()
          .orElseThrow(() -> new IllegalStateException("Missing edge predecessor: " + this));
    }

    private void setPredecessor(Node pPredecessor) {
      predecessor = pPredecessor;
    }

    public Optional<Node> getSuccessor() {
      return Optional.ofNullable(successor);
    }

    Node getSuccessorOrElseThrow() {
      return getSuccessor()
          .orElseThrow(() -> new IllegalStateException("Missing edge successor: " + this));
    }

    private void setSuccessor(Node pSuccessor) {
      successor = pSuccessor;
    }

    public void detachAll() {

      if (predecessor != null) {
        predecessor.detachLeaving(this);
      }

      if (successor != null) {
        successor.detachEntering(this);
      }
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[originalCfaEdge=%s, predecessor=%s, successor=%s]",
          getClass(),
          originalCfaEdge,
          predecessor,
          successor);
    }
  }
}
