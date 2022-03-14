// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

abstract class ReachDefAnalysis<D, N, E> {

  private final Graph<N, E> graph;

  private final Dominance.DomTree<N> domTree;
  private final Dominance.DomFrontiers<N> domFrontiers;

  private final Multimap<D, E> variableDefEdges;
  private final Multimap<N, Def.Combiner<D, E>> nodeDefCombiners;
  private final Map<D, Deque<Def<D, E>>> variableDefStacks;

  protected ReachDefAnalysis(
      Graph<N, E> pGraph, Dominance.DomTree<N> pDomTree, Dominance.DomFrontiers<N> pDomFrontiers) {

    graph = pGraph;

    domTree = pDomTree;
    domFrontiers = pDomFrontiers;

    variableDefEdges = ArrayListMultimap.create();
    nodeDefCombiners = ArrayListMultimap.create();
    variableDefStacks = new HashMap<>();
  }

  /** Returns set of defs for the specified edge. */
  protected abstract Set<D> getEdgeDefs(E pEdge);

  private Deque<Def<D, E>> getDefStack(D pVariable) {
    return variableDefStacks.computeIfAbsent(pVariable, key -> new ArrayDeque<>());
  }

  protected final Iterable<Def<D, E>> iterateDefsNewestFirst(D pVariable) {
    Iterator<Def<D, E>> iterator = getDefStack(pVariable).iterator();
    return () -> Iterators.unmodifiableIterator(iterator);
  }

  protected final Iterable<Def<D, E>> iterateDefsOldestFirst(D pVariable) {
    Iterator<Def<D, E>> descIterator = getDefStack(pVariable).descendingIterator();
    return () -> Iterators.unmodifiableIterator(descIterator);
  }

  protected Collection<Def<D, E>> getReachDefs(D pVariable) {
    return Collections.singleton(getDefStack(pVariable).peek());
  }

  protected final void insertCombiner(N pNode, D pVariable) {
    nodeDefCombiners.put(pNode, new Def.Combiner<D, E>(pVariable));
  }

  protected void insertCombiners(Dominance.DomFrontiers<N> pDomFrontiers) {

    for (D variable : variableDefEdges.keySet()) {

      Set<N> defNodes = new HashSet<>();

      for (E defEdge : variableDefEdges.get(variable)) {
        defNodes.add(graph.getPredecessor(defEdge));
      }

      for (N node : pDomFrontiers.getIteratedFrontier(defNodes)) {
        insertCombiner(node, variable);
      }
    }
  }

  protected void pushEdge(E pEdge) {

    // update def stacks
    for (D edgeDefVar : getEdgeDefs(pEdge)) {
      Deque<Def<D, E>> defStack = getDefStack(edgeDefVar);
      defStack.push(new Def.Wrapper<>(edgeDefVar, pEdge));
    }

    // update successor's combiners
    for (Def.Combiner<D, E> combiner : nodeDefCombiners.get(graph.getSuccessor(pEdge))) {
      for (Def<D, E> def : getReachDefs(combiner.getVariable())) {
        combiner.add(def);
      }
    }
  }

  protected void popEdge(E pEdge) {
    for (D variable : getEdgeDefs(pEdge)) {
      getDefStack(variable).pop();
    }
  }

  protected void pushNode(N pNode) {
    for (Def.Combiner<D, E> combiner : nodeDefCombiners.get(pNode)) {
      getDefStack(combiner.getVariable()).push(combiner);
    }
  }

  protected void popNode(N pNode) {
    for (Def.Combiner<D, E> combiner : nodeDefCombiners.get(pNode)) {
      getDefStack(combiner.getVariable()).pop();
    }
  }

  private boolean hasMultipleEnteringEdges(N pNode) {

    Iterator<E> iterator = graph.getEnteringEdges(pNode).iterator();
    if (iterator.hasNext()) {
      iterator.next();
      return iterator.hasNext();
    } else {
      return false;
    }
  }

  /**
   * A dangling edge is an edge that does not exist in the dom-tree.
   *
   * <pre>{@code
   * Example:
   *
   * Graph:
   *              a--->[2]---c
   *              /          \
   * [START]--->[1]           [4]--->[END]
   *              \          /
   *              b--->[3]---d
   *
   * DomTree:
   *              a--->[2]
   *              /
   * [START]--->[1]--->[4]--->[END]
   *              \
   *              b--->[3]
   *
   * d,c are dangling edges in this example
   *
   * DomTree (with dangling edges):
   *              a--->[2]---c--->
   *              /
   * [START]--->[1]--->[4]--->[END]
   *              \
   *              b--->[3]---d--->
   * }</pre>
   */
  private boolean isDanglingEdge(Dominance.DomTraversable<N> pParent, E pEdge) {

    N successor = graph.getSuccessor(pEdge);

    if (hasMultipleEnteringEdges(successor)) {
      for (Dominance.DomTraversable<N> child : pParent) {
        if (child.getNode().equals(successor)) {
          return false; // there is an edge in the dom-tree for pEdge
        }
      }
    } else {
      return false; // only nodes that have at least 2 entering edges can cause dangling edges
    }

    return true; // there is no edge in the dom-tree for pEdge -> dangling
  }

  protected void traverseDomTree(Dominance.DomTraversable<N> pDomTraversable) {

    Deque<Iterator<Dominance.DomTraversable<N>>> stack = new ArrayDeque<>();
    Dominance.DomTraversable<N> current = pDomTraversable;

    stack.push(current.iterator());
    pushNode(current.getNode());

    while (true) { // break when root gets popped from the stack (see end of 'visit parent')

      Iterator<Dominance.DomTraversable<N>> children = stack.peek();

      if (children.hasNext()) { // visit child

        Dominance.DomTraversable<N> next = children.next();
        Optional<E> optEdge = graph.getEdge(current.getNode(), next.getNode());

        if (optEdge.isPresent()) {
          pushEdge(optEdge.orElseThrow());
        }

        stack.push(next.iterator());
        pushNode(next.getNode());

        for (E edge : graph.getLeavingEdges(next.getNode())) {
          if (isDanglingEdge(next, edge)) {
            pushEdge(edge);
            popEdge(edge);
          }
        }

        current = next;

      } else { // visit parent

        Dominance.DomTraversable<N> prev = current;
        current = current.getParent();

        popNode(prev.getNode());
        stack.pop();

        if (current != null) {

          Optional<E> optEdge = graph.getEdge(current.getNode(), prev.getNode());

          if (optEdge.isPresent()) {
            popEdge(optEdge.orElseThrow());
          }

        } else {
          assert stack.isEmpty();
          break;
        }
      }
    }
  }

  private void registerDefs() {

    for (N node : domTree) {
      for (E edge : graph.getLeavingEdges(node)) {
        for (D varDef : getEdgeDefs(edge)) {
          variableDefEdges.put(varDef, edge);
        }
      }
    }
  }

  public void run() {

    registerDefs();
    insertCombiners(domFrontiers);
    traverseDomTree(Dominance.createDomTraversable(domTree));
  }

  public interface Graph<N, E> {

    N getPredecessor(E pEdge);

    N getSuccessor(E pEdge);

    Optional<E> getEdge(N pPredecessor, N pSuccessor);

    Iterable<E> getLeavingEdges(N pNode);

    Iterable<E> getEnteringEdges(N pNode);
  }

  public abstract static class Def<V, E> {

    private final V variable;

    private Def(V pVariable) {
      variable = pVariable;
    }

    final V getVariable() {
      return variable;
    }

    public abstract Optional<E> getEdge();

    public abstract void collect(Set<Def<V, E>> pDefs);

    private static class Wrapper<V, E> extends Def<V, E> {

      private final E edge;

      private Wrapper(V pVariable, E pEdge) {
        super(pVariable);
        edge = pEdge;
      }

      @Override
      public Optional<E> getEdge() {
        return Optional.of(edge);
      }

      @Override
      public void collect(Set<Def<V, E>> pDefs) {
        pDefs.add(this);
      }
    }

    private static final class Combiner<V, E> extends Def<V, E> {

      private final List<Def<V, E>> defs;

      private Combiner(V pVariable) {
        super(pVariable);
        defs = new ArrayList<>();
      }

      private void add(Def<V, E> pDef) {
        defs.add(pDef);
      }

      @Override
      public Optional<E> getEdge() {
        return Optional.empty();
      }

      @Override
      public void collect(Set<Def<V, E>> pDefs) {
        for (Def<V, E> def : defs) {
          if (pDefs.add(def) && def instanceof Combiner) {
            def.collect(pDefs);
          }
        }
      }
    }
  }
}
