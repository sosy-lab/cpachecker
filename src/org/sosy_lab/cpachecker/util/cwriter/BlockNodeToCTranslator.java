// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.cpa.invariants.CompoundBitVectorIntervalManagerFactory;
import org.sosy_lab.cpachecker.cpa.invariants.EdgeAnalyzer;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.FunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.Statement.SimpleStatement;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class BlockNodeToCTranslator extends AbstractToCTranslator {

  public BlockNodeToCTranslator(Configuration pConfig) throws InvalidConfigurationException {
    super(pConfig);
  }

  public String translateBlockNode(BlockNode blockNode, CFA pCfa)
      throws CPAException, InvalidConfigurationException, IOException {
    createdStatements.clear();
    globalDefinitionsList.clear();
    functions.clear();

    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be written to C for C programs, at the moment");
    }

    // the final C program may contain `abort()` statements, so we need a suitable declaration
    globalDefinitionsList.add("extern void abort();");
    translate(blockNode, pCfa);

    return generateCCode();
  }

  private void translate(BlockNode blockNode, CFA cfa) throws CPAException {
    Deque<NodeAndBlock> waitlist = new ArrayDeque<>();
    Multimap<CFANode, NodeAndBlock> incomingBlocks = HashMultimap.create();

    CompoundStatement rootBlock = new CompoundStatement(null);
    FunctionDefinition f = new FunctionDefinition("main()", rootBlock);
    functions.add(f);

    EdgeAnalyzer edgeAnalyzer =
        new EdgeAnalyzer(
            CompoundBitVectorIntervalManagerFactory.forbidSignedWrapAround(),
            cfa.getMachineModel());

    Map<String, CType> undeclaredVariables =
        collectUndeclaredLocalVariableTypes(blockNode, edgeAnalyzer);
    addMissingVariableDeclarations(rootBlock, undeclaredVariables);

    pushToWaitlist(waitlist, new NodeAndBlock(blockNode.getInitialLocation(), rootBlock));

    while (!waitlist.isEmpty()) {
      NodeAndBlock current = getNextElement(waitlist);
      CFANode currentNode = current.getNode();

      if (createdStatements.containsKey(currentNode)) {
        current.getCurrentBlock().addStatement(createGoto(currentNode, currentNode));
        continue;
      }

      CompoundStatement originalBlock = current.getCurrentBlock();
      CompoundStatement currentBlock =
          getBlockToContinueWith(currentNode, originalBlock, incomingBlocks.get(currentNode));

      if (currentBlock != originalBlock) {
        current = new NodeAndBlock(currentNode, currentBlock);
      }

      Collection<NodeAndBlock> nextNodes = handleNode(currentNode, currentBlock, blockNode);
      for (NodeAndBlock next : nextNodes) {
        CFANode nextNode = next.getNode();
        incomingBlocks.put(nextNode, current);
        pushToWaitlist(waitlist, next);
      }
    }
  }

  private Map<String, CType> collectUndeclaredLocalVariableTypes(
      BlockNode blockNode, EdgeAnalyzer edgeAnalyzer) {
    Map<String, CType> localTypes = new HashMap<>();

    for (CFAEdge edge : blockNode.getEdges()) {
      Map<MemoryLocation, CType> edgeVars = edgeAnalyzer.getInvolvedVariableTypes(edge);
      for (Map.Entry<MemoryLocation, CType> entry : edgeVars.entrySet()) {
        String varName = entry.getKey().getIdentifier();
        localTypes.put(varName, entry.getValue());
      }
    }

    Set<String> declaredVariables = new HashSet<>();
    for (CFAEdge edge : blockNode.getEdges()) {
      if (edge instanceof CDeclarationEdge) {
        CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
        if (decl instanceof CVariableDeclaration) {
          declaredVariables.add(((CVariableDeclaration) decl).getName());
        }
      }
    }

    localTypes.keySet().removeAll(declaredVariables);
    return localTypes;
  }

  private void addMissingVariableDeclarations(
      CompoundStatement blockBody, Map<String, CType> variableTypes) {
    for (Map.Entry<String, CType> entry : variableTypes.entrySet()) {
      String varName = entry.getKey();
      CType type = entry.getValue();
      if (type.toString().equals("int")) {
        String initStmt = "int " + varName + " = __VERIFIER_nondet_int();";
        blockBody.addStatement(new SimpleStatement(initStmt));
      }
    }
  }

  private boolean isEdgeInsideBlock(CFAEdge edge, BlockNode block) {
    return block.getEdges().contains(edge)
        && block.getNodes().contains(edge.getPredecessor())
        && block.getNodes().contains(edge.getSuccessor());
  }

  private ImmutableCollection<NodeAndBlock> handleNode(
      CFANode node, CompoundStatement block, BlockNode containerBlock) throws CPAException {

    ImmutableList.Builder<NodeAndBlock> nextNodes = ImmutableList.builder();

    if (node instanceof CFATerminationNode || node.getNumLeavingEdges() == 0) {
      block.addStatement(createSimpleStatement(node, "abort();"));
      return ImmutableList.of();
    }

    if (node instanceof CFALabelNode) {
      block.addStatement(createLabel((CFALabelNode) node));
    }

    Collection<Pair<CFAEdge, CompoundStatement>> outgoingEdges =
        handlePotentialBranching(node, block, containerBlock);

    for (Pair<CFAEdge, CompoundStatement> pair : outgoingEdges) {
      CFAEdge edge = pair.getFirst();
      CompoundStatement currentBlock = pair.getSecond();

      if (!isEdgeInsideBlock(edge, containerBlock)) {
        continue;
      }

      if (!(edge instanceof CAssumeEdge)) {
        String stmt = translateSimpleEdge(edge);
        if (!stmt.isEmpty()) {
          currentBlock.addStatement(createSimpleStatement(node, stmt, edge));
        }
      }

      CFANode successor = getSuccessorNode(edge);
      if (containerBlock.getNodes().contains(successor)) {
        nextNodes.add(new NodeAndBlock(successor, currentBlock));
      }
    }

    if (!createdStatements.containsKey(node)) {
      block.addStatement(createEmptyStatement(node));
    }

    return nextNodes.build();
  }

  private ImmutableCollection<Pair<CFAEdge, CompoundStatement>> handlePotentialBranching(
      CFANode node, CompoundStatement startBlock, BlockNode containerBlock) {

    Collection<CFAEdge> outgoingEdges =
        getRelevantLeavingEdges(node).filter(e -> isEdgeInsideBlock(e, containerBlock)).toList();
    if (outgoingEdges.isEmpty()) {
      return ImmutableSet.of();
    }
    if (outgoingEdges.size() == 1) {
      CFAEdge edge = outgoingEdges.iterator().next();
      if (edge instanceof CAssumeEdge assume) {
        String condition =
            assume.getTruthAssumption()
                ? assume.getExpression().toASTString(AAstNodeRepresentation.DEFAULT)
                : "!(" + assume.getExpression().toASTString(AAstNodeRepresentation.DEFAULT) + ")";

        CompoundStatement newBlock =
            addIfStatementToBlock(node, startBlock, "if (" + condition + ")", edge);
        return ImmutableSet.of(Pair.of(edge, newBlock));
      }
      return ImmutableSet.of(Pair.of(edge, startBlock));
    }

    assert outgoingEdges.size() == 2;
    for (CFAEdge edge : outgoingEdges) {
      assert edge instanceof CAssumeEdge;
    }

    ImmutableList.Builder<Pair<CFAEdge, CompoundStatement>> branches = ImmutableList.builder();
    List<CFAEdge> edges = new ArrayList<>(outgoingEdges);

    if (!getRealTruthAssumption((CAssumeEdge) edges.get(0))) {
      edges = swapElements(edges);
    }

    for (CFAEdge edge : edges) {
      CAssumeEdge assume = (CAssumeEdge) edge;
      String condition =
          assume.getTruthAssumption()
              ? "if (" + assume.getExpression().toASTString(AAstNodeRepresentation.DEFAULT) + ")"
              : "if (!("
                  + assume.getExpression().toASTString(AAstNodeRepresentation.DEFAULT)
                  + "))";

      if (edges.get(1) == edge) {
        condition = "else";
      }

      CompoundStatement newBlock = addIfStatementToBlock(node, startBlock, condition, edge);
      branches.add(Pair.of(edge, newBlock));
    }

    return branches.build();
  }

  private void pushToWaitlist(Deque<NodeAndBlock> pWaitlist, NodeAndBlock pNodeAndBlock) {
    // Make sure that join nodes are always handled soon-ishly by pushing them to the waitlist;
    // to keep the structure of the generated program as shallow as possible.
    if (hasMoreThanOneElement(getPredecessorNodes(pNodeAndBlock.getNode()))) {
      pWaitlist.push(pNodeAndBlock);
    } else {
      pWaitlist.offer(pNodeAndBlock);
    }
  }
}
