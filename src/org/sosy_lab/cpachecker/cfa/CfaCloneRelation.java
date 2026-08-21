// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalence.Wrapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Relation between the nodes and edges of cloned functions and the nodes and edges of the functions
 * they were cloned from.
 *
 * <p>Some CFA postprocessings create copies of functions, e.g., {@link
 * org.sosy_lab.cpachecker.cfa.postprocessing.global.CFACloner CFACloner} creates several copies of
 * every function so that each thread of a concurrent program can use its own copy, and {@link
 * org.sosy_lab.cpachecker.cfa.postprocessing.global.FunctionCallUnwinder FunctionCallUnwinder}
 * creates copies of functions to unwind recursion. All nodes and edges of such a copy are new
 * objects, so without additional information they cannot be related to the CFA as it was created by
 * the parser (for example to the edges stored in an {@link
 * org.sosy_lab.cpachecker.util.ast.ASTElement ASTElement}). This class stores which node and edge
 * was created as a copy of which node and edge and therefore allows this matching
 *
 * <p>Copies of copies are resolved transitively, i.e., all nodes and edges of this relation are
 * mapped to a node or edge that is not a copy itself.
 */
public final class CfaCloneRelation {

  private static final CfaCloneRelation EMPTY =
      new CfaCloneRelation(ImmutableMap.of(), ImmutableMap.of());

  // CFAEdge uses only its predecessor and successor for equals and hashCode, which is not precise
  // enough for this relation (e.g., a summary edge and its statement edge are equal), therefore the
  // edges are wrapped for identity-based lookups.
  private final ImmutableMap<Wrapper<CFAEdge>, CFAEdge> clonedToOriginalEdge;
  private final ImmutableMap<CFANode, CFANode> clonedToOriginalNode;

  private CfaCloneRelation(
      ImmutableMap<Wrapper<CFAEdge>, CFAEdge> pClonedToOriginalEdge,
      ImmutableMap<CFANode, CFANode> pClonedToOriginalNode) {
    clonedToOriginalEdge = pClonedToOriginalEdge;
    clonedToOriginalNode = pClonedToOriginalNode;
  }

  /** Returns the relation for a CFA that does not contain any cloned function. */
  public static CfaCloneRelation empty() {
    return EMPTY;
  }

  /** Returns a new builder for a relation that does not contain any clone yet. */
  public static Builder builder() {
    return new Builder();
  }

  public boolean isEmpty() {
    return clonedToOriginalEdge.isEmpty() && clonedToOriginalNode.isEmpty();
  }

  /**
   * Returns the edge the given edge was cloned from.
   *
   * <p>Only edges that already existed when their function was cloned are part of this relation.
   * The edges of the supergraph (i.e., function call, function return, and summary edges) are
   * inserted after the cloning and are therefore not contained in it. Use {@link
   * #getOriginalNode(CFANode)} to relate such an edge to the original CFA.
   *
   * @param pEdge the edge to get the original edge for
   * @return the edge {@code pEdge} was cloned from, or {@code pEdge} itself if it is not a clone
   */
  public CFAEdge getOriginalEdge(CFAEdge pEdge) {
    checkNotNull(pEdge);
    return clonedToOriginalEdge.getOrDefault(Equivalence.identity().wrap(pEdge), pEdge);
  }

  /**
   * Returns the node the given node was cloned from.
   *
   * @param pNode the node to get the original node for
   * @return the node {@code pNode} was cloned from, or {@code pNode} itself if it is not a clone
   */
  public CFANode getOriginalNode(CFANode pNode) {
    checkNotNull(pNode);
    return clonedToOriginalNode.getOrDefault(pNode, pNode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clonedToOriginalEdge, clonedToOriginalNode);
  }

  @Override
  public boolean equals(Object pObject) {
    if (this == pObject) {
      return true;
    }
    return pObject instanceof CfaCloneRelation other
        && clonedToOriginalEdge.equals(other.clonedToOriginalEdge)
        && clonedToOriginalNode.equals(other.clonedToOriginalNode);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("clonedEdges", clonedToOriginalEdge.size())
        .add("clonedNodes", clonedToOriginalNode.size())
        .toString();
  }

  /** Builder for {@link CfaCloneRelation} that resolves clones of clones transitively. */
  public static final class Builder {

    // the maps keep the insertion order so that the built relation is deterministic
    private final Map<Wrapper<CFAEdge>, CFAEdge> clonedToOriginalEdge = new LinkedHashMap<>();
    private final Map<CFANode, CFANode> clonedToOriginalNode = new LinkedHashMap<>();

    private Builder() {}

    /** Adds all clones of the given relation to this builder. */
    @CanIgnoreReturnValue
    public Builder addAll(CfaCloneRelation pCloneRelation) {
      clonedToOriginalEdge.putAll(pCloneRelation.clonedToOriginalEdge);
      clonedToOriginalNode.putAll(pCloneRelation.clonedToOriginalNode);
      return this;
    }

    /**
     * Records that the given edge was created as a copy of the given original edge.
     *
     * <p>If the original edge is a clone itself, the new edge is mapped to the edge the original
     * edge was cloned from.
     */
    @CanIgnoreReturnValue
    public Builder addClonedEdge(CFAEdge pClone, CFAEdge pOriginal) {
      Wrapper<CFAEdge> original = Equivalence.identity().wrap(checkNotNull(pOriginal));
      clonedToOriginalEdge.put(
          Equivalence.identity().wrap(checkNotNull(pClone)),
          clonedToOriginalEdge.getOrDefault(original, pOriginal));
      return this;
    }

    /**
     * Records that the given node was created as a copy of the given original node.
     *
     * <p>If the original node is a clone itself, the new node is mapped to the node the original
     * node was cloned from.
     */
    @CanIgnoreReturnValue
    public Builder addClonedNode(CFANode pClone, CFANode pOriginal) {
      checkNotNull(pOriginal);
      clonedToOriginalNode.put(
          checkNotNull(pClone), clonedToOriginalNode.getOrDefault(pOriginal, pOriginal));
      return this;
    }

    public CfaCloneRelation build() {
      if (clonedToOriginalEdge.isEmpty() && clonedToOriginalNode.isEmpty()) {
        return EMPTY;
      }
      return new CfaCloneRelation(
          ImmutableMap.copyOf(clonedToOriginalEdge), ImmutableMap.copyOf(clonedToOriginalNode));
    }
  }
}
