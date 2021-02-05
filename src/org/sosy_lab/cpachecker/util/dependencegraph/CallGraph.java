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
import com.google.common.collect.Lists;
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

/**
 * Class representing a call graph.
 *
 * @param <P> the procedure type of the call graph.
 */
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

  static <P, N> CallGraph<P> createCallGraph(
      Function<? super N, ? extends Iterable<? extends SuccessorResult<? extends P, ? extends N>>>
          pSuccessorFunction,
      Collection<? extends N> pStartNodes) {

    List<Node<P>> nodes = new ArrayList<>();
    Map<P, Node<P>> nodeMap = new HashMap<>();
    Deque<N> waitlist = new ArrayDeque<>();
    Set<N> waitlisted = new HashSet<>();

    for (N startNode : pStartNodes) {
      if (waitlisted.add(startNode)) {
        waitlist.add(startNode);
      }
    }

    while (!waitlist.isEmpty()) {

      N node = waitlist.remove();

      for (SuccessorResult<? extends P, ? extends N> edge : pSuccessorFunction.apply(node)) {

        if (edge.getType() == SuccessorResult.Type.CALL_EDGE) {
          insertEdge(nodes, nodeMap, edge.getPredecessorProcedure(), edge.getSuccessorProcedure());
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

  Optional<Node<P>> getNode(P pProcedure) {
    return Optional.ofNullable(nodeMap.get(pProcedure));
  }

  Node<P> getNodeById(int pId) {
    return nodes.get(pId);
  }

  ImmutableSet<P> getReachableFrom(Collection<? extends P> pProcedures) {

    Deque<Node<P>> waitlist = new ArrayDeque<>();
    Set<Node<P>> waitlisted = new HashSet<>();

    for (P procedure : pProcedures) {
      Node<P> node = nodeMap.get(procedure);
      if (node != null && waitlisted.add(node)) {
        waitlist.add(node);
      }
    }

    while (!waitlist.isEmpty()) {
      for (Node<P> successor : waitlist.remove().getSuccessors()) {
        if (waitlisted.add(successor)) {
          waitlist.add(successor);
        }
      }
    }

    ImmutableSet.Builder<P> builder = ImmutableSet.builderWithExpectedSize(waitlisted.size());
    waitlisted.forEach(node -> builder.add(node.getProcedure()));

    return builder.build();
  }

  ImmutableSet<P> getRecursiveProcedures() {

    Deque<Node<P>> waitlist = new ArrayDeque<>();
    Multimap<Node<P>, Node<P>> callers = HashMultimap.create(); // node -> its (transitive) callers

    // add direct callers
    for (Node<P> caller : nodes) {
      for (Node<P> callee : caller.getSuccessors()) {
        if (callers.put(callee, caller)) {
          waitlist.add(callee);
        }
      }
    }

    // iterate until fixpoint is reached
    while (!waitlist.isEmpty()) {

      Node<P> node = waitlist.remove();
      Collection<Node<P>> nodeCallers = callers.get(node);

      for (Node<P> nodeCallee : node.getSuccessors()) {
        if (callers.putAll(nodeCallee, nodeCallers)) {
          waitlist.add(nodeCallee);
        }
      }
    }

    // find nodes that are their own (transitive) caller
    Set<P> recursiveProcedures = new HashSet<>();
    for (Node<P> node : nodes) {
      if (callers.containsEntry(node, node)) {
        recursiveProcedures.add(node.getProcedure());
      }
    }

    return ImmutableSet.copyOf(recursiveProcedures);
  }

  ImmutableList<P> getPostOrder(P pStart) {

    Node<P> startNode = nodeMap.get(pStart);

    if (startNode == null) {
      return ImmutableList.of();
    }

    List<P> postOrderList = new ArrayList<>();
    Deque<Node<P>> stack = new ArrayDeque<>();
    Set<Node<P>> stacked = new HashSet<>();

    stack.push(startNode);
    stacked.add(startNode);

    outer:
    while (!stack.isEmpty()) {

      Node<P> caller = stack.peek();

      for (Node<P> calee : caller.getSuccessors()) {
        if (stacked.add(calee)) {
          stack.push(calee);
          continue outer;
        }
      }

      postOrderList.add(stack.pop().getProcedure());
    }

    return ImmutableList.copyOf(Lists.reverse(postOrderList));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + nodes.toString();
  }

  static final class SuccessorResult<P, N> {

    private final Type type;

    private final P predecessorProcedure;
    private final P successorProcedure;
    private final N sucessor;

    private SuccessorResult(
        Type pType, P pPredecessorProcedure, P pSuccessorProcedure, N pSucessor) {

      type = pType;

      predecessorProcedure = pPredecessorProcedure;
      successorProcedure = pSuccessorProcedure;
      sucessor = pSucessor;
    }

    static <P, N> SuccessorResult<P, N> createNonCallSuccessor(P pProcedure, N pSuccessor) {
      return new SuccessorResult<>(Type.NON_CALL_EDGE, pProcedure, pProcedure, pSuccessor);
    }

    static <P, N> SuccessorResult<P, N> createCallSuccessor(
        P pCallerProcedure, P pCalleeProcedure, N pSuccessor) {
      return new SuccessorResult<>(Type.CALL_EDGE, pCallerProcedure, pCalleeProcedure, pSuccessor);
    }

    private Type getType() {
      return type;
    }

    private P getPredecessorProcedure() {
      return predecessorProcedure;
    }

    private P getSuccessorProcedure() {
      return successorProcedure;
    }

    private N getSucessor() {
      return sucessor;
    }

    private enum Type {
      NON_CALL_EDGE,
      CALL_EDGE;
    }
  }

  static final class Node<P> {

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

    int getId() {
      return id;
    }

    P getProcedure() {
      return procedure;
    }

    ImmutableList<Node<P>> getPredecessors() {
      return ImmutableList.copyOf(predecessors);
    }

    ImmutableList<Node<P>> getSuccessors() {
      return ImmutableList.copyOf(successors);
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
          predecessors.stream().map(Node::getProcedure).collect(ImmutableList.toImmutableList());
      sb.append(predecessorProcedures.toString());
      sb.append(',');

      sb.append("successors=");
      List<P> successorProcedures =
          successors.stream().map(Node::getProcedure).collect(ImmutableList.toImmutableList());
      sb.append(successorProcedures.toString());

      sb.append(']');

      return sb.toString();
    }
  }
}
