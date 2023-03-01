// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterators;
import com.google.common.graph.EndpointPair;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This class provides an implementation of {@link CfaNetwork} where the CFA represented by the
 * {@link CfaNetwork} always matches the CFA without summary edges that is represented by its
 * elements (e.g., {@link CFAEdge#getSuccessor()} and {@link
 * ConsistentCfaNetwork#successor(CFAEdge)} always return the same value).
 */
public final class ConsistentCfaNetwork extends AbstractCfaNetwork {

  private final Collection<CFANode> nodes;
  private final Collection<FunctionEntryNode> entryNodes;

  private ConsistentCfaNetwork(
      Collection<CFANode> pNodes, Collection<FunctionEntryNode> pEntryNodes) {
    nodes = checkNotNull(pNodes);
    entryNodes = checkNotNull(pEntryNodes);
  }

  /**
   * Returns a new consistent {@link CfaNetwork} for the specified nodes.
   *
   * <p>All changes, including changes to the specified collections, are reflected in the returned
   * {@link CfaNetwork} view. The CFA represented by the returned {@link CfaNetwork} always matches
   * the CFA without summary edges that is represented by its elements (e.g., {@link
   * CFAEdge#getSuccessor()} and {@link CfaNetwork#successor(CFAEdge)} always return the same
   * value).
   *
   * <p>IMPORTANT: Ignoring all summary edges, there must be no parallel edges (i.e., edges that
   * connect the same nodes in the same order) and must never be added in the future (if the CFA is
   * mutable). Additionally, {@code pNodes} and {@code pEntryNodes} must not contain any duplicates
   * and never add them in the future. Be aware that these requirements are not enforced if Java
   * assertions are disabled.
   *
   * @param pNodes the nodes of the CFA (a {@link Collection} for flexibility, but must not contain
   *     any duplicates)
   * @param pEntryNodes the function entry nodes of the CFA (a {@link Collection} for flexibility,
   *     but must not contain any duplicates)
   * @return a new consistent {@link CfaNetwork} for the specified nodes
   * @throws NullPointerException if any parameter is {@code null}
   */
  public static CfaNetwork of(
      Collection<CFANode> pNodes, Collection<FunctionEntryNode> pEntryNodes) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new ConsistentCfaNetwork(pNodes, pEntryNodes));
  }

  // network-level accessors

  @Override
  public Set<CFANode> nodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Iterators.unmodifiableIterator(nodes.iterator());
      }

      @Override
      public int size() {
        return nodes.size();
      }

      @Override
      public boolean contains(Object pObject) {
        return nodes.contains(pObject);
      }

      @Override
      public boolean containsAll(Collection<?> pCollection) {
        return nodes.containsAll(pCollection);
      }
    };
  }

  // element-level accessors

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.enteringEdges(pNode).iterator();
      }

      @Override
      public int size() {
        return inDegree(pNode);
      }
    };
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.leavingEdges(pNode).iterator();
      }

      @Override
      public int size() {
        return outDegree(pNode);
      }
    };
  }

  @Override
  public int inDegree(CFANode pNode) {
    return pNode.getNumEnteringEdges();
  }

  @Override
  public int outDegree(CFANode pNode) {
    return pNode.getNumLeavingEdges();
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
  }

  // `CfaNetwork` specific

  @Override
  public Set<FunctionEntryNode> entryNodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<FunctionEntryNode> iterator() {
        return Iterators.unmodifiableIterator(entryNodes.iterator());
      }

      @Override
      public int size() {
        return entryNodes.size();
      }

      @Override
      public boolean contains(Object pObject) {
        return entryNodes.contains(pObject);
      }

      @Override
      public boolean containsAll(Collection<?> pCollection) {
        return entryNodes.containsAll(pCollection);
      }
    };
  }

  @Override
  public CFANode predecessor(CFAEdge pEdge) {
    return pEdge.getPredecessor();
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return pEdge.getSuccessor();
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return pFunctionEntryNode.getExitNode();
  }
}
