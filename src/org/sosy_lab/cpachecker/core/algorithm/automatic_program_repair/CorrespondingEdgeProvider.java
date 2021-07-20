// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CorrespondingEdgeProvider {
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

    if (functionCallEdge == null) {
      throw new FunctionReturnWithoutFunctionCallException();
    }

    return functionCallEdge;
  }

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

    if (returnEdge == null) {
      throw new FunctionCallWithoutFunctionReturnException();
    }

    return returnEdge;
  }

  static CFAEdge findCorrespondingEdge(CFAEdge edge, CFA cfa) {
    final CFATraversal.EdgeCollectingCFAVisitor edgeCollectingVisitor =
        new CFATraversal.EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), edgeCollectingVisitor);
    FluentIterable<CFAEdge> edges = from(edgeCollectingVisitor.getVisitedEdges());

    for (CFAEdge edge1 : edges) {
      if (edge1.toString().equals(edge.toString())) {
        return edge1;
      }
    }

    throw new CorrespondingEdgeNotFound();
  }
}

class CorrespondingEdgeNotFound extends RuntimeException {}

class FunctionReturnWithoutFunctionCallException extends RuntimeException {}

class FunctionCallWithoutFunctionReturnException extends RuntimeException {}
