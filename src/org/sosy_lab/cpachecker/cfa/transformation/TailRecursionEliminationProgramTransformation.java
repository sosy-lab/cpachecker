// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.graph.Traverser;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
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
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class TailRecursionEliminationProgramTransformation extends ProgramTransformation{

  @Override
  public Optional<ProgramTransformationInformation> transform(CFA pCFA, CFANode pNode) {

    // check if CFA is a supergraph
    boolean isSuperGraph =
        pCFA.getMetadata().getConnectedness() != CfaConnectedness.UNCONNECTED_FUNCTIONS;

    // check transformation conditions depending on isSuperGraph
    Optional<TransformationData> transformationDataOptional;
    if (isSuperGraph){
      transformationDataOptional = canBeAppliedOnSuperGraph(pNode);
    } else {
      transformationDataOptional = canBeApplied(pNode);
    }
    TransformationData transformationData;
    if (transformationDataOptional.isEmpty()){
      return Optional.empty();
    } else {
      transformationData = transformationDataOptional.orElseThrow();
    }

    // perform transformation
    CFANode newEntryNode = null;
    CFANode newExitNode = null;
    ImmutableList.Builder<CFANode> nodes = ImmutableList.builder();
    ImmutableList.Builder<CFAEdge> edges = ImmutableList.builder();
    ImmutableMap.Builder<CFANode, CFANode> nodeMapBuilder = new ImmutableMap.Builder<>();
    Traverser<CFANode> cfaNetworkTraverser = Traverser.forGraph(pCFA.asGraph());
    Iterable<CFANode> cfaNodeIterable = cfaNetworkTraverser.breadthFirst(pNode);
    ImmutableList<? extends AParameterDeclaration> parameters = pNode.getFunction().getParameters();
    cfaNodeIterable = Iterables.filter(cfaNodeIterable, (CFANode node) -> {
      assert node != null;
      return node.getFunctionName().equals(transformationData.functionName);
    });

    // first pass: add new nodes
    for (CFANode currentNode : cfaNodeIterable) {
      // dont add a normal node for the function exit node
      if (currentNode.getNodeNumber() != transformationData.exitNode.getNodeNumber()) {
        // dont add nodes for tail recursive call nodes
        if (currentNode.getNodeNumber()
                != transformationData.tmpVarDeclarationEdge.getSuccessor().getNodeNumber()
            && currentNode.getNodeNumber()
                != transformationData.tmpVarAssignmentEdge.getSuccessor().getNodeNumber()) {
          CFANode newNode = CFANode.newDummyCFANode(transformationData.functionName);
          nodeMapBuilder.put(currentNode, newNode);
          if (currentNode.getNodeNumber() == pNode.getNodeNumber()) {
            newEntryNode = newNode;
          }
          nodes.add(newNode);
        }
      } else {
        FunctionExitNode newNode = new FunctionExitNode(pNode.getFunction());
        newNode.setEntryNode((FunctionEntryNode) pNode);
        newExitNode = newNode;
        nodeMapBuilder.put(currentNode, newNode);
        nodes.add(newNode);
      }
    }
    for (int i = 0; i < parameters.size()-1; i++) {
      CFANode newNode = CFANode.newDummyCFANode(transformationData.functionName);
      nodes.add(newNode);
    }
    ImmutableList<CFANode> nodesList = nodes.build();
    ImmutableMap<CFANode, CFANode> nodeMap = nodeMapBuilder.buildKeepingLast();

    // second pass: add new edges
    cfaNodeIterable = cfaNetworkTraverser.breadthFirst(pNode);
    cfaNodeIterable = Iterables.filter(cfaNodeIterable, (CFANode node) -> {
      assert node != null;
      return node.getFunctionName().equals(transformationData.functionName);
    });
    for (CFANode currentNode : cfaNodeIterable) {
      for (CFAEdge currentEdge : currentNode.getAllLeavingEdges()) {
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
    ImmutableList.Builder<CFANode> parameterNodes = new ImmutableList.Builder<>();
    CFANode nodeBeforeParams = nodeMap.get(transformationData.tmpVarDeclarationEdge.getPredecessor());
    ImmutableList<CExpression> parameterExpressions;
    if (isSuperGraph) {
      parameterExpressions = ((CFunctionSummaryEdge) transformationData.tmpVarAssignmentEdge).getExpression().getFunctionCallExpression().getParameterExpressions();
    } else {
      parameterExpressions = ((CFunctionCallAssignmentStatement) ((CStatementEdge) transformationData.tmpVarAssignmentEdge).getStatement()).getFunctionCallExpression().getParameterExpressions();
    }
    CFunctionDeclaration functionDeclaration = (CFunctionDeclaration) ((FunctionEntryNode) pNode).getFunctionDefinition();
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
      CVariableDeclaration parameterDeclaration = functionDeclaration.getParameters().get(i).asVariableDeclaration();
      CStatement parameterAssignment = new CExpressionAssignmentStatement(FileLocation.DUMMY, new CIdExpression(FileLocation.DUMMY,
          (CType) parameters.get(i).getType(), parameters.get(i).getName(), parameterDeclaration), parameterExpression);
      CStatementEdge newEdge = new CStatementEdge(parameterAssignment.toASTString(), parameterAssignment, FileLocation.DUMMY, preNode, succNode);
      edges.add(newEdge);
      preNode.addLeavingEdge(newEdge);
      succNode.addEnteringEdge(newEdge);
      parameterNodes.add(preNode);
      if (i < parameters.size() - 1) {
        preNode = nodesList.get(nodeMap.size() + i);
      }
    }

    ImmutableList<CFAEdge> edgesList = edges.build();

    return Optional.of(
        new ProgramTransformationInformation(
            new SubCFA(
                transformationData.entryNode,
                transformationData.exitNode,
                newEntryNode,
                newExitNode,
                ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION,
                ProgramTransformationBehaviour.PRECISE,
                ImmutableSet.copyOf(nodesList),
                ImmutableSet.copyOf(edgesList)),
            new TailRecursionEliminationRecovery(
                ImmutableBiMap.copyOf(nodeMap).inverse(),
                parameters.size(),
                nodeMap.get(transformationData.nodeBeforeExitCondition),
                parameterNodes.build(),
                transformationData.tmpVarDeclarationEdge,
                transformationData.tmpVarAssignmentEdge,
                transformationData.tmpVarReturnEdge)));
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
    exitNode = functionEntryNode.getExitNode().orElseThrow(); // TODO maybe change this
    functionName = pNode.getFunctionName();

    // check 2: at the start of the function is the exit condition check
    CFANode currentNode = pNode;
    CFAEdge currentEdge;
    while (currentNode.getLeavingEdges().size() == 1) {
      currentEdge = currentNode.getLeavingEdges().first().get();
      if (!(currentEdge instanceof BlankEdge || currentEdge instanceof CDeclarationEdge)) {
        break;
      }
      currentNode = currentEdge.getSuccessor();
    }

    if (currentNode.getLeavingEdges().size() == 2) {
      if (!(currentNode.getLeavingEdges().first().get() instanceof CAssumeEdge
          && currentNode.getLeavingEdges().last().get() instanceof CAssumeEdge)) {
        return Optional.empty();
      }
      nodeBeforeExitCondition = currentNode;
    } else {
      return Optional.empty();
    }

    // check 3: we have a tail recursive function call
    boolean isTailRecursive = false;
    FluentIterable<CFAEdge> enteringEdges = exitNode.getEnteringEdges();
    for (CFAEdge edge : enteringEdges) {
      if (edge instanceof CReturnStatementEdge returnEdge) {
        CReturnStatement returnStatement = returnEdge.getReturnStatement();
        if (returnStatement.getReturnValue().isPresent()) {
          CExpression returnExpression = returnStatement.getReturnValue().orElseThrow();
          if (returnExpression instanceof CLeftHandSide returnLeftHandSide) {
            if (returnLeftHandSide instanceof CIdExpression returnIdExpression) {
              FluentIterable<CFAEdge> predecessorEdges = edge.getPredecessor().getEnteringEdges();
              if (predecessorEdges.size() == 1) {
                CFAEdge predecessorEdge = predecessorEdges.first().get();
                if (predecessorEdge instanceof CStatementEdge predecessorStatementEdge) {
                  if (predecessorStatementEdge.getStatement()
                      instanceof
                      CFunctionCallAssignmentStatement predecessorFunctionCallAssignmentStatement) {
                    if (predecessorFunctionCallAssignmentStatement
                        .getFunctionCallExpression()
                        .getDeclaration()
                        .getQualifiedName()
                        .equals(functionName)) {
                      if (predecessorEdge.getPredecessor().getEnteringEdges().size() == 1) {
                        if (predecessorEdge.getPredecessor().getEnteringEdges().first().get()
                            instanceof CDeclarationEdge) {
                          tmpVarName = returnIdExpression.getName();
                          tmpVarDeclarationEdge =
                              predecessorEdge.getPredecessor().getEnteringEdges().first().get();
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
    }
    if (!isTailRecursive) {
      return Optional.empty();
    }

    return Optional.of(new TransformationData(entryNode, exitNode, functionName, tmpVarName, tmpVarDeclarationEdge, tmpVarAssignmentEdge, tmpVarReturnEdge, nodeBeforeExitCondition));
  }

  private static Optional<TransformationData> canBeAppliedOnSuperGraph(CFANode pNode){
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
    exitNode = functionEntryNode.getExitNode().orElseThrow(); // TODO maybe change this
    functionName = pNode.getFunctionName();

    // check 2: at the start of the function is the exit condition check
    CFANode currentNode = pNode;
    CFAEdge currentEdge;
    while (currentNode.getLeavingEdges().size() == 1) {
      currentEdge = currentNode.getLeavingEdges().first().get();
      if (!(currentEdge instanceof BlankEdge || currentEdge instanceof CDeclarationEdge)) {
        break;
      }
      currentNode = currentEdge.getSuccessor();
    }

    if (currentNode.getLeavingEdges().size() == 2) {
      if (!(currentNode.getLeavingEdges().first().get() instanceof CAssumeEdge
          && currentNode.getLeavingEdges().last().get() instanceof CAssumeEdge)) {
        return Optional.empty();
      }
      nodeBeforeExitCondition = currentNode;
    } else {
      return Optional.empty();
    }

    // check 3: we have a tail recursive function call
    boolean isTailRecursive = false;
    FluentIterable<CFAEdge> enteringEdges = exitNode.getEnteringEdges();
    for (CFAEdge edge : enteringEdges) {
      if (edge instanceof CReturnStatementEdge returnEdge) {
        CReturnStatement returnStatement = returnEdge.getReturnStatement();
        if (returnStatement.getReturnValue().isPresent()) {
          CExpression returnExpression = returnStatement.getReturnValue().orElseThrow();
          if (returnExpression instanceof CLeftHandSide returnLeftHandSide) {
            if (returnLeftHandSide instanceof CIdExpression returnIdExpression) {
              FluentIterable<CFAEdge> predecessorEdges = edge.getPredecessor().getEnteringEdges();
              if (predecessorEdges.size() == 1) {
                CFAEdge predecessorEdge = predecessorEdges.first().get();
                if (predecessorEdge instanceof CFunctionReturnEdge predecessorFunctionCallEdge) {
                  CFunctionSummaryEdge summaryEdge = predecessorFunctionCallEdge.getSummaryEdge();
                  if (summaryEdge.getExpression().getFunctionCallExpression().getDeclaration().getQualifiedName().equals(functionName)) {
                    tmpVarName = returnIdExpression.getName();
                    for (CFAEdge assumeEdge : nodeBeforeExitCondition.getLeavingEdges()){
                      // meh
                      if (assumeEdge.getSuccessor().getLeavingEdges().size() == 1) {
                        tmpVarDeclarationEdge = assumeEdge.getSuccessor().getLeavingEdges().first().get();
                      }
                    }
                    //tmpVarDeclarationEdge = null;
                    tmpVarAssignmentEdge = summaryEdge;
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
    if (!isTailRecursive) {
      return Optional.empty();
    }

    return Optional.of(new TransformationData(entryNode, exitNode, functionName, tmpVarName, tmpVarDeclarationEdge, tmpVarAssignmentEdge, tmpVarReturnEdge, nodeBeforeExitCondition));
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
  ){}
}
