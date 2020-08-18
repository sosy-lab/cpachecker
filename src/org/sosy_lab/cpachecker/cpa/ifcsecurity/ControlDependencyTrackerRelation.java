// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * CPA-Transfer-Relation for tracking the Active Control Dependencies
 */
public class ControlDependencyTrackerRelation extends ForwardingTransferRelation<ControlDependencyTrackerState, ControlDependencyTrackerState, Precision> {

  @SuppressWarnings("unused")
  private LogManager logger;
  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;
  /** Internal Variable: Control Dependencies */
  private Map<CFANode, NavigableSet<CFANode>> rcd;

  /**
   * Constructor for Transfer-Relation of ControlDependencyTracker
   *
   * @param pLogger Logger
   * @param pShutdownNotifier ShutdownNotifier
   * @param pRcd ControlDependencies
   */
  public ControlDependencyTrackerRelation(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Map<CFANode, NavigableSet<CFANode>> pRcd) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    rcd=pRcd;
  }

  @Override
  protected ControlDependencyTrackerState handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException {
    //Add a new Controldependency to track
    ControlDependencyTrackerState result=state.clone();
    CFANode from=pCfaEdge.getPredecessor();
    CFANode to=pCfaEdge.getSuccessor();
    result.getGuards().addDependancy(from, to, pExpression, pTruthAssumption);
    return result;
  }

  @Override
  protected ControlDependencyTrackerState handleFunctionCallEdge(CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments, List<CParameterDeclaration> pParameters,
      String pCalledFunctionName) throws CPATransferException {
    return state;
  }

  @Override
  protected ControlDependencyTrackerState handleFunctionReturnEdge(CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall, CFunctionCall pSummaryExpr, String pCallerFunctionName)
          throws CPATransferException {
    return state;
  }

  @Override
  protected ControlDependencyTrackerState handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    return state;
  }

  @Override
  protected ControlDependencyTrackerState handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
      throws CPATransferException {
    return state;
  }

  @Override
  protected ControlDependencyTrackerState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    return state;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected ControlDependencyTrackerState handleBlankEdge(BlankEdge pCfaEdge) {
    //Clone for Merge reasons
    return state.clone();
  }

  @Override
  protected ControlDependencyTrackerState handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge) throws CPATransferException {
    return state;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    assert pState instanceof ControlDependencyTrackerState;

    ControlDependencyTrackerState trackerState = (ControlDependencyTrackerState) pState;

    //Remove Unneeded Control Dependencies
    CFANode currentNode=pCfaEdge.getSuccessor();
    trackerState.getGuards().changeContextStack(currentNode, rcd.get(currentNode));

    //Assume-Edges
    //DependencyTrackerCPA strengthens the information contained in Variables of a control branch
    if(pCfaEdge instanceof CAssumeEdge){
      for(AbstractState astate: pOtherStates){
        if(astate instanceof DependencyTrackerState){
          DependencyTrackerState ostate=(DependencyTrackerState) astate;
          Map<Variable, NavigableSet<Variable>> dependencies = ostate.getDependencies();
          NavigableSet<Variable> newmap = new TreeSet<>();
          for (Variable var : trackerState.getGuards().getTopVariables()) {
            if (dependencies.containsKey(var)) {
              newmap.addAll(dependencies.get(var));
            } else {
              newmap.add(var);
            }
          }
          trackerState.getGuards().changeTopVariables(newmap);
        }
      }
    }

    return Collections.singleton(pState);
  }
}
