// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance.DomTree;

/** Utility class for computing control dependencies. */
final class ControlDependenceBuilder {

  private ControlDependenceBuilder() {}

  private static boolean ignoreFunctionEdge(CFAEdge pEdge) {
    return pEdge instanceof CFunctionCallEdge || pEdge instanceof CFunctionReturnEdge;
  }

  private static Iterable<CFAEdge> functionEdges(Set<CFANode> pFunctionNodes) {

    return FluentIterable.from(pFunctionNodes)
        .transformAndConcat(CFAUtils::allLeavingEdges)
        .filter(edge -> !ignoreFunctionEdge(edge));
  }

  private static Set<CFAEdge> getFunctionCallDependentEdges(
      Set<ControlDependency> pDependencies, Set<CFAEdge> pCallEdges) {

    Set<CFAEdge> functionCallDependent = new HashSet<>();
    boolean changed = true;
    while (changed) {

      changed = false;

      for (ControlDependency dependency : pDependencies) {

        CFAEdge controlEdge = dependency.getControlEdge();
        CFAEdge dependentEdge = dependency.getDependentEdge();

        if (pCallEdges.contains(controlEdge) || functionCallDependent.contains(controlEdge)) {
          if (functionCallDependent.add(dependentEdge)) {
            changed = true;
          }
        }
      }
    }

    return functionCallDependent;
  }

  static ImmutableSet<ControlDependency> computeControlDependencies(
      FunctionEntryNode pEntryNode, boolean pDependOnBothAssumptions) {

    DomTree<CFANode> postDomTree = DominanceUtils.createFunctionPostDomTree(pEntryNode);
    Set<CFANode> postDomTreeNodes = new HashSet<>();
    Iterators.addAll(postDomTreeNodes, postDomTree.iterator());

    Set<ControlDependency> dependencies = new HashSet<>();
    Set<CFAEdge> dependentEdges = new HashSet<>();

    // Compute control dependencies by using dominance frontiers created from the post-DomTree.

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
                  dependencies.add(new ControlDependency(assumeEdge, dependentEdge));
                  dependentEdges.add(dependentEdge);
                }
              }
            }
          }
        }
      }
    }

    // Collect all CFANodes in the pEntryNode function.

    NodeCollectingCFAVisitor nodeCollector = new NodeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreFunctionCalls().traverse(pEntryNode, nodeCollector);

    // Some function nodes are missing from the post-DomTree. This happens when a path from the node
    // to the function exit node does not exist. These nodes are handled the following way:
    //   1. Edges directly connected to these nodes are collected, resulting in set E.
    //   2. A subset C that contains all assume edges in E is created.
    //   3. The following dependencies are added: { e depends on c | e in E, c in C, e != c }

    Set<CFAEdge> edgesWithoutDominator = new HashSet<>();
    for (CFANode node : nodeCollector.getVisitedNodes()) {
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
            dependencies.add(new ControlDependency(assumeEdge, dependentEdge));
            dependentEdges.add(dependentEdge);
          }
        }
      }
    }

    // Add dependencies between all CFAEdges not dependent on any other CFAEdge and all function
    // call edges entering the function.

    Set<CFAEdge> callEdges = new HashSet<>();
    for (CFAEdge callEdge : CFAUtils.enteringEdges(pEntryNode)) {
      if (callEdge instanceof CFunctionCallEdge) {
        callEdges.add(callEdge);
      }
    }

    for (CFAEdge edge : functionEdges(nodeCollector.getVisitedNodes())) {
      if (!dependentEdges.contains(edge)) {
        for (CFAEdge callEdge : callEdges) {
          dependencies.add(new ControlDependency(callEdge, edge));
        }
      }
    }

    // Check whether all CFAEdges are, directly or indirectly, control dependent on the function's
    // entering call edges. If such a dependency doesn't already exist, it's subsequently added.

    Set<CFAEdge> functionCallDependent = getFunctionCallDependentEdges(dependencies, callEdges);

    for (CFAEdge edge : functionEdges(nodeCollector.getVisitedNodes())) {
      if (!functionCallDependent.contains(edge)) {
        for (CFAEdge callEdge : callEdges) {
          dependencies.add(new ControlDependency(callEdge, edge));
        }
      }
    }

    // Add dependencies between the function's entering call edges and their corresponding summary
    // edges.

    for (CFAEdge callEdge : callEdges) {
      if (callEdge instanceof CFunctionCallEdge) {
        CFAEdge summaryEdge = ((CFunctionCallEdge) callEdge).getSummaryEdge();
        dependencies.add(new ControlDependency(summaryEdge, callEdge));
      }
    }

    return ImmutableSet.copyOf(dependencies);
  }

  static final class ControlDependency {

    private final CFAEdge controlEdge;
    private final CFAEdge dependentEdge;

    private ControlDependency(CFAEdge pControlEdge, CFAEdge pDependentEdge) {
      controlEdge = pControlEdge;
      dependentEdge = pDependentEdge;
    }

    CFAEdge getControlEdge() {
      return controlEdge;
    }

    CFAEdge getDependentEdge() {
      return dependentEdge;
    }

    @Override
    public int hashCode() {
      return Objects.hash(controlEdge, dependentEdge);
    }

    @Override
    public boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (pObject == null) {
        return false;
      }

      if (getClass() != pObject.getClass()) {
        return false;
      }

      ControlDependency other = (ControlDependency) pObject;
      return Objects.equals(controlEdge, other.controlEdge)
          && Objects.equals(dependentEdge, other.dependentEdge);
    }

    @Override
    public String toString() {
      return String.format(Locale.ENGLISH, "((%s) depends on (%s))", dependentEdge, controlEdge);
    }
  }
}
