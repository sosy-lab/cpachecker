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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcRetrievalStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcStorageStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnValueAssignStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadExitStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadJoinStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnPcRetrieval;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnPcStorage;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.thread_vars.ThreadVars;
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
   * while loop. Returns null if pThreadNode has no leaving edges i.e. its pc is -1.
   */
  @Nullable
  public static SeqCaseClause createCaseFromThreadNode(
      final MPORThread pThread,
      final ImmutableSet<MPORThread> pAllThreads,
      Set<ThreadNode> pCoveredNodes,
      ThreadNode pThreadNode,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubEdges,
      FunctionVars pFuncVars,
      ThreadVars pThreadVars) {

    pCoveredNodes.add(pThreadNode);

    int originPc = pThreadNode.pc;
    ImmutableList.Builder<SeqCaseBlockStatement> stmts = ImmutableList.builder();
    ImmutableList.Builder<ThreadEdge> threadEdges = ImmutableList.builder();

    // no edges -> exit node reached (assert fail or main / start routine exit node)
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == EXIT_PC;
      return null;

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      // handle all CFunctionReturnEdges: exiting function -> pc not relevant, assign return pc
      assert pFuncVars.returnPcRetrievals.containsKey(pThreadNode);
      FunctionReturnPcRetrieval retrieval = pFuncVars.returnPcRetrievals.get(pThreadNode);
      assert retrieval != null;
      stmts.add(new SeqReturnPcRetrievalStatement(retrieval.threadId, retrieval.returnPcVar));

    } else {
      // TODO create separate methods here to handle the different cases for better overview
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {

        threadEdges.add(threadEdge);
        CFAEdge edge = threadEdge.cfaEdge;
        SubstituteEdge sub = pSubEdges.get(threadEdge);
        assert sub != null;
        int targetPc = threadEdge.getSuccessor().pc;
        CFANode successor = threadEdge.getSuccessor().cfaNode;

        if (successor instanceof FunctionExitNode
            && successor.getFunction().getType().equals(pThread.startRoutine)) {
          // exiting thread -> assign 0 to thread_active var if possible and set exit pc
          CExpressionAssignmentStatement activeAssign =
              new CExpressionAssignmentStatement(
                  FileLocation.DUMMY,
                  Objects.requireNonNull(pThreadVars.active.get(pThread)).idExpression,
                  SeqIntegerLiteralExpression.INT_0);
          stmts.add(new SeqThreadExitStatement(activeAssign, pThread.id));

        } else if (emptyCaseCode(sub)) {
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
            assert pFuncVars.returnPcStorages.containsKey(threadEdge);
            FunctionReturnPcStorage storage = pFuncVars.returnPcStorages.get(threadEdge);
            assert storage != null;
            stmts.add(new SeqReturnPcStorageStatement(storage.returnPcVar, storage.value));

          } else if (sub.cfaEdge instanceof CFunctionCallEdge funcCall) {
            String funcName =
                funcCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();
            if (funcName.equals(SeqToken.reach_error)) {
              stmts.add(new SeqReachErrorStatement()); // inject non-inlined reach_error
            }
            // assert that both call and summary edge are present
            assert pThreadNode.leavingEdges().size() >= 2;
            assert pFuncVars.parameterAssignments.containsKey(threadEdge);
            ImmutableList<FunctionParameterAssignment> assigns =
                pFuncVars.parameterAssignments.get(threadEdge);
            assert assigns != null;
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
            SubstituteEdge subA = pSubEdges.get(statementA);
            SubstituteEdge subB = pSubEdges.get(statementB);
            assert subA != null && subB != null;
            int newTargetPc = statementB.getSuccessor().pc;
            stmts.add(
                new SeqConstCpaCheckerTmpStatement(decEdge, subA, subB, pThread.id, newTargetPc));

          } else if (sub.cfaEdge instanceof CReturnStatementEdge) {
            assert sub.cfaEdge.getSuccessor() != null;
            assert pFuncVars.returnValueAssignments.containsKey(threadEdge);
            // TODO add support and test for pthread_join(id, &start_routine_return)
            //  where start_routine_return is assigned the return value of the threads start routine
            // returning from non-start-routine function: assign return value to return vars
            ImmutableSet<FunctionReturnValueAssignment> assigns =
                pFuncVars.returnValueAssignments.get(threadEdge);
            assert assigns != null;
            if (assigns.isEmpty()) { // -> function does not return anything, i.e. return;
              stmts.add(new SeqBlankStatement(pThread.id, targetPc));
            } else {
              // just get the first element in the set for the RETURN_PC
              CIdExpression returnPc = assigns.iterator().next().returnPcStorage.returnPcVar;
              stmts.add(
                  new SeqReturnValueAssignStatements(returnPc, assigns, pThread.id, targetPc));
            }

          } else if (isExplicitlyHandledPthreadFunc(edge)) {
            PthreadFuncType funcType = PthreadFuncType.getPthreadFuncType(edge);
            switch (funcType) {
              case PTHREAD_CREATE:
                CExpression pthreadT = PthreadUtil.extractPthreadT(edge);
                MPORThread thread =
                    PthreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
                CExpressionAssignmentStatement activeAssign =
                    SeqStatements.buildExprAssign(
                        Objects.requireNonNull(pThreadVars.active.get(thread)).idExpression,
                        SeqIntegerLiteralExpression.INT_1);
                stmts.add(new SeqThreadCreationStatement(activeAssign, pThread.id, targetPc));
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
                    SeqStatements.buildExprAssign(
                        Objects.requireNonNull(pThreadVars.locked.get(unlockedMutexT)).idExpression,
                        SeqIntegerLiteralExpression.INT_0);
                stmts.add(new SeqMutexUnlockStatement(lockedFalse, pThread.id, targetPc));
                break;

              case PTHREAD_JOIN:
                MPORThread targetThread =
                    PthreadUtil.extractThread(pThreadVars.joins.keySet(), edge);
                CIdExpression targetThreadActive =
                    Objects.requireNonNull(pThreadVars.active.get(targetThread)).idExpression;
                CIdExpression threadJoins =
                    Objects.requireNonNull(
                            Objects.requireNonNull(pThreadVars.joins.get(pThread))
                                .get(targetThread))
                        .idExpression;
                stmts.add(
                    new SeqThreadJoinStatement(
                        targetThreadActive, threadJoins, pThread.id, targetPc));
                break;

              case __VERIFIER_ATOMIC_BEGIN:
                assert pThreadVars.atomicInUse.isPresent();
                CIdExpression atomicInUse =
                    Objects.requireNonNull(pThreadVars.atomicInUse.orElseThrow().idExpression);
                assert pThreadVars.begins.containsKey(pThread);
                CIdExpression beginsVar =
                    Objects.requireNonNull(pThreadVars.begins.get(pThread)).idExpression;
                stmts.add(
                    new SeqAtomicBeginStatement(atomicInUse, beginsVar, pThread.id, targetPc));
                break;

              case __VERIFIER_ATOMIC_END:
                assert pThreadVars.atomicInUse.isPresent();
                // assign 0 to ATOMIC_IN_USE variable
                CExpressionAssignmentStatement atomicInUseFalse =
                    SeqStatements.buildExprAssign(
                        Objects.requireNonNull(pThreadVars.atomicInUse.orElseThrow().idExpression),
                        SeqIntegerLiteralExpression.INT_0);
                stmts.add(new SeqAtomicEndStatement(atomicInUseFalse, pThread.id, targetPc));
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
    return new SeqCaseClause(
        anyGlobalAccess(threadEdges.build()),
        originPc,
        new SeqCaseBlock(stmts.build(), Terminator.CONTINUE));
  }

  /** Returns ""pString"" */
  public static String wrapInQuotationMarks(String pString) {
    return SeqSyntax.QUOTATION_MARK + pString + SeqSyntax.QUOTATION_MARK;
  }

  /** Returns "{ pString }" */
  public static String wrapInCurlyInwards(String pString) {
    return SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + pString
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  /** Returns "} pExpression {" */
  public static String wrapInCurlyOutwards(String pString) {
    return SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.SPACE
        + pString
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  /** Returns "pString {" */
  public static String appendOpeningCurly(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  /** Returns "pString }" */
  public static String appendClosingCurly(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  /** Returns pString with the specified amount of tabs as prefix and a new line \n as suffix. */
  public static String prependTabsWithNewline(int pTabs, String pString) {
    return prependTabsWithoutNewline(pTabs, pString) + SeqSyntax.NEWLINE;
  }

  /** Returns pString with the specified amount of tabs as prefix. */
  public static String prependTabsWithoutNewline(int pTabs, String pString) {
    return repeat(SeqSyntax.TAB, pTabs) + pString;
  }

  public static String repeat(String pString, int pAmount) {
    return pString.repeat(Math.max(0, pAmount));
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
  private static boolean emptyCaseCode(SubstituteEdge pSub) {

    if (pSub.cfaEdge instanceof BlankEdge) {
      assert pSub.cfaEdge.getCode().isEmpty();
      return true;
    } else if (pSub.cfaEdge instanceof CDeclarationEdge decEdge) {
      CDeclaration dec = decEdge.getDeclaration();
      if (!(dec instanceof CVariableDeclaration varDec)) {
        return true; // all non vars are declared beforehand
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
