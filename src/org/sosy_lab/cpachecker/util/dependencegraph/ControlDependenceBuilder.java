// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

final class ControlDependenceBuilder {

  private ControlDependenceBuilder() {}

  private static boolean ignoreFunctionEdge(CFAEdge pEdge) {
    return pEdge instanceof CFunctionCallEdge || pEdge instanceof CFunctionReturnEdge;
  }

  public static void compute(
      MutableCFA pCfa,
      FunctionEntryNode pEntryNode,
      DepConsumer pDepConsumer,
      boolean pDependOnBothAssumptions,
      ShutdownNotifier pShutdownNotifier)
      throws InterruptedException {

    DomTree<CFANode> postDomTree = DominanceUtils.createFunctionPostDomTree(pEntryNode);

    DomFrontiers<CFANode> frontiers = Dominance.createDomFrontiers(postDomTree);
    Set<CFAEdge> dependentEdges = new HashSet<>();

    for (CFANode dependentNode : postDomTree) {
      int nodeId = postDomTree.getId(dependentNode);
      for (CFANode branchNode : frontiers.getFrontier(dependentNode)) {
        for (CFAEdge assumeEdge : CFAUtils.leavingEdges(branchNode)) {
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

    Set<CFAEdge> noDomEdges = new HashSet<>();
    if (CFAUtils.existsPath(
        pEntryNode, pEntryNode.getExitNode(), CFAUtils::allLeavingEdges, pShutdownNotifier)) {
      for (CFANode node : pCfa.getFunctionNodes(pEntryNode.getFunction().getQualifiedName())) {
        int nodeId = postDomTree.getId(node);
        if (!postDomTree.hasParent(nodeId)) {
          Iterables.addAll(noDomEdges, CFAUtils.allEnteringEdges(node));
          Iterables.addAll(noDomEdges, CFAUtils.allLeavingEdges(node));
        }
      }
    } else {
      // Sometimes there is no path from the function entry node to the function exit node.
      // In this case, domTree is incomplete as it does not contain all function nodes.
      // Calling domTree.getId would throw an exception for these missing nodes.
      for (CFANode node : pCfa.getFunctionNodes(pEntryNode.getFunction().getQualifiedName())) {
        Iterables.addAll(noDomEdges, CFAUtils.allEnteringEdges(node));
        Iterables.addAll(noDomEdges, CFAUtils.allLeavingEdges(node));
      }
    }

    Set<CFAEdge> noDomAssumes = new HashSet<>();
    for (CFAEdge edge : noDomEdges) {
      if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        noDomAssumes.add(edge);
      }
    }

    for (CFAEdge dependentEdge : noDomEdges) {
      if (!ignoreFunctionEdge(dependentEdge)) {
        for (CFAEdge assumeEdge : noDomAssumes) {
          if (!assumeEdge.equals(dependentEdge)) {
            pDepConsumer.accept(assumeEdge, dependentEdge);
            dependentEdges.add(dependentEdge);
          }
        }
      }
    }

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

  interface DepConsumer {

    void accept(CFAEdge pControlEdge, CFAEdge pDependentEdge);
  }
}
