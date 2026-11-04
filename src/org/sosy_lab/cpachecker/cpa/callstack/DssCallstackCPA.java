// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.OptionalAnnotation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Callstack CPA for the block analyses of distributed summary synthesis. */
public final class DssCallstackCPA extends CallstackCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DssCallstackCPA.class);
  }

  /** Whether no function of the program can call itself, see {@link DssCallstackEffect}. */
  private final boolean recursionFree;

  public DssCallstackCPA(
      Configuration pConfiguration, LogManager pLogger, @OptionalAnnotation @Nullable CFA pCfa)
      throws InvalidConfigurationException {
    super(pConfiguration, pLogger);
    if (getCallstackOptions().traverseBackwards()) {
      throw new InvalidConfigurationException("DssCallstackCPA only supports forward analyses");
    }
    // without a CFA, recursion cannot be ruled out
    recursionFree = pCfa != null && !hasRecursion(pCfa);
  }

  private static boolean hasRecursion(CFA pCfa) {
    MutableGraph<String> calls = GraphBuilder.directed().allowsSelfLoops(true).build();
    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      if (edge instanceof FunctionCallEdge) {
        calls.putEdge(
            edge.getPredecessor().getFunctionName(), edge.getSuccessor().getFunctionName());
      }
    }
    return Graphs.hasCycle(calls);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new DssCallstackDomain();
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOther) {
    return new DssCallstackDomain().isLessOrEqual(pState, pOther);
  }

  @Override
  public boolean isCoveredByRecursiveState(AbstractState pState, AbstractState pOther) {
    return isCoveredBy(pState, pOther);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return createState(null, pNode.getFunctionName(), pNode, false);
  }

  /** Creates a DSS callstack state, optionally one that may stand for an unknown callstack. */
  public DssCallstackState createState(
      @Nullable CallstackState pPreviousState,
      String pFunction,
      CFANode pCallerNode,
      boolean pCanBeTopState) {
    CallstackState wrappedState =
        new CallstackState(DssCallstackState.unwrap(pPreviousState), pFunction, pCallerNode);
    return new DssCallstackState(wrappedState, pCanBeTopState);
  }

  @Override
  public DssCallstackTransferRelation getTransferRelation() {
    return new DssCallstackTransferRelation(getCallstackOptions(), getLogger(), recursionFree);
  }
}
