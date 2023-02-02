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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * Represents a {@link CFA} as a {@link Network}.
 *
 * <p>All connections between elements of a CFA (i.e., nodes and edges) are defined by a {@link
 * CfaNetwork}. Depending on the implementation, the CFA represented by a {@link CfaNetwork} may
 * differ from the CFA represented by its elements (e.g., {@link CFAEdge#getSuccessor()} and {@link
 * CfaNetwork#successor(CFAEdge)} may not return the same value). It's important to only use methods
 * provided by {@link CfaNetwork} if more than a single CFA node and/or edge is involved.
 *
 * <p>For performance reasons, some expensive checks are only performed if Java assertions are
 * enabled. Even though this is bad practice in general, this is also the case for some
 * preconditions. E.g., for some implementations, checking whether a CFA node or edge actually
 * belongs to a {@link CfaNetwork} can be quite expensive, so this isn't necessarily checked for all
 * method arguments if Java assertions are disabled.
 *
 * <p>All returned sets are unmodifiable views, so modification attempts throw an exception.
 * However, modifications to a {@link CfaNetwork} will be reflected in its returned set views. A
 * {@link CfaNetwork} must not be modified while any of its set view are iterated, as this might
 * lead to incorrect iterations.
 */
public interface CfaNetwork extends Network<CFANode, CFAEdge> {

  /**
   * Returns a new {@link CfaNetwork} view that represents the specified {@link CFA}.
   *
   * <p>All changes to the specified CFA are reflected in the returned {@link CfaNetwork}. The CFA
   * represented by the returned {@link CfaNetwork} always matches the CFA represented by its
   * elements (e.g., {@link CFAEdge#getSuccessor()} and {@link CfaNetwork#successor(CFAEdge)} always
   * return the same value).
   *
   * <p>IMPORTANT: The specified CFA must not contain any parallel edges (i.e., edges that connect
   * the same nodes in the same order) and never add them in the future (if the CFA is mutable).
   * Additionally, the collections returned by {@link CFA#getAllNodes()} and {@link
   * CFA#getAllFunctionHeads()} must not contain any duplicates and never add them in the future. Be
   * aware that these requirements are not enforced if Java assertions are disabled.
   *
   * @param pCfa the CFA to create a {@link CfaNetwork} view for
   * @return a new {@link CfaNetwork} view that represents the specified {@link CFA}
   * @throws NullPointerException if {@code pCfa == null}
   */
  public static CfaNetwork wrap(CFA pCfa) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(WrappingCfaNetwork.wrap(pCfa));
  }

  /**
   * Returns a new {@link CfaNetwork} that represents a single function.
   *
   * <p>The function is specified using the function entry node. The returned {@link CfaNetwork}
   * only contains nodes that are part of the specified function. Only if both endpoints of an edge
   * are part of a {@link CfaNetwork}, the edge is also part of the {@link CfaNetwork}.
   *
   * <p>All changes to the specified function are reflected in the returned {@link CfaNetwork}.
   *
   * <p>IMPORTANT: The specified function must not contain any parallel edges (i.e., edges that
   * connect the same nodes in the same order) and never add them in the future (if the CFA is
   * mutable). Be aware that this requirement is not enforced if Java assertions are disabled.
   *
   * @param pFunctionEntryNode the function's entry node
   * @return a new {@link CfaNetwork} that represents the function specified by its function entry
   *     node
   */
  public static CfaNetwork forFunction(FunctionEntryNode pFunctionEntryNode) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        SingleFunctionCfaNetwork.forFunction(pFunctionEntryNode));
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
   * Returns the function entry node for the specified function summary edge in this {@link
   * CfaNetwork}.
   *
   * @param pFunctionSummaryEdge the function summary edge to get the function entry node for
   * @return the function entry node for the specified function summary edge in this {@link
   *     CfaNetwork}
   * @throws NullPointerException if {@code pFunctionSummaryEdge == null}
   * @throws IllegalArgumentException if {@code pFunctionSummaryEdge} is not an element of this
   *     {@link CfaNetwork} (only when Java assertions are enabled, it's guaranteed that this check
   *     is performed)
   * @throws IllegalStateException if there is no corresponding function entry node for the
   *     specified function summary edge in this {@link CfaNetwork}
   */
  FunctionEntryNode functionEntryNode(FunctionSummaryEdge pFunctionSummaryEdge);

  /**
   * Returns the corresponding function exit node for the specified function entry node in this
   * {@link CfaNetwork}.
   *
   * <p>A possible reason for a function to having a corresponding function exit node is that the
   * function never returns (e.g., always aborts, infinite loop, etc.).
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

  /**
   * Returns the function summary edge for the specified function call edge in this {@link
   * CfaNetwork}.
   *
   * @param pFunctionCallEdge the function call edge to get the function summary edge for
   * @return the function summary edge for the specified function call edge in this {@link
   *     CfaNetwork}
   * @throws NullPointerException if {@code pFunctionCallEdge == null}
   * @throws IllegalArgumentException if {@code pFunctionCallEdge} is not an element of this {@link
   *     CfaNetwork} (only when Java assertions are enabled, it's guaranteed that this check is
   *     performed)
   * @throws IllegalStateException if there is no corresponding function summary edge for the
   *     specified function call edge in this {@link CfaNetwork}
   */
  FunctionSummaryEdge functionSummaryEdge(FunctionCallEdge pFunctionCallEdge);

  /**
   * Returns the function summary edge for the specified function return edge in this {@link
   * CfaNetwork}.
   *
   * @param pFunctionReturnEdge the function return edge to get the function summary edge for
   * @return the function summary edge for the specified function return edge in this {@link
   *     CfaNetwork}
   * @throws NullPointerException if {@code pFunctionReturnEdge == null}
   * @throws IllegalArgumentException if {@code pFunctionReturnEdge} is not an element of this
   *     {@link CfaNetwork} (only when Java assertions are enabled, it's guaranteed that this check
   *     is performed)
   * @throws IllegalStateException if there is no corresponding function summary edge for the
   *     specified function return edge in this {@link CfaNetwork}
   */
  FunctionSummaryEdge functionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge);

  // on-the-fly filters & transformers

  /**
   * Returns a view of this {@link CfaNetwork} that only contains nodes for which the specified
   * predicate evaluates to {@code true}.
   *
   * <p>Only if both endpoints of an edge are part of a {@link CfaNetwork}, the edge is also part of
   * the {@link CfaNetwork}.
   *
   * <p>Modifications of this {@link CfaNetwork} are reflected in the view.
   *
   * @param pRetainPredicate predicate that specifies the nodes that should be part of the returned
   *     {@link CfaNetwork}
   * @throws NullPointerException if {@code pRetainPredicate == null}
   * @return a view of this {@link CfaNetwork} that only contains nodes for which the specified
   *     predicate evaluates to {@code true}
   */
  default CfaNetwork filterNodes(Predicate<CFANode> pRetainPredicate) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new NodeFilteringCfaNetwork(this, pRetainPredicate));
  }

  /**
   * Returns a view of this {@link CfaNetwork} that only contains edges for which the specified
   * predicate evaluates to {@code true}.
   *
   * <p>Modifications of this {@link CfaNetwork} are reflected in the view.
   *
   * @param pRetainPredicate predicate that specifies the edges that should be part of the returned
   *     {@link CfaNetwork}
   * @throws NullPointerException if {@code pRetainPredicate == null}
   * @return a view of this {@link CfaNetwork} that only contains edges for which the specified
   *     predicate evaluates to {@code true}
   */
  default CfaNetwork filterEdges(Predicate<CFAEdge> pRetainPredicate) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new EdgeFilteringCfaNetwork(this, pRetainPredicate));
  }

  /**
   * Returns a view of this {@link CfaNetwork} in which all edges of the wrapped {@link CfaNetwork}
   * are replaced on-the-fly with their corresponding transformed edges.
   *
   * <p>The specified function returns the transformed edge for a given edge. The given edge and
   * transformed edge must have the same endpoints. The function is applied every time an edge is
   * accessed, so the function may be called multiple times for the same given edge.
   *
   * <p>Modifications of this {@link CfaNetwork} are reflected in the view.
   *
   * @param pEdgeTransformer function that returns the transformed edge for a given CFA edge
   * @throws NullPointerException if {@code pEdgeTransformer == null}
   * @return a view of this {@link CfaNetwork} in which all edges are replaced by their function
   *     result
   */
  default CfaNetwork transformEdges(Function<CFAEdge, CFAEdge> pEdgeTransformer) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new EdgeTransformingCfaNetwork(this, pEdgeTransformer));
  }

  /**
   * Returns a view of this {@link CfaNetwork} that doesn't contain any summary edges.
   *
   * <p>Modifications of this {@link CfaNetwork} are reflected in the view.
   *
   * @return a view of this {@link CfaNetwork} that doesn't contain any summary edges
   */
  default CfaNetwork withoutSummaryEdges() {
    return filterEdges(edge -> !(edge instanceof FunctionSummaryEdge));
  }

  /**
   * Returns a view of this {@link CfaNetwork} that doesn't contain any super-edges (i.e., function
   * call and return edges).
   *
   * <p>Modifications of this {@link CfaNetwork} are reflected in the view.
   *
   * @return a view of this {@link CfaNetwork} that doesn't contain any super-edges
   */
  default CfaNetwork withoutSuperEdges() {
    return filterEdges(
        edge -> !(edge instanceof FunctionCallEdge) && !(edge instanceof FunctionReturnEdge));
  }
}
