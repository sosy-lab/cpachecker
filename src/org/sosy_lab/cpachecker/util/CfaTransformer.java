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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** An instance of this class can be used to modify specific parts of a control flow graph (CFA). */
public abstract class CfaTransformer {

  public abstract Optional<Node> getNode(CFANode pCfaNode);

  public static final class Node {

    private final CFANode oldCfaNode;
    private final List<Edge> enteringEdges;
    private final List<Edge> leavingEdges;

    private Node(CFANode pOldCfaNode) {

      oldCfaNode = pOldCfaNode;

      enteringEdges = new ArrayList<>();
      leavingEdges = new ArrayList<>();
    }

    public static Node createFrom(CFANode pOldCfaNode) {

      Objects.requireNonNull(pOldCfaNode, "pOldCfaNode must not be null");

      return new Node(pOldCfaNode);
    }

    public CFANode getOldCfaNode() {
      return oldCfaNode;
    }

    public void attachEntering(Edge pEdge) {

      Objects.requireNonNull(pEdge, "pEdge must not be null");
      Preconditions.checkArgument(
          !pEdge.getSuccessor().isPresent(),
          "Cannot attach entering edge that already has a successor");

      pEdge.setSuccessor(Optional.of(this));
      addEnteringEdge(pEdge);
    }

    public void detachEntering(Edge pEdge) {

      Objects.requireNonNull(pEdge, "pEdge must not be null");
      Preconditions.checkArgument(
          !pEdge.getSuccessor().isEmpty(), "Cannot detach entering edge that has no successor");

      pEdge.setSuccessor(Optional.empty());
      removeEnteringEdge(pEdge);
    }

    public void attachLeaving(Edge pEdge) {

      Objects.requireNonNull(pEdge, "pEdge must not be null");
      Preconditions.checkArgument(
          !pEdge.getPredecessor().isPresent(),
          "Cannot attach leaving edge that already has a predecessor");

      pEdge.setPredecessor(Optional.of(this));
      addLeavingEdge(pEdge);
    }

    public void detachLeaving(Edge pEdge) {

      Objects.requireNonNull(pEdge, "pEdge must not be null");
      Preconditions.checkArgument(
          !pEdge.getPredecessor().isEmpty(), "Cannot detach leaving edge that has no predecessor");

      pEdge.setPredecessor(Optional.empty());
      removeLeavingEdge(pEdge);
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
    public void splitAndInsertEntering(Edge pEdge, Node pNewNode) {

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
    public void splitAndInsertLeaving(Edge pEdge, Node pNewNode) {

      for (Iterator<CfaTransformer.Edge> iterator = newLeavingIterator(); iterator.hasNext(); ) {
        CfaTransformer.Edge leavingEdge = iterator.next();
        iterator.remove();
        pNewNode.attachLeaving(leavingEdge);
      }

      pNewNode.attachEntering(pEdge);
      attachLeaving(pEdge);
    }

    private void addEnteringEdge(Edge pEdge) {
      enteringEdges.add(pEdge);
    }

    private void removeEnteringEdge(Edge pEdge) {
      enteringEdges.remove(pEdge);
    }

    public Iterator<Edge> newEnteringIterator() {
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
          current.setSuccessor(Optional.empty());
        }
      };
    }

    public Iterable<Edge> iterateEntering() {
      return this::newEnteringIterator;
    }

    private void addLeavingEdge(Edge pEdge) {
      leavingEdges.add(pEdge);
    }

    private void removeLeavingEdge(Edge pEdge) {
      leavingEdges.remove(pEdge);
    }

    public Iterator<Edge> newLeavingIterator() {
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
          current.setPredecessor(Optional.empty());
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
          "%s[oldCfaNode=%s, enteringEdges=%s, leavingEdges=%s]",
          getClass(),
          oldCfaNode,
          enteringEdges,
          leavingEdges);
    }
  }

  public static final class Edge {

    private final CFAEdge oldCfaEdge;
    private Optional<Node> predecessor;
    private Optional<Node> successor;

    private Edge(CFAEdge pOldCfaEdge) {
      oldCfaEdge = pOldCfaEdge;
      predecessor = Optional.empty();
      successor = Optional.empty();
    }

    public static Edge createFrom(CFAEdge pOldCfaEdge) {

      Objects.requireNonNull(pOldCfaEdge, "pOldCfaEdge must not be null");

      return new Edge(pOldCfaEdge);
    }

    public CFAEdge getOldCfaEdge() {
      return oldCfaEdge;
    }

    public Optional<Node> getPredecessor() {
      return predecessor;
    }

    Node getPredecessorOrElseThrow() {
      return getPredecessor()
          .orElseThrow(() -> new IllegalStateException("Missing edge predecessor: " + this));
    }

    private void setPredecessor(Optional<Node> pPredecessor) {
      predecessor = pPredecessor;
    }

    public Optional<Node> getSuccessor() {
      return successor;
    }

    Node getSuccessorOrElseThrow() {
      return getSuccessor()
          .orElseThrow(() -> new IllegalStateException("Missing edge successor: " + this));
    }

    private void setSuccessor(Optional<Node> pSuccessor) {
      successor = pSuccessor;
    }

    public void detachAll() {

      if (predecessor.isPresent()) {
        predecessor.orElseThrow().detachLeaving(this);
      }

      if (successor.isPresent()) {
        successor.orElseThrow().detachEntering(this);
      }
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[oldCfaEdge=%s, predecessor=%s, successor=%s]",
          getClass(),
          oldCfaEdge,
          predecessor,
          successor);
    }
  }
}
