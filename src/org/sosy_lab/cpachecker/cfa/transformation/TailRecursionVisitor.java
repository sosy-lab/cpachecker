// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class TailRecursionVisitor implements CFAVisitor {

  private String functionName;
  private Optional<CFANode> nodeBeforeExitCondition = Optional.empty();
  private Optional<CFANode> nodeBeforeReturn = Optional.empty();
  private Optional<CFAEdge> tmpVarDeclarationEdge = Optional.empty();
  private Optional<CFAEdge> tmpVarAssignmentEdge = Optional.empty();
  private Optional<CFAEdge> tmpVarReturnEdge = Optional.empty();
  private Optional<CFAEdge> recursiveFunctionCallEdge = Optional.empty();
  ArrayList<CFAEdge> visitedEdges = new ArrayList<>();

  public TailRecursionVisitor(String pFunctionName) {
    functionName = pFunctionName;
  }

  @Override
  public TraversalProcess visitEdge(CFAEdge edge) {
    if (visitedEdges.contains(edge)) {
      return TraversalProcess.SKIP;
    }
    switch (edge) {
      case BlankEdge blankEdge:
        visitedEdges.add(blankEdge);
        return TraversalProcess.CONTINUE;
      case CAssumeEdge assumeEdge:
        visitedEdges.add(assumeEdge);
        if (nodeBeforeExitCondition.isEmpty()) {
          nodeBeforeExitCondition = Optional.of(assumeEdge.getPredecessor());
        }
        return TraversalProcess.CONTINUE;
      case CReturnStatementEdge returnStatementEdge:
        visitedEdges.add(returnStatementEdge);
        if (returnStatementEdge.getSuccessor().hasEdgeTo(returnStatementEdge.getPredecessor())) {
          if (tmpVarReturnEdge.isEmpty()) {
            tmpVarReturnEdge = Optional.of(returnStatementEdge);
            return TraversalProcess.CONTINUE;
          }
        } else {
          if (nodeBeforeReturn.isEmpty()) {
            nodeBeforeReturn = Optional.of(returnStatementEdge.getPredecessor());
          }
        }
        return TraversalProcess.CONTINUE;
      case CFunctionReturnEdge functionReturnEdge:
        visitedEdges.add(functionReturnEdge);
        if (functionReturnEdge.getSuccessor().getFunctionName().equals(functionName)) {
          if (tmpVarReturnEdge.isEmpty()) {
            tmpVarReturnEdge = Optional.of(functionReturnEdge.getSuccessor().getLeavingEdges().first().get());
          }
        }
        return TraversalProcess.CONTINUE;
      case CFunctionCallEdge functionCallEdge:
        visitedEdges.add(functionCallEdge);
        if (functionCallEdge.getFunctionCallExpression().getDeclaration().getQualifiedName().equals(functionName)) {
          if (recursiveFunctionCallEdge.isEmpty()) {
            recursiveFunctionCallEdge = Optional.of(functionCallEdge);
          } else {
            return TraversalProcess.ABORT;
          }
        }
        return TraversalProcess.CONTINUE;
      case CFunctionSummaryEdge functionSummaryEdge:
        visitedEdges.add(functionSummaryEdge);
        if (functionSummaryEdge.getExpression().getFunctionCallExpression().getDeclaration().getQualifiedName().equals(functionName)) {
          if (functionSummaryEdge.getExpression() instanceof CFunctionCallAssignmentStatement) {
            if (tmpVarAssignmentEdge.isEmpty()) {
              tmpVarAssignmentEdge = Optional.of(functionSummaryEdge);
              if (tmpVarDeclarationEdge.isEmpty()) {
                tmpVarDeclarationEdge = Optional.of(functionSummaryEdge.getPredecessor().getEnteringEdge(0));
              }
            } else {
              return TraversalProcess.ABORT;
            }
          }
        }
        return TraversalProcess.CONTINUE;
      default:
        visitedEdges.add(edge);
        return TraversalProcess.CONTINUE;
    }
  }

  @Override
  public TraversalProcess visitNode(CFANode node) {
    if (node.getFunctionName().equals(functionName)) {
      return TraversalProcess.CONTINUE;
    }
    return TraversalProcess.SKIP;
  }

  public boolean isTailRecursive() {
    return (getNodeBeforeExitCondition().isPresent()
        && getNodeBeforeReturn().isPresent()
        && tmpVarDeclarationEdge.isPresent()
        && tmpVarAssignmentEdge.isPresent()
        &&  tmpVarReturnEdge.isPresent()
        && recursiveFunctionCallEdge.isPresent());
  }

  public Optional<CFANode> getNodeBeforeExitCondition() {
    return nodeBeforeExitCondition;
  }

  public Optional<CFANode> getNodeBeforeReturn() {
    return nodeBeforeReturn;
  }

  public Optional<CFAEdge> getTmpVarDeclarationEdge() {
    return tmpVarDeclarationEdge;
  }

  public Optional<CFAEdge> getTmpVarAssignmentEdge() {
    return tmpVarAssignmentEdge;
  }

  public Optional<CFAEdge> getTmpVarReturnEdge() {
    return tmpVarReturnEdge;
  }

  public Optional<CFAEdge> getRecursiveFunctionCallEdge() {
    return recursiveFunctionCallEdge;
  }

  public List<CFAEdge> getVisitedEdges() {
    return ImmutableList.copyOf(visitedEdges);
  }
}
