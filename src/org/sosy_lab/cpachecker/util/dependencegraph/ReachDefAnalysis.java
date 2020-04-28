package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTraversable;

public abstract class ReachDefAnalysis<V, N, E> {

  private final Graph<N, E> graph;
  private final DomTraversable<N> domTraversable;
  private final DomFrontiers<N> domFrontiers;

  private final Multimap<V, E> variableDefEdges;
  private final Multimap<N, Def.Combiner<V, E>> nodeDefCombiners;
  private final Map<V, Deque<Def<V, E>>> variableDefStacks;

  protected ReachDefAnalysis(
      Graph<N, E> pGraph, DomTraversable<N> pDomTraversable, DomFrontiers<N> pDomFrontiers) {

    graph = pGraph;
    domTraversable = pDomTraversable;
    domFrontiers = pDomFrontiers;

    variableDefEdges = ArrayListMultimap.create();
    nodeDefCombiners = ArrayListMultimap.create();
    variableDefStacks = new HashMap<>();
  }

  /** Returns set of defs for the specified edge. */
  protected abstract Set<V> getEdgeDefs(E pEdge);

  private Deque<Def<V, E>> getDefStack(V pVariable) {
    return variableDefStacks.computeIfAbsent(pVariable, key -> new ArrayDeque<>());
  }

  protected final Iterable<Def<V, E>> getDefStackIterator(V pVariable) {
    Iterator<Def<V, E>> iterator = getDefStack(pVariable).iterator();
    return () -> Iterators.unmodifiableIterator(iterator);
  }

  protected final Iterable<Def<V, E>> getDefQueueIterator(V pVariable) {
    Iterator<Def<V, E>> descIterator = getDefStack(pVariable).descendingIterator();
    return () -> Iterators.unmodifiableIterator(descIterator);
  }

  protected final Def<V, E> getReachDef(V pVariable) {
    return getDefStack(pVariable).peek();
  }

  protected final void insertCombiner(N pNode, V pVariable) {
    nodeDefCombiners.put(pNode, new Def.Combiner<V, E>(pVariable));
  }

  protected void insertCombiners(DomFrontiers<N> pDomFrontiers) {

    for (V variable : variableDefEdges.keySet()) {

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
    for (V edgeDefVar : getEdgeDefs(pEdge)) {
      Deque<Def<V, E>> defStack = getDefStack(edgeDefVar);
      defStack.push(new Def.Wrapper<>(edgeDefVar, pEdge));
    }

    // update successor combiners
    for (Def.Combiner<V, E> combiner : nodeDefCombiners.get(graph.getSuccessor(pEdge))) {
      Def<V, E> def = getReachDef(combiner.getVariable());
      if (def != null) {
        combiner.add(def);
      }
    }
  }

  private void popEdge(E pEdge) {
    for (V variable : getEdgeDefs(pEdge)) {
      getDefStack(variable).pop();
    }
  }

  private void pushNode(N pNode) {
    for (Def.Combiner<V, E> combiner : nodeDefCombiners.get(pNode)) {
      getDefStack(combiner.getVariable()).push(combiner);
    }
  }

  private void popNode(N pNode) {
    for (Def.Combiner<V, E> combiner : nodeDefCombiners.get(pNode)) {
      getDefStack(combiner.getVariable()).pop();
    }
  }

  /**
   * A dangling edge is an edge that does not exist in the dom-tree.
   *
   * <p><code>
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
   * </code>
   */
  private boolean isDanglingEdge(DomTraversable<N> pParent, E pEdge) {

    N successor = graph.getSuccessor(pEdge);

    if (graph.getNumEnteringEdges(successor) > 1) {
      for (DomTraversable<N> child : pParent) {
        if (child.getNode().equals(successor)) {
          return false; // there is an edge in the dom-tree for pEdge
        }
      }
    } else {
      return false; // only nodes that have at least 2 entering edges can cause dangling edges
    }

    return true; // there is no edge in the dom-tree for pEdge -> dangling
  }

  protected void traverseDomTree(DomTraversable<N> pDomTraversable) {

    Deque<Iterator<DomTraversable<N>>> stack = new ArrayDeque<>();
    DomTraversable<N> current = pDomTraversable;

    stack.push(current.iterator());
    pushNode(current.getNode());

    while (true) { // break when root gets popped from the stack (see end of 'visit parent')

      Iterator<DomTraversable<N>> children = stack.peek();

      if (children.hasNext()) { // visit child

        DomTraversable<N> next = children.next();
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

        DomTraversable<N> prev = current;
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

  public void run() {
    insertCombiners(domFrontiers);
    traverseDomTree(domTraversable);
  }

  public interface Graph<N, E> {

    N getPredecessor(E pEdge);

    N getSuccessor(E pEdge);

    Optional<E> getEdge(N pPred, N pSucc);

    Iterable<E> getLeavingEdges(N pNode);

    int getNumEnteringEdges(N pNode);
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
