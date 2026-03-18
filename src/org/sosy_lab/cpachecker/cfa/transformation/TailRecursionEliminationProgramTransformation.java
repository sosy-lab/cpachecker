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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class TailRecursionEliminationProgramTransformation extends ProgramTransformation{

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
    // check 2: is one of the return statements a recursive function call
    // i.e. declaration of a __CPAchecker_TMP_0 variable + assignment with the recursive function call + return of this variable
    boolean isTailRecursive = false;
    Optional<String> tmpVarName = Optional.empty();
    Optional<CFAEdge> tmpVarDeclarationEdge = Optional.empty();
    Optional<CFAEdge> tmpVarAssignmentEdge = Optional.empty();
    Optional<CFAEdge> tmpVarReturnEdge = Optional.empty();
    FluentIterable<CFAEdge> enteringEdges = exitNode.getEnteringEdges();
    for(CFAEdge edge : enteringEdges) {
      CReturnStatement returnStatement = ((CReturnStatementEdge) edge).getReturnStatement();
      if (returnStatement.getReturnValue().isPresent()) {
        CExpression returnExpression = returnStatement.getReturnValue().get();
        if (returnExpression instanceof CLeftHandSide returnLeftHandSide) {
          if (returnLeftHandSide instanceof CIdExpression returnIdExpression) {
            //if(returnIdExpression.getName().equals("__CPAchecker_TMP_0")) {
              FluentIterable<CFAEdge> predecessorEdges = edge.getPredecessor().getEnteringEdges();
              if (predecessorEdges.size() == 1) {
                CFAEdge predecessorEdge = predecessorEdges.first().get();
                if (predecessorEdge instanceof CStatementEdge predecessorStatementEdge) {
                  if (predecessorStatementEdge.getStatement() instanceof CFunctionCallAssignmentStatement predecessorFunctionCallAssignmentStatement) {
                    if(predecessorFunctionCallAssignmentStatement.getFunctionCallExpression().getDeclaration().getQualifiedName().equals(functionName)){
                      if (predecessorEdge.getPredecessor().getEnteringEdges().size() == 1) {
                        if (predecessorEdge.getPredecessor().getEnteringEdges().first().get() instanceof CDeclarationEdge) {
                          tmpVarName = Optional.of(returnIdExpression.getName());
                          tmpVarDeclarationEdge = Optional.of(predecessorEdge.getPredecessor().getEnteringEdges().first().get());
                          tmpVarAssignmentEdge = Optional.of(predecessorEdge);
                          tmpVarReturnEdge = Optional.of(edge);
                          isTailRecursive = true;
                          break;
                        }
                      }
                    }
                  }
                }
              }
            //}
          }
        }
      }
    }
    if (!isTailRecursive || tmpVarName.isEmpty() || tmpVarDeclarationEdge.isEmpty() || tmpVarAssignmentEdge.isEmpty() || tmpVarReturnEdge.isEmpty()) {
      return Optional.empty();
    }
    // check 3: the first statement must be a conditional check of the exit condition
    // i.e. 1 Function start dummy edges followed by a node with two assertion edges
    if (pNode.getLeavingEdges().size() != 1) {
      return Optional.empty();
    } else {
      CFAEdge currentFunctionStartEdge = pNode.getLeavingEdges().first().get();
      FluentIterable<CFAEdge> nextFunctionStartEdges = currentFunctionStartEdge.getSuccessor().getLeavingEdges();
      if (currentFunctionStartEdge.getEdgeType() != CFAEdgeType.BlankEdge) {
        return Optional.empty();
      }
      if (nextFunctionStartEdges.size() != 1) {
        return Optional.empty();
      } else {
        currentFunctionStartEdge = nextFunctionStartEdges.first().get();
        nextFunctionStartEdges = currentFunctionStartEdge.getSuccessor().getLeavingEdges();
      }
      if (!(currentFunctionStartEdge instanceof CDeclarationEdge)) {
        return Optional.empty();
      }
      if (nextFunctionStartEdges.size() != 1) {
        return Optional.empty();
      } else {
        currentFunctionStartEdge = nextFunctionStartEdges.first().get();
        nextFunctionStartEdges = currentFunctionStartEdge.getSuccessor().getLeavingEdges();
      }
      if (currentFunctionStartEdge.getEdgeType() != CFAEdgeType.BlankEdge) {
        return Optional.empty();
      }
      if (nextFunctionStartEdges.size() != 2) {
        return Optional.empty();
      } else {
        if (!(nextFunctionStartEdges.first().get() instanceof CAssumeEdge && nextFunctionStartEdges.last().get() instanceof CAssumeEdge)) {
          return Optional.empty();
        }
      }
    }

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

    // first pass: add new nodes
    for(CFANode currentNode : cfaNodeIterable) {
      // dont add a normal node for the function exit node
      if (currentNode.getNodeNumber() != exitNode.getNodeNumber()) {
        // dont add nodes for tail recursive call nodes
        if (currentNode.getNodeNumber()
                != tmpVarDeclarationEdge.get().getSuccessor().getNodeNumber()
            && currentNode.getNodeNumber()
                != tmpVarAssignmentEdge.get().getSuccessor().getNodeNumber()) {
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
        } else {
        FunctionExitNode newNode = new FunctionExitNode(pNode.getFunction());
        nodeMap.put(currentNode.getNodeNumber(), newNode.getNodeNumber());
        nodes.add(newNode);
      }
    }
    for (int i = 0; i < parameters.size()-1; i++) {
      CFANode newNode = CFANode.newDummyCFANode(functionName);
      nodes.add(newNode);
    }
    ImmutableList<CFANode> nodesList = nodes.build();

    // second pass: add new edges
    cfaNodeIterable = cfaNetworkTraverser.breadthFirst(pNode);
    cfaNodeIterable = Iterables.filter(cfaNodeIterable, (CFANode node) -> {
      assert node != null;
      return node.getFunctionName().equals(functionName);
    });
    for(CFANode currentNode : cfaNodeIterable) {
      for(CFAEdge currentEdge : currentNode.getAllLeavingEdges()){
        if (currentEdge.getSuccessor().getFunctionName().equals(functionName) && nodeMap.containsKey(currentNode.getNodeNumber()) && nodeMap.containsKey(currentEdge.getSuccessor().getNodeNumber())) {
          Optional<Integer> newPredecessorNodeIndex = getNodeIndex(nodeMap.get(currentNode.getNodeNumber()), nodesList);
          Optional<Integer> newSuccessorNodeIndex = getNodeIndex(nodeMap.get(currentEdge.getSuccessor().getNodeNumber()), nodesList);
          if (newPredecessorNodeIndex.isPresent() && newSuccessorNodeIndex.isPresent()) {
            CFAEdge newEdge = ProgramTransformationCFAEdgeCreator.copyCFAEdge(
                currentEdge, nodesList.get(newPredecessorNodeIndex.get()), nodesList.get(newSuccessorNodeIndex.get()));
            edges.add(newEdge);
            newEdge.getPredecessor().addLeavingEdge(newEdge);
            newEdge.getSuccessor().addEnteringEdge(newEdge);
          }
        }
      }
    }
    // TODO add parameter edges
    Optional<Integer> nodeBeforeParams = getNodeIndex(nodeMap.get(tmpVarDeclarationEdge.get().getPredecessor().getNodeNumber()), nodesList);
    if (nodeBeforeParams.isEmpty()) {
      return Optional.empty();
    }
    ImmutableList<CExpression> parameterExpressions = ((CFunctionCallAssignmentStatement)(((CStatementEdge)tmpVarAssignmentEdge.get()).getStatement())).getFunctionCallExpression().getParameterExpressions();
    CFANode preNode = nodesList.get(nodeBeforeParams.get());
    CFANode succNode = nodesList.get(nodeBeforeParams.get());
    for (int i = 0; i < parameters.size(); i++) {
      if (i == parameters.size()-1) {
        succNode = nodesList.getFirst();
      } else {
        succNode = nodesList.get(nodeMap.size() + i);
      }
      CExpression parameterExpression = parameterExpressions.get(i);
      CStatement parameterAssignment = new CExpressionAssignmentStatement(FileLocation.DUMMY, new CIdExpression(FileLocation.DUMMY,
          (CType) parameters.get(i).getType(), parameters.get(i).getName(), null), parameterExpression);
      CStatementEdge newEdge = new CStatementEdge("", parameterAssignment, FileLocation.DUMMY, preNode, succNode);
      edges.add(newEdge);
      preNode.addLeavingEdge(newEdge);
      succNode.addEnteringEdge(newEdge);
      if (i < parameters.size() - 1) {
        preNode = nodesList.get(nodeMap.size() + i);
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
