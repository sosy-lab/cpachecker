// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;

public class StronglyConnectedComponents {

  private static final int UNDEFINED_INDEX = Integer.MAX_VALUE;
  private int index = 0;

  private final Multimap<Vertex, Vertex> successors;
  private final Deque<Vertex> stack;
  private final Set<Vertex> vertices;

  private StronglyConnectedComponents(
      BlockNodeWithoutGraphInformation pStartNode,
      Function<BlockNodeWithoutGraphInformation, Iterable<BlockNodeWithoutGraphInformation>>
          pSuccessors) {
    stack = new ArrayDeque<>();
    successors = ArrayListMultimap.create();
    Set<Edge> edges = new LinkedHashSet<>();
    List<BlockNodeWithoutGraphInformation> waitlist = new ArrayList<>();
    waitlist.add(pStartNode);
    Map<BlockNodeWithoutGraphInformation, Vertex> cache = new LinkedHashMap<>();
    while (!waitlist.isEmpty()) {
      BlockNodeWithoutGraphInformation current = waitlist.remove(0);
      for (BlockNodeWithoutGraphInformation t : pSuccessors.apply(current)) {
        Vertex v1 = cache.getOrDefault(current, new Vertex(current));
        Vertex v2 = cache.getOrDefault(t, new Vertex(t));
        cache.put(current, v1);
        cache.put(t, v2);
        Edge edge = new Edge(v1, v2);
        if (!edges.contains(edge)) {
          waitlist.add(t);
          edges.add(edge);
          successors.put(v1, v2);
        }
      }
    }
    vertices = ImmutableSet.copyOf(cache.values());
  }

  public static ImmutableList<List<BlockNodeWithoutGraphInformation>> performTarjanAlgorithm(
      BlockNodeWithoutGraphInformation pStartNode,
      Function<BlockNodeWithoutGraphInformation, Iterable<BlockNodeWithoutGraphInformation>>
          pSuccessors) {
    return ImmutableList.copyOf(
        new StronglyConnectedComponents(pStartNode, pSuccessors).findStronglyConnectedComponents());
  }

  private record Edge(Vertex predecessor, Vertex successor) {}

  private static class Vertex {

    private int index;
    private int lowLink;
    private boolean onStack;

    private final BlockNodeWithoutGraphInformation wrapped;

    private Vertex(BlockNodeWithoutGraphInformation pWrapped) {
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

    private BlockNodeWithoutGraphInformation getWrapped() {
      return wrapped;
    }

    @Override
    public boolean equals(Object pO) {
      if (pO instanceof Vertex vertex) {
        return wrapped.equals(vertex.getWrapped());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getClass(), wrapped);
    }
  }

  private List<List<BlockNodeWithoutGraphInformation>> findStronglyConnectedComponents() {
    if (vertices.size() >= Integer.MAX_VALUE - 1) {
      throw new AssertionError("Cannot handle >=" + (Integer.MAX_VALUE - 1) + " vertices");
    }
    List<List<BlockNodeWithoutGraphInformation>> components = new ArrayList<>();
    for (Vertex vertex : vertices) {
      if (vertex.getIndex() == UNDEFINED_INDEX) {
        findStronglyConnectedComponents(vertex, components);
      }
    }
    return components;
  }

  private void findStronglyConnectedComponents(
      Vertex v, List<List<BlockNodeWithoutGraphInformation>> pComponents) {
    v.setIndex(index);
    v.setLowLink(index);
    index++;
    stack.push(v);
    v.setOnStack(true);
    for (Vertex w : successors.get(v)) {
      if (w.getIndex() == UNDEFINED_INDEX) {
        findStronglyConnectedComponents(w, pComponents);
        v.setLowLink(Integer.min(v.getLowLink(), w.getLowLink()));
      } else if (w.isOnStack()) {
        v.setLowLink(Integer.min(v.getLowLink(), w.getIndex()));
      }
    }
    if (v.getLowLink() == v.getIndex()) {
      List<BlockNodeWithoutGraphInformation> stronglyConnected = new ArrayList<>();
      while (!stack.isEmpty()) {
        Vertex w = stack.pop();
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
