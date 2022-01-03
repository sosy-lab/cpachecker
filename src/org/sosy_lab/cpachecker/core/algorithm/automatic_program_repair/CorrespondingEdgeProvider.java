// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CorrespondingEdgeProvider {
  /**
   * This function will find the function call edge that corresponds to the given function summary
   * edge. If this edge is not present an exception is thrown because this state is illegal.
   */
  public static CFunctionCallEdge findCorrespondingFunctionCallEdge(
      CFunctionSummaryEdge functionSummaryEdge) {
    CFunctionEntryNode functionEntryNode = functionSummaryEdge.getFunctionEntry();
    CFunctionCallEdge functionCallEdge = null;

    for (CFAEdge enteringEdge : CFAUtils.enteringEdges(functionEntryNode)) {
      if (enteringEdge instanceof CFunctionCallEdge) {
        functionCallEdge = (CFunctionCallEdge) enteringEdge;
        break;
      }
    }

    checkState(
        functionCallEdge != null, "CFunctionSummaryEdge without corresponding CFunctionCallEdge");

    return functionCallEdge;
  }
  /**
   * This function will find the function return edge that corresponds to the given function summary
   * edge. If this edge is not present an exception is thrown because this state is illegal.
   */
  public static CFunctionReturnEdge findCorrespondingFunctionReturnEdge(
      CFunctionSummaryEdge functionSummaryEdge) {
    FunctionExitNode functionExitNode = functionSummaryEdge.getFunctionEntry().getExitNode();
    CFunctionReturnEdge returnEdge = null;

    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(functionExitNode)) {
      if (leavingEdge instanceof CFunctionReturnEdge) {
        returnEdge = (CFunctionReturnEdge) leavingEdge;
        break;
      }
    }

    checkState(
        returnEdge != null, "CFunctionSummaryEdge without corresponding CFunctionReturnEdge.");

    return returnEdge;
  }

  /**
   * Given an assume edge that marks the start of a loop, this function will find the corresponding
   * assume edge that contains the opposite condition. If this edge is not present an exception is
   * thrown because this state is illegal.
   */
  public static CAssumeEdge findCorrespondingAssumeEdge(CAssumeEdge originalEdge) {
    CFANode predecessorNode = originalEdge.getPredecessor();
    CAssumeEdge correspondingAssumeEdge = null;

    for (CFAEdge currentEdge : CFAUtils.leavingEdges(predecessorNode)) {

      if (currentEdge instanceof CAssumeEdge) {
        CAssumeEdge currentAssumeEdge = (CAssumeEdge) currentEdge;
        if (isOppositeCondition(originalEdge, currentAssumeEdge)) {
          correspondingAssumeEdge = currentAssumeEdge;
          break;
        }
      }
    }

    checkState(
        correspondingAssumeEdge != null,
        "AssumeEdge in loop start without corresponding opposite condition.");

    return correspondingAssumeEdge;
  }

  public static boolean isOppositeCondition(CAssumeEdge edge1, CAssumeEdge edge2) {
    return edge1.getFileLocation().equals(edge2.getFileLocation())
        && edge1.getTruthAssumption() != edge2.getTruthAssumption();
  }

  /**
   * Given a cloned cfa and an edge out of the original cfa, this function will return the cloned
   * instance of the given edge. Equality of edges is assumed based file location, predecessor,
   * successor, and the code it represents.
   */
  public static CFAEdge findCorrespondingEdge(CFAEdge originalEdge, CFA clonedCFA) {
    final CFATraversal.EdgeCollectingCFAVisitor edgeCollectingVisitor =
        new CFATraversal.EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverseOnce(clonedCFA.getMainFunction(), edgeCollectingVisitor);
    FluentIterable<CFAEdge> edges = from(edgeCollectingVisitor.getVisitedEdges());

    for (CFAEdge currentEdge : edges) {
      if (areEdgesEqual(originalEdge, currentEdge)) {
        return currentEdge;
      }
    }

    throw new RuntimeException("Could not find the corresponding edge in the given CFA.");
  }

  private static boolean areEdgesEqual(CFAEdge edge1, CFAEdge edge2) {
    return edge1.getFileLocation().equals(edge2.getFileLocation())
        && edge1.getCode().equals(edge2.getCode());
  }
}
