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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqAssignReturnPcStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqAssumeStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqDefaultStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqMutexLockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqParameterAssignStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnPcAssignStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqReturnValueAssignStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadJoinStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqThreadTerminationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.helper_vars.FunctionVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.helper_vars.PthreadVars;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
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
      ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs,
      FunctionVars pFuncVars,
      PthreadVars pPthreadVars) {

    pCoveredNodes.add(pThreadNode);

    int originPc = pThreadNode.pc;
    ImmutableList.Builder<SeqCaseBlockStatement> stmts = ImmutableList.builder();

    // no edges -> exit node reached (assert fail or main / start routine exit node)
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == TERMINATION_PC;
      return null;

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      // handle all CFunctionReturnEdges: exiting function -> pc not relevant, assign return pc
      assert pFuncVars.pcToReturnPcAssigns.containsKey(pThreadNode);
      CExpressionAssignmentStatement assign = pFuncVars.pcToReturnPcAssigns.get(pThreadNode);
      assert assign != null;
      stmts.add(new SeqAssignReturnPcStatement(assign));

    } else {
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {
        CFAEdge edge = threadEdge.cfaEdge;
        CFAEdge sub = pEdgeSubs.get(threadEdge);
        assert sub != null;
        int targetPc = threadEdge.getSuccessor().pc;
        CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(pThread.id, targetPc);

        if (emptyCaseCode(sub)) {
          assert pThreadNode.leavingEdges().size() == 1;
          stmts.add(new SeqBlankStatement(pcUpdate, targetPc));

        } else {
          // use (else) if (condition) for all assumes (if, for, while, switch, ...)
          if (sub instanceof CAssumeEdge assumeEdge) {
            assert allEdgesAssume(pThreadNode.leavingEdges());
            SeqControlFlowStatementType stmtType =
                firstEdge ? SeqControlFlowStatementType.IF : SeqControlFlowStatementType.ELSE_IF;
            if (firstEdge) {
              firstEdge = false;
            }
            SeqControlFlowStatement stmt = new SeqControlFlowStatement(assumeEdge, stmtType);
            stmts.add(new SeqAssumeStatement(stmt, pcUpdate));

          } else if (sub instanceof CFunctionSummaryEdge) {
            assert pThreadNode.leavingEdges().size() >= 2;
            assert pFuncVars.returnPcToPcAssigns.containsKey(threadEdge);
            CExpressionAssignmentStatement assign = pFuncVars.returnPcToPcAssigns.get(threadEdge);
            assert assign != null;
            stmts.add(new SeqReturnPcAssignStatement(assign));

          } else if (sub instanceof CFunctionCallEdge) {
            assert pThreadNode.leavingEdges().size() >= 2;
            assert pFuncVars.paramAssigns.containsKey(threadEdge);
            ImmutableList<CExpressionAssignmentStatement> assigns =
                pFuncVars.paramAssigns.get(threadEdge);
            assert assigns != null;
            if (assigns.isEmpty()) {
              stmts.add(new SeqBlankStatement(pcUpdate, targetPc));
            } else {
              for (int i = 0; i < assigns.size(); i++) {
                CExpressionAssignmentStatement assign = assigns.get(i);
                // if this is the last param assign, add the pcUpdate, otherwise empty
                stmts.add(
                    new SeqParameterAssignStatement(
                        assign,
                        i == assigns.size() - 1 ? Optional.of(pcUpdate) : Optional.empty()));
              }
            }

          } else if (sub instanceof CDeclarationEdge) {
            // TODO remove this and add atomicity with assumptions
            // "leftover" declaration: const CPAchecker_TMP var
            ThreadNode succ = threadEdge.getSuccessor();
            ThreadEdge succEdge = succ.leavingEdges().iterator().next();
            ThreadNode succSucc = succEdge.getSuccessor();
            ThreadEdge succSuccEdge = succSucc.leavingEdges().iterator().next();
            assert succ.leavingEdges().size() == 1;
            assert succSucc.leavingEdges().size() == 1;
            pCoveredNodes.add(succ);
            pCoveredNodes.add(succSucc);
            // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
            CFAEdge succSub = pEdgeSubs.get(succEdge);
            CFAEdge succSuccSub = pEdgeSubs.get(succSuccEdge);
            assert succSub != null && succSuccSub != null;
            stmts.add(new SeqDefaultStatement((CStatementEdge) succEdge.cfaEdge, pcUpdate));

          } else if (sub instanceof CReturnStatementEdge retStmt) {
            assert pFuncVars.returnStmts.containsKey(threadEdge);
            if (retStmt.getSuccessor().getFunction().getType().equals(pThread.startRoutine)) {
              // exiting thread -> assign 0 to thread_active var if possible and set exit pc
              CExpressionAssignmentStatement activeAssign =
                  new CExpressionAssignmentStatement(
                      FileLocation.DUMMY,
                      pPthreadVars.threadActive.get(pThread.id),
                      SeqIntegerLiteralExpression.INT_0);
              stmts.add(new SeqThreadTerminationStatement(pThread.id, activeAssign));

            } else {
              // returning from any other function: assign return value to all return vars
              ImmutableSet<CExpressionAssignmentStatement> assigns =
                  pFuncVars.returnStmts.get(threadEdge);
              assert assigns != null;
              // TODO need to create a switch case to only assign the context return value!
              for (CExpressionAssignmentStatement assign : assigns) {
                stmts.add(new SeqReturnValueAssignStatement(assign, pcUpdate));
              }
            }

          } else if (isRelevantPthreadFunc(edge)) {
            PthreadFuncType funcType = PthreadFuncType.getPthreadFuncType(edge);
            switch (funcType) {
              case PTHREAD_CREATE:
                CExpression pthreadT = PthreadUtil.extractPthreadT(edge);
                MPORThread thread =
                    PthreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
                CExpressionAssignmentStatement activeAssign =
                    SeqStatements.buildExprAssign(
                        pPthreadVars.threadActive.get(thread.id),
                        SeqIntegerLiteralExpression.INT_1);
                stmts.add(new SeqThreadCreationStatement(activeAssign, pcUpdate));
                break;

              /*case PTHREAD_MUTEX_LOCK:
                // TODO general idea for lock:
                //  if (m_locked) { __t_awaits_m = 1; }
                //  else { __t_awaits_m = 0; m_locked = 1; pc[...] = ...; }
                //  continue;
                //  then add assumption over locked, awaits and next_thread
                break;

              case PTHREAD_MUTEX_UNLOCK:
                CIdExpression aPthreadMutexT = PthreadUtil.extractPthreadMutexT(sub);
                assert pPthreadVars.mutexLocked.containsKey(aPthreadMutexT);
                CExpressionAssignmentStatement lockedAssign =
                    SeqStatements.buildExprAssign(
                        pPthreadVars.mutexLocked.get(aPthreadMutexT), SeqExpressions.INT_0);
                stmts.add(
                    new SeqCaseBlockStatement(
                        pThread.id, false, Optional.of(lockedAssign.toASTString()), targetPc));
                break;*/

              case PTHREAD_MUTEX_LOCK:
              case PTHREAD_MUTEX_UNLOCK:
                CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(sub);
                assert pPthreadVars.mutexLocked.containsKey(pthreadMutexT);
                // if lock -> assign 1 to locked, otherwise 0
                /*CExpression value =
                    funcType.equals(PthreadFuncType.PTHREAD_MUTEX_LOCK)
                        ? SeqIntegerLiteralExpression.INT_1
                        : SeqIntegerLiteralExpression.INT_0;
                CExpressionAssignmentStatement lockedAssign =
                    SeqStatements.buildExprAssign(
                        pPthreadVars.mutexLocked.get(pthreadMutexT), value);*/
                stmts.add(new SeqMutexLockStatement());
                break;

              case PTHREAD_JOIN:
                MPORThread targetThread =
                    PthreadUtil.extractThread(pPthreadVars.threadJoins.keySet(), edge);
                CExpressionAssignmentStatement joinsTrueAssign =
                    SeqStatements.buildExprAssign(
                        pPthreadVars.threadJoins.get(pThread).get(targetThread),
                        SeqIntegerLiteralExpression.INT_1);
                stmts.add(new SeqThreadJoinStatement(joinsTrueAssign, pcUpdate));
                break;

              default:
                throw new IllegalArgumentException(
                    "unhandled relevant pthread method: " + funcType.name);
            }

          } else {
            assert sub instanceof CStatementEdge;
            stmts.add(new SeqDefaultStatement((CStatementEdge) sub, pcUpdate));
          }
        }
      }
    }
    return new SeqCaseClause(originPc, stmts.build());
  }

  /** Returns "(pString)" */
  public static String wrapInBracketsInwards(String pString) {
    return SeqSyntax.BRACKET_LEFT + pString + SeqSyntax.BRACKET_RIGHT;
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

  public static String generateCase(String pCaseNumber, String pCodeBlock) {
    return SeqToken.CASE
        + SeqSyntax.SPACE
        + pCaseNumber
        + SeqSyntax.COLON
        + SeqSyntax.SPACE
        + pCodeBlock
        + (pCodeBlock.endsWith(SeqSyntax.SEMICOLON) ? SeqSyntax.EMPTY_STRING : SeqSyntax.SEMICOLON)
        + SeqSyntax.SPACE
        + SeqToken.BREAK
        + SeqSyntax.SEMICOLON
        + SeqSyntax.NEWLINE;
  }

  // Helpers =====================================================================================

  public static boolean isConstCPAcheckerTMP(CVariableDeclaration pVarDec) {
    return pVarDec.getType().isConst()
        && !pVarDec.isGlobal()
        && pVarDec.getName().contains(SeqToken.CPACHECKER_TMP);
  }

  // TODO here for test purposes
  private static boolean allEdgesAssume(Set<ThreadEdge> pThreadEdges) {
    for (ThreadEdge threadEdge : pThreadEdges) {
      if (!(threadEdge.cfaEdge instanceof AssumeEdge)) {
        return false;
      }
    }
    return true;
  }

  private static boolean emptyCaseCode(CFAEdge pEdge) {
    if (pEdge instanceof BlankEdge) {
      assert pEdge.getCode().isEmpty(); // TODO test, remove later
      return true;
    } else if (pEdge instanceof CDeclarationEdge decEdge) {
      CDeclaration dec = decEdge.getDeclaration();
      if (!(dec instanceof CVariableDeclaration varDec)) {
        return true; // all non vars are declared beforehand
      } else {
        // code of const int CPAchecker_TMP vars is included in the cases
        // TODO make sure that the two successors code is within the same case
        return !isConstCPAcheckerTMP(varDec);
      }
    } else if (PthreadFuncType.callsAnyPthreadFunc(pEdge)) {
      return !isRelevantPthreadFunc(pEdge);
    }
    return false;
  }

  /**
   * Returns true if the semantics of the pthread method in pEdge has to be considered in the
   * sequentialization.
   */
  private static boolean isRelevantPthreadFunc(CFAEdge pEdge) {
    return PthreadFuncType.callsPthreadFunc(pEdge, PthreadFuncType.PTHREAD_CREATE)
        || PthreadFuncType.callsPthreadFunc(pEdge, PthreadFuncType.PTHREAD_JOIN)
        || PthreadFuncType.callsPthreadFunc(pEdge, PthreadFuncType.PTHREAD_MUTEX_LOCK)
        || PthreadFuncType.callsPthreadFunc(pEdge, PthreadFuncType.PTHREAD_MUTEX_UNLOCK);
  }
}
