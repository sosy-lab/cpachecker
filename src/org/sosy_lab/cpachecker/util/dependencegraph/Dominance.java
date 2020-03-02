package org.sosy_lab.cpachecker.util.dependencegraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Dominance {

  public static final int UNDEFINED = -1;

  private <T> Map<T, Integer> createReversePostOrder(
      T start, final Function<? super T, ? extends Iterable<? extends T>> succSupplier) {

    Map<T, Integer> visited = new HashMap<>();
    Deque<T> stack = new ArrayDeque<>();
    int counter = 0;

    stack.push(start);

    while (!stack.isEmpty()) {

      T current = stack.pop();

      for (T next : succSupplier.apply(current)) {
        if (!visited.containsKey(next)) {
          stack.push(next);
          visited.put(next, UNDEFINED);
        }
      }

      visited.put(current, counter);
      counter++;
    }

    for (Map.Entry<T, Integer> entry : visited.entrySet()) {
      int id = Math.abs(entry.getValue() - counter + 1);
      entry.setValue(id);
    }

    return visited;
  }

  public <T> Result<T> doms(
      T start,
      Function<? super T, ? extends Iterable<? extends T>> succSupplier,
      Function<? super T, ? extends Iterable<? extends T>> predSupplier) {

    Map<T, Integer> visited = createReversePostOrder(start, succSupplier);
    Node[] nodes = new Node[visited.size()];

    @SuppressWarnings("unchecked")
    T[] ordered = (T[]) new Object[visited.size()];

    List<Integer> predecessors = new ArrayList<>();
    for (Map.Entry<T, Integer> entry : visited.entrySet()) {

      int id = entry.getValue();
      for (final T next : predSupplier.apply(entry.getKey())) {
        Integer value = visited.get(next);
        assert value != null : "Unknown node (missing order-ID): " + next;
        predecessors.add(value);
      }

      nodes[id] = new Node(predecessors);
      ordered[id] = entry.getKey();

      predecessors.clear();
    }

    int[] doms = calculate(nodes);

    return new Result<>(visited, ordered, nodes, doms);
  }

  private int[] calculate(final Node[] pNodes) {

    final int start = pNodes.length - 1; // start node is node with the highest number
    int[] doms = new int[pNodes.length]; // doms[x] == immediate dominator of x
    boolean changed = true;

    // set all immediate dominators as undefined
    for (int i = 0; i < pNodes.length; i++) {
      doms[i] = UNDEFINED;
    }

    doms[start] = start; // start node is (only) dominated by itself

    while (changed) {
      changed = false;

      // all nodes in reverse postorder (except start)
      for (int number = 0; number < start; number++) {
        final Node node = pNodes[number];
        int idom = UNDEFINED; // immediate dominator for node

        // all predecessors of node (any order)
        for (int i = 0; i < node.getSize(); i++) {
          final int pred = node.getPredecessor(i);

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
        if (doms[number] != idom) {
          doms[number] = idom;
          changed = true;
        }
      }
    }

    return doms;
  }

  private int intersect(final int[] pDoms, final int pN1, final int pN2) {

    int f1 = pN1;
    int f2 = pN2;

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

  public <T> List<Set<Integer>> frontiers(final Result<T> result) {

    List<Set<Integer>> frontiers = new ArrayList<>(result.getSize());
    for (int i = 0; i < result.getSize(); i++) {
      frontiers.add(new HashSet<>());
    }

    for (int i = 0; i < result.getSize(); i++) {
      final Node node = result.nodes[i];

      if (node.getSize() >= 2) {
        for (int j = 0; j < node.getSize(); j++) {
          int runner = node.getPredecessor(j);

          while (runner != UNDEFINED && runner != result.doms[i]) {
            frontiers.get(runner).add(i);
            runner = result.doms[runner];
          }
        }
      }
    }

    return frontiers;
  }

  private static final class Node {

    private final int[] predecessors;

    public Node(List<Integer> predecessors) {

      int[] array = new int[predecessors.size()];
      for (int index = 0; index < array.length; index++) {
        array[index] = predecessors.get(index);
      }

      this.predecessors = array;
    }

    public int getSize() {
      return predecessors.length;
    }

    public int getPredecessor(final int index) {
      return predecessors[index];
    }
  }

  public static final class Result<T> {

    private final Map<T, Integer> mapping;
    private final T[] ordered;

    private final Node[] nodes;
    private final int[] doms;

    private Result(Map<T, Integer> mapping, T[] ordered, Node[] nodes, int[] doms) {
      this.mapping = mapping;
      this.ordered = ordered;
      this.nodes = nodes;
      this.doms = doms;
    }

    public int getSize() {
      return ordered.length;
    }

    public int getNumber(T node) {
      return mapping.get(node);
    }

    public T getNode(int number) {
      return ordered[number];
    }

    public int getDom(int number) {
      return doms[number];
    }
  }
}
