// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Records representing the CFA transformation that are facilitated by the {@link CCfaTransformer}
 * with the {@link CfaMutableNetwork} as a data structure.
 */
public class CfaTransformationRecords {

  private final Optional<CFA> cfaBeforeTransformation;
  private final ImmutableSet<CFAEdge> allEdgesOfUntransformedCfa;

  private final ImmutableSet<CFAEdge> edgesAddedBeforeAstNodeSubstitution;
  private final ImmutableSet<CFAEdge> edgesRemovedBeforeAstNodeSubstitution;
  private final ImmutableBiMap<CFAEdge, CFAEdge> newEdgeToOldEdgeAfterAstNodeSubstitution;

  private final ImmutableSet<CFANode> nodesAddedBeforeAstNodeSubstitution;
  private final ImmutableSet<CFANode> nodesRemovedBeforeAstNodeSubstitution;
  private final ImmutableBiMap<CFANode, CFANode> newNodeToOldNodeAfterAstNodeSubstitution;

  private final ImmutableSet<CFAEdge> newEdges;
  private final ImmutableSet<CFAEdge> missingEdges;

  private final ImmutableSet<CFANode> newNodes;
  private final ImmutableSet<CFANode> missingNodes;

  /**
   * Creates CfaTransformationRecords from the given changes.
   *
   * @param pCfaBeforeTransformation The untransformed CFA.
   * @param pEdgesAddedBeforeAstNodeSubstitution The CFAEdges that were added to the {@link
   *     CfaMutableNetwork} of the untransformed CFA before {@link AAstNode} substitution with the
   *     {@link CCfaTransformer}
   * @param pEdgesRemovedBeforeAstNodeSubstitution The CFAEdges that were removed from the {@link
   *     CfaMutableNetwork} of the untransformed CFA before {@link AAstNode} substitution with the
   *     {@link CCfaTransformer}
   * @param pOldEdgeToNewEdgeAfterAstNodeSubstitution The mapping from a CFAEdge of the {@link
   *     CfaMutableNetwork} to its substitute after {@link AAstNode} substitution with the {@link
   *     CCfaTransformer}
   * @param pNodesAddedBeforeAstNodeSubstitution The CFANodes that were added to the {@link
   *     CfaMutableNetwork} of the untransformed CFA before {@link AAstNode} substitution with the
   *     {@link CCfaTransformer}
   * @param pNodesRemovedBeforeAstNodeSubstitution The CFANodes that were removed from the {@link
   *     CfaMutableNetwork} of the untransformed CFA before {@link AAstNode} substitution with the
   *     {@link CCfaTransformer}
   * @param pOldNodeToNewNodeAfterAstNodeSubstitution The mapping from a CFANode of the {@link
   *     CfaMutableNetwork} to its substitute after {@link AAstNode} substitution with the {@link
   *     CCfaTransformer}
   */
  public CfaTransformationRecords(
      final Optional<CFA> pCfaBeforeTransformation,
      final Set<CFAEdge> pEdgesAddedBeforeAstNodeSubstitution,
      final Set<CFAEdge> pEdgesRemovedBeforeAstNodeSubstitution,
      final BiMap<CFAEdge, CFAEdge> pOldEdgeToNewEdgeAfterAstNodeSubstitution,
      final Set<CFANode> pNodesAddedBeforeAstNodeSubstitution,
      final Set<CFANode> pNodesRemovedBeforeAstNodeSubstitution,
      final BiMap<CFANode, CFANode> pOldNodeToNewNodeAfterAstNodeSubstitution) {

    cfaBeforeTransformation = checkNotNull(pCfaBeforeTransformation);
    allEdgesOfUntransformedCfa =
        pCfaBeforeTransformation.isPresent()
            ? getAllEdges(pCfaBeforeTransformation.orElseThrow())
            : ImmutableSet.of();

    edgesAddedBeforeAstNodeSubstitution =
        ImmutableSet.copyOf(checkNotNull(pEdgesAddedBeforeAstNodeSubstitution));
    edgesRemovedBeforeAstNodeSubstitution =
        ImmutableSet.copyOf(checkNotNull(pEdgesRemovedBeforeAstNodeSubstitution));
    newEdgeToOldEdgeAfterAstNodeSubstitution =
        ImmutableBiMap.copyOf(checkNotNull(pOldEdgeToNewEdgeAfterAstNodeSubstitution)).inverse();

    nodesAddedBeforeAstNodeSubstitution =
        ImmutableSet.copyOf(checkNotNull(pNodesAddedBeforeAstNodeSubstitution));
    nodesRemovedBeforeAstNodeSubstitution =
        ImmutableSet.copyOf(checkNotNull(pNodesRemovedBeforeAstNodeSubstitution));
    newNodeToOldNodeAfterAstNodeSubstitution =
        ImmutableBiMap.copyOf(checkNotNull(pOldNodeToNewNodeAfterAstNodeSubstitution)).inverse();

    final ImmutableSet.Builder<CFAEdge> newEdgesBuilder = ImmutableSet.builder();
    final ImmutableSet.Builder<CFAEdge> missingEdgesBuilder = ImmutableSet.builder();
    collectNonTrivialEdgeChanges(
        pEdgesAddedBeforeAstNodeSubstitution,
        pEdgesRemovedBeforeAstNodeSubstitution,
        pOldEdgeToNewEdgeAfterAstNodeSubstitution,
        newEdgesBuilder,
        missingEdgesBuilder);
    newEdges = newEdgesBuilder.build();
    missingEdges = missingEdgesBuilder.build();

    final ImmutableSet.Builder<CFANode> newNodesBuilder = ImmutableSet.builder();
    final ImmutableSet.Builder<CFANode> missingNodesBuilder = ImmutableSet.builder();
    collectNonTrivialNodeChanges(
        pNodesAddedBeforeAstNodeSubstitution,
        pNodesRemovedBeforeAstNodeSubstitution,
        pOldNodeToNewNodeAfterAstNodeSubstitution,
        newNodesBuilder,
        missingNodesBuilder);
    newNodes = newNodesBuilder.build();
    missingNodes = missingNodesBuilder.build();
  }

  /** Returns CfaTransformationRecords indicating an untransformed CFA. */
  public static CfaTransformationRecords createTransformationRecordsForUntransformedCfa(
      final CFA pCfa) {
    checkNotNull(pCfa);

    return new CfaTransformationRecords(
        /* pCfaBeforeTransformation = */ Optional.of(pCfa),
        /* pEdgesAddedBeforeAstNodeSubstitution = */ ImmutableSet.of(),
        /* pEdgesRemovedBeforeAstNodeSubstitution =  */ ImmutableSet.of(),
        /* pOldEdgeToNewEdgeAfterAstNodeSubstitution = */ ImmutableBiMap.of(),
        /* pNodesAddedBeforeAstNodeSubstitution =  */ ImmutableSet.of(),
        /* pNodesRemovedBeforeAstNodeSubstitution = */ ImmutableSet.of(),
        /* pOldNodeToNewNodeAfterAstNodeSubstitution = */ ImmutableBiMap.of());
  }

  /** Returns CfaTransformationRecords indicating a completely transformed CFA. */
  public static CfaTransformationRecords createTransformationRecordsForCompletelyTransformedCfa(
      final CFA pCfa) {
    checkNotNull(pCfa);

    final ImmutableSet<CFAEdge> allEdges = getAllEdges(pCfa);
    final ImmutableSet<CFANode> allNodes = ImmutableSet.copyOf(pCfa.getAllNodes());

    // we model a completely transformed CFA as a CFA where all CFAEdges and CFANodes were added
    // before a trivial identity ASTNode substitution, because there is no possibility to create
    // CfaTransformationRecords for CFAEdges or CFANodes that were added after ASTNode substitution
    final ImmutableBiMap.Builder<CFAEdge, CFAEdge> identityBiMapOfEdges = ImmutableBiMap.builder();
    allEdges.forEach(edge -> identityBiMapOfEdges.put(edge, edge));
    final ImmutableBiMap.Builder<CFANode, CFANode> identityBiMapOfNodes = ImmutableBiMap.builder();
    allNodes.forEach(node -> identityBiMapOfNodes.put(node, node));

    return new CfaTransformationRecords(
        /* pCfaBeforeTransformation = */ Optional.empty(),
        /* pEdgesAddedBeforeAstNodeSubstitution = */ allEdges,
        /* pEdgesRemovedBeforeAstNodeSubstitution =  */ ImmutableSet.of(),
        /* pOldEdgeToNewEdgeAfterAstNodeSubstitution = */ identityBiMapOfEdges.buildOrThrow(),
        /* pNodesAddedBeforeAstNodeSubstitution =  */ allNodes,
        /* pNodesRemovedBeforeAstNodeSubstitution = */ ImmutableSet.of(),
        /* pOldNodeToNewNodeAfterAstNodeSubstitution = */ identityBiMapOfNodes.buildOrThrow());
  }

  public Optional<CFA> getCfaBeforeTransformation() {
    return cfaBeforeTransformation;
  }

  public ImmutableSet<CFAEdge> getEdgesAddedBeforeAstNodeSubstitution() {
    return edgesAddedBeforeAstNodeSubstitution;
  }

  public ImmutableSet<CFAEdge> getEdgesRemovedBeforeAstNodeSubstitution() {
    return edgesRemovedBeforeAstNodeSubstitution;
  }

  public ImmutableBiMap<CFAEdge, CFAEdge> getNewEdgeToOldEdgeAfterAstNodeSubstitution() {
    return newEdgeToOldEdgeAfterAstNodeSubstitution;
  }

  public ImmutableSet<CFANode> getNodesAddedBeforeAstNodeSubstitution() {
    return nodesAddedBeforeAstNodeSubstitution;
  }

  public ImmutableSet<CFANode> getNodesRemovedBeforeAstNodeSubstitution() {
    return nodesRemovedBeforeAstNodeSubstitution;
  }

  public ImmutableBiMap<CFANode, CFANode> getNewNodeToOldNodeAfterAstNodeSubstitution() {
    return newNodeToOldNodeAfterAstNodeSubstitution;
  }

  /**
   * Returns the truly new {@link CFAEdge}s in the transformed CFA, i.e., skips the ones that were
   * only trivial substitutes of the same type and with the same AST node.
   */
  public ImmutableSet<CFAEdge> getNewEdges() {
    return newEdges;
  }

  /**
   * Returns the {@link CFAEdge}s from the untransformed CFA that are truly missing in the
   * transformed CFA, either because they were removed or non-trivially substituted.
   */
  public ImmutableSet<CFAEdge> getMissingEdges() {
    return missingEdges;
  }

  /**
   * Returns the truly new {@link CFANode}s in the transformed CFA, i.e., skips the ones that are
   * only trivial substitutes of the same type and with the same AST nodes.
   */
  public ImmutableSet<CFANode> getNewNodes() {
    return newNodes;
  }

  /**
   * Returns the {@link CFANode}s from the untransformed CFA that are truly missing in the
   * transformed CFA, either because they were removed or non-trivially substituted.
   */
  public ImmutableSet<CFANode> getMissingNodes() {
    return missingNodes;
  }

  /**
   * Returns whether the given CFAEdge is one of the truly new CFAEdges in the transformed CFA.
   *
   * @param pEdge The edge
   * @return {@code true} if the edge was added to the {@link CfaMutableNetwork} and/or its {@link
   *     AAstNode} was non-trivially substituted, {@code false} if the edge is part of the
   *     untransformed CFA or if its ASTNode was only trivially substituted
   */
  public boolean isNew(final CFAEdge pEdge) {
    return newEdges.contains(pEdge);
  }

  /**
   * Returns whether the given CFANode is one of the truly new CFANodes in the transformed CFA.
   *
   * @param pNode The node
   * @return {@code true} if the node was added to the {@link CfaMutableNetwork} and/or its {@link
   *     AAstNode} was non-trivially substituted, {@code false} if the edge is part of the
   *     untransformed CFA or if its ASTNode was only trivially substituted
   */
  public boolean isNew(final CFANode pNode) {
    return newNodes.contains(pNode);
  }

  /**
   * Returns the edge from the untransformed CFA that was substituted with the given edge if there
   * is one, {@code Optional.empty()} otherwise.
   */
  public Optional<CFAEdge> getEdgeBeforeTransformation(final CFAEdge pEdge) {

    if (!newEdgeToOldEdgeAfterAstNodeSubstitution.containsKey(pEdge)) {
      // edge must be part of the untransformed CFA
      assert allEdgesOfUntransformedCfa.contains(pEdge)
          : "CFAEdge " + pEdge + " is neither part of the untransformed nor the transformed CFA";
      return Optional.of(pEdge);
    }
    final CFAEdge pEdgeBeforeSubstitution = newEdgeToOldEdgeAfterAstNodeSubstitution.get(pEdge);

    if (edgesAddedBeforeAstNodeSubstitution.contains(pEdgeBeforeSubstitution)) {
      return Optional.empty();
    }
    return Optional.ofNullable(pEdgeBeforeSubstitution);
  }

  private static ImmutableSet<CFAEdge> getAllEdges(final CFA pCfa) {
    checkNotNull(pCfa);

    final ImmutableSet.Builder<CFAEdge> allEdges = ImmutableSet.builder();
    pCfa.getAllNodes().stream().map(CFAUtils::allLeavingEdges).forEach(allEdges::addAll);
    return allEdges.build();
  }

  private static void collectNonTrivialEdgeChanges(
      final Set<CFAEdge> pEdgesAddedBeforeSubstitution,
      final Set<CFAEdge> pEdgesRemovedBeforeSubstitution,
      final BiMap<CFAEdge, CFAEdge> pOldEdgeToNewEdgeAfterAstNodeSubstitution,
      final ImmutableSet.Builder<CFAEdge> pNewEdgesBuilder,
      final ImmutableSet.Builder<CFAEdge> pMissingEdgesBuilder) {

    for (final CFAEdge addedEdge : pEdgesAddedBeforeSubstitution) {
      // add the substitute edge if there is one, add the edge itself otherwise
      pNewEdgesBuilder.add(
          pOldEdgeToNewEdgeAfterAstNodeSubstitution.getOrDefault(addedEdge, addedEdge));
    }

    for (final Entry<CFAEdge, CFAEdge> oldAndNewEdge :
        pOldEdgeToNewEdgeAfterAstNodeSubstitution.entrySet()) {
      final CFAEdge oldEdge = oldAndNewEdge.getKey();
      final CFAEdge newEdge = oldAndNewEdge.getValue();

      // assert that the substitution did not affect the edge class
      assert oldEdge.getClass().equals(newEdge.getClass());

      if (pEdgesAddedBeforeSubstitution.contains(oldEdge)) {
        // the edge was already handled in the first loop
        continue;
      }

      // context: the old edge already existed before the transformation
      // check whether it was only a trivial identity substitution
      if (!oldEdge.getRawAST().equals(newEdge.getRawAST())) {
        pNewEdgesBuilder.add(newEdge);
        pMissingEdgesBuilder.add(oldEdge);
      }
    }

    pMissingEdgesBuilder.addAll(pEdgesRemovedBeforeSubstitution);
  }

  private static void collectNonTrivialNodeChanges(
      final Set<CFANode> pNodesAddedBeforeSubstitution,
      final Set<CFANode> pNodesRemovedBeforeSubstitution,
      final BiMap<CFANode, CFANode> pOldNodeToNewNodeAfterAstNodeSubstitution,
      final ImmutableSet.Builder<CFANode> pNewNodesBuilder,
      final ImmutableSet.Builder<CFANode> pMissingNodesBuilder) {

    for (final CFANode addedNode : pNodesAddedBeforeSubstitution) {
      // add the substitute node if there is one, add the node itself otherwise
      pNewNodesBuilder.add(
          pOldNodeToNewNodeAfterAstNodeSubstitution.getOrDefault(addedNode, addedNode));
    }

    for (final Entry<CFANode, CFANode> oldAndNewNode :
        pOldNodeToNewNodeAfterAstNodeSubstitution.entrySet()) {
      final CFANode oldNode = oldAndNewNode.getKey();
      final CFANode newNode = oldAndNewNode.getValue();

      // assert that the substitution did not affect the node class
      assert oldNode.getClass().equals(newNode.getClass());

      if (pNodesAddedBeforeSubstitution.contains(oldNode)) {
        // the node was already handled in the first loop
        continue;
      }

      // context: the old node already existed before the transformation
      // continue if it was only a trivial identity substitution
      if (oldNode instanceof CFunctionEntryNode) {
        final CFunctionEntryNode oldEntryNode = (CFunctionEntryNode) oldNode;
        final CFunctionEntryNode newEntryNode = (CFunctionEntryNode) newNode;

        if (oldEntryNode.getFunctionDefinition().equals(newEntryNode.getFunctionDefinition())
            && oldEntryNode.getReturnVariable().equals(newEntryNode.getReturnVariable())) {
          continue;
        }

      } else if (oldNode instanceof CFALabelNode) {
        final CFALabelNode oldLabelNode = (CFALabelNode) oldNode;
        final CFALabelNode newLabelNode = (CFALabelNode) newNode;

        if (oldLabelNode.getLabel().equals(newLabelNode.getLabel())) {
          continue;
        }

      } else {
        // TODO is it okay not to consider FunctionDeclaration changes in every node?
        // do nothing for other types of CFANodes
        continue;
      }

      pNewNodesBuilder.add(newNode);
      pMissingNodesBuilder.add(oldNode);
    }

    pMissingNodesBuilder.addAll(pNodesRemovedBeforeSubstitution);
  }
}
