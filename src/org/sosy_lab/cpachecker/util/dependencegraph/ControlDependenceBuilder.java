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
      boolean pDependOnBothAssumptions) {

    DomTree<CFANode> postDomTree = DominanceUtils.createFunctionPostDomTree(pEntryNode);
    DomFrontiers<CFANode> frontiers = Dominance.createDomFrontiers(postDomTree);

    Set<CFANode> postDomTreeNodes = new HashSet<>();
    Iterators.addAll(postDomTreeNodes, postDomTree.iterator());

    Set<CFAEdge> dependentEdges = new HashSet<>();

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

    Set<CFAEdge> noDomEdges = new HashSet<>();
    for (CFANode node : pCfa.getFunctionNodes(pEntryNode.getFunction().getQualifiedName())) {
      if (postDomTreeNodes.contains(node)) {
        int nodeId = postDomTree.getId(node);
        if (!postDomTree.hasParent(nodeId)) {
          Iterables.addAll(noDomEdges, CFAUtils.allEnteringEdges(node));
          Iterables.addAll(noDomEdges, CFAUtils.allLeavingEdges(node));
        }
      }
    }

    Set<CFAEdge> noDomAssumeEdges = new HashSet<>();
    for (CFAEdge edge : noDomEdges) {
      if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        noDomAssumeEdges.add(edge);
      }
    }

    for (CFAEdge dependentEdge : noDomEdges) {
      if (!ignoreFunctionEdge(dependentEdge)) {
        for (CFAEdge assumeEdge : noDomAssumeEdges) {
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
