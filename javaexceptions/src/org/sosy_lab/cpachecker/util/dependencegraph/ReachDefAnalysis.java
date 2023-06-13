// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.graph.Graph;
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
import org.sosy_lab.cpachecker.util.graph.dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

/**
 * Abstract reaching definition analysis implementation based on dominator tree traversal.
 *
 * @param <D> the type of defined values (we are interested in definitions and uses of values that
 *     have this type)
 * @param <N> the input graph's node type
 * @param <E> the input graph's edge type
 */
abstract class ReachDefAnalysis<D, N, E> {

  private final InputGraph<N, E> graph;

  private final DomTree<N> domTree;
  private final DomFrontiers<N> domFrontiers;

  private final Multimap<D, E> variableDefEdges;
  private final Multimap<N, Def.Combiner<D, E>> nodeDefCombiners;
  private final Map<D, Deque<Def<D, E>>> variableDefStacks;

  protected ReachDefAnalysis(
      InputGraph<N, E> pGraph, DomTree<N> pDomTree, DomFrontiers<N> pDomFrontiers) {

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

  protected void insertCombiners(DomFrontiers<N> pDomFrontiers) {

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
  private boolean isDanglingEdge(Graph<N> pDomTree, N pParentNode, E pEdge) {

    N edgeSuccessor = graph.getSuccessor(pEdge);

    if (hasMultipleEnteringEdges(edgeSuccessor)) {
      for (N successor : pDomTree.successors(pParentNode)) {
        if (successor.equals(edgeSuccessor)) {
          return false; // there is an edge in the dom-tree for pEdge
        }
      }
    } else {
      return false; // only nodes that have at least 2 entering edges can cause dangling edges
    }

    return true; // there is no edge in the dom-tree for pEdge -> dangling
  }

  protected void traverseDomTree(Graph<N> pDomTree, N pRootNode) {

    Deque<Iterator<N>> stack = new ArrayDeque<>();
    N currentNode = pRootNode;

    stack.push(pDomTree.successors(currentNode).iterator());
    pushNode(currentNode);

    while (true) { // break when root gets popped from the stack (see end of 'visit parent')

      Iterator<N> children = stack.peek();

      if (children.hasNext()) { // visit child

        N nextNode = children.next();
        Optional<E> optEdge = graph.getEdge(currentNode, nextNode);

        if (optEdge.isPresent()) {
          pushEdge(optEdge.orElseThrow());
        }

        stack.push(pDomTree.successors(nextNode).iterator());
        pushNode(nextNode);

        for (E edge : graph.getLeavingEdges(nextNode)) {
          if (isDanglingEdge(pDomTree, nextNode, edge)) {
            pushEdge(edge);
            popEdge(edge);
          }
        }

        currentNode = nextNode;

      } else { // visit parent

        N prevNode = currentNode;
        currentNode = Iterables.getOnlyElement(pDomTree.predecessors(currentNode), null);

        popNode(prevNode);
        stack.pop();

        if (currentNode != null) {

          Optional<E> optEdge = graph.getEdge(currentNode, prevNode);

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
    domTree.getRoot().ifPresent(rootNode -> traverseDomTree(domTree.asGraph(), rootNode));
  }

  // TODO: use Guava network instead
  public interface InputGraph<N, E> {

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
