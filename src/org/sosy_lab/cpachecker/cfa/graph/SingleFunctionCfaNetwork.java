// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Traverser;
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** A {@link CfaNetwork} representing a single function specified by its function entry node. */
final class SingleFunctionCfaNetwork extends AbstractCfaNetwork {

  private final FunctionEntryNode functionEntryNode;

  private SingleFunctionCfaNetwork(FunctionEntryNode pFunctionEntryNode) {
    functionEntryNode = checkNotNull(pFunctionEntryNode);
  }

  static CfaNetwork forFunction(FunctionEntryNode pFunctionEntryNode) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new SingleFunctionCfaNetwork(pFunctionEntryNode));
  }

  private boolean isSuperEdge(CFAEdge pEdge) {
    return pEdge instanceof FunctionCallEdge || pEdge instanceof FunctionReturnEdge;
  }

  @Override
  public Set<CFANode> nodes() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Traverser.forGraph(SingleFunctionCfaNetwork.this)
            .depthFirstPreOrder(functionEntryNode)
            .iterator();
      }
    };
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.enteringEdges(pNode).filter(edge -> !isSuperEdge(edge)).iterator();
      }
    };
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return CFAUtils.leavingEdges(pNode).filter(edge -> !isSuperEdge(edge)).iterator();
      }
    };
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return EndpointPair.ordered(pEdge.getPredecessor(), pEdge.getSuccessor());
  }

  @Override
  public Set<FunctionEntryNode> entryNodes() {
    return ImmutableSet.of(functionEntryNode);
  }
}
