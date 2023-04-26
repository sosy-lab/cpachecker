// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Generic implementation of Tarjan's algorithm (doi.org/10.1137/0201010)
 *
 * @param <T> Any type with a sensible implementation of hashCode and equals
 */
public class StronglyConnectedComponents<T> {

  private static final int UNDEFINED_INDEX = Integer.MAX_VALUE;
  private int index = 0;

  private final Multimap<Vertex<T>, Vertex<T>> successors;
  private final Deque<Vertex<T>> stack;
  private final Set<Vertex<T>> vertices;

  private StronglyConnectedComponents(T pStartNode, Function<T, Iterable<T>> pSuccessors) {
    stack = new ArrayDeque<>();
    successors = ArrayListMultimap.create();
    Set<Edge<T>> edges = new LinkedHashSet<>();
    List<T> waitlist = new ArrayList<>();
    waitlist.add(pStartNode);
    Map<T, Vertex<T>> cache = new LinkedHashMap<>();
    while (!waitlist.isEmpty()) {
      T current = waitlist.remove(0);
      for (T t : pSuccessors.apply(current)) {
        Vertex<T> v1 = cache.getOrDefault(current, new Vertex<>(current));
        Vertex<T> v2 = cache.getOrDefault(t, new Vertex<>(t));
        cache.put(current, v1);
        cache.put(t, v2);
        Edge<T> edge = new Edge<>(v1, v2);
        if (!edges.contains(edge)) {
          waitlist.add(t);
          edges.add(edge);
          successors.put(v1, v2);
        }
      }
    }
    vertices = ImmutableSet.copyOf(cache.values());
  }

  /**
   * Perform Tarjan's Algorithm to find strongly connected components in a directed graph.
   *
   * @param pStartNode root node from which all nodes are reachable
   * @param pSuccessors successor function to find all successors for a given node
   * @return list of an enumeration of strongly connected components
   * @param <T> any type with a sensible implementation of hashCode and equals.
   */
  public static <T> ImmutableList<ImmutableList<T>> performTarjanAlgorithm(
      T pStartNode, Function<T, Iterable<T>> pSuccessors) {
    return transformedImmutableListCopy(
        new StronglyConnectedComponents<>(pStartNode, pSuccessors)
            .findStronglyConnectedComponents(),
        ImmutableList::copyOf);
  }

  private record Edge<T>(Vertex<T> predecessor, Vertex<T> successor) {}

  private static class Vertex<T> {

    private int index;
    private int lowLink;
    private boolean onStack;

    private final T wrapped;

    private Vertex(T pWrapped) {
      index = UNDEFINED_INDEX;
      lowLink = UNDEFINED_INDEX;
      onStack = false;
      wrapped = pWrapped;
    }

    private void setIndex(int pIndex) {
      index = pIndex;
    }

    private void setLowLink(int pLowLink) {
      lowLink = pLowLink;
    }

    private void setOnStack(boolean pOnStack) {
      onStack = pOnStack;
    }

    private int getIndex() {
      return index;
    }

    private int getLowLink() {
      return lowLink;
    }

    private boolean isOnStack() {
      return onStack;
    }

    private T getWrapped() {
      return wrapped;
    }

    @Override
    public boolean equals(Object pO) {
      // since only 1 vertex exists per wrapped item, this is fine
      if (pO instanceof Vertex<?> vertex) {
        return wrapped.equals(vertex.getWrapped());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getClass(), wrapped);
    }
  }

  private List<List<T>> findStronglyConnectedComponents() {
    if (vertices.size() >= Integer.MAX_VALUE - 1) {
      throw new AssertionError("Cannot handle >=" + (Integer.MAX_VALUE - 1) + " vertices");
    }
    List<List<T>> components = new ArrayList<>();
    for (Vertex<T> vertex : vertices) {
      if (vertex.getIndex() == UNDEFINED_INDEX) {
        findStronglyConnectedComponents(vertex, components);
      }
    }
    return components;
  }

  private void findStronglyConnectedComponents(Vertex<T> v, List<List<T>> pComponents) {
    v.setIndex(index);
    v.setLowLink(index);
    index++;
    stack.push(v);
    v.setOnStack(true);
    for (Vertex<T> w : successors.get(v)) {
      if (w.getIndex() == UNDEFINED_INDEX) {
        findStronglyConnectedComponents(w, pComponents);
        v.setLowLink(Integer.min(v.getLowLink(), w.getLowLink()));
      } else if (w.isOnStack()) {
        v.setLowLink(Integer.min(v.getLowLink(), w.getIndex()));
      }
    }
    if (v.getLowLink() == v.getIndex()) {
      List<T> stronglyConnected = new ArrayList<>();
      while (!stack.isEmpty()) {
        Vertex<T> w = stack.pop();
        w.setOnStack(false);
        stronglyConnected.add(w.getWrapped());
        if (v.equals(w)) {
          break;
        }
      }
      pComponents.add(stronglyConnected);
    }
  }
}
