// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGTransferRelation
    extends ForwardingTransferRelation<Collection<SMGState>, SMGState, SMGPrecision> {

  private final SMGOptions options;
  @SuppressWarnings("unused")
  private final MachineModel machineModel;
  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  public SMGTransferRelation(
      SMGOptions pOptions,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier) {
    options = pOptions;
    machineModel = pMachineModel;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  protected Collection<SMGState> postProcessing(Collection<SMGState> pSuccessors, CFAEdge edge) {
    Set<CSimpleDeclaration> outOfScopeVars = edge.getSuccessor().getOutOfScopeVariables();
    return transformedImmutableSetCopy(pSuccessors, successorState -> {
      SMGState prunedState = successorState.copyAndPruneOutOfScopeVariables(outOfScopeVars);
      return checkAndSetErrorRelation(prunedState);
    });
  }



  @SuppressWarnings("unused")
  private SMGState checkAndSetErrorRelation(SMGState pPrunedState) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Set<SMGState> handleBlankEdge(BlankEdge cfaEdge) throws CPATransferException {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      if (isEntryFunction(cfaEdge)) {
        return handleReturnEntryFunction(state);
      }
    }

    return Collections.singleton(state);
  }

  private Set<SMGState> handleReturnEntryFunction(SMGState pState) {
    SMGState lastState = pState;
    if (options.isHandleNonFreedMemoryInMainAsMemLeak()) {
      lastState = lastState.dropStackFrame();
    }
    return Collections.singleton(lastState.copyAndPruneUnreachable());
  }

  private boolean isEntryFunction(CFAEdge pCfaEdge) {
    return pCfaEdge.getSuccessor().getNumLeavingEdges() == 0;
  }

  @Override
  protected Collection<SMGState> handleReturnStatementEdge(CReturnStatementEdge returnEdge)
      throws CPATransferException {
    Optional<SMGObject> returnObjectOptional =
        state.getHeap().getReturnObjectForCurrentStackFrame();
    SMGState successor = state;
    if(returnObjectOptional.isPresent()) {
      successor = assignStatementToState(successor, returnObjectOptional.orElseThrow(), returnEdge);
    }

    if (isEntryFunction(returnEdge)) {
      return handleReturnEntryFunction(successor);
    }
    return Collections.singleton(successor);
  }

  @SuppressWarnings("unused")
  private SMGState assignStatementToState(
      SMGState pState,
      SMGObject pRegion,
      CReturnStatementEdge pReturnEdge) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Collection<SMGState> handleFunctionReturnEdge(
      CFunctionReturnEdge functionReturnEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws CPATransferException {
    return null;
  }

  @Override
  protected Collection<SMGState> handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> paramDecl,
      String calledFunctionName)
      throws CPATransferException {

    return null;
  }

  // Current SMGState is not fully persistent
  @Override
  protected void
      setInfo(AbstractState abstractState, Precision abstractPrecision, CFAEdge cfaEdge) {
    super.setInfo(abstractState, abstractPrecision, cfaEdge);

  }

  @Override
  protected Collection<SMGState>
      handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
          throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  protected Collection<SMGState> handleStatementEdge(CStatementEdge pCfaEdge, CStatement cStmt)
      throws CPATransferException {

    return null;
  }

  @Override
  protected List<SMGState> handleDeclarationEdge(CDeclarationEdge edge, CDeclaration cDecl)
      throws CPATransferException {
    return null;

  }


  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      Iterable<AbstractState> elements,
      CFAEdge cfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    return null;
  }

}
