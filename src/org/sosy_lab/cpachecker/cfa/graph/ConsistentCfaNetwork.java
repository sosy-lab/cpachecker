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
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This class provides a skeletal implementation of {@link CfaNetwork} where the CFA represented by
 * the {@link ConsistentCfaNetwork} always matches the CFA, represented by its individual elements
 * (e.g., {@link CFAEdge#getSuccessor()} and {@link ConsistentCfaNetwork#successor(CFAEdge)} always
 * return the same value), if summary edges are ignored.
 *
 * <p>All changes to a CFA are reflected in a {@link ConsistentCfaNetwork}.
 *
 * <p>IMPORTANT: Ignoring all summary edges, there must be no parallel edges (i.e., multiple
 * directed edges from some node {@code u} to some node {@code v}) and must never be added in the
 * future (if the CFA is mutable). Be aware that this requirement is not enforced, if Java
 * assertions are disabled.
 */
public abstract class ConsistentCfaNetwork extends AbstractCfaNetwork {

  /**
   * Returns a set for the specified duplicate-free node collection.
   *
   * @param <N> the type of CFA node
   * @param pNodes the duplicate-free node collection to get a set for
   * @return a set for the specified duplicate-free node collection
   */
  protected final <N extends CFANode> Set<N> duplicateFreeNodeCollectionToSet(
      Collection<N> pNodes) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<N> iterator() {
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
  public CFANode predecessor(CFAEdge pEdge) {
    return pEdge.getPredecessor();
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return pEdge.getSuccessor();
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionExitNode pFunctionExitNode) {
    return pFunctionExitNode.getEntryNode();
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return pFunctionEntryNode.getExitNode();
  }

  // on-the-fly filters

  @Override
  public CfaNetwork withFilteredFunctions(Predicate<AFunctionDeclaration> pRetainPredicate) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new EntryExitConsistentCfaNetwork(super.withFilteredFunctions(pRetainPredicate)));
  }

  private static final class EntryExitConsistentCfaNetwork extends ForwardingCfaNetwork {

    private final CfaNetwork delegate;

    private EntryExitConsistentCfaNetwork(CfaNetwork pDelegate) {
      delegate = pDelegate;
    }

    @Override
    protected CfaNetwork delegate() {
      return delegate;
    }

    @Override
    public FunctionEntryNode functionEntryNode(FunctionExitNode pFunctionExitNode) {
      return pFunctionExitNode.getEntryNode();
    }

    @Override
    public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
      return pFunctionEntryNode.getExitNode();
    }

    @Override
    public CfaNetwork withFilteredFunctions(Predicate<AFunctionDeclaration> pRetainPredicate) {
      return CheckingCfaNetwork.wrapIfAssertionsEnabled(
          new EntryExitConsistentCfaNetwork(delegate.withFilteredFunctions(pRetainPredicate)));
    }
  }
}
