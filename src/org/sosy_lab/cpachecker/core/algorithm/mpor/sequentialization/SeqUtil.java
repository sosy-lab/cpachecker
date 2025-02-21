// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqAssumeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqAtomicBeginStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqAtomicEndStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqConstCpaCheckerTmpStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqDefaultStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqMutexLockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqParameterAssignStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReachErrorStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcReadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcWriteStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnValueAssignStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadJoinStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnPcRead;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnPcWrite;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.GhostFunctionVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.GhostThreadVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqUtil {

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  // TODO create CaseBuilder class
  /**
   * Returns a {@link SeqCaseClause} which represents case statements in the sequentializations
   * while loop. Returns {@link Optional#empty()} if pThreadNode has no leaving edges i.e. its pc is
   * -1.
   */
  public static Optional<SeqCaseClause> buildCaseClauseFromThreadNode(
      final MPORThread pThread,
      final ImmutableSet<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ThreadNode pThreadNode,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
      // TODO group Function and Thread Vars into SeqVars
      GhostFunctionVariables pFuncVars,
      GhostThreadVariables pThreadVars)
      throws UnrecognizedCodeException {

    pCoveredNodes.add(pThreadNode);

    int originPc = pThreadNode.pc;
    ImmutableList.Builder<SeqCaseBlockStatement> stmts = ImmutableList.builder();
    ImmutableList.Builder<ThreadEdge> threadEdges = ImmutableList.builder();

    // no edges -> exit node of thread reached -> no case because no edges with code
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == EXIT_PC;
      return Optional.empty();

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      // handle all CFunctionReturnEdges: exiting function -> pc not relevant, assign return pc
      assert pFuncVars.returnPcReads.containsKey(pThreadNode);
      FunctionReturnPcRead read = Objects.requireNonNull(pFuncVars.returnPcReads.get(pThreadNode));
      stmts.add(new SeqReturnPcReadStatement(read.threadId, read.returnPcVar));

    } else {
      // TODO create separate methods here to handle the different cases for better overview
      //  (or at least buildStatementFromEdge function)
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {

        threadEdges.add(threadEdge);
        CFAEdge edge = threadEdge.cfaEdge;
        SubstituteEdge sub = Objects.requireNonNull(pSubEdges.get(threadEdge));
        int targetPc = threadEdge.getSuccessor().pc;
        CFANode successor = threadEdge.getSuccessor().cfaNode;

        if (blankCaseBlock(sub, successor, pThread)) {
          assert pThreadNode.leavingEdges().size() == 1;
          stmts.add(new SeqBlankStatement(pThread.id, targetPc));

        } else {
          // use (else) if (condition) for all assumes (if, for, while, switch, ...)
          if (sub.cfaEdge instanceof CAssumeEdge assumeEdge) {
            assert allEdgesAssume(pThreadNode.leavingEdges());
            SeqControlFlowStatementType stmtType =
                firstEdge ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
            if (firstEdge) {
              firstEdge = false;
            }
            SeqControlFlowStatement stmt = new SeqControlFlowStatement(assumeEdge, stmtType);
            stmts.add(new SeqAssumeStatement(stmt, pThread.id, targetPc));

          } else if (sub.cfaEdge instanceof CFunctionSummaryEdge) {
            // assert that both call and summary edge are present
            assert pThreadNode.leavingEdges().size() >= 2;
            assert pFuncVars.returnPcWrites.containsKey(threadEdge);
            FunctionReturnPcWrite write =
                Objects.requireNonNull(pFuncVars.returnPcWrites.get(threadEdge));
            stmts.add(new SeqReturnPcWriteStatement(write.returnPcVar, write.value));

          } else if (sub.cfaEdge instanceof CFunctionCallEdge funcCall) {
            if (isReachErrorCall(funcCall)) {
              // inject non-inlined reach_error
              stmts.add(new SeqReachErrorStatement());
            }
            // assert that both call and summary edge are present
            assert pThreadNode.leavingEdges().size() >= 2;
            assert pFuncVars.parameterAssignments.containsKey(threadEdge);
            ImmutableList<FunctionParameterAssignment> assigns =
                Objects.requireNonNull(pFuncVars.parameterAssignments.get(threadEdge));
            if (assigns.isEmpty()) {
              stmts.add(new SeqBlankStatement(pThread.id, targetPc));
            } else {
              for (int i = 0; i < assigns.size(); i++) {
                FunctionParameterAssignment assign = assigns.get(i);
                // if this is the last param assign, add the pcUpdate, otherwise empty
                boolean lastParam = i == assigns.size() - 1;
                stmts.add(
                    new SeqParameterAssignStatement(
                        assign.statement,
                        lastParam ? Optional.of(pThread.id) : Optional.empty(),
                        lastParam ? Optional.of(targetPc) : Optional.empty()));
              }
            }

          } else if (sub.cfaEdge instanceof CDeclarationEdge decEdge) {
            // "leftover" declaration: const CPAchecker_TMP var
            ThreadNode successorA = threadEdge.getSuccessor();
            assert successorA.leavingEdges().size() == 1;
            ThreadEdge statementA = successorA.leavingEdges().iterator().next();
            assert statementA.cfaEdge instanceof CStatementEdge;
            ThreadNode successorB = statementA.getSuccessor();
            assert successorB.leavingEdges().size() == 1;
            ThreadEdge statementB = successorB.leavingEdges().iterator().next();
            assert statementB.cfaEdge instanceof CStatementEdge;
            // add successors to visited edges / nodes
            threadEdges.add(statementA);
            threadEdges.add(statementB);
            pCoveredNodes.add(successorA);
            pCoveredNodes.add(successorB);
            // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
            SubstituteEdge subA = Objects.requireNonNull(pSubEdges.get(statementA));
            SubstituteEdge subB = Objects.requireNonNull(pSubEdges.get(statementB));
            int newTargetPc = statementB.getSuccessor().pc;
            stmts.add(
                new SeqConstCpaCheckerTmpStatement(decEdge, subA, subB, pThread.id, newTargetPc));

          } else if (sub.cfaEdge instanceof CReturnStatementEdge) {
            // TODO add support and test for pthread_join(id, &start_routine_return)
            //  where start_routine_return is assigned the return value of the threads start routine
            // returning from non-start-routine function: assign return value to return vars
            ImmutableSet<FunctionReturnValueAssignment> assigns =
                Objects.requireNonNull(pFuncVars.returnValueAssignments.get(threadEdge));
            if (assigns.isEmpty()) { // -> function does not return anything, i.e. return;
              stmts.add(new SeqBlankStatement(pThread.id, targetPc));
            } else {
              // just get the first element in the set for the RETURN_PC
              CIdExpression returnPc = assigns.iterator().next().returnPcWrite.returnPcVar;
              stmts.add(
                  new SeqReturnValueAssignStatements(returnPc, assigns, pThread.id, targetPc));
            }

          } else if (isExplicitlyHandledPthreadFunc(edge)) {
            PthreadFuncType funcType = PthreadFuncType.getPthreadFuncType(edge);
            switch (funcType) {
              case PTHREAD_CREATE:
                CExpression pthreadT = PthreadUtil.extractPthreadT(edge);
                MPORThread createdThread =
                    PthreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
                stmts.add(new SeqThreadCreationStatement(createdThread.id, pThread.id, targetPc));
                break;

              case PTHREAD_MUTEX_LOCK:
                CIdExpression lockedMutexT = PthreadUtil.extractPthreadMutexT(threadEdge.cfaEdge);
                assert pThreadVars.locked.containsKey(lockedMutexT);
                CIdExpression lockedVar =
                    Objects.requireNonNull(pThreadVars.locked.get(lockedMutexT)).idExpression;
                assert pThreadVars.locks.containsKey(pThread);
                assert Objects.requireNonNull(pThreadVars.locks.get(pThread))
                    .containsKey(lockedMutexT);
                CIdExpression mutexAwaits =
                    Objects.requireNonNull(
                            Objects.requireNonNull(pThreadVars.locks.get(pThread))
                                .get(lockedMutexT))
                        .idExpression;
                stmts.add(new SeqMutexLockStatement(lockedVar, mutexAwaits, pThread.id, targetPc));
                break;

              case PTHREAD_MUTEX_UNLOCK:
                CIdExpression unlockedMutexT = PthreadUtil.extractPthreadMutexT(sub.cfaEdge);
                assert pThreadVars.locked.containsKey(unlockedMutexT);
                // assign 0 to locked variable
                CExpressionAssignmentStatement lockedFalse =
                    SeqExpressionAssignmentStatement.build(
                        Objects.requireNonNull(pThreadVars.locked.get(unlockedMutexT)).idExpression,
                        SeqIntegerLiteralExpression.INT_0);
                stmts.add(new SeqMutexUnlockStatement(lockedFalse, pThread.id, targetPc));
                break;

              case PTHREAD_JOIN:
                MPORThread targetThread =
                    PthreadUtil.extractThread(pThreadVars.joins.keySet(), edge);
                CIdExpression threadJoins =
                    Objects.requireNonNull(
                            Objects.requireNonNull(pThreadVars.joins.get(pThread))
                                .get(targetThread))
                        .idExpression;
                stmts.add(
                    new SeqThreadJoinStatement(targetThread.id, threadJoins, pThread.id, targetPc));
                break;

              case __VERIFIER_ATOMIC_BEGIN:
                assert pThreadVars.atomicLocked.isPresent();
                CIdExpression atomicLocked =
                    Objects.requireNonNull(pThreadVars.atomicLocked.orElseThrow().idExpression);
                assert pThreadVars.begins.containsKey(pThread);
                CIdExpression beginsVar =
                    Objects.requireNonNull(pThreadVars.begins.get(pThread)).idExpression;
                stmts.add(
                    new SeqAtomicBeginStatement(atomicLocked, beginsVar, pThread.id, targetPc));
                break;

              case __VERIFIER_ATOMIC_END:
                assert pThreadVars.atomicLocked.isPresent();
                // assign 0 to ATOMIC_LOCKED variable
                CExpressionAssignmentStatement atomicLockedFalse =
                    SeqExpressionAssignmentStatement.build(
                        Objects.requireNonNull(pThreadVars.atomicLocked.orElseThrow().idExpression),
                        SeqIntegerLiteralExpression.INT_0);
                stmts.add(new SeqAtomicEndStatement(atomicLockedFalse, pThread.id, targetPc));
                break;

              default:
                throw new AssertionError("unhandled relevant pthread method: " + funcType.name);
            }
          } else {
            assert sub.cfaEdge instanceof CStatementEdge;
            stmts.add(new SeqDefaultStatement((CStatementEdge) sub.cfaEdge, pThread.id, targetPc));
          }
        }
      }
    }
    return Optional.of(
        new SeqCaseClause(
            anyGlobalAccess(threadEdges.build()),
            pThreadNode.cfaNode.isLoopStart(),
            originPc,
            new SeqCaseBlock(stmts.build(), Terminator.CONTINUE)));
  }

  // Helpers =====================================================================================

  public static boolean isConstCPAcheckerTMP(CVariableDeclaration pVarDec) {
    return pVarDec.getType().isConst()
        && !pVarDec.isGlobal()
        && pVarDec.getName().contains(SeqToken.__CPAchecker_TMP_);
  }

  private static boolean allEdgesAssume(List<ThreadEdge> pThreadEdges) {
    for (ThreadEdge threadEdge : pThreadEdges) {
      if (!(threadEdge.cfaEdge instanceof AssumeEdge)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if any {@link CFAEdge} of the given {@link ThreadEdge}s read or write a
   * global variable.
   */
  protected static boolean anyGlobalAccess(List<ThreadEdge> pThreadEdges) {
    GlobalAccessChecker gac = new GlobalAccessChecker();
    for (ThreadEdge threadEdge : pThreadEdges) {
      if (!(threadEdge.cfaEdge instanceof CFunctionSummaryEdge)) {
        if (gac.hasGlobalAccess(threadEdge.cfaEdge)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if pSub results in a case block with only pc adjustments, i.e. no code changing
   * the input program state.
   */
  private static boolean blankCaseBlock(
      SubstituteEdge pSub, CFANode pSuccessor, MPORThread pThread) {

    // exiting start routine of thread -> blank, just set pc[i] = -1;
    if (pSuccessor instanceof FunctionExitNode
        && pSuccessor.getFunction().getType().equals(pThread.startRoutine)) {
      // TODO this needs to be refactored once we support start routine return values
      //  that can be used with pthread_join -> block may not be blank but sets the return value
      return true;

    } else if (pSuccessor.getFunctionName().equals(SeqToken.reach_error)) {
      // if we enter reach_error, include only call edge (to inject reach_error)
      return !(pSub.cfaEdge instanceof CFunctionCallEdge);

    } else if (pSub.cfaEdge instanceof BlankEdge) {
      // blank edges have no code
      assert pSub.cfaEdge.getCode().isEmpty();
      return true;

    } else if (pSub.cfaEdge instanceof CDeclarationEdge decEdge) {
      CDeclaration dec = decEdge.getDeclaration();
      if (!(dec instanceof CVariableDeclaration varDec)) {
        // all non vars (functions, structs, ...) are declared beforehand
        return true;
      } else {
        // declaration of const int CPAchecker_TMP vars is included in the cases
        return !isConstCPAcheckerTMP(varDec);
      }

    } else if (PthreadFuncType.callsAnyPthreadFunc(pSub.cfaEdge)) {
      // not explicitly handled PthreadFunc -> empty case code
      return !isExplicitlyHandledPthreadFunc(pSub.cfaEdge);
    }
    return false;
  }

  /**
   * Returns true if the semantics of the pthread method in pEdge is considered in the
   * sequentialization, i.e. the case block contains code. A function may be supported by MPOR but
   * not considered in the sequentialization.
   */
  private static boolean isExplicitlyHandledPthreadFunc(CFAEdge pEdge) {
    if (PthreadFuncType.callsAnyPthreadFunc(pEdge)) {
      return PthreadFuncType.getPthreadFuncType(pEdge).isExplicitlyHandled;
    }
    return false;
  }

  private static boolean isReachErrorCall(CFunctionCallEdge pFunctionCallEdge) {
    return pFunctionCallEdge
        .getFunctionCallExpression()
        .getDeclaration()
        .getOrigName()
        .equals(SeqToken.reach_error);
  }

  public static SeqFunctionCallExpression createPORAssumption(int pThreadId, int pPc)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression threadId = SeqIntegerLiteralExpression.buildIntLiteralExpr(pThreadId);
    CBinaryExpression prevEquals =
        SeqBinaryExpression.buildBinaryExpression(
            SeqIdExpression.PREV_THREAD, threadId, BinaryOperator.EQUALS);
    CBinaryExpression pcEquals =
        SeqBinaryExpression.buildBinaryExpression(
            SeqExpressions.getPcExpression(pThreadId),
            SeqIntegerLiteralExpression.buildIntLiteralExpr(pPc),
            BinaryOperator.EQUALS);
    CToSeqExpression nextThread =
        new CToSeqExpression(
            SeqBinaryExpression.buildBinaryExpression(
                SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.EQUALS));
    SeqLogicalNotExpression notAnd =
        new SeqLogicalNotExpression(new SeqLogicalAndExpression(prevEquals, pcEquals));
    SeqLogicalOrExpression or = new SeqLogicalOrExpression(notAnd, nextThread);
    return new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(or));
  }
}
