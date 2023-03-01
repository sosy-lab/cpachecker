// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.graph.Network;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;

/**
 * The graph representation of a CFA, without any summary edges.
 *
 * <p>Ignoring all summary edges, all connections between elements of a CFA (i.e., its nodes and
 * edges) are defined by a {@link CfaNetwork}. Depending on the implementation, the CFA represented
 * by a {@link CfaNetwork} may differ from the CFA represented by its individual elements (e.g.,
 * {@link CFAEdge#getSuccessor()} and {@link CfaNetwork#successor(CFAEdge)} may not return the same
 * value). It's important to only use methods provided by {@link CfaNetwork}, if more than a single
 * CFA node and/or edge is involved.
 *
 * <p>For performance reasons, some expensive checks are only performed if Java assertions are
 * enabled. Even though this is bad practice in general, this is also the case for some
 * preconditions. E.g., for some implementations, checking whether a CFA node or edge actually
 * belongs to a {@link CfaNetwork} can be quite expensive, so this isn't necessarily checked for all
 * method arguments, if Java assertions are disabled.
 *
 * <p>All returned sets are unmodifiable views, so modification attempts throw an exception.
 * However, modifications to a {@link CfaNetwork} will be reflected in its returned set views. A
 * {@link CfaNetwork} must not be modified while any of its set view are iterated, as this might
 * lead to incorrect iterations.
 */
public interface CfaNetwork extends Network<CFANode, CFAEdge> {

  /**
   * Returns a new {@link CfaNetwork} that represents the specified single function.
   *
   * <p>The function is specified using its function entry node. The returned {@link CfaNetwork}
   * only contains nodes that are part of the specified function. Only if both endpoints of an edge
   * are part of a {@link CfaNetwork} and the edge is not a summary edge, the edge is also part of
   * the returned {@link CfaNetwork}.
   *
   * <p>The returned {@link CfaNetwork} is a view, so it reflects all changes to the specified
   * function.
   *
   * <p>IMPORTANT: Ignoring all summary edges, the specified function must not contain any parallel
   * edges (i.e., multiple directed edges from some node {@code u} to some node {@code v}) and never
   * add them in the future (if the CFA is mutable). Be aware that this requirement is not enforced,
   * if Java assertions are disabled.
   *
   * @param pFunctionEntryNode the function's entry node
   * @return a new {@link CfaNetwork} that represents the function specified by its function entry
   *     node
   */
  public static CfaNetwork forFunction(FunctionEntryNode pFunctionEntryNode) {
    return SingleFunctionCfaNetwork.forFunction(pFunctionEntryNode);
  }

  /**
   * Returns a set containing all function entry nodes of this {@link CfaNetwork}.
   *
   * <p>The returned set is unmodifiable, but modifications of this {@link CfaNetwork} are reflected
   * in the returned set.
   *
   * @return a set containing all function entry nodes of this {@link CfaNetwork}
   */
  Set<FunctionEntryNode> entryNodes();

  /**
   * Returns the predecessor of the specified CFA edge in this {@link CfaNetwork}.
   *
   * @param pEdge the CFA edge to get the predecessor for
   * @return the predecessor of the specified CFA edge in this {@link CfaNetwork}
   * @throws NullPointerException if {@code pEdge == null}
   * @throws IllegalArgumentException if {@code pEdge} is not an element of this {@link CfaNetwork}
   *     (only when Java assertions are enabled, it's guaranteed that this check is performed)
   */
  CFANode predecessor(CFAEdge pEdge);

  /**
   * Returns the successor of the specified CFA edge in this {@link CfaNetwork}.
   *
   * @param pEdge the CFA edge to get the successor for
   * @return the successor of the specified CFA edge in this {@link CfaNetwork}
   * @throws NullPointerException if {@code pEdge == null}
   * @throws IllegalArgumentException if {@code pEdge} is not an element of this {@link CfaNetwork}
   *     (only when Java assertions are enabled, it's guaranteed that this check is performed)
   */
  CFANode successor(CFAEdge pEdge);

  /**
   * Returns the corresponding function exit node for the specified function entry node in this
   * {@link CfaNetwork}, if such a function exit node exists.
   *
   * <p>A possible reason for a function entry node without a corresponding function exit node is
   * that the function never returns (e.g., always aborts or always executes an obvious infinite
   * loop).
   *
   * @param pFunctionEntryNode the function entry node to get the function exit node for
   * @return If there is a corresponding function exit node for the specified function entry node in
   *     this {@link CfaNetwork}, an optional containing the function exit node is returned.
   *     Otherwise, if such a function exit node doesn't exist, an empty optional is returned.
   * @throws NullPointerException if {@code pFunctionEntryNode == null}
   * @throws IllegalArgumentException if {@code pFunctionEntryNode} is not an element of this {@link
   *     CfaNetwork} (only when Java assertions are enabled, it's guaranteed that this check is
   *     performed)
   */
  Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode);

  // on-the-fly filters & transformers

  /**
   * Returns a view of this {@link CfaNetwork} that only contains nodes for which the specified
   * predicate evaluates to {@code true}.
   *
   * <p>Only if both endpoints of an edge are part of a {@link CfaNetwork}, the edge is also part of
   * the {@link CfaNetwork}.
   *
   * <p>The returned {@link CfaNetwork} is a view, so it reflects all changes to this {@link
   * CfaNetwork}. This {@link CfaNetwork} is not changed by this method.
   *
   * @param pRetainPredicate the predicate that specifies the nodes that should be part of the
   *     returned {@link CfaNetwork}
   * @throws NullPointerException if {@code pRetainPredicate == null}
   * @return a view of this {@link CfaNetwork} that only contains nodes for which the specified
   *     predicate evaluates to {@code true}
   */
  default CfaNetwork withFilteredNodes(Predicate<CFANode> pRetainPredicate) {
    return NodeFilteringCfaNetwork.of(this, pRetainPredicate);
  }

  /**
   * Returns a view of this {@link CfaNetwork} that only contains edges for which the specified
   * predicate evaluates to {@code true}.
   *
   * <p>The returned {@link CfaNetwork} is a view, so it reflects all changes to this {@link
   * CfaNetwork}. This {@link CfaNetwork} is not changed by this method.
   *
   * @param pRetainPredicate the predicate that specifies the edges that should be part of the
   *     returned {@link CfaNetwork}
   * @throws NullPointerException if {@code pRetainPredicate == null}
   * @return a view of this {@link CfaNetwork} that only contains edges for which the specified
   *     predicate evaluates to {@code true}
   */
  default CfaNetwork withFilteredEdges(Predicate<CFAEdge> pRetainPredicate) {
    return EdgeFilteringCfaNetwork.of(this, pRetainPredicate);
  }

  /**
   * Returns a view of this {@link CfaNetwork} in which all edges are replaced on-the-fly with their
   * corresponding transformed edges.
   *
   * <p>The specified function returns the transformed edge for a given edge. The given edge and
   * transformed edge must have the same endpoints and the transformed edge must not be a summary
   * edge. The function is applied every time an edge is accessed, so the function may be called
   * multiple times for the same given edge.
   *
   * <p>The returned {@link CfaNetwork} is a view, so it reflects all changes to this {@link
   * CfaNetwork}. This {@link CfaNetwork} is not changed by this method.
   *
   * @param pEdgeTransformer the function that returns the transformed edge for a given CFA edge
   * @throws NullPointerException if {@code pEdgeTransformer == null}
   * @return a view of this {@link CfaNetwork} in which all edges are replaced by their function
   *     result
   */
  default CfaNetwork withTransformedEdges(Function<CFAEdge, CFAEdge> pEdgeTransformer) {
    return EdgeTransformingCfaNetwork.of(this, pEdgeTransformer);
  }

  /**
   * Returns a view of this {@link CfaNetwork} that doesn't contain any super-edges (i.e., function
   * call and return edges).
   *
   * <p>The returned {@link CfaNetwork} is a view, so it reflects all changes to this {@link
   * CfaNetwork}. This {@link CfaNetwork} is not changed by this method.
   *
   * @return a view of this {@link CfaNetwork} that doesn't contain any super-edges
   */
  default CfaNetwork withoutSuperEdges() {
    return withFilteredEdges(
        edge -> !(edge instanceof FunctionCallEdge) && !(edge instanceof FunctionReturnEdge));
  }
}
