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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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

    // check transformation conditions
    Optional<TransformationData> transformationDataOptional = canBeApplied(pNode);
    TransformationData transformationData;
    if (transformationDataOptional.isEmpty()){
      return Optional.empty();
    } else {
      transformationData = transformationDataOptional.get();
    }

    // perform transformation
    //ImmutableSet<CFANode> newNodes;
    //CFANode newEntryNode;
    //CFANode newExitNode;
    //HashMap<CFANode,CFANode> nodeMap;

    CFANode newEntryNode = null;
    CFANode newExitNode = null;
    ImmutableList.Builder<CFANode> nodes = ImmutableList.builder();
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    HashMap<CFANode,CFANode> nodeMap = new HashMap<>();
    Traverser<CFANode> cfaNetworkTraverser = Traverser.forGraph(pCFA.asGraph());
    Iterable<CFANode> cfaNodeIterable = cfaNetworkTraverser.breadthFirst(pNode);
    ImmutableList<? extends AParameterDeclaration> parameters = pNode.getFunction().getParameters();
    cfaNodeIterable = Iterables.filter(cfaNodeIterable, (CFANode node) -> {
      assert node != null;
      return node.getFunctionName().equals(transformationData.functionName);
    });

    // first pass: add new nodes
    for(CFANode currentNode : cfaNodeIterable) {
      // dont add a normal node for the function exit node
      if (currentNode.getNodeNumber() != transformationData.exitNode.getNodeNumber()) {
        // dont add nodes for tail recursive call nodes
        if (currentNode.getNodeNumber()
                != transformationData.tmpVarDeclarationEdge.getSuccessor().getNodeNumber()
            && currentNode.getNodeNumber()
                != transformationData.tmpVarAssignmentEdge.getSuccessor().getNodeNumber()) {
          CFANode newNode = CFANode.newDummyCFANode(transformationData.functionName);
          nodeMap.put(currentNode, newNode);
          if (currentNode.getNodeNumber() == pNode.getNodeNumber()) {
            newEntryNode = newNode;
          }
          nodes.add(newNode);
        }
        } else {
        FunctionExitNode newNode = new FunctionExitNode(pNode.getFunction());
        newExitNode = newNode;
        nodeMap.put(currentNode, newNode);
        nodes.add(newNode);
      }
    }
    for (int i = 0; i < parameters.size()-1; i++) {
      CFANode newNode = CFANode.newDummyCFANode(transformationData.functionName);
      nodes.add(newNode);
    }
    ImmutableList<CFANode> nodesList = nodes.build();

    // second pass: add new edges
    cfaNodeIterable = cfaNetworkTraverser.breadthFirst(pNode);
    cfaNodeIterable = Iterables.filter(cfaNodeIterable, (CFANode node) -> {
      assert node != null;
      return node.getFunctionName().equals(transformationData.functionName);
    });
    for(CFANode currentNode : cfaNodeIterable) {
      for(CFAEdge currentEdge : currentNode.getAllLeavingEdges()){
        if (currentEdge.getSuccessor().getFunctionName().equals(transformationData.functionName) && nodeMap.containsKey(currentNode) && nodeMap.containsKey(currentEdge.getSuccessor())) {
          CFANode newPredecessorNode = nodeMap.get(currentNode);
          CFANode newSuccessorNode = nodeMap.get(currentEdge.getSuccessor());
          CFAEdge newEdge = ProgramTransformationCFAEdgeCreator.copyCFAEdge(
                currentEdge, newPredecessorNode, newSuccessorNode);
          edges.add(newEdge);
          newEdge.getPredecessor().addLeavingEdge(newEdge);
          newEdge.getSuccessor().addEnteringEdge(newEdge);
        }
      }
    }
    // add parameter edges
    CFANode nodeBeforeParams = nodeMap.get(transformationData.tmpVarDeclarationEdge.getPredecessor());
    ImmutableList<CExpression> parameterExpressions = ((CFunctionCallAssignmentStatement)(((CStatementEdge)transformationData.tmpVarAssignmentEdge).getStatement())).getFunctionCallExpression().getParameterExpressions();
    CFANode preNode = nodeBeforeParams;
    CFANode succNode;
    for (int i = 0; i < parameters.size(); i++) {
      if (i == parameters.size()-1) {
        succNode = nodeMap.get(transformationData.nodeBeforeExitCondition);
        succNode.setLoopStart();
      } else {
        succNode = nodesList.get(nodeMap.size() + i);
      }
      CExpression parameterExpression = parameterExpressions.get(i);
      CVariableDeclaration parameterDeclaration;
      CDeclarationEdge declarationEdge = (CDeclarationEdge) pNode.getLeavingEdges().first().get().getSuccessor().getLeavingEdges().first().get();
      if (declarationEdge.getDeclaration() instanceof CFunctionDeclaration cFunctionDeclaration) {
        parameterDeclaration = cFunctionDeclaration.getParameters().get(i).asVariableDeclaration();
      } else {
        return Optional.empty();
      }
      CStatement parameterAssignment = new CExpressionAssignmentStatement(FileLocation.DUMMY, new CIdExpression(FileLocation.DUMMY,
          (CType) parameters.get(i).getType(), parameters.get(i).getName(), parameterDeclaration), parameterExpression);
      CStatementEdge newEdge = new CStatementEdge(parameterAssignment.toASTString(), parameterAssignment, FileLocation.DUMMY, preNode, succNode);
      edges.add(newEdge);
      preNode.addLeavingEdge(newEdge);
      succNode.addEnteringEdge(newEdge);
      if (i < parameters.size() - 1) {
        preNode = nodesList.get(nodeMap.size() + i);
      }
    }

    ImmutableList<CFAEdge> edgesList = edges.build();

    return Optional.of(new SubCFA(
        transformationData.entryNode,
        transformationData.exitNode,
        newEntryNode,
        newExitNode,
        ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION,
        ProgramTransformationBehaviour.PRECISE,
        ImmutableSet.copyOf(nodesList),
        ImmutableSet.copyOf(edgesList)
    ));
  }

  private static Optional<TransformationData> canBeApplied(CFANode pNode){
    // needed information
    CFANode entryNode = pNode;  //TODO maybe change this
    CFANode exitNode;
    String functionName;
    String tmpVarName = null;
    CFAEdge tmpVarDeclarationEdge = null;
    CFAEdge tmpVarAssignmentEdge = null;
    CFAEdge tmpVarReturnEdge = null;
    CFANode nodeBeforeExitCondition;

    // check 1: are we at the start of a function with a return node
    if (!(pNode instanceof FunctionEntryNode functionEntryNode)) {
      return Optional.empty();
    }
    if (functionEntryNode.getExitNode().isEmpty()) {
      return Optional.empty();
    }
    exitNode = functionEntryNode.getExitNode().get(); // TODO maybe change this
    functionName = pNode.getFunctionName();

    // check 2: at the start of the function is the exit condition check
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
        } else {
          nodeBeforeExitCondition = currentFunctionStartEdge.getSuccessor();
        }
      }
    }

    // check 3: we have a tail recursive function call
    boolean isTailRecursive = false;
    FluentIterable<CFAEdge> enteringEdges = exitNode.getEnteringEdges();
    for(CFAEdge edge : enteringEdges) {
      CReturnStatement returnStatement = ((CReturnStatementEdge) edge).getReturnStatement();
      if (returnStatement.getReturnValue().isPresent()) {
        CExpression returnExpression = returnStatement.getReturnValue().get();
        if (returnExpression instanceof CLeftHandSide returnLeftHandSide) {
          if (returnLeftHandSide instanceof CIdExpression returnIdExpression) {
            FluentIterable<CFAEdge> predecessorEdges = edge.getPredecessor().getEnteringEdges();
            if (predecessorEdges.size() == 1) {
              CFAEdge predecessorEdge = predecessorEdges.first().get();
              if (predecessorEdge instanceof CStatementEdge predecessorStatementEdge) {
                if (predecessorStatementEdge.getStatement() instanceof CFunctionCallAssignmentStatement predecessorFunctionCallAssignmentStatement) {
                  if(predecessorFunctionCallAssignmentStatement.getFunctionCallExpression().getDeclaration().getQualifiedName().equals(functionName)){
                    if (predecessorEdge.getPredecessor().getEnteringEdges().size() == 1) {
                      if (predecessorEdge.getPredecessor().getEnteringEdges().first().get() instanceof CDeclarationEdge) {
                        tmpVarName = returnIdExpression.getName();
                        tmpVarDeclarationEdge = predecessorEdge.getPredecessor().getEnteringEdges().first().get();
                        tmpVarAssignmentEdge = predecessorEdge;
                        tmpVarReturnEdge = edge;
                        isTailRecursive = true;
                        break;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    if (!isTailRecursive) {
      return Optional.empty();
    }

    return Optional.of(new TransformationData(entryNode,exitNode,functionName,tmpVarName,tmpVarDeclarationEdge,tmpVarAssignmentEdge,tmpVarReturnEdge,nodeBeforeExitCondition));
  }

  private record TransformationData(
      CFANode entryNode,
      CFANode exitNode,
      String functionName,
      String tmpVarName,
      CFAEdge tmpVarDeclarationEdge,
      CFAEdge tmpVarAssignmentEdge,
      CFAEdge tmpVarReturnEdge,
      CFANode nodeBeforeExitCondition
  ){};
}
