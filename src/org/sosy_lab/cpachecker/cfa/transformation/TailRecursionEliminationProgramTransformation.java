// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.graph.Traverser;
import java.util.HashMap;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;

public class TailRecursionEliminationProgramTransformation extends ProgramTransformation{
  public ProgramTransformationBehaviour behaviour = ProgramTransformationBehaviour.PRECISE;

  @Override
  public Optional<SubCFA> transform(CFA pCFA, CFANode pNode) {

    // TODO check transformation conditions
    // check 1: are we at the start of a function with a return node
    if (!(pNode instanceof FunctionEntryNode functionEntryNode)) {
      return Optional.empty();
    }
    if (functionEntryNode.getExitNode().isEmpty()) {
      return Optional.empty();
    }
    CFANode exitNode = functionEntryNode.getExitNode().get();
    String functionName = pNode.getFunctionName();
    // check 2: is the last operation a recursive function call
    // i.e. declaration of a __CPAchecker_TMP_0 variable + assignment with the recursive function call + return of this variable
    FluentIterable<CFAEdge> enteringEdges = exitNode.getEnteringEdges();
    for(CFAEdge edge : enteringEdges) {
      CReturnStatement returnStatement = ((CReturnStatementEdge) edge).getReturnStatement();
      if (returnStatement.getReturnValue().isEmpty()) {
        return  Optional.empty();
      }
      CExpression returnExpression = returnStatement.getReturnValue().get();
      if (returnExpression instanceof CLeftHandSide returnLeftHandSide) {
        if (returnLeftHandSide instanceof CIdExpression returnIdExpression) {
          // check for __CPAchecker_TMP_0
          if(returnIdExpression.getName().equals("__CPAchecker_TMP_0")) {

          }
        } else {
          return Optional.empty();
        }
      } else {
        return Optional.empty();
      }
    }
    // check 3: the first statement must be a conditional check of the exit condition
    // i.e. 1 Function start dummy edge followed by a node with two assertion edges

    // TODO perform transformation
    CFANode newEntryNode = null;
    CFANode newExitNode = null;
    ImmutableList.Builder<CFANode> nodes = ImmutableList.builder();
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    HashMap<Integer, Integer> nodeMap = new HashMap<>();
    Traverser<CFANode> cfaNetworkTraverser = Traverser.forGraph(pCFA.asGraph());
    Iterable<CFANode> cfaNodeIterable = cfaNetworkTraverser.breadthFirst(pNode);
    ImmutableList<? extends AParameterDeclaration> parameters = pNode.getFunction().getParameters();
    cfaNodeIterable = Iterables.filter(cfaNodeIterable, (CFANode node) -> {
      assert node != null;
      return node.getFunctionName().equals(functionName);
    });

    // add new nodes
    for(CFANode currentNode : cfaNodeIterable) {
      CFANode newNode = CFANode.newDummyCFANode(functionName);
      nodeMap.put(currentNode.getNodeNumber(), newNode.getNodeNumber());
      if (currentNode.getNodeNumber() == exitNode.getNodeNumber()) {
        newExitNode = newNode;
      } else if (currentNode.getNodeNumber() == pNode.getNodeNumber()) {
        newNode.setLoopStart();
        newEntryNode = newNode;
      }
      nodes.add(newNode);
    }
    for (int i = 0; i < parameters.size(); i++) {
      CFANode newNode = CFANode.newDummyCFANode(functionName);
      nodes.add(newNode);
    }
    ImmutableList<CFANode> nodesList = nodes.build();

    // add new edges
    for(CFANode currentNode : cfaNodeIterable) {  // TODO can Iterables be reused??
      for(CFAEdge currentEdge : currentNode.getAllLeavingEdges()){
        if (currentEdge.getSuccessor().getFunctionName().equals(functionName)) {
          // TODO check if edge is the recursive function call
          // i.e. the declaration of __CPAchecker_TMP_0
          // TODO if true skip edge copying and add new edges for parameter nodes
          int newPredecessorNodeIndex = getNodeIndex(nodeMap.get(currentNode.getNodeNumber()), nodesList).orElseThrow();
          Optional<Integer> newSuccessorNodeIndex = getNodeIndex(nodeMap.get(currentEdge.getSuccessor().getNodeNumber()), nodesList);
          if (newSuccessorNodeIndex.isPresent()) {
            edges.add(
                ProgramTransformationCFAEdgeCreator.copyCFAEdge(
                    currentEdge, nodesList.get(newPredecessorNodeIndex), nodesList.get(newSuccessorNodeIndex.get())));
          } else {
            // C does not allow jumps out of functions
            return Optional.empty();
          }
        }
      }
    }
    ImmutableList<CFAEdge> edgesList = edges.build();


    return Optional.of(new SubCFA(
        pNode,
        exitNode,
        newEntryNode,
        newExitNode,
        ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION,
        ProgramTransformationBehaviour.PRECISE,
        ImmutableSet.copyOf(nodesList),
        ImmutableSet.copyOf(edgesList)
    ));
  }

  private static Optional<Integer> getNodeIndex(int nodeNumber, @NonNull ImmutableList<CFANode> nodeList) {
    for (int i = 0; i < nodeList.size(); i++) {
      if (nodeList.get(i).getNodeNumber() == nodeNumber) {
        return Optional.of(i);
      }
    }
    return Optional.empty();
  }
}
