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
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause.CaseBlockTerminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqAssumeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqConstCpaCheckerTmpStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqDefaultStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqMutexLockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqMutexUnlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqParameterAssignStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcRetrievalStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcStorageStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnValueAssignStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadJoinStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadTerminationStatement;
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

public class SeqUtil {

  public static final int INIT_PC = 0;

  public static final int TERMINATION_PC = -1;

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

    // no edges -> exit node reached (assert fail or main / start routine exit node)
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == TERMINATION_PC;
      return null;

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      // handle all CFunctionReturnEdges: exiting function -> pc not relevant, assign return pc
      assert pFuncVars.returnPcRetrievals.containsKey(pThreadNode);
      FunctionReturnPcRetrieval retrieval = pFuncVars.returnPcRetrievals.get(pThreadNode);
      assert retrieval != null;
      stmts.add(new SeqReturnPcRetrievalStatement(retrieval.assignmentStatement));

    } else {
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {
        CFAEdge edge = threadEdge.cfaEdge;
        SubstituteEdge sub = pSubEdges.get(threadEdge);
        assert sub != null;
        int targetPc = threadEdge.getSuccessor().pc;
        CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(pThread.id, targetPc);

        if (emptyCaseCode(sub.cfaEdge)) {
          assert pThreadNode.leavingEdges().size() == 1;
          stmts.add(new SeqBlankStatement(pcUpdate, targetPc));

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
            stmts.add(new SeqAssumeStatement(stmt, pcUpdate));

          } else if (sub.cfaEdge instanceof CFunctionSummaryEdge) {
            assert pThreadNode.leavingEdges().size() >= 2;
            assert pFuncVars.returnPcStorages.containsKey(threadEdge);
            FunctionReturnPcStorage storage = pFuncVars.returnPcStorages.get(threadEdge);
            assert storage != null;
            stmts.add(new SeqReturnPcStorageStatement(storage.assignmentStatement));

          } else if (sub.cfaEdge instanceof CFunctionCallEdge) {
            assert pThreadNode.leavingEdges().size() >= 2;
            assert pFuncVars.parameterAssignments.containsKey(threadEdge);
            ImmutableList<FunctionParameterAssignment> assigns =
                pFuncVars.parameterAssignments.get(threadEdge);
            assert assigns != null;
            if (assigns.isEmpty()) {
              stmts.add(new SeqBlankStatement(pcUpdate, targetPc));
            } else {
              for (int i = 0; i < assigns.size(); i++) {
                FunctionParameterAssignment assign = assigns.get(i);
                // if this is the last param assign, add the pcUpdate, otherwise empty
                stmts.add(
                    new SeqParameterAssignStatement(
                        assign.statement,
                        i == assigns.size() - 1 ? Optional.of(pcUpdate) : Optional.empty()));
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
            pCoveredNodes.add(successorA);
            pCoveredNodes.add(successorB);

            // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
            SubstituteEdge subA = pSubEdges.get(statementA);
            SubstituteEdge subB = pSubEdges.get(statementB);
            assert subA != null && subB != null;
            CExpressionAssignmentStatement skippedPcUpdate =
                SeqStatements.buildPcUpdate(pThread.id, statementB.getSuccessor().pc);
            stmts.add(new SeqConstCpaCheckerTmpStatement(decEdge, subA, subB, skippedPcUpdate));

          } else if (sub.cfaEdge instanceof CReturnStatementEdge retStmt) {
            assert pFuncVars.returnValueAssignments.containsKey(threadEdge);
            if (retStmt.getSuccessor().getFunction().getType().equals(pThread.startRoutine)) {
              // exiting thread -> assign 0 to thread_active var if possible and set exit pc
              CExpressionAssignmentStatement activeAssign =
                  new CExpressionAssignmentStatement(
                      FileLocation.DUMMY,
                      pThreadVars.active.get(pThread.id).idExpression,
                      SeqIntegerLiteralExpression.INT_0);
              stmts.add(new SeqThreadTerminationStatement(pThread.id, activeAssign));

            } else {
              // returning from any other function: assign return value to all return vars
              ImmutableSet<FunctionReturnValueAssignment> assigns =
                  pFuncVars.returnValueAssignments.get(threadEdge);
              assert assigns != null;
              assert !assigns.isEmpty();
              CIdExpression returnPc = assigns.iterator().next().returnPcStorage.returnPc;
              stmts.add(
                  new SeqReturnValueAssignStatements(returnPc, assigns, pcUpdate, pThread.id));
            }

          } else if (isExplicitlyHandledPthreadFunc(sub.cfaEdge)) {
            PthreadFuncType funcType = PthreadFuncType.getPthreadFuncType(edge);
            switch (funcType) {
              case PTHREAD_CREATE:
                CExpression pthreadT = PthreadUtil.extractPthreadT(edge);
                MPORThread thread =
                    PthreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
                CExpressionAssignmentStatement activeAssign =
                    SeqStatements.buildExprAssign(
                        pThreadVars.active.get(thread.id).idExpression,
                        SeqIntegerLiteralExpression.INT_1);
                stmts.add(new SeqThreadCreationStatement(activeAssign, pcUpdate));
                break;

              case PTHREAD_MUTEX_LOCK:
                CIdExpression lockedMutexT = PthreadUtil.extractPthreadMutexT(threadEdge.cfaEdge);
                assert pThreadVars.locked.containsKey(lockedMutexT);
                CIdExpression lockedVar =
                    Objects.requireNonNull(pThreadVars.locked.get(lockedMutexT)).idExpression;
                assert pThreadVars.awaits.containsKey(pThread);
                assert Objects.requireNonNull(pThreadVars.awaits.get(pThread))
                    .containsKey(lockedMutexT);
                CIdExpression mutexAwaits =
                    Objects.requireNonNull(
                            Objects.requireNonNull(pThreadVars.awaits.get(pThread))
                                .get(lockedMutexT))
                        .idExpression;
                stmts.add(new SeqMutexLockStatement(lockedVar, mutexAwaits, pcUpdate));
                break;

              case PTHREAD_MUTEX_UNLOCK:
                CIdExpression unlockedMutexT = PthreadUtil.extractPthreadMutexT(sub.cfaEdge);
                assert pThreadVars.locked.containsKey(unlockedMutexT);
                // assign 0 to locked variable
                CExpressionAssignmentStatement lockedFalse =
                    SeqStatements.buildExprAssign(
                        Objects.requireNonNull(pThreadVars.locked.get(unlockedMutexT)).idExpression,
                        SeqIntegerLiteralExpression.INT_0);
                stmts.add(new SeqMutexUnlockStatement(lockedFalse, pcUpdate));
                break;

              case PTHREAD_JOIN:
                MPORThread targetThread =
                    PthreadUtil.extractThread(pThreadVars.joins.keySet(), edge);
                CIdExpression targetThreadActive =
                    pThreadVars.active.get(targetThread.id).idExpression;
                CIdExpression threadJoins =
                    Objects.requireNonNull(
                            Objects.requireNonNull(pThreadVars.joins.get(pThread))
                                .get(targetThread))
                        .idExpression;
                stmts.add(new SeqThreadJoinStatement(targetThreadActive, threadJoins, pcUpdate));
                break;

              default:
                throw new AssertionError("unhandled relevant pthread method: " + funcType.name);
            }

          } else {
            assert sub.cfaEdge instanceof CStatementEdge;
            stmts.add(new SeqDefaultStatement((CStatementEdge) sub.cfaEdge, pcUpdate));
          }
        }
      }
    }
    return new SeqCaseClause(originPc, stmts.build(), CaseBlockTerminator.CONTINUE);
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

  /** Returns pString with the specified amount of tabs as prefix and adds a new line \n. */
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
        && pVarDec.getName().contains(SeqToken.__CPACHECKER_TMP_);
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
   * Returns true if pEdge results in a case block with only pc adjustments, i.e. no code changing
   * the input program state.
   */
  private static boolean emptyCaseCode(CFAEdge pEdge) {
    if (pEdge instanceof BlankEdge) {
      assert pEdge.getCode().isEmpty();
      return true;
    } else if (pEdge instanceof CDeclarationEdge decEdge) {
      CDeclaration dec = decEdge.getDeclaration();
      if (!(dec instanceof CVariableDeclaration varDec)) {
        return true; // all non vars are declared beforehand
      } else {
        // declaration of const int CPAchecker_TMP vars is included in the cases
        return !isConstCPAcheckerTMP(varDec);
      }
    } else if (PthreadFuncType.callsAnyPthreadFunc(pEdge)) {
      // unsupported PthreadFunc -> empty case code
      return !isExplicitlyHandledPthreadFunc(pEdge);
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
}
