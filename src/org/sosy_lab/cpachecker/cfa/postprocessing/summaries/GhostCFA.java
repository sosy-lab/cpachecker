// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Strategy.StrategyQualifier;

public class GhostCFA {


  private final StrategiesEnum strategy;
  private final CFANode startGhostCfaNode;
  private final CFANode stopGhostCfaNode;
  private final CFANode startOriginalCfaNode;
  private final CFANode stopOriginalCfaNode;
  private Optional<CFAEdge> startNodesConnection;
  private Optional<CFAEdge> endNodesConnection;
  private Set<CFAEdge> allEdges = null;
  private Set<CFANode> allNodes = null;
  private List<AExpression> parameters;
  private StrategyQualifier strategyQualifier;
  private List<List<AExpression>> usedParameters = new ArrayList<>();

  public GhostCFA(
      CFANode pStartGhostCfaNode,
      CFANode pStopGhostCfaNode,
      CFANode pStartOriginalCfaNode,
      CFANode pStopOriginalCfaNode,
      StrategiesEnum pStrategy,
      List<AExpression> pParameters, StrategyQualifier pStrategyQualifier) {

    this.setStartNodesConnection(Optional.empty());
    this.setEndNodesConnection(Optional.empty());
    this.startGhostCfaNode = pStartGhostCfaNode;
    this.stopGhostCfaNode = pStopGhostCfaNode;
    this.startOriginalCfaNode = pStartOriginalCfaNode;
    this.stopOriginalCfaNode = pStopOriginalCfaNode;
    this.strategy = pStrategy;
    this.parameters = pParameters;
    this.strategyQualifier = pStrategyQualifier;
    this.usedParameters.add(pParameters);
    this.collectEdges();
    this.collectNodes();
  }

  public void connectOriginalAndGhostCFA() {
    if (getStartNodesConnection().isEmpty()) {
      CFAEdge startNodesConnectionLocal =
          new BlankEdge(
              "Start Ghost Connection Strategy: " + this.getStrategy().name(),
              FileLocation.DUMMY,
              getStartOriginalCfaNode(),
              getStartGhostCfaNode(),
              "Start Ghost Connection Strategy: " + this.getStrategy().name(),
              strategy);
      CFACreationUtils.addEdgeUnconditionallyToCFA(startNodesConnectionLocal);
      setStartNodesConnection(Optional.of(startNodesConnectionLocal));
    }

    if (getEndNodesConnection().isEmpty()) {
      CFAEdge endNodesConnectionLocal =
          new BlankEdge(
              "End Ghost Connection Strategy: " + this.getStrategy().name(),
              FileLocation.DUMMY,
              getStopGhostCfaNode(),
              getStopOriginalCfaNode(),
              "End Ghost Connection Strategy: " + this.getStrategy().name(),
              strategy);
      CFACreationUtils.addEdgeUnconditionallyToCFA(endNodesConnectionLocal);
      setStartNodesConnection(Optional.of(endNodesConnectionLocal));
    }
  }

  public CFANode getStartGhostCfaNode() {
    return startGhostCfaNode;
  }

  public CFANode getStopGhostCfaNode() {
    return stopGhostCfaNode;
  }

  public CFANode getStartOriginalCfaNode() {
    return startOriginalCfaNode;
  }

  public CFANode getStopOriginalCfaNode() {
    return stopOriginalCfaNode;
  }

  public Optional<CFAEdge> getStartNodesConnection() {
    return startNodesConnection;
  }

  public StrategiesEnum getStrategy() {
    return strategy;
  }

  public List<List<AExpression>> getUsedParameters() {
    return this.usedParameters;
  }

  private void setStartNodesConnection(Optional<CFAEdge> pStartNodesConnection) {
    startNodesConnection = pStartNodesConnection;
  }

  public Optional<CFAEdge> getEndNodesConnection() {
    return endNodesConnection;
  }

  private void setEndNodesConnection(Optional<CFAEdge> pEndNodesConnection) {
    endNodesConnection = pEndNodesConnection;
  }

  private void collectEdges() {
    if (allEdges != null) {
      return;
    }

    allEdges = new HashSet<>();
    Set<CFAEdge> currentEdges = new HashSet<>(this.getStartGhostCfaNode().getLeavingEdges());

    Set<CFAEdge> newEdges = new HashSet<>();
    while (!currentEdges.isEmpty()) {
      for (CFAEdge e : currentEdges) {
        for (CFAEdge e2 : e.getSuccessor().getLeavingEdges()) {
          if (allEdges.add(e2)) {
            newEdges.add(e2);
          }
        }
      }
      currentEdges = newEdges;
      newEdges.clear();
    }
  }

  private void collectNodes() {
    if (allNodes != null) {
      return;
    }

    allNodes = new HashSet<>();
    Set<CFANode> currentNodes = new HashSet<>();
    currentNodes.add(this.getStartGhostCfaNode());
    allNodes.addAll(currentNodes);
    Set<CFANode> newNodes = new HashSet<>();
    while (!currentNodes.isEmpty()) {
      for (CFANode n : currentNodes) {
        for (CFAEdge e : n.getLeavingEdges()) {
          CFANode n2 = e.getSuccessor();
          if (allNodes.add(n2)) {
            newNodes.add(n2);
          }
        }
      }
      currentNodes = newNodes;
      newNodes = new HashSet<>();
    }
  }

  public List<ALeftHandSide> getParameterVariables() {

    List<ALeftHandSide> parameterVariables = new ArrayList<>();

    CFANode currentNode = startGhostCfaNode;
    for (int i = 0; i < parameters.size(); i++) {
      assert currentNode.getNumLeavingEdges() == 1
          : "The first elements of the ghost CFA should be parameters.";

      CFAEdge currentEdge = currentNode.getLeavingEdge(0);
      assert currentEdge instanceof CStatementEdge : "Each parameter should be a statement edge.";

      CStatement statement = ((CStatementEdge) currentEdge).getStatement();
      assert statement instanceof CExpressionAssignmentStatement
          : "Every parameter should be a variable assignment.";

      ALeftHandSide parameterVariable =
          ((CExpressionAssignmentStatement) statement).getLeftHandSide();

      parameterVariables.add(parameterVariable);
    }

    return parameterVariables;
  }

  public void updateParameters(List<AExpression> pParameters) {
    assert pParameters.size() == parameters.size()
        : "Currently changing the amount of parameters is not supported";

    parameters = pParameters;

    List<ALeftHandSide> parameterVariables = getParameterVariables();

    CFANode currentNode = startGhostCfaNode;
    for (int i = 0; i < parameters.size(); i++) {
      assert currentNode.getNumLeavingEdges() == 1
          : "The first elements of the ghost CFA should be parameters.";

      CFAEdge currentEdge = currentNode.getLeavingEdge(0);
      assert currentEdge instanceof CStatementEdge : "Each parameter should be a statement edge.";
      CFACreationUtils.removeEdgeFromNodes(currentEdge);

      ALeftHandSide parameterVariable = parameterVariables.get(i);

      CExpressionAssignmentStatement newParameterExpression =
          (CExpressionAssignmentStatement)
              new AExpressionFactory()
                  .from(pParameters.get(i))
                  .assignTo(parameterVariable);

      CFACreationUtils.addEdgeUnconditionallyToCFA(
          new CStatementEdge(
              newParameterExpression.toString(),
              newParameterExpression,
              FileLocation.DUMMY,
              currentEdge.getPredecessor(),
              currentEdge.getSuccessor()));

      currentNode = currentEdge.getSuccessor();
    }

    this.usedParameters.add(pParameters);
  }

  public Set<CFANode> getAllNodes() {
    return allNodes;
  }

  public StrategyQualifier getStrategyQualifier() {
    return strategyQualifier;
  }

  public GhostCFA copy() {
    return new GhostCFA(
        startGhostCfaNode,
        stopGhostCfaNode,
        startOriginalCfaNode,
        stopOriginalCfaNode,
        strategy,
        parameters,
        strategyQualifier);
  }

  @Override
  public int hashCode() {
    return this.startGhostCfaNode.hashCode()
        + this.stopGhostCfaNode.hashCode()
        + this.strategy.ordinal();
  }

  @Override
  public final boolean equals(Object pObj) {
    if (!(pObj instanceof GhostCFA)) {
      return false;
    }

    GhostCFA otherGhostCFA = (GhostCFA) pObj;

    if (this.parameters.size() != otherGhostCFA.parameters.size()) {
      return false;
    }

    for (int i = 0; i < this.parameters.size(); i++) {
      AExpression expr1 = this.parameters.get(i);
      AExpression expr2 = otherGhostCFA.parameters.get(i);

      // TODO: Implement equality for AExpressions
      if (expr1.getClass() != expr2.getClass()) {
        return false;
      }

      if (expr1 instanceof AIntegerLiteralExpression) {
        if (!((AIntegerLiteralExpression) expr1)
            .getValue()
            .equals(((AIntegerLiteralExpression) expr2).getValue())) {
          return false;
        }
      }
    }

    return this.startOriginalCfaNode == otherGhostCFA.startOriginalCfaNode
        && this.stopOriginalCfaNode == otherGhostCFA.stopOriginalCfaNode
        && this.strategy == otherGhostCFA.strategy;
  }
}
