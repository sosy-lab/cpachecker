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
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
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

  public static SeqConstCpaCheckerTmpStatement buildConstCpaCheckerTmpStatement(
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
    SubstituteEdge subA = Objects.requireNonNull(pSubEdges.get(statementA));
    SubstituteEdge subB = Objects.requireNonNull(pSubEdges.get(statementB));
    int newTargetPc = statementB.getSuccessor().pc;

    return new SeqConstCpaCheckerTmpStatement(
        (CDeclarationEdge) pThreadEdge.cfaEdge, subA, subB, pPcLeftHandSide, newTargetPc);
  }

  public static Optional<SeqCaseBlockStatement> tryBuildCaseBlockStatementFromEdge(
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

    if (isBlankCaseBlock(pThread, pSubstituteEdge, successor)) {
      return Optional.of(new SeqBlankStatement(pcLeftHandSide, targetPc));

    } else {
      if (pSubstituteEdge.cfaEdge instanceof CAssumeEdge assumeEdge) {
        return Optional.of(buildAssumeStatement(pFirstEdge, assumeEdge, pcLeftHandSide, targetPc));

      } else if (pSubstituteEdge.cfaEdge instanceof CFunctionSummaryEdge functionSummary) {
        // function summaries -> store calling context in return_pc
        if (MPORUtil.isReachErrorCall(functionSummary)) {
          // TODO this is the only reason we use Optional here... maybe merge with blank statements?
          return Optional.empty();
        } else {
          return Optional.of(buildReturnPcWriteStatement(pThreadEdge, pGhostVariables.function));
        }

      } else if (pSubstituteEdge.cfaEdge instanceof CFunctionCallEdge functionCall) {
        // function calls -> store parameters in ghost variables
        if (MPORUtil.isReachErrorCall(functionCall)) {
          // inject non-inlined reach_error
          return Optional.of(new SeqReachErrorStatement());
        }
        assert pGhostVariables.function.parameterAssignments.containsKey(pThreadEdge);
        ImmutableList<FunctionParameterAssignment> assignments =
            Objects.requireNonNull(pGhostVariables.function.parameterAssignments.get(pThreadEdge));
        if (assignments.isEmpty()) {
          return Optional.of(new SeqBlankStatement(pcLeftHandSide, targetPc));
        }
        // TODO refactor so that we can use a single statement for all parameter assignments
        for (int i = 0; i < assignments.size(); i++) {
          FunctionParameterAssignment assign = assignments.get(i);
          // if this is the last param assign, add the pcUpdate, otherwise empty
          boolean lastParam = i == assignments.size() - 1;
          return Optional.of(
              new SeqParameterAssignStatement(
                  assign.statement,
                  lastParam ? Optional.of(pcLeftHandSide) : Optional.empty(),
                  lastParam ? Optional.of(targetPc) : Optional.empty()));
        }

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
      return new SeqReturnValueAssignStatements(returnPc, assigns, pcLeftHandSide, pTargetPc);
    }
  }

  private static SeqCaseBlockStatement buildStatementFromPthreadFunction(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      GhostPcVariables pPcLeftHandSides,
      GhostThreadVariables pThreadVars,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    PthreadFunctionType pthreadFunctionType = PthreadFunctionType.getPthreadFuncType(cfaEdge);
    CLeftHandSide pcLeftHandSide = pPcLeftHandSides.get(pThread.id);

    return switch (pthreadFunctionType) {
      case PTHREAD_CREATE ->
          buildThreadCreationStatement(pThread, pAllThreads, cfaEdge, pTargetPc, pPcLeftHandSides);
      case PTHREAD_MUTEX_LOCK ->
          buildMutexLockStatement(pThread, pThreadEdge, pTargetPc, pcLeftHandSide, pThreadVars);
      case PTHREAD_MUTEX_UNLOCK ->
          buildMutexUnlockStatement(cfaEdge, pTargetPc, pcLeftHandSide, pThreadVars);
      case PTHREAD_JOIN ->
          buildThreadJoinStatement(
              pThread, cfaEdge, pTargetPc, pPcLeftHandSides, pThreadVars, pBinaryExpressionBuilder);
      case __VERIFIER_ATOMIC_BEGIN ->
          buildAtomicBeginStatement(pThread, pTargetPc, pPcLeftHandSides, pThreadVars);
      case __VERIFIER_ATOMIC_END ->
          buildAtomicEndStatement(pThread, pTargetPc, pPcLeftHandSides, pThreadVars);
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
      GhostPcVariables pPcLeftHandSides) {

    CExpression pthreadT = PthreadUtil.extractPthreadT(pCfaEdge);
    MPORThread createdThread = PthreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
    return new SeqThreadCreationStatement(
        createdThread.id, pThread.id, pTargetPc, pPcLeftHandSides);
  }

  private static SeqMutexLockStatement buildMutexLockStatement(
      MPORThread pThread,
      ThreadEdge pThreadEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      GhostThreadVariables pThreadVars) {

    CIdExpression lockedMutexT = PthreadUtil.extractPthreadMutexT(pThreadEdge.cfaEdge);
    assert pThreadVars.locked.containsKey(lockedMutexT);
    CIdExpression lockedVar =
        Objects.requireNonNull(pThreadVars.locked.get(lockedMutexT)).idExpression;
    assert pThreadVars.locks.containsKey(pThread);
    assert Objects.requireNonNull(pThreadVars.locks.get(pThread)).containsKey(lockedMutexT);
    CIdExpression mutexAwaits =
        Objects.requireNonNull(
                Objects.requireNonNull(pThreadVars.locks.get(pThread)).get(lockedMutexT))
            .idExpression;
    return new SeqMutexLockStatement(lockedVar, mutexAwaits, pPcLeftHandSide, pTargetPc);
  }

  private static SeqMutexUnlockStatement buildMutexUnlockStatement(
      CFAEdge pCfaEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      GhostThreadVariables pThreadVars) {

    CIdExpression unlockedMutexT = PthreadUtil.extractPthreadMutexT(pCfaEdge);
    assert pThreadVars.locked.containsKey(unlockedMutexT);
    // assign 0 to locked variable
    CExpressionAssignmentStatement lockedFalse =
        SeqExpressionAssignmentStatement.build(
            Objects.requireNonNull(pThreadVars.locked.get(unlockedMutexT)).idExpression,
            SeqIntegerLiteralExpression.INT_0);
    return new SeqMutexUnlockStatement(lockedFalse, pPcLeftHandSide, pTargetPc);
  }

  private static SeqThreadJoinStatement buildThreadJoinStatement(
      MPORThread pThread,
      CFAEdge pCfaEdge,
      int pTargetPc,
      GhostPcVariables pPcLeftHandSides,
      GhostThreadVariables pThreadVars,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    MPORThread targetThread = PthreadUtil.extractThread(pThreadVars.joins.keySet(), pCfaEdge);
    CIdExpression threadJoins =
        Objects.requireNonNull(
                Objects.requireNonNull(pThreadVars.joins.get(pThread)).get(targetThread))
            .idExpression;
    return new SeqThreadJoinStatement(
        targetThread.id,
        threadJoins,
        pThread.id,
        pTargetPc,
        pPcLeftHandSides,
        pBinaryExpressionBuilder);
  }

  private static SeqAtomicBeginStatement buildAtomicBeginStatement(
      MPORThread pThread,
      int pTargetPc,
      GhostPcVariables pPcLeftHandSides,
      GhostThreadVariables pThreadVars) {

    assert pThreadVars.atomicLocked.isPresent();
    CIdExpression atomicLocked =
        Objects.requireNonNull(pThreadVars.atomicLocked.orElseThrow().idExpression);
    assert pThreadVars.begins.containsKey(pThread);
    CIdExpression beginsVar = Objects.requireNonNull(pThreadVars.begins.get(pThread)).idExpression;
    return new SeqAtomicBeginStatement(
        atomicLocked, beginsVar, pPcLeftHandSides.get(pThread.id), pTargetPc);
  }

  private static SeqAtomicEndStatement buildAtomicEndStatement(
      MPORThread pThread,
      int pTargetPc,
      GhostPcVariables pPcLeftHandSides,
      GhostThreadVariables pThreadVars) {

    assert pThreadVars.atomicLocked.isPresent();
    // assign 0 to ATOMIC_LOCKED variable
    CExpressionAssignmentStatement atomicLockedFalse =
        SeqExpressionAssignmentStatement.build(
            Objects.requireNonNull(pThreadVars.atomicLocked.orElseThrow().idExpression),
            SeqIntegerLiteralExpression.INT_0);
    return new SeqAtomicEndStatement(
        atomicLockedFalse, pPcLeftHandSides.get(pThread.id), pTargetPc);
  }

  public static SeqScalarPcAssumeStatement buildScalarPcAssumeStatement(SeqStatement pStatement) {
    return new SeqScalarPcAssumeStatement(pStatement);
  }

  /**
   * Returns true if the resulting case block has only pc adjustments, i.e. no code changing the
   * input program state.
   */
  private static boolean isBlankCaseBlock(
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

    } else if (pSubstituteEdge.cfaEdge instanceof CDeclarationEdge decEdge) {
      CDeclaration dec = decEdge.getDeclaration();
      if (!(dec instanceof CVariableDeclaration varDec)) {
        // all non vars (functions, structs, ...) are declared beforehand
        return true;
      } else {
        // declaration of const int CPAchecker_TMP vars is included in the cases
        return !SeqUtil.isConstCPAcheckerTMP(varDec);
      }

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
