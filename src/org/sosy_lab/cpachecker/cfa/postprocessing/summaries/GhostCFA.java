// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class GhostCFA {
  private final StrategiesEnum strategy;
  private final CFANode startGhostCfaNode;
  private final CFANode stopGhostCfaNode;
  private final CFANode startOriginalCfaNode;
  private final CFANode stopOriginalCfaNode;
  private Optional<CFAEdge> startNodesConnection;
  private Optional<CFAEdge> endNodesConnection;

  public GhostCFA(
      CFANode pStartGhostCfaNode,
      CFANode pStopGhostCfaNode,
      CFANode pStartOriginalCfaNode,
      CFANode pStopOriginalCfaNode,
      StrategiesEnum pStrategy) {

    this.setStartNodesConnection(Optional.empty());
    this.setEndNodesConnection(Optional.empty());
    this.startGhostCfaNode = pStartGhostCfaNode;
    this.stopGhostCfaNode = pStopGhostCfaNode;
    this.startOriginalCfaNode = pStartOriginalCfaNode;
    this.stopOriginalCfaNode = pStopOriginalCfaNode;
    this.strategy = pStrategy;
  }

  public void connectOriginalAndGhostCFA() {
    if (getStartNodesConnection().isEmpty()) {
      CFAEdge startNodesConnectionLocal =
          new BlankEdge(
              "Blank",
              FileLocation.DUMMY,
              getStartOriginalCfaNode(),
              getStartGhostCfaNode(),
              "Blank");
      getStartOriginalCfaNode().addLeavingEdge(startNodesConnectionLocal);
      getStartGhostCfaNode().addLeavingEdge(startNodesConnectionLocal);
      setStartNodesConnection(Optional.of(startNodesConnectionLocal));
    }

    if (getEndNodesConnection().isEmpty()) {
      CFAEdge endNodesConnectionLocal =
          new BlankEdge(
              "Blank",
              FileLocation.DUMMY,
              getStartOriginalCfaNode(),
              getStartGhostCfaNode(),
              "Blank");
      getStopOriginalCfaNode().addLeavingEdge(endNodesConnectionLocal);
      getStopGhostCfaNode().addLeavingEdge(endNodesConnectionLocal);
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
}
