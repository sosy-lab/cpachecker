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
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.EdgeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.ForwardsVisitor;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.NodeType;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph.VisitResult;
import org.sosy_lab.cpachecker.util.graph.dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;
import org.sosy_lab.cpachecker.util.graph.dominance.DominanceUtils;

/**
 * Class for computing control dependencies and inserting them into a {@link SystemDependenceGraph}.
 *
 * @param <N> the node type of the SDG
 */
final class ControlDependenceBuilder<N extends Node<AFunctionDeclaration, CFAEdge, ?>> {

  private final SystemDependenceGraph.Builder<AFunctionDeclaration, CFAEdge, ?, N> builder;
  private final Optional<AFunctionDeclaration> procedure;
  private final Set<CFAEdge> dependentEdges;

  private ControlDependenceBuilder(
      SystemDependenceGraph.Builder<AFunctionDeclaration, CFAEdge, ?, N> pBuilder,
      FunctionEntryNode pEntryNode) {

    builder = pBuilder;
    procedure = Optional.of(pEntryNode.getFunction());
    dependentEdges = new HashSet<>();
  }

  private static boolean ignoreFunctionEdge(CFAEdge pEdge) {
    return pEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge;
  }

  private static Iterable<CFAEdge> functionEdges(Set<CFANode> pFunctionNodes) {

    return FluentIterable.from(pFunctionNodes)
        .transformAndConcat(CFAUtils::allLeavingEdges)
        .filter(edge -> !ignoreFunctionEdge(edge));
  }

  /**
   * Compute control dependencies for a specified function and insert them into a {@link
   * SystemDependenceGraph}.
   *
   * @param pBuilder the SDG builder used to insert dependencies
   * @param pEntryNode the function (specified by its entry node) to compute control dependencies
   *     for
   * @param pDependOnBothAssumptions whether to always depend on both assume edges of a branching,
   *     even if it would be sufficient to only depend on one of the assume edges
   */
  static void insertControlDependencies(
      SystemDependenceGraph.Builder<AFunctionDeclaration, CFAEdge, ?, ?> pBuilder,
      FunctionEntryNode pEntryNode,
      boolean pDependOnBothAssumptions) {

    ControlDependenceBuilder<?> controlDependenceBuilder =
        new ControlDependenceBuilder<>(pBuilder, pEntryNode);

    DomTree<CFANode> postDomTree = DominanceUtils.createFunctionPostDomTree(pEntryNode);
    Set<CFANode> postDomTreeNodes = new HashSet<>();
    Iterators.addAll(postDomTreeNodes, postDomTree.iterator());

    controlDependenceBuilder.insertControlDependencies(
        postDomTree, postDomTreeNodes, pDependOnBothAssumptions);

    NodeCollectingCFAVisitor nodeCollector = new NodeCollectingCFAVisitor();
    CFATraversal.dfs().ignoreFunctionCalls().traverse(pEntryNode, nodeCollector);

    controlDependenceBuilder.insertMissingControlDependencies(
        postDomTree, postDomTreeNodes, nodeCollector.getVisitedNodes());

    controlDependenceBuilder.insertEntryControlDependencies(nodeCollector.getVisitedNodes());
  }

  /**
   * Compute control dependencies using dominance frontiers created from the post-DomTree and insert
   * these dependencies into the system dependence graph.
   *
   * <p>Implementation detail: how post-DomTrees are used to find control dependencies is described
   * in "The Program Dependence Graph and Its Use in Optimization" (Ferrante et al.)
   */
  private void insertControlDependencies(
      DomTree<CFANode> pPostDomTree,
      Set<CFANode> pPostDomTreeNodes,
      boolean pDependOnBothAssumptions) {

    DomFrontiers<CFANode> frontiers = DomFrontiers.forDomTree(pPostDomTree);
    for (CFANode dependentNode : pPostDomTree) {
      for (CFANode branchNode : frontiers.getFrontier(dependentNode)) {
        for (CFAEdge assumeEdge : CFAUtils.leavingEdges(branchNode)) {
          CFANode assumeSuccessor = assumeEdge.getSuccessor();
          if (pPostDomTreeNodes.contains(assumeSuccessor)) {

            if (pDependOnBothAssumptions
                || assumeSuccessor.equals(dependentNode)
                || pPostDomTree.isAncestorOf(dependentNode, assumeSuccessor)) {

              for (CFAEdge dependentEdge : CFAUtils.allLeavingEdges(dependentNode)) {
                if (!ignoreFunctionEdge(dependentEdge) && !assumeEdge.equals(dependentEdge)) {

                  Optional<CFAEdge> dependentStatement = Optional.of(dependentEdge);
                  Optional<CFAEdge> controlStatement = Optional.of(assumeEdge);

                  builder
                      .node(NodeType.STATEMENT, procedure, dependentStatement, Optional.empty())
                      .depends(EdgeType.CONTROL_DEPENDENCY, Optional.empty())
                      .on(NodeType.STATEMENT, procedure, controlStatement, Optional.empty());

                  dependentEdges.add(dependentEdge);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Insert necessary control dependencies that were overlooked by post-DomTree based {@link
   * #insertControlDependencies(DomTree, Set, boolean)}.
   */
  private void insertMissingControlDependencies(
      DomTree<CFANode> pPostDomTree, Set<CFANode> pPostDomTreeNodes, Set<CFANode> pFunctionNodes) {

    // Some function nodes are missing from the post-DomTree. This happens when a path from the node
    // to the function exit node does not exist. These nodes are handled the following way:
    //   1. Edges directly connected to these nodes are collected, resulting in set E.
    //   2. A subset C that contains all assume edges in E is created.
    //   3. The following dependencies are added: { e depends on c | e in E, c in C, e != c }

    Set<CFAEdge> edgesWithoutDominator = new HashSet<>();
    for (CFANode node : pFunctionNodes) {
      if (!(node instanceof FunctionExitNode)) {
        if (!pPostDomTreeNodes.contains(node) || pPostDomTree.getParent(node).isEmpty()) {
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

            Optional<CFAEdge> dependentStatement = Optional.of(dependentEdge);
            Optional<CFAEdge> controlStatement = Optional.of(assumeEdge);

            builder
                .node(NodeType.STATEMENT, procedure, dependentStatement, Optional.empty())
                .depends(EdgeType.CONTROL_DEPENDENCY, Optional.empty())
                .on(NodeType.STATEMENT, procedure, controlStatement, Optional.empty());

            dependentEdges.add(dependentEdge);
          }
        }
      }
    }
  }

  /**
   * Insert control dependencies into the SDG such that all statements contained in the function are
   * directly or indirectly control dependent on the SDG function entry node.
   *
   * <p>Example (simplified):
   *
   * <pre>{@code
   * Function:
   *
   * int f(int x) {
   *   int y;
   *   if (x > 0) {
   *     y = x - 1;
   *   } else {
   *     y = x + 1;
   *   }
   *   return y;
   * }
   *
   * Before invoking this method:
   *
   * { ENTRY of f(int) }  { int y; }  { return y; }
   *
   * { [x > 0] } -----> { y = x - 1; }
   *
   * { [!(x > 0)] } -----> { y = x + 1; }
   *
   * After invoking this method:
   *
   *                  { int y; } <----- { ENTRY of f(int) } -----> { return y; }
   *                                        |      |
   *                                        |      |
   * { y = x - 1; } <----- { [x > 0] } <-----      -----> { [!(x > 0)] } -----> { y = x + 1; }
   *
   * }</pre>
   */
  private void insertEntryControlDependencies(Set<CFANode> pFunctionNodes) {

    for (CFAEdge edge : functionEdges(pFunctionNodes)) {
      if (!dependentEdges.contains(edge)) {
        builder
            .node(NodeType.STATEMENT, procedure, Optional.of(edge), Optional.empty())
            .depends(EdgeType.CONTROL_DEPENDENCY, Optional.empty())
            .on(NodeType.ENTRY, procedure, Optional.empty(), Optional.empty());
      }
    }

    Set<CFAEdge> entryNodeDependent = new HashSet<>();
    N entryNode =
        builder.node(NodeType.ENTRY, procedure, Optional.empty(), Optional.empty()).getNode();

    builder.traverse(
        ImmutableSet.of(entryNode),
        new ForwardsVisitor<>() {

          @Override
          public VisitResult visitNode(N pNode) {

            if (pNode.getType() == NodeType.ENTRY) {
              return VisitResult.CONTINUE;
            }

            Optional<CFAEdge> statement = pNode.getStatement();
            if (statement.isPresent() && entryNodeDependent.add(statement.orElseThrow())) {
              return VisitResult.CONTINUE;
            } else {
              return VisitResult.SKIP;
            }
          }

          @Override
          public VisitResult visitEdge(EdgeType pType, N pPredecessor, N pSuccessor) {
            return pType == EdgeType.CONTROL_DEPENDENCY ? VisitResult.CONTINUE : VisitResult.SKIP;
          }
        });

    for (CFAEdge edge : functionEdges(pFunctionNodes)) {
      if (!entryNodeDependent.contains(edge)) {
        builder
            .node(NodeType.STATEMENT, procedure, Optional.of(edge), Optional.empty())
            .depends(EdgeType.CONTROL_DEPENDENCY, Optional.empty())
            .on(NodeType.ENTRY, procedure, Optional.empty(), Optional.empty());
      }
    }
  }
}
