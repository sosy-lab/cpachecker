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
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This class provides a skeletal implementation of {@link CfaNetwork} where the CFA represented by
 * the {@link CfaNetwork} always matches the CFA represented by its elements (e.g., {@link
 * CFAEdge#getSuccessor()} and {@link ConsistentCfaNetwork#successor(CFAEdge)} always return the
 * same value).
 */
public abstract class ConsistentCfaNetwork extends AbstractCfaNetwork {

  /**
   * Returns a new consistent {@link CfaNetwork} for the specified nodes.
   *
   * <p>All changes are reflected in the returned {@link CfaNetwork} view. The CFA represented by
   * the returned {@link CfaNetwork} always matches the CFA represented by its elements (e.g.,
   * {@link CFAEdge#getSuccessor()} and {@link CfaNetwork#successor(CFAEdge)} always return the same
   * value).
   *
   * <p>IMPORTANT: There must be no parallel edges (i.e., edges that connect the same nodes in the
   * same order) and must never be added in the future (if the CFA is mutable). Additionally, {@code
   * pNodes} and {@code pEntryNodes} must not contain any duplicates and never add them in the
   * future. Be aware that these requirements are not enforced if Java assertions are disabled.
   *
   * @param pNodes the nodes of the CFA (a {@link Collection} for flexibility, but must not contain
   *     any duplicates)
   * @param pEntryNodes the function entry nodes of the CFA (a {@link Collection} for flexibility,
   *     but must not contain any duplicates)
   * @returns a new consistent {@link CfaNetwork} for the specified nodes
   * @throws NullPointerException if any parameter is {@code null}
   */
  public static CfaNetwork of(
      Collection<CFANode> pNodes, Collection<FunctionEntryNode> pEntryNodes) {
    checkNotNull(pNodes);
    checkNotNull(pEntryNodes);

    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new ConsistentCfaNetwork() {

          @Override
          public Set<CFANode> nodes() {
            return new UnmodifiableSetView<>() {

              @Override
              public Iterator<CFANode> iterator() {
                return Iterators.unmodifiableIterator(pNodes.iterator());
              }

              @Override
              public int size() {
                return pNodes.size();
              }

              @Override
              public boolean contains(Object pObject) {
                return pNodes.contains(pObject);
              }

              @Override
              public boolean containsAll(Collection<?> pCollection) {
                return pNodes.containsAll(pCollection);
              }
            };
          }

          @Override
          public Set<FunctionEntryNode> entryNodes() {
            return new UnmodifiableSetView<>() {

              @Override
              public Iterator<FunctionEntryNode> iterator() {
                return Iterators.unmodifiableIterator(pEntryNodes.iterator());
              }

              @Override
              public int size() {
                return pEntryNodes.size();
              }

              @Override
              public boolean contains(Object pObject) {
                return pEntryNodes.contains(pObject);
              }

              @Override
              public boolean containsAll(Collection<?> pCollection) {
                return pEntryNodes.containsAll(pCollection);
              }
            };
          }
        });
  }

  // element-level accessors

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.allEnteringEdges(pNode).iterator();
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
        return CFAUtils.allLeavingEdges(pNode).iterator();
      }

      @Override
      public int size() {
        return outDegree(pNode);
      }
    };
  }

  @Override
  public int inDegree(CFANode pNode) {
    int inDegree = pNode.getNumEnteringEdges();

    if (pNode.getEnteringSummaryEdge() != null) {
      inDegree++;
    }

    return inDegree;
  }

  @Override
  public int outDegree(CFANode pNode) {
    int outDegree = pNode.getNumLeavingEdges();

    if (pNode.getLeavingSummaryEdge() != null) {
      outDegree++;
    }

    return outDegree;
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
  }

  // `CfaNetwork` specific

  @Override
  public CFANode predecessor(CFAEdge pEdge) {
    return pEdge.getPredecessor();
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return pEdge.getSuccessor();
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionSummaryEdge pFunctionSummaryEdge) {
    return pFunctionSummaryEdge.getFunctionEntry();
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return pFunctionEntryNode.getExitNode();
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionCallEdge pFunctionCallEdge) {
    return pFunctionCallEdge.getSummaryEdge();
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge) {
    return pFunctionReturnEdge.getSummaryEdge();
  }
}
