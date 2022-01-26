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
 * was mutated. The edge has to be exchanged in the cfa with a new, mutated edge in the the
 * constructor of the extending classes.
 */
public abstract class Mutation {
  CFAEdge suspiciousEdge;
  CFA cfa;

  public Mutation(CFAEdge pSuspiciousEdge, CFA pCfa) {
    cfa = pCfa;
    suspiciousEdge = pSuspiciousEdge;
  }

  /**
   * The given edge will be inserted into the cfa by replacing the leaving edges of the predecessor
   * edges and the entering edges of the successor edge.
   */
  public static void exchangeEdge(CFAEdge edgeToInsert, CFAEdge edgeToReplace) {

    final CFANode predecessorNode = edgeToInsert.getPredecessor();
    final CFANode successorNode = edgeToInsert.getSuccessor();

    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(predecessorNode)) {
      if (areEdgesEqual(leavingEdge, edgeToReplace)) {
        predecessorNode.removeLeavingEdge(leavingEdge);
        predecessorNode.addLeavingEdge(edgeToInsert);
      }
    }

    for (CFAEdge enteringEdge : CFAUtils.enteringEdges(successorNode)) {
      if (areEdgesEqual(enteringEdge, edgeToReplace)) {
        successorNode.removeEnteringEdge(enteringEdge);
        successorNode.addEnteringEdge(edgeToInsert);
      }
    }
  }

  private static boolean areEdgesEqual(CFAEdge edge1, CFAEdge edge2) {

    boolean areLineNumbersEqual = edge1.getLineNumber() == edge2.getLineNumber();

    String code1 = edge1.getCode();
    String code2 = edge2.getCode();

    return code1.equals(code2) && areLineNumbersEqual;
  }

  public CFA getCFA() {
    return cfa;
  }

  public abstract CFAEdge getNewEdge();

  public CFAEdge getSuspiciousEdge() {
    return suspiciousEdge;
  }
}
