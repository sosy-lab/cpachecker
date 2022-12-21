// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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

  public void updateParameters(List<AExpression> pParameters) {
    assert pParameters.size() == parameters.size()
        : "Currently changing the amount of parameters is not supported";

    parameters = pParameters;

    // TODO Change the CFA in order to match the new parameters
    // TODO: What should be the interface for the parameters in the CFA?
    //      For example:
    //          the first parameters.size() elements being variables and replacing their assignments
  }

  public Set<CFANode> getAllNodes() {
    return allNodes;
  }

  public StrategyQualifier getStrategyQualifier() {
    // TODO: Make this dependent on the parameters
    return strategyQualifier;
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

    return this.startOriginalCfaNode == otherGhostCFA.startOriginalCfaNode
        && this.stopOriginalCfaNode == otherGhostCFA.stopOriginalCfaNode
        && this.strategy == otherGhostCFA.strategy
        // TODO: How to compare if all the elements of a List are equal correctly?
        && this.parameters == otherGhostCFA.parameters;
  }
}
