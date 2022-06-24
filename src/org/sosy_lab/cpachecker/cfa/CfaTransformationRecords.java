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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CfaTransformationRecords {

  private final Set<CFAEdge> addedEdges;
  private final Set<CFAEdge> removedEdges;
  private final BiMap<CFAEdge, CFAEdge> newEdgeToOldEdgeAfterAstNodeSubstitution;
  private final Set<CFANode> addedNodes;
  private final Set<CFANode> removedNodes;
  private final BiMap<CFANode, CFANode> newNodeToOldNodeAfterAstNodeSubstitution;

  public CfaTransformationRecords(
      final Set<CFAEdge> pAddedEdges,
      final Set<CFAEdge> pRemovedEdges,
      final BiMap<CFAEdge, CFAEdge> pNewEdgeToOldEdgeAfterAstNodeSubstitution,
      final Set<CFANode> pAddedNodes,
      final Set<CFANode> pRemovedNodes,
      final BiMap<CFANode, CFANode> pNewNodeToOldNodeAfterAstNodeSubstitution) {

    addedEdges = pAddedEdges;
    removedEdges = pRemovedEdges;
    newEdgeToOldEdgeAfterAstNodeSubstitution = pNewEdgeToOldEdgeAfterAstNodeSubstitution;
    addedNodes = pAddedNodes;
    removedNodes = pRemovedNodes;
    newNodeToOldNodeAfterAstNodeSubstitution = pNewNodeToOldNodeAfterAstNodeSubstitution;
  }

  public Set<CFAEdge> getAddedEdges() {
    return ImmutableSet.copyOf(addedEdges);
  }

  public Set<CFAEdge> getRemovedEdges() {
    return ImmutableSet.copyOf(removedEdges);
  }

  public BiMap<CFAEdge, CFAEdge> getNewEdgeToOldEdgeAfterAstNodeSubstitution() {
    return ImmutableBiMap.copyOf(newEdgeToOldEdgeAfterAstNodeSubstitution);
  }

  public Set<CFANode> getAddedNodes() {
    return ImmutableSet.copyOf(addedNodes);
  }

  public Set<CFANode> getRemovedNodes() {
    return ImmutableSet.copyOf(removedNodes);
  }

  public BiMap<CFANode, CFANode> getNewNodeToOldNodeAfterAstNodeSubstitution() {
    return ImmutableBiMap.copyOf(newNodeToOldNodeAfterAstNodeSubstitution);
  }
}
