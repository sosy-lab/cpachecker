// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.tree;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BlockNode {

  private final String id;

  private final CFANode startNode;
  private final CFANode lastNode;
  private final Set<CFANode> nodesInBlock;

  private final Set<BlockNode> predecessors;
  private final Set<BlockNode> successors;

  private final BooleanFormulaManagerView bmgr;
  private BooleanFormula precondition;
  private BooleanFormula postcondition;

  private final ConcurrentMap<String, BooleanFormula> preconditionUpdates;
  private final ConcurrentMap<String, BooleanFormula> postconditionUpdates;

  public BlockNode(@NonNull CFANode pStartNode, @NonNull CFANode pLastNode, @NonNull Set<CFANode> pNodesInBlock, BooleanFormulaManagerView pBmgr) {
    startNode = pStartNode;
    lastNode = pLastNode;

    predecessors = new HashSet<>();
    successors = new HashSet<>();

    nodesInBlock = new LinkedHashSet<>(pNodesInBlock);
    bmgr = pBmgr;

    // make sure that no node is missing, set removes duplicates automatically
    nodesInBlock.add(pStartNode);
    nodesInBlock.add(pLastNode);

    precondition = bmgr.makeTrue();
    postcondition = bmgr.makeTrue();

    id = nodesInBlock.stream().map(CFANode::toString).collect(Collectors.joining(","));
    preconditionUpdates = new ConcurrentHashMap<>();
    postconditionUpdates = new ConcurrentHashMap<>();
  }

  public void sendPostconditionToSuccessors() {
    successors.forEach(node -> node.receivePrecondition(id, postcondition));
  }

  public void sendPreconditionToPredecessors() {
    predecessors.forEach(node -> node.receivePostcondition(id, precondition));
  }

  private void receivePrecondition(String from, BooleanFormula precond) {
    preconditionUpdates.put(from, precond);
  }

  private void receivePostcondition(String from, BooleanFormula postcond) {
    postconditionUpdates.put(from, postcond);
  }

  public void setPrecondition(BooleanFormula precond) {
    precondition = precond;
  }

  public void setPostcondition(BooleanFormula postcond) {
    postcondition = postcond;
  }

  public void linkSuccessor(BlockNode node) {
    addSuccessors(node);
    node.addPredecessors(this);
  }

  private void addPredecessors(BlockNode... pred) {
    predecessors.addAll(ImmutableList.copyOf(pred));
  }

  private void addSuccessors(BlockNode... succ) {
    successors.addAll(ImmutableList.copyOf(succ));
  }

  public Set<BlockNode> getPredecessors() {
    return predecessors;
  }

  public Set<BlockNode> getSuccessors() {
    return successors;
  }

  public CFANode getStartNode() {
    return startNode;
  }

  public CFANode getLastNode() {
    return lastNode;
  }

  public ConcurrentMap<String, BooleanFormula> getPostconditionUpdates() {
    return postconditionUpdates;
  }

  public ConcurrentMap<String, BooleanFormula> getPreconditionUpdates() {
    return preconditionUpdates;
  }

  public String getId() {
    return id;
  }

  public Set<CFANode> getNodesInBlock() {
    return nodesInBlock;
  }

  public BooleanFormulaManagerView getBmgr() {
    return bmgr;
  }

  @Override
  public boolean equals(Object pO) {
    if (pO instanceof  BlockNode) {
      BlockNode blockNode = (BlockNode) pO;
      return startNode.equals(blockNode.startNode) && lastNode.equals(blockNode.lastNode)
          && nodesInBlock.equals(blockNode.nodesInBlock);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startNode, lastNode, nodesInBlock);
  }

  @Override
  public String toString() {
    return "BlockNode{" +
        "startNode=" + startNode +
        ", lastNode=" + lastNode +
        ", nodesInBlock=" + nodesInBlock +
        '}';
  }
}
