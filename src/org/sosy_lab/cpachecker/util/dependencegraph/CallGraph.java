// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class CallGraph<P> {

  private final ImmutableList<Node<P>> nodes;
  private final ImmutableMap<P, Node<P>> nodeMap;

  private CallGraph(ImmutableList<Node<P>> pNodes, ImmutableMap<P, Node<P>> pNodeMap) {

    nodes = pNodes;
    nodeMap = pNodeMap;
  }

  private static <P> Node<P> node(List<Node<P>> pNodes, Map<P, Node<P>> pNodeMap, P pProcedure) {

    Node<P> node = pNodeMap.computeIfAbsent(pProcedure, key -> new Node<>(pNodes.size(), key));

    if (pNodeMap.size() > pNodes.size()) {
      pNodes.add(node);
    }

    return node;
  }

  private static <P> void insertEdge(
      List<Node<P>> pNodes, Map<P, Node<P>> pNodeMap, P pPredecessor, P pSuccessor) {

    Node<P> predecessorCallNode = node(pNodes, pNodeMap, pPredecessor);
    Node<P> successorCallNode = node(pNodes, pNodeMap, pSuccessor);

    predecessorCallNode.addSuccessor(successorCallNode);
    successorCallNode.addPredecessor(predecessorCallNode);
  }

  public static <P, N> CallGraph<P> createCallGraph(
      Function<N, Iterable<Edge<P, N>>> pLeavingEdgesFunction, Collection<N> pStartNodes) {

    List<Node<P>> nodes = new ArrayList<>();
    Map<P, Node<P>> nodeMap = new HashMap<>();
    Deque<N> waitlist = new ArrayDeque<>();
    Set<N> waitlisted = new HashSet<>();

    for (N startNode : pStartNodes) {
      if (waitlisted.add(startNode)) {
        waitlist.add(startNode);
      }
    }

    while (!waitlisted.isEmpty()) {

      N node = waitlist.remove();

      for (Edge<P, N> edge : pLeavingEdgesFunction.apply(node)) {

        if (edge.getType() == Edge.Type.CALL_EDGE) {
          insertEdge(nodes, nodeMap, edge.getPredecessorProcedure(), edge.getSuccessorProcedure());
        } else if (edge.getType() == Edge.Type.RETURN_EDGE) {
          insertEdge(nodes, nodeMap, edge.getSuccessorProcedure(), edge.getPredecessorProcedure());
        }

        N successor = edge.getSucessor();
        if (waitlisted.add(successor)) {
          waitlist.add(successor);
        }
      }
    }

    for (Node<P> node : nodes) {
      node.finish();
    }

    return new CallGraph<>(ImmutableList.copyOf(nodes), ImmutableMap.copyOf(nodeMap));
  }

  public Optional<Node<P>> getNode(P pProcedure) {
    return Optional.ofNullable(nodeMap.get(pProcedure));
  }

  public Node<P> getNodeById(int pId) {
    return nodes.get(pId);
  }

  public ImmutableSet<P> getRecursiveProcedures() {

    Deque<Node<P>> waitlist = new ArrayDeque<>();
    Multimap<Node<P>, Node<P>> callers = HashMultimap.create();

    waitlist.addAll(nodes);

    while (!waitlist.isEmpty()) {

      Node<P> caller = waitlist.remove();

      for (Node<P> callee : caller.getSuccessors()) {
        if (callers.put(callee, caller)) {
          waitlist.add(callee);
        }
      }
    }

    Set<P> recursiveProcedures = new HashSet<>();
    for (Node<P> node : nodes) {
      if (callers.containsEntry(node, node)) {
        recursiveProcedures.add(node.getProcedure());
      }
    }

    return ImmutableSet.copyOf(recursiveProcedures);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + nodes.toString();
  }

  public static final class Edge<P, N> {

    private final Type type;

    private final P predecessorProcedure;
    private final P successorProcedure;
    private final N sucessor;

    private Edge(Type pType, P pPredecessorProcedure, P pSuccessorProcedure, N pSucessor) {

      type = pType;

      predecessorProcedure = pPredecessorProcedure;
      successorProcedure = pSuccessorProcedure;
      sucessor = pSucessor;
    }

    public static <P, N> Edge<P, N> createStandardEdge(P pProcedure, N pSuccessor) {
      return new Edge<>(Type.STANDARD_EDGE, pProcedure, pProcedure, pSuccessor);
    }

    public static <P, N> Edge<P, N> createCallEdge(
        P pCallerProcedure, P pCalleeProcedure, N pSuccessor) {
      return new Edge<>(Type.CALL_EDGE, pCallerProcedure, pCalleeProcedure, pSuccessor);
    }

    public static <P, N> Edge<P, N> createReturnEdge(
        P pCalleeProcedure, P pCallerProcedure, N pSuccessor) {
      return new Edge<>(Type.RETURN_EDGE, pCalleeProcedure, pCallerProcedure, pSuccessor);
    }

    private Type getType() {
      return type;
    }

    private P getPredecessorProcedure() {
      return predecessorProcedure;
    }

    private N getSucessor() {
      return sucessor;
    }

    private P getSuccessorProcedure() {
      return successorProcedure;
    }

    private enum Type {
      STANDARD_EDGE,
      CALL_EDGE,
      RETURN_EDGE;
    }
  }

  public static final class Node<P> {

    private int id;
    private final P procedure;

    private List<Node<P>> predecessors;
    private List<Node<P>> successors;

    private Node(int pId, P pProcedure) {

      id = pId;
      procedure = pProcedure;

      predecessors = new ArrayList<>();
      successors = new ArrayList<>();
    }

    private void addPredecessor(Node<P> pNode) {
      predecessors.add(pNode);
    }

    private void addSuccessor(Node<P> pNode) {
      successors.add(pNode);
    }

    private void finish() {

      predecessors = ImmutableList.copyOf(predecessors);
      successors = ImmutableList.copyOf(successors);
    }

    public int getId() {
      return id;
    }

    public P getProcedure() {
      return procedure;
    }

    public ImmutableList<Node<P>> getPredecessors() {
      return ImmutableList.copyOf(predecessors);
    }

    public ImmutableList<Node<P>> getSuccessors() {
      return ImmutableList.copyOf(predecessors);
    }

    @Override
    public String toString() {

      StringBuilder sb = new StringBuilder();

      sb.append(CallGraph.class.getSimpleName());
      sb.append('.');
      sb.append(getClass().getSimpleName());
      sb.append('[');

      sb.append("id=");
      sb.append(id);
      sb.append(',');

      sb.append("procedure='");
      sb.append(procedure.toString());
      sb.append("',");

      sb.append("predecessors=");
      List<P> predecessorProcedures =
          predecessors.stream().map(Node::getProcedure).collect(Collectors.toList());
      sb.append(predecessorProcedures.toString());
      sb.append(',');

      sb.append("successors=");
      List<P> successorProcedures =
          successors.stream().map(Node::getProcedure).collect(Collectors.toList());
      sb.append(successorProcedures.toString());

      sb.append(']');

      return sb.toString();
    }
  }
}
