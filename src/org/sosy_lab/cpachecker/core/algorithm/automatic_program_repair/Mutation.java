// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This class represents the mutation of an edge in the CFA. The suspicious edge is the edge that
 * is being mutated. The edge has to be replaced in the the constructor of the extending classes.
 */
public abstract class Mutation {
  CFAEdge suspiciousEdge;
  CFA cfa;

  public Mutation(CFAEdge pSuspiciousEdge, CFA pCfa) {
    cfa = pCfa;
    suspiciousEdge = pSuspiciousEdge;
  }

  public CFA getCFA() {
    return cfa;
  }

  public abstract CFAEdge getNewEdge();

  public CFAEdge getSuspiciousEdge() {
    return suspiciousEdge;
  }


  /**
   * The given edge will be inserted into the cfa by replacing the leaving edges of the predecessor
   * edges and the entering edges of the successor edge.
   */
  public static void exchangeEdge(CFAEdge edgeToInsert) {

    final CFANode predecessorNode = edgeToInsert.getPredecessor();
    final CFANode successorNode = edgeToInsert.getSuccessor();

    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(predecessorNode)) {
      if (leavingEdge.getLineNumber() == edgeToInsert.getLineNumber()) {
        predecessorNode.removeLeavingEdge(leavingEdge);
        predecessorNode.addLeavingEdge(edgeToInsert);
      }
    }

    for (CFAEdge enteringEdge : CFAUtils.enteringEdges(successorNode)) {
      if (enteringEdge.getLineNumber() == edgeToInsert.getLineNumber()) {
        successorNode.removeEnteringEdge(enteringEdge);
        successorNode.addEnteringEdge(edgeToInsert);
      }
    }
  }
}
