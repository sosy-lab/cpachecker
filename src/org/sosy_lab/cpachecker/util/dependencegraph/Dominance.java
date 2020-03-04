package org.sosy_lab.cpachecker.util.dependencegraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class Dominance {

  public static final int UNDEFINED = -1;

  private <T> Map<T, Integer> createReversePostOrder(
      T pStartNode, Function<? super T, ? extends Iterable<? extends T>> pSuccFunc) {

    Map<T, Integer> visited = new HashMap<>();
    Deque<T> stack = new ArrayDeque<>();
    int counter = 0;

    stack.push(pStartNode);

    while (!stack.isEmpty()) {

      T current = stack.pop();

      for (T next : pSuccFunc.apply(current)) {
        if (!visited.containsKey(next)) {
          stack.push(next);
          visited.put(next, UNDEFINED);
        }
      }

      visited.put(current, counter);
      counter++;
    }

    final int offset = (counter - 1);
    visited.replaceAll((node, value) -> Math.abs(value - offset));

    return visited;
  }

  private <T> DomInput createDomInput(
      Map<T, Integer> pIds,
      T[] pNodes,
      Function<? super T, ? extends Iterable<? extends T>> pPredFunc) {

    List<int[]> predsList = new ArrayList<>(Collections.nCopies(pIds.size(), new int[0]));

    List<Integer> preds = new ArrayList<>();
    for (Map.Entry<T, Integer> entry : pIds.entrySet()) {
      int id = entry.getValue();

      for (T next : pPredFunc.apply(entry.getKey())) {
        Integer predId = pIds.get(next);
        assert predId != null : "Unknown node (missing order-ID): " + next;
        preds.add(predId);
      }

      predsList.set(id, preds.stream().mapToInt(i -> i).toArray());
      pNodes[id] = entry.getKey();
      preds.clear();
    }

    return new DomInput(predsList);
  }

  public <T> DomTree<T> createDomTree(
      T pStartNode,
      Function<? super T, ? extends Iterable<? extends T>> pSuccFunc,
      Function<? super T, ? extends Iterable<? extends T>> pPredFunc) {

    Objects.requireNonNull(pStartNode, "start-node");
    Objects.requireNonNull(pSuccFunc, "successor-function");
    Objects.requireNonNull(pPredFunc, "predecessor-function");

    Map<T, Integer> ids = createReversePostOrder(pStartNode, pSuccFunc);

    @SuppressWarnings("unchecked")
    T[] nodes = (T[]) new Object[ids.size()];

    DomInput input = createDomInput(ids, nodes, pPredFunc);

    int[] doms = calculateDoms(input);

    return new DomTree<>(input, ids, nodes, doms);
  }

  private int[] calculateDoms(final DomInput pInput) {

    final int start = pInput.getLength() - 1; // start node is node with the highest number
    int[] doms = new int[pInput.getLength()]; // doms[x] == immediate dominator of x
    boolean changed = true;

    Arrays.fill(doms, UNDEFINED); // no immediate dominator is known
    doms[start] = start; // start node is (only) dominated by itself

    while (changed) {
      changed = false;

      // all nodes in reverse postorder (except start)
      for (int id = 0; id < start; id++) {
        final int[] preds = pInput.getPredecessors(id);
        int idom = UNDEFINED; // immediate dominator for node

        // all predecessors of node (any order)
        for (int index = 0; index < preds.length; index++) {
          final int pred = preds[index];

          // does predecessor have an immediate dominator?
          if (doms[pred] != UNDEFINED) {
            // is idom initialized?
            if (idom != UNDEFINED) {
              // update idom using predecessor
              idom = intersect(doms, pred, idom);
            } else {
              // initialize idom with predecessor
              idom = pred;
            }
          }
        }

        // update immediate dominator for node?
        if (doms[id] != idom) {
          doms[id] = idom;
          changed = true;
        }
      }
    }

    return doms;
  }

  private int intersect(final int[] pDoms, final int pId1, final int pId2) {

    int f1 = pId1;
    int f2 = pId2;

    while (f1 != f2) {
      while (f1 < f2) {
        f1 = pDoms[f1];
      }
      while (f2 < f1) {
        f2 = pDoms[f2];
      }
    }

    return f1;
  }

  public <T> DomFrontiers<T> createDomFrontiers(DomTree<T> pDomTree) {

    Objects.requireNonNull(pDomTree, "dom-tree");

    DomFrontiers.Frontier[] frontiers = calculateFrontiers(pDomTree.getInput(), pDomTree.getDoms());

    return new DomFrontiers<>(pDomTree.getIds(), pDomTree.getNodes(), frontiers);
  }

  private DomFrontiers.Frontier[] calculateFrontiers(final DomInput pInput, final int[] pDoms) {

    DomFrontiers.Frontier[] frontiers = new DomFrontiers.Frontier[pInput.getLength()];
    for (int id = 0; id < frontiers.length; id++) {
      frontiers[id] = new DomFrontiers.Frontier();
    }

    for (int id = 0; id < pInput.getLength(); id++) {
      final int[] preds = pInput.getPredecessors(id);

      if (preds.length > 1) {
        for (int index = 0; index < preds.length; index++) {
          int runner = preds[index];

          while (runner != UNDEFINED && runner != pDoms[id]) {
            frontiers[runner].putInt(id);
            runner = pDoms[runner];
          }
        }
      }
    }
    
    return frontiers;
  }

  private static final class DomInput {

    private final List<int[]> data;

    private DomInput(List<int[]> pData) {
      data = pData;
    }

    private int getLength() {
      return data.size();
    }

    private int[] getPredecessors(int pId) {
      return data.get(pId);
    }
  }

  public static final class DomTree<T> {

    private final DomInput input;
    private final Map<T, Integer> ids;
    private final T[] nodes;
    private final int[] doms;

    private DomTree(DomInput pInput, Map<T, Integer> pIds, T[] pNodes, int[] pDoms) {
      input = pInput;
      ids = pIds;
      nodes = pNodes;
      doms = pDoms;
    }

    private DomInput getInput() {
      return input;
    }

    private Map<T, Integer> getIds() {
      return ids;
    }

    private T[] getNodes() {
      return nodes;
    }

    private int[] getDoms() {
      return doms;
    }

    public int getSize() {
      return doms.length;
    }

    public int getId(T pNode) {
      return ids.get(pNode);
    }

    public T getNode(int pId) {
      return nodes[pId];
    }

    public int getParent(int pId) {
      return doms[pId];
    }

    public boolean hasParent(int pId) {
      return doms[pId] != UNDEFINED && doms[pId] != pId;
    }

    @Override
    public String toString() {

      StringBuilder sb = new StringBuilder();
      sb.append('[');

      for (int id = 0; id < doms.length; id++) {

        sb.append(getNode(id));

        if (hasParent(id)) {
          sb.append(" --> ");
          sb.append(getNode(getParent(id)));
        }

        if (id != doms.length - 1) {
          sb.append(", ");
        }
      }

      sb.append(']');

      return sb.toString();
    }
  }

  public static final class DomFrontiers<T> {

    private final Map<T, Integer> ids;
    private final T[] nodes;
    private final Frontier[] frontiers;

    private DomFrontiers(Map<T, Integer> pIds, T[] pNodes, Frontier[] pFrontiers) {
      ids = pIds;
      nodes = pNodes;
      frontiers = pFrontiers;
    }

    public List<T> getNodes() {
      return Collections.unmodifiableList(Arrays.asList(nodes));
    }

    public Set<T> getFrontier(int pId) {

      Frontier frontier = frontiers[pId];
      Set<T> nodeSet = new HashSet<>();

      for (int id : frontier.set) {
        nodeSet.add(nodes[id]);
      }

      return Collections.unmodifiableSet(nodeSet);
    }

    public Set<T> getFrontier(T pNode) {
      return getFrontier(ids.get(pNode));
    }

    @Override
    public String toString() {
      return Arrays.toString(frontiers);
    }

    // TODO: better set for primitive ints
    private static final class Frontier {

      private Set<Integer> set;

      private Frontier() {
        set = new HashSet<>();
      }

      private void putInt(int pId) {
        set.add(pId);
      }

      @Override
      public String toString() {
        return set.toString();
      }
    }
  }
}
