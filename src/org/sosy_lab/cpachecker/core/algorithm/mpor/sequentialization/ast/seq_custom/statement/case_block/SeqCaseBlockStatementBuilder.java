// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.GhostVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnPcWrite;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.GhostFunctionVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.GhostPcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.GhostThreadVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqCaseBlockStatementBuilder {

  public static SeqReturnPcReadStatement buildReturnPcReadStatement(
      CLeftHandSide pPcLeftHandSide, CIdExpression pReturnPcVariable) {

    return new SeqReturnPcReadStatement(pPcLeftHandSide, pReturnPcVariable);
  }

  public static ImmutableList<SeqCaseBlockStatement> buildStatementsFromThreadNode(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      ThreadNode pThreadNode,
      CLeftHandSide pPcLeftHandSide,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostVariables pGhostVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqCaseBlockStatement> rStatements = ImmutableList.builder();

    boolean firstEdge = true;
    for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {
      if (MPORUtil.isConstCpaCheckerTmpDeclaration(threadEdge.cfaEdge)) {
        // handle const CPAchecker_TMP first because it requires successor nodes and edges
        rStatements.add(
            SeqCaseBlockStatementBuilder.buildConstCpaCheckerTmpStatement(
                threadEdge, pPcLeftHandSide, pCoveredNodes, pSubstituteEdges));
      } else {
        SubstituteEdge substitute = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
        Optional<SeqCaseBlockStatement> statement =
            SeqCaseBlockStatementBuilder.tryBuildCaseBlockStatementFromEdge(
                pThread,
                pAllThreads,
                firstEdge,
                threadEdge,
                substitute,
                pGhostVariables,
                pBinaryExpressionBuilder);
        if (statement.isPresent()) {
          rStatements.add(statement.orElseThrow());
        }
      }
      firstEdge = false;
    }
    return rStatements.build();
  }

  private static SeqConstCpaCheckerTmpStatement buildConstCpaCheckerTmpStatement(
      ThreadEdge pThreadEdge,
      CLeftHandSide pPcLeftHandSide,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges) {

    // ensure there are two single successors that are both statement edges
    ThreadNode successorA = pThreadEdge.getSuccessor();
    assert successorA.leavingEdges().size() == 1;
    ThreadEdge statementA = successorA.leavingEdges().iterator().next();
    assert statementA.cfaEdge instanceof CStatementEdge;
    ThreadNode successorB = statementA.getSuccessor();
    assert successorB.leavingEdges().size() == 1;
    ThreadEdge statementB = successorB.leavingEdges().iterator().next();
    assert statementB.cfaEdge instanceof CStatementEdge;

    // add successors to visited edges / nodes
    pCoveredNodes.add(successorA);
    pCoveredNodes.add(successorB);

    // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
    CFAEdge cfaEdge = Objects.requireNonNull(pSubEdges.get(pThreadEdge)).cfaEdge;
    assert cfaEdge instanceof CDeclarationEdge : "cfaEdge must declare const CPAchecker_TMP";
    SubstituteEdge subA = Objects.requireNonNull(pSubEdges.get(statementA));
    SubstituteEdge subB = Objects.requireNonNull(pSubEdges.get(statementB));
    int newTargetPc = statementB.getSuccessor().pc;

    return new SeqConstCpaCheckerTmpStatement(
        (CDeclarationEdge) cfaEdge, subA, subB, pPcLeftHandSide, newTargetPc);
  }

  private static Optional<SeqCaseBlockStatement> tryBuildCaseBlockStatementFromEdge(
      final MPORThread pThread,
      final ImmutableSet<MPORThread> pAllThreads,
      boolean pFirstEdge,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      GhostVariables pGhostVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFAEdge edge = pThreadEdge.cfaEdge;
    int targetPc = pThreadEdge.getSuccessor().pc;
    CFANode successor = pThreadEdge.getSuccessor().cfaNode;
    CLeftHandSide pcLeftHandSide = pGhostVariables.pc.get(pThread.id);

    if (yieldsBlankCaseBlock(pThread, pSubstituteEdge, successor)) {
      return Optional.of(new SeqBlankStatement(pcLeftHandSide, targetPc));

    } else {
      if (pSubstituteEdge.cfaEdge instanceof CAssumeEdge assumeEdge) {
        return Optional.of(buildAssumeStatement(pFirstEdge, assumeEdge, pcLeftHandSide, targetPc));

      } else if (pSubstituteEdge.cfaEdge instanceof CFunctionSummaryEdge functionSummary) {
        return handleFunctionSummaryEdge(functionSummary, pThreadEdge, pGhostVariables);

      } else if (pSubstituteEdge.cfaEdge instanceof CFunctionCallEdge functionCall) {
        return Optional.of(
            handleFunctionCallEdge(
                pThread.id, functionCall, pThreadEdge, targetPc, pGhostVariables));

      } else if (pSubstituteEdge.cfaEdge instanceof CReturnStatementEdge) {
        return Optional.of(
            buildReturnValueAssignmentStatement(
                pThreadEdge, targetPc, pcLeftHandSide, pGhostVariables.function));

      } else if (isExplicitlyHandledPthreadFunction(edge)) {
        return Optional.of(
            buildStatementFromPthreadFunction(
                pThread,
                pAllThreads,
                pThreadEdge,
                pSubstituteEdge,
                targetPc,
                pGhostVariables.pc,
                pGhostVariables.thread,
                pBinaryExpressionBuilder));
      }
    }
    // "leftover" edges should be statement edges
    assert pSubstituteEdge.cfaEdge instanceof CStatementEdge;
    return Optional.of(
        new SeqDefaultStatement(
            (CStatementEdge) pSubstituteEdge.cfaEdge, pcLeftHandSide, targetPc));
  }

  private static SeqAssumeStatement buildAssumeStatement(
      boolean pFirstEdge, CAssumeEdge pAssumeEdge, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    // use (else) if (condition) for all assume edges (if, for, while, switch, ...)
    SeqControlFlowStatementType statementType =
        pFirstEdge ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
    SeqControlFlowStatement statement = new SeqControlFlowStatement(pAssumeEdge, statementType);
    return new SeqAssumeStatement(statement, pPcLeftHandSide, pTargetPc);
  }

  private static SeqReturnPcWriteStatement buildReturnPcWriteStatement(
      ThreadEdge pThreadEdge, GhostFunctionVariables pFunctionVariables) {

    assert pFunctionVariables.returnPcWrites.containsKey(pThreadEdge);
    FunctionReturnPcWrite write =
        Objects.requireNonNull(pFunctionVariables.returnPcWrites.get(pThreadEdge));
    return new SeqReturnPcWriteStatement(write.returnPcVar, write.value);
  }

  private static Optional<SeqCaseBlockStatement> handleFunctionSummaryEdge(
      CFunctionSummaryEdge pFunctionSummaryEdge,
      ThreadEdge pThreadEdge,
      GhostVariables pGhostVariables) {

    // function summaries -> store calling context in return_pc
    if (MPORUtil.isReachErrorCall(pFunctionSummaryEdge)) {
      // TODO this is the only reason we use Optional here... maybe merge with blank statements?
      return Optional.empty();
    } else {
      return Optional.of(buildReturnPcWriteStatement(pThreadEdge, pGhostVariables.function));
    }
  }

  private static SeqCaseBlockStatement handleFunctionCallEdge(
      int pThreadId,
      CFunctionCallEdge pFunctionCallEdge,
      ThreadEdge pThreadEdge,
      int pTargetPc,
      GhostVariables pGhostVariables) {

    // function calls -> store parameters in ghost variables
    if (MPORUtil.isReachErrorCall(pFunctionCallEdge)) {
      // inject non-inlined reach_error
      return new SeqReachErrorStatement();
    }
    assert pGhostVariables.function.parameterAssignments.containsKey(pThreadEdge);
    CLeftHandSide pcLeftHandSide = pGhostVariables.pc.get(pThreadId);
    ImmutableList<FunctionParameterAssignment> assignments =
        Objects.requireNonNull(pGhostVariables.function.parameterAssignments.get(pThreadEdge));
    if (assignments.isEmpty()) {
      return new SeqBlankStatement(pcLeftHandSide, pTargetPc);
    }
    return new SeqParameterAssignmentStatements(assignments, pcLeftHandSide, pTargetPc);
  }

  private static SeqCaseBlockStatement buildReturnValueAssignmentStatement(
      ThreadEdge pThreadEdge,
      int pTargetPc,
      CLeftHandSide pcLeftHandSide,
      GhostFunctionVariables pFunctionVariables) {

    // TODO add support and test for pthread_join(id, &start_routine_return)
    //  where start_routine_return is assigned the return value of the threads start routine
    // returning from non-start-routine function: assign return value to return vars
    ImmutableSet<FunctionReturnValueAssignment> assigns =
        Objects.requireNonNull(pFunctionVariables.returnValueAssignments.get(pThreadEdge));
    if (assigns.isEmpty()) { // -> function does not return anything, i.e. return;
      return new SeqBlankStatement(pcLeftHandSide, pTargetPc);
    } else {
      // just get the first element in the set for the RETURN_PC
      CIdExpression returnPc = assigns.iterator().next().returnPcWrite.returnPcVar;
      return new SeqReturnValueAssignmentStatements(returnPc, assigns, pcLeftHandSide, pTargetPc);
    }
  }

  private static SeqCaseBlockStatement buildStatementFromPthreadFunction(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      GhostPcVariables pPcVariables,
      GhostThreadVariables pThreadVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    PthreadFunctionType pthreadFunctionType = PthreadFunctionType.getPthreadFuncType(cfaEdge);
    CLeftHandSide pcLeftHandSide = pPcVariables.get(pThread.id);

    return switch (pthreadFunctionType) {
      case PTHREAD_CREATE ->
          buildThreadCreationStatement(pThread, pAllThreads, cfaEdge, pTargetPc, pPcVariables);
      case PTHREAD_MUTEX_LOCK ->
          buildMutexLockStatement(
              pThread, pThreadEdge, pTargetPc, pcLeftHandSide, pThreadVariables);
      case PTHREAD_MUTEX_UNLOCK ->
          buildMutexUnlockStatement(cfaEdge, pTargetPc, pcLeftHandSide, pThreadVariables);
      case PTHREAD_JOIN ->
          buildThreadJoinStatement(
              pThread,
              cfaEdge,
              pTargetPc,
              pPcVariables,
              pThreadVariables,
              pBinaryExpressionBuilder);
      case __VERIFIER_ATOMIC_BEGIN ->
          buildAtomicBeginStatement(pThread, pTargetPc, pPcVariables, pThreadVariables);
      case __VERIFIER_ATOMIC_END ->
          buildAtomicEndStatement(pThread, pTargetPc, pPcVariables, pThreadVariables);
      default ->
          throw new AssertionError(
              "unhandled relevant pthread method: " + pthreadFunctionType.name);
    };
  }

  private static SeqThreadCreationStatement buildThreadCreationStatement(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      CFAEdge pCfaEdge,
      int pTargetPc,
      GhostPcVariables pPcVariables) {

    CExpression pthreadT = PthreadUtil.extractPthreadT(pCfaEdge);
    MPORThread createdThread = PthreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
    return new SeqThreadCreationStatement(createdThread.id, pThread.id, pTargetPc, pPcVariables);
  }

  private static SeqMutexLockStatement buildMutexLockStatement(
      MPORThread pThread,
      ThreadEdge pThreadEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      GhostThreadVariables pThreadVariables) {

    CIdExpression lockedMutexT = PthreadUtil.extractPthreadMutexT(pThreadEdge.cfaEdge);
    assert pThreadVariables.locked.containsKey(lockedMutexT);
    CIdExpression lockedVar =
        Objects.requireNonNull(pThreadVariables.locked.get(lockedMutexT)).idExpression;
    assert pThreadVariables.locks.containsKey(pThread);
    assert Objects.requireNonNull(pThreadVariables.locks.get(pThread)).containsKey(lockedMutexT);
    CIdExpression mutexAwaits =
        Objects.requireNonNull(
                Objects.requireNonNull(pThreadVariables.locks.get(pThread)).get(lockedMutexT))
            .idExpression;
    return new SeqMutexLockStatement(lockedVar, mutexAwaits, pPcLeftHandSide, pTargetPc);
  }

  private static SeqMutexUnlockStatement buildMutexUnlockStatement(
      CFAEdge pCfaEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      GhostThreadVariables pThreadVariables) {

    CIdExpression unlockedMutexT = PthreadUtil.extractPthreadMutexT(pCfaEdge);
    assert pThreadVariables.locked.containsKey(unlockedMutexT);
    // assign 0 to locked variable
    CExpressionAssignmentStatement lockedFalse =
        SeqExpressionAssignmentStatement.build(
            Objects.requireNonNull(pThreadVariables.locked.get(unlockedMutexT)).idExpression,
            SeqIntegerLiteralExpression.INT_0);
    return new SeqMutexUnlockStatement(lockedFalse, pPcLeftHandSide, pTargetPc);
  }

  private static SeqThreadJoinStatement buildThreadJoinStatement(
      MPORThread pThread,
      CFAEdge pCfaEdge,
      int pTargetPc,
      GhostPcVariables pPcVariables,
      GhostThreadVariables pThreadVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    MPORThread targetThread = PthreadUtil.extractThread(pThreadVariables.joins.keySet(), pCfaEdge);
    CIdExpression threadJoins =
        Objects.requireNonNull(
                Objects.requireNonNull(pThreadVariables.joins.get(pThread)).get(targetThread))
            .idExpression;
    return new SeqThreadJoinStatement(
        targetThread.id,
        threadJoins,
        pThread.id,
        pTargetPc,
        pPcVariables,
        pBinaryExpressionBuilder);
  }

  private static SeqAtomicBeginStatement buildAtomicBeginStatement(
      MPORThread pThread,
      int pTargetPc,
      GhostPcVariables pPcVariables,
      GhostThreadVariables pThreadVariables) {

    assert pThreadVariables.atomicLocked.isPresent();
    CIdExpression atomicLocked =
        Objects.requireNonNull(pThreadVariables.atomicLocked.orElseThrow().idExpression);
    assert pThreadVariables.begins.containsKey(pThread);
    CIdExpression beginsVar =
        Objects.requireNonNull(pThreadVariables.begins.get(pThread)).idExpression;
    return new SeqAtomicBeginStatement(
        atomicLocked, beginsVar, pPcVariables.get(pThread.id), pTargetPc);
  }

  private static SeqAtomicEndStatement buildAtomicEndStatement(
      MPORThread pThread,
      int pTargetPc,
      GhostPcVariables pPcVariables,
      GhostThreadVariables pThreadVariables) {

    assert pThreadVariables.atomicLocked.isPresent();
    // assign 0 to ATOMIC_LOCKED variable
    CExpressionAssignmentStatement atomicLockedFalse =
        SeqExpressionAssignmentStatement.build(
            Objects.requireNonNull(pThreadVariables.atomicLocked.orElseThrow().idExpression),
            SeqIntegerLiteralExpression.INT_0);
    return new SeqAtomicEndStatement(atomicLockedFalse, pPcVariables.get(pThread.id), pTargetPc);
  }

  public static SeqScalarPcAssumeStatement buildScalarPcAssumeStatement(SeqStatement pStatement) {
    return new SeqScalarPcAssumeStatement(pStatement);
  }

  /**
   * Returns true if the resulting case block has only pc adjustments, i.e. no code changing the
   * input program state.
   */
  private static boolean yieldsBlankCaseBlock(
      MPORThread pThread, SubstituteEdge pSubstituteEdge, CFANode pSuccessor) {

    // exiting start routine of thread -> blank, just set pc[i] = -1;
    if (pSuccessor instanceof FunctionExitNode
        && pSuccessor.getFunction().getType().equals(pThread.startRoutine)) {
      // TODO this needs to be refactored once we support start routine return values
      //  that can be used with pthread_join -> block may not be blank but sets the return value
      return true;

    } else if (pSuccessor.getFunctionName().equals(SeqToken.reach_error)) {
      // if we enter reach_error, include only call edge (to inject reach_error)
      return !(pSubstituteEdge.cfaEdge instanceof CFunctionCallEdge);

    } else if (pSubstituteEdge.cfaEdge instanceof BlankEdge) {
      // blank edges have no code
      assert pSubstituteEdge.cfaEdge.getCode().isEmpty();
      return true;

    } else if (pSubstituteEdge.cfaEdge instanceof CDeclarationEdge) {
      // all variables, functions, structs... are declared beforehand -> blank
      return true;

    } else if (PthreadFunctionType.callsAnyPthreadFunc(pSubstituteEdge.cfaEdge)) {
      // not explicitly handled PthreadFunc -> empty case code
      return !isExplicitlyHandledPthreadFunction(pSubstituteEdge.cfaEdge);
    }
    return false;
  }

  // TODO move to PthreadUtil?
  /**
   * Returns true if the semantics of the pthread method in pEdge is considered in the
   * sequentialization, i.e. the case block contains code. A function may be supported by MPOR but
   * not considered in the sequentialization.
   */
  private static boolean isExplicitlyHandledPthreadFunction(CFAEdge pEdge) {
    if (PthreadFunctionType.callsAnyPthreadFunc(pEdge)) {
      return PthreadFunctionType.getPthreadFuncType(pEdge).isExplicitlyHandled;
    }
    return false;
  }
}
