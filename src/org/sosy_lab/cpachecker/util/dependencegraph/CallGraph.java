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

  private final ImmutableList<Node.ImmutableNode<P>> nodes;
  private final ImmutableMap<P, Node.ImmutableNode<P>> nodeMap;

  private CallGraph(
      ImmutableList<Node.ImmutableNode<P>> pNodes,
      ImmutableMap<P, Node.ImmutableNode<P>> pNodeMap) {

    nodes = pNodes;
    nodeMap = pNodeMap;
  }

  private static <P> Node.MutableNode<P> createNodeIfAbsent(
      List<Node.MutableNode<P>> pNodes, Map<P, Node.MutableNode<P>> pNodeMap, P pProcedure) {

    Node.MutableNode<P> node =
        pNodeMap.computeIfAbsent(pProcedure, key -> new Node.MutableNode<>(pNodes.size(), key));

    if (pNodeMap.size() > pNodes.size()) {
      pNodes.add(node);
    }

    return node;
  }

  private static <P> void insertEdge(
      List<Node.MutableNode<P>> pNodes,
      Map<P, Node.MutableNode<P>> pNodeMap,
      P pPredecessor,
      P pSuccessor) {

    Node.MutableNode<P> predecessorCallNode = createNodeIfAbsent(pNodes, pNodeMap, pPredecessor);
    Node.MutableNode<P> successorCallNode = createNodeIfAbsent(pNodes, pNodeMap, pSuccessor);

    predecessorCallNode.addSuccessor(successorCallNode);
    successorCallNode.addPredecessor(predecessorCallNode);
  }

  static <P, N> CallGraph<P> createCallGraph(
      Function<? super N, ? extends Iterable<? extends SuccessorResult<? extends P, ? extends N>>>
          pSuccessorFunction,
      Collection<? extends N> pStartNodes) {

    List<Node.MutableNode<P>> nodes = new ArrayList<>();
    Map<P, Node.MutableNode<P>> nodeMap = new HashMap<>();
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

    ImmutableList.Builder<Node.ImmutableNode<P>> immutableNodesBuilder =
        ImmutableList.builderWithExpectedSize(nodes.size());
    List<ImmutableList.Builder<Node<P>>> immutablePredecessors = new ArrayList<>(nodes.size());
    List<ImmutableList.Builder<Node<P>>> immutableSuccessors = new ArrayList<>(nodes.size());
    ImmutableMap.Builder<P, Node.ImmutableNode<P>> immutableNodeMapBuilder =
        ImmutableMap.builderWithExpectedSize(nodeMap.size());

    for (Node.MutableNode<P> mutableNode : nodes) {
      Node.ImmutableNode<P> immutableNode =
          new Node.ImmutableNode<>(mutableNode.getId(), mutableNode.getProcedure());
      immutableNodesBuilder.add(immutableNode);
      immutablePredecessors.add(
          ImmutableList.builderWithExpectedSize(mutableNode.getPredecessors().size()));
      immutableSuccessors.add(
          ImmutableList.builderWithExpectedSize(mutableNode.getSuccessors().size()));
      immutableNodeMapBuilder.put(immutableNode.getProcedure(), immutableNode);
    }

    ImmutableList<Node.ImmutableNode<P>> immutableNodes = immutableNodesBuilder.build();

    for (Node.MutableNode<P> mutablePredecessorNode : nodes) {
      int predecessorId = mutablePredecessorNode.getId();
      for (Node<P> mutableSuccessorNode : mutablePredecessorNode.getSuccessors()) {
        int successorId = mutableSuccessorNode.getId();
        immutablePredecessors.get(successorId).add(immutableNodes.get(predecessorId));
        immutableSuccessors.get(predecessorId).add(immutableNodes.get(successorId));
      }
    }

    for (int index = 0; index < immutableNodes.size(); index++) {
      Node.ImmutableNode<P> immutableNode = immutableNodes.get(index);
      immutableNode.predecessors = immutablePredecessors.get(index).build();
      immutableNode.successors = immutableSuccessors.get(index).build();
    }

    return new CallGraph<>(immutableNodes, immutableNodeMapBuilder.buildOrThrow());
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

      for (Node<P> callee : caller.getSuccessors()) {
        if (stacked.add(callee)) {
          stack.push(callee);
          continue outer;
        }
      }

      postOrderList.add(stack.pop().getProcedure());
    }

    return ImmutableList.copyOf(Lists.reverse(postOrderList));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + nodes;
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

  abstract static class Node<P> {

    private int id;
    private final P procedure;

    private Node(int pId, P pProcedure) {

      id = pId;
      procedure = pProcedure;
    }

    final int getId() {
      return id;
    }

    final P getProcedure() {
      return procedure;
    }

    abstract ImmutableList<Node<P>> getPredecessors();

    abstract ImmutableList<Node<P>> getSuccessors();

    @Override
    public final String toString() {

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
          getPredecessors().stream()
              .map(Node::getProcedure)
              .collect(ImmutableList.toImmutableList());
      sb.append(predecessorProcedures.toString());
      sb.append(',');

      sb.append("successors=");
      List<P> successorProcedures =
          getSuccessors().stream().map(Node::getProcedure).collect(ImmutableList.toImmutableList());
      sb.append(successorProcedures.toString());

      sb.append(']');

      return sb.toString();
    }

    private static final class MutableNode<P> extends Node<P> {

      private final List<Node<P>> predecessors;
      private final List<Node<P>> successors;

      private MutableNode(int pId, P pProcedure) {
        super(pId, pProcedure);

        predecessors = new ArrayList<>();
        successors = new ArrayList<>();
      }

      @Override
      ImmutableList<Node<P>> getPredecessors() {
        return ImmutableList.copyOf(predecessors);
      }

      private void addPredecessor(Node<P> pNode) {
        predecessors.add(pNode);
      }

      @Override
      ImmutableList<Node<P>> getSuccessors() {
        return ImmutableList.copyOf(successors);
      }

      private void addSuccessor(Node<P> pNode) {
        successors.add(pNode);
      }
    }

    private static final class ImmutableNode<P> extends Node<P> {

      private ImmutableList<Node<P>> predecessors;
      private ImmutableList<Node<P>> successors;

      private ImmutableNode(int pId, P pProcedure) {
        super(pId, pProcedure);
      }

      @Override
      ImmutableList<Node<P>> getPredecessors() {
        return predecessors;
      }

      @Override
      ImmutableList<Node<P>> getSuccessors() {
        return successors;
      }
    }
  }
}
