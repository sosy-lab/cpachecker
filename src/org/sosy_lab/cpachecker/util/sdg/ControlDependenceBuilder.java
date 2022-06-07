// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.graph.dominance.DomFrontiers;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;
import org.sosy_lab.cpachecker.util.graph.dominance.DominanceUtils;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph.Node;
import org.sosy_lab.cpachecker.util.sdg.traversal.ForwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.SdgVisitResult;

/**
 * Class for computing control dependencies and inserting them into a {@link SystemDependenceGraph}.
 *
 * @param <P> the procedure type of the SDG
 * @param <N> the node type of the SDG
 */
public final class ControlDependenceBuilder<P, N extends Node<P, CFAEdge, ?>> {

  private final SystemDependenceGraph.Builder<P, CFAEdge, ?, N> builder;
  private final P procedure;
  private final Set<CFAEdge> dependentEdges;

  private ControlDependenceBuilder(
      SystemDependenceGraph.Builder<P, CFAEdge, ?, N> pBuilder, P pProcedure) {

    builder = pBuilder;
    procedure = pProcedure;
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
   * @param <P> the procedure type of the SDG
   * @param pBuilder the SDG builder used to insert dependencies
   * @param pEntryNode the function (specified by its entry node) to compute control dependencies
   *     for
   * @param pProcedure the procedure that the nodes of the function (specified by its entry node)
   *     belong to
   * @param pDependOnBothAssumptions whether to always depend on both assume edges of a branching,
   *     even if it would be sufficient to only depend on one of the assume edges
   */
  public static <P> void insertControlDependencies(
      SystemDependenceGraph.Builder<P, CFAEdge, ?, ?> pBuilder,
      FunctionEntryNode pEntryNode,
      P pProcedure,
      boolean pDependOnBothAssumptions) {

    ControlDependenceBuilder<?, ?> controlDependenceBuilder =
        new ControlDependenceBuilder<>(pBuilder, pProcedure);

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
                      .node(SdgNodeType.STATEMENT, procedure, dependentStatement, Optional.empty())
                      .depends(SdgEdgeType.CONTROL_DEPENDENCY, Optional.empty())
                      .on(SdgNodeType.STATEMENT, procedure, controlStatement, Optional.empty());

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
                .node(SdgNodeType.STATEMENT, procedure, dependentStatement, Optional.empty())
                .depends(SdgEdgeType.CONTROL_DEPENDENCY, Optional.empty())
                .on(SdgNodeType.STATEMENT, procedure, controlStatement, Optional.empty());

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
            .node(SdgNodeType.STATEMENT, procedure, Optional.of(edge), Optional.empty())
            .depends(SdgEdgeType.CONTROL_DEPENDENCY, Optional.empty())
            .on(SdgNodeType.ENTRY, procedure, Optional.empty(), Optional.empty());
      }
    }

    Set<CFAEdge> entryNodeDependent = new HashSet<>();
    N entryNode =
        builder.node(SdgNodeType.ENTRY, procedure, Optional.empty(), Optional.empty()).getNode();

    builder.traverse(
        ImmutableSet.of(entryNode),
        new ForwardsSdgVisitor<>() {

          @Override
          public SdgVisitResult visitNode(N pNode) {

            if (pNode.getType() == SdgNodeType.ENTRY) {
              return SdgVisitResult.CONTINUE;
            }

            Optional<CFAEdge> statement = pNode.getStatement();
            if (statement.isPresent() && entryNodeDependent.add(statement.orElseThrow())) {
              return SdgVisitResult.CONTINUE;
            } else {
              return SdgVisitResult.SKIP;
            }
          }

          @Override
          public SdgVisitResult visitEdge(SdgEdgeType pType, N pPredecessor, N pSuccessor) {
            return pType == SdgEdgeType.CONTROL_DEPENDENCY
                ? SdgVisitResult.CONTINUE
                : SdgVisitResult.SKIP;
          }
        });

    for (CFAEdge edge : functionEdges(pFunctionNodes)) {
      if (!entryNodeDependent.contains(edge)) {
        builder
            .node(SdgNodeType.STATEMENT, procedure, Optional.of(edge), Optional.empty())
            .depends(SdgEdgeType.CONTROL_DEPENDENCY, Optional.empty())
            .on(SdgNodeType.ENTRY, procedure, Optional.empty(), Optional.empty());
      }
    }
  }
}
