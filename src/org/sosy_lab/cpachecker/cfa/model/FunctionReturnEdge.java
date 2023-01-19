// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class FunctionReturnEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = 7267973320703716417L;
  private final FunctionSummaryEdge summaryEdge;

  protected FunctionReturnEdge(
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      FunctionSummaryEdge pSummaryEdge) {

    super("", pFileLocation, pPredecessor, pSuccessor);
    summaryEdge = pSummaryEdge;
  }

  public FunctionSummaryEdge getSummaryEdge() {
    return summaryEdge;
  }

  @Override
  public String getCode() {
    return "";
  }

  @Override
  public String getDescription() {
    return "Return edge from "
        + getPredecessor().getFunctionName()
        + " to "
        + getSuccessor().getFunctionName()
        + ": "
        + summaryEdge.getExpression();
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionReturnEdge;
  }

  public FunctionExitNode functionExitNode() {
    CFANode node = getPredecessor();
    // skip blank edges if necessary
    while (!(node instanceof FunctionExitNode)
        && node.getNumEnteringEdges() == 1
        && node.getEnteringEdge(0).getEdgeType() == CFAEdgeType.BlankEdge) {
      node = node.getEnteringEdge(0).getPredecessor();
    }
    if (node instanceof FunctionExitNode functionExitNode) {
      return functionExitNode;
    } else {
      throw new IllegalStateException("Cannot determine function exit node for " + this);
    }
  }

  public FunctionEntryNode getFunctionEntry() {
    return summaryEdge.getFunctionEntry();
  }
}
