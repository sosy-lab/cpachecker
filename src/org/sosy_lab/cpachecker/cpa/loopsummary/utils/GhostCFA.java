// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.utils;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class GhostCFA {
  private final CFANode startNode;
  private final CFANode stopNode;

  public GhostCFA(CFANode startNode, CFANode stopNode) {
    this.startNode = startNode;
    this.stopNode = stopNode;

    /*CFAEdge dummyFalseEdgeForRemovinGhostCFAOnRefinement =
        new CAssumeEdge(
            "false GHOST CFA",
            FileLocation.DUMMY,
            this.startNode,
            this.stopNode,
            CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT),
            true);
    this.startNode.addLeavingEdge(dummyFalseEdgeForRemovinGhostCFAOnRefinement);
    this.stopNode.addEnteringEdge(dummyFalseEdgeForRemovinGhostCFAOnRefinement);*/
  }

  public CFANode getStartNode() {
    return startNode;
  }

  public CFANode getStopNode() {
    return stopNode;
  }
}
