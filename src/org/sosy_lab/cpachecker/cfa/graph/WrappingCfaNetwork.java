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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.UnmodifiableSetView;

final class WrappingCfaNetwork implements CfaNetwork {

  private final CFA cfa;

  WrappingCfaNetwork(CFA pCfa) {
    cfa = checkNotNull(pCfa);
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
  public CFANode predecessor(CFAEdge pEdge) {
    return pEdge.getPredecessor();
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
  public CFANode successor(CFAEdge pEdge) {
    return pEdge.getSuccessor();
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
  }

  @Override
  public Set<CFANode> nodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Iterators.unmodifiableIterator(cfa.getAllNodes().iterator());
      }

      @Override
      public int size() {
        return cfa.getAllNodes().size();
      }

      @Override
      public boolean contains(Object pObject) {
        return cfa.getAllNodes().contains(pObject);
      }

      @Override
      public boolean containsAll(Collection<?> pCollection) {
        return cfa.getAllNodes().containsAll(pCollection);
      }
    };
  }

  @Override
  public Optional<FunctionExitNode> getFunctionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return Optional.of(pFunctionEntryNode.getExitNode());
  }

  @Override
  public FunctionSummaryEdge getFunctionSummaryEdge(FunctionCallEdge pFunctionCallEdge) {
    return pFunctionCallEdge.getSummaryEdge();
  }

  @Override
  public FunctionSummaryEdge getFunctionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge) {
    return pFunctionReturnEdge.getSummaryEdge();
  }

  @Override
  public FunctionEntryNode getFunctionEntryNode(FunctionSummaryEdge pFunctionSummaryEdge) {
    return pFunctionSummaryEdge.getFunctionEntry();
  }
}
