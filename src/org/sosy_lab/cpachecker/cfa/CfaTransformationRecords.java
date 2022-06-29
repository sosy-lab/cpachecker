// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;

public class CfaTransformationRecords {

  private final CFA originalCfa;
  private final Set<CFAEdge> addedEdges;
  private final Set<CFAEdge> removedEdges;
  private final BiMap<CFAEdge, CFAEdge> oldEdgeToNewEdgeAfterAstNodeSubstitution;
  private final Set<CFANode> addedNodes;
  private final Set<CFANode> removedNodes;
  private final BiMap<CFANode, CFANode> oldNodeToNewNodeAfterAstNodeSubstitution;

  private final Set<CFAEdge> newEdges;
  private final Set<CFAEdge> missingEdges;

  private final Set<CFANode> newNodes;
  private final Set<CFANode> missingNodes;

  public CfaTransformationRecords(
      final CFA pOriginalCfa,
      final Set<CFAEdge> pAddedEdges,
      final Set<CFAEdge> pRemovedEdges,
      final BiMap<CFAEdge, CFAEdge> pOldEdgeToNewEdgeAfterAstNodeSubstitution,
      final Set<CFANode> pAddedNodes,
      final Set<CFANode> pRemovedNodes,
      final BiMap<CFANode, CFANode> pOldNodeToNewNodeAfterAstNodeSubstitution) {

    originalCfa = pOriginalCfa;
    addedEdges = pAddedEdges;
    removedEdges = pRemovedEdges;
    oldEdgeToNewEdgeAfterAstNodeSubstitution = pOldEdgeToNewEdgeAfterAstNodeSubstitution;
    addedNodes = pAddedNodes;
    removedNodes = pRemovedNodes;
    oldNodeToNewNodeAfterAstNodeSubstitution = pOldNodeToNewNodeAfterAstNodeSubstitution;

    final ImmutableSet.Builder<CFAEdge> newEdgesBuilder = ImmutableSet.builder();
    final ImmutableSet.Builder<CFAEdge> missingEdgesBuilder = ImmutableSet.builder();
    collectNonTrivialEdgeChanges(
        pAddedEdges,
        pRemovedEdges,
        pOldEdgeToNewEdgeAfterAstNodeSubstitution,
        newEdgesBuilder,
        missingEdgesBuilder);
    newEdges = newEdgesBuilder.build();
    missingEdges = missingEdgesBuilder.build();

    final ImmutableSet.Builder<CFANode> newNodesBuilder = ImmutableSet.builder();
    final ImmutableSet.Builder<CFANode> missingNodesBuilder = ImmutableSet.builder();
    collectNonTrivialNodeChanges(
        pAddedNodes,
        pRemovedNodes,
        pOldNodeToNewNodeAfterAstNodeSubstitution,
        newNodesBuilder,
        missingNodesBuilder);
    newNodes = newNodesBuilder.build();
    missingNodes = missingNodesBuilder.build();
  }

  public static CfaTransformationRecords getTransformationRecordsForUntransformedCfa(
      final CFA pCfa) {

    return new CfaTransformationRecords(
        /* pOriginalCfa = */ pCfa,
        /* pAddedEdges = */ ImmutableSet.of(),
        /* pRemovedEdges =  */ ImmutableSet.of(),
        /* pOldEdgeToNewEdgeAfterAstNodeSubstitution = */ ImmutableBiMap.of(),
        /* pAddedNodes =  */ ImmutableSet.of(),
        /* pRemovedNodes = */ ImmutableSet.of(),
        /* pOldNodeToNewNodeAfterAstNodeSubstitution = */ ImmutableBiMap.of());
  }

  public CFA getOriginalCfa() {
    return originalCfa;
  }

  public Set<CFAEdge> getAddedEdges() {
    return ImmutableSet.copyOf(addedEdges);
  }

  public Set<CFAEdge> getRemovedEdges() {
    return ImmutableSet.copyOf(removedEdges);
  }

  public BiMap<CFAEdge, CFAEdge> getOldEdgeToNewEdgeAfterAstNodeSubstitution() {
    return ImmutableBiMap.copyOf(oldEdgeToNewEdgeAfterAstNodeSubstitution);
  }

  public Set<CFANode> getAddedNodes() {
    return ImmutableSet.copyOf(addedNodes);
  }

  public Set<CFANode> getRemovedNodes() {
    return ImmutableSet.copyOf(removedNodes);
  }

  public BiMap<CFANode, CFANode> getOldNodeToNewNodeAfterAstNodeSubstitution() {
    return ImmutableBiMap.copyOf(oldNodeToNewNodeAfterAstNodeSubstitution);
  }

  /**
   * Returns the truly new {@link CFAEdge}s in the transformed CFA, i.e., skips the ones that were
   * only trivial substitutes of the same type and with the same AST node.
   */
  public Set<CFAEdge> getNewEdges() {
    return ImmutableSet.copyOf(newEdges);
  }

  /**
   * Returns the {@link CFAEdge}s from the untransformed CFA that are truly missing in the
   * transformed CFA, either because they were removed or non-trivially substituted.
   */
  public Set<CFAEdge> getMissingEdges() {
    return ImmutableSet.copyOf(missingEdges);
  }

  /**
   * Returns the truly new {@link CFANode}s in the transformed CFA, i.e., skips the ones that are
   * only trivial substitutes of the same type and with the same AST nodes.
   */
  public Set<CFANode> getNewNodes() {
    return ImmutableSet.copyOf(newNodes);
  }

  /**
   * Returns the {@link CFANode}s from the untransformed CFA that are truly missing in the
   * transformed CFA, either because they were removed or non-trivially substituted.
   */
  public Set<CFANode> getMissingNodes() {
    return ImmutableSet.copyOf(missingNodes);
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

      } else {
        // TODO Do we really want to consider FunctionDeclaration changes in every node?
        if (oldNode.getFunction().equals(newNode.getFunction())) {
          continue;
        }
      }

      pNewNodesBuilder.add(newNode);
      pMissingNodesBuilder.add(oldNode);
    }

    pMissingNodesBuilder.addAll(pNodesRemovedBeforeSubstitution);
  }
}
