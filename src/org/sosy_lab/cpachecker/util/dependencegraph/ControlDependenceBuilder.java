// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

/** Utility class for computing control dependencies. */
final class ControlDependenceBuilder {

  private ControlDependenceBuilder() {}

  private static boolean ignoreFunctionEdge(CFAEdge pEdge) {
    return pEdge instanceof CFunctionCallEdge || pEdge instanceof CFunctionReturnEdge;
  }

  static void compute(
      MutableCFA pCfa,
      FunctionEntryNode pEntryNode,
      DepConsumer pDepConsumer,
      boolean pDependOnBothAssumptions) {

    DomTree<CFANode> postDomTree = DominanceUtils.createFunctionPostDomTree(pEntryNode);
    Set<CFANode> postDomTreeNodes = new HashSet<>();
    Iterators.addAll(postDomTreeNodes, postDomTree.iterator());

    Set<CFAEdge> dependentEdges = new HashSet<>();

    DomFrontiers<CFANode> frontiers = Dominance.createDomFrontiers(postDomTree);
    for (CFANode dependentNode : postDomTree) {
      int nodeId = postDomTree.getId(dependentNode);
      for (CFANode branchNode : frontiers.getFrontier(dependentNode)) {
        for (CFAEdge assumeEdge : CFAUtils.leavingEdges(branchNode)) {
          if (postDomTreeNodes.contains(assumeEdge.getSuccessor())) {
            int assumeSuccessorId = postDomTree.getId(assumeEdge.getSuccessor());
            if (pDependOnBothAssumptions
                || nodeId == assumeSuccessorId
                || postDomTree.isAncestorOf(nodeId, assumeSuccessorId)) {
              for (CFAEdge dependentEdge : CFAUtils.allLeavingEdges(dependentNode)) {
                if (!ignoreFunctionEdge(dependentEdge) && !assumeEdge.equals(dependentEdge)) {
                  pDepConsumer.accept(assumeEdge, dependentEdge);
                  dependentEdges.add(dependentEdge);
                }
              }
            }
          }
        }
      }
    }

    // Some function nodes are missing from the post-DomTree. This happens when a path from the node
    // to the function exit node does not exist. These nodes are handled the following way:
    //   1. Edges directly connected to these nodes are collected, resulting in set E.
    //   2. A subset C that contains all assume edges in E is created.
    //   3. The following dependencies are added: { e depends on c | e in E, c in C, e != c }

    Set<CFAEdge> edgesWithoutDominator = new HashSet<>();
    for (CFANode node : pCfa.getFunctionNodes(pEntryNode.getFunction().getQualifiedName())) {
      if (!(node instanceof FunctionExitNode)) {
        if (!postDomTreeNodes.contains(node) || !postDomTree.hasParent(postDomTree.getId(node))) {
          Iterables.addAll(edgesWithoutDominator, CFAUtils.allEnteringEdges(node));
          Iterables.addAll(edgesWithoutDominator, CFAUtils.allLeavingEdges(node));
        }
      }
    }

    Set<CFAEdge> assumeEdgesWithoutDominator = new HashSet<>();
    for (CFAEdge edge : edgesWithoutDominator) {
      if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        assumeEdgesWithoutDominator.add(edge);
      }
    }

    for (CFAEdge dependentEdge : edgesWithoutDominator) {
      if (!ignoreFunctionEdge(dependentEdge)) {
        for (CFAEdge assumeEdge : assumeEdgesWithoutDominator) {
          if (!assumeEdge.equals(dependentEdge)) {
            pDepConsumer.accept(assumeEdge, dependentEdge);
            dependentEdges.add(dependentEdge);
          }
        }
      }
    }

    // add dependencies on function call and summary edges

    Set<CFAEdge> callEdges = new HashSet<>();
    for (CFAEdge callEdge : CFAUtils.enteringEdges(pEntryNode)) {
      if (callEdge instanceof CFunctionCallEdge) {
        CFAEdge summaryEdge = ((CFunctionCallEdge) callEdge).getSummaryEdge();
        callEdges.add(callEdge);
        pDepConsumer.accept(summaryEdge, callEdge);
      }
    }

    for (CFANode node : pCfa.getFunctionNodes(pEntryNode.getFunction().getQualifiedName())) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (!dependentEdges.contains(edge) && !ignoreFunctionEdge(edge)) {
          for (CFAEdge callEdge : callEdges) {
            pDepConsumer.accept(callEdge, edge);
          }
        }
      }
    }
  }

  @FunctionalInterface
  interface DepConsumer {
    void accept(CFAEdge pControlEdge, CFAEdge pDependentEdge);
  }
}
