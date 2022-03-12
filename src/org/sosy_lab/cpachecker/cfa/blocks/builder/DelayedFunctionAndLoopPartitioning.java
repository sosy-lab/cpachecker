// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blocks.builder;

import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;

/**
 * <code>PartitioningHeuristic</code> that creates blocks for each loop- and function-body. In
 * contrast to <code>FunctionAndLoopPartitioning</code> the heuristics tries to skip possible
 * initial definitions at the blocks.
 */
public class DelayedFunctionAndLoopPartitioning extends FunctionAndLoopPartitioning {

  private static final CFATraversal TRAVERSE_CFA_INSIDE_FUNCTION =
      CFATraversal.dfs().ignoreFunctionCalls();

  public DelayedFunctionAndLoopPartitioning(LogManager pLogger, CFA pCfa, Configuration pConfig)
      throws InvalidConfigurationException {
    super(pLogger, pCfa, pConfig);
  }

  @Override
  protected Set<CFANode> getBlockForNode(CFANode pBlockHead) {
    if (pBlockHead instanceof FunctionEntryNode) {
      Set<CFANode> blockNodes = TRAVERSE_CFA_INSIDE_FUNCTION.collectNodesReachableFrom(pBlockHead);
      return removeInitialDeclarations(pBlockHead, blockNodes);
    }

    return super.getBlockForNode(pBlockHead);
  }

  private Set<CFANode> removeInitialDeclarations(CFANode functionNode, Set<CFANode> functionBody) {
    if (functionNode.getNumEnteringEdges() == 0) {
      // this is the main function
      return functionBody;
    }

    // TODO: currently a call edge must not be branch as otherwise we may find the error locations
    // multiple times within a single run as the analysis does explore all branches to depth 1 even
    // if in one branch a error is found

    assert functionNode.getNumLeavingEdges() == 1;
    CFANode currentNode = functionNode.getLeavingEdge(0).getSuccessor(); // skip initial blank edge
    functionBody.remove(functionNode);

    int skippedDeclarations = 0;

    while (currentNode.getNumLeavingEdges() == 1
        && currentNode.getLeavingEdge(0).getSuccessor().getNumLeavingEdges() == 1) {
      assert currentNode.getNumEnteringEdges() == 1;
      CFAEdge edge = currentNode.getLeavingEdge(0);
      if (edge.getEdgeType() != CFAEdgeType.DeclarationEdge) {
        break;
      }
      // it is a declaration -> skip it
      skippedDeclarations++;
      functionBody.remove(edge.getPredecessor());
      currentNode = edge.getSuccessor();
    }

    while (currentNode.getNumLeavingEdges() == 1
        && skippedDeclarations > 0
        && currentNode.getLeavingEdge(0).getSuccessor().getNumLeavingEdges() == 1) {
      assert currentNode.getNumEnteringEdges() == 1;
      CFAEdge edge = currentNode.getLeavingEdge(0);
      if (edge.getEdgeType() != CFAEdgeType.StatementEdge) {
        break;
      }
      // skip as many (hopefully) definitions
      skippedDeclarations--;
      functionBody.remove(edge.getPredecessor());
      currentNode = edge.getSuccessor();
    }

    assert currentNode.getNumEnteringEdges() == 1;
    return functionBody;
  }
}
