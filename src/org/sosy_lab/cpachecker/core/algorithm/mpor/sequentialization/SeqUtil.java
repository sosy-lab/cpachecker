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
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.EdgeCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ElseIfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCase;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCaseStmt;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.statements.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.statements.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

public class SeqUtil {

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  // TODO create CaseBuilder class
  /**
   * Returns a {@link SeqLoopCase} which represents case statements in the sequentializations while
   * loop. Returns null if pThreadNode has no leaving edges i.e. its pc is -1.
   */
  @Nullable
  public static SeqLoopCase createCaseFromThreadNode(
      final MPORThread pThread,
      Set<ThreadNode> pCoveredNodes,
      ThreadNode pThreadNode,
      ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs,
      ImmutableMap<ThreadEdge, ImmutableList<AssignExpr>> pParamAssigns,
      ImmutableMap<ThreadEdge, ImmutableSet<AssignExpr>> pReturnStmts,
      ImmutableMap<ThreadEdge, AssignExpr> pReturnPcAssigns,
      ImmutableMap<ThreadNode, AssignExpr> pPcToReturnPcAssigns,
      ImmutableMap<CIdExpression, CIdExpression> pThreadActiveVars,
      ImmutableMap<CIdExpression, CIdExpression> pMutexLockedVars,
      ImmutableMap<MPORThread, ImmutableMap<MPORThread, CIdExpression>> pThreadJoiningVars) {

    pCoveredNodes.add(pThreadNode);

    int originPc = pThreadNode.pc;
    ImmutableList.Builder<SeqLoopCaseStmt> stmts = ImmutableList.builder();

    // no edges -> exit node reached (assert fail or main / start routine exit node)
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == EXIT_PC; // TODO test, remove later
      return null;

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      // handle all CFunctionReturnEdges: exiting function -> pc not relevant, assign return pc
      assert pPcToReturnPcAssigns.containsKey(pThreadNode);
      AssignExpr assign = pPcToReturnPcAssigns.get(pThreadNode);
      assert assign != null;
      stmts.add(
          new SeqLoopCaseStmt(pThread.id, false, Optional.of(assign.toString()), Optional.empty()));

    } else {
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {
        CFAEdge edge = threadEdge.cfaEdge;
        CFAEdge sub = pEdgeSubs.get(threadEdge);
        assert sub != null;
        Optional<Integer> targetPc = Optional.of(threadEdge.getSuccessor().pc);

        if (emptyCaseCode(sub)) {
          assert pThreadNode.leavingEdges().size() == 1; // TODO test, remove later
          stmts.add(new SeqLoopCaseStmt(pThread.id, false, Optional.empty(), targetPc));

        } else {
          // use (else) if (condition) for assumes, no matter if induced by if, for, while...
          if (sub instanceof CAssumeEdge) {
            assert allEdgesAssume(pThreadNode.leavingEdges()); // TODO test, remove later
            IfExpr ifExpr = new IfExpr(new EdgeCodeExpr(sub));
            if (firstEdge) {
              firstEdge = false;
              stmts.add(
                  new SeqLoopCaseStmt(pThread.id, true, Optional.of(ifExpr.toString()), targetPc));
            } else {
              ElseIfExpr elseIfExpr = new ElseIfExpr(ifExpr);
              stmts.add(
                  new SeqLoopCaseStmt(
                      pThread.id, true, Optional.of(elseIfExpr.toString()), targetPc));
            }

          } else if (sub instanceof CFunctionSummaryEdge) {
            assert pReturnPcAssigns.containsKey(threadEdge);
            AssignExpr assign = pReturnPcAssigns.get(threadEdge);
            assert assign != null;
            stmts.add(
                new SeqLoopCaseStmt(
                    pThread.id, false, Optional.of(assign.toString()), Optional.empty()));

          } else if (sub instanceof CFunctionCallEdge) {
            assert pParamAssigns.containsKey(threadEdge);
            ImmutableList<AssignExpr> assigns = pParamAssigns.get(threadEdge);
            assert assigns != null;
            if (assigns.isEmpty()) {
              stmts.add(new SeqLoopCaseStmt(pThread.id, false, Optional.empty(), targetPc));
            } else {
              for (int i = 0; i < assigns.size(); i++) {
                AssignExpr assign = assigns.get(i);
                // if this is the last param assign, add the targetPc, otherwise empty
                stmts.add(
                    new SeqLoopCaseStmt(
                        pThread.id,
                        false,
                        Optional.of(assign.toString()),
                        i == assigns.size() - 1 ? targetPc : Optional.empty()));
              }
            }

          } else if (sub instanceof CDeclarationEdge) {
            // "leftover" declaration: const CPAchecker_TMP var
            ThreadNode succ = threadEdge.getSuccessor();
            ThreadEdge succEdge = succ.leavingEdges().iterator().next();
            ThreadNode succSucc = succEdge.getSuccessor();
            ThreadEdge succSuccEdge = succSucc.leavingEdges().iterator().next();
            assert succ.leavingEdges().size() == 1; // TODO test purposes
            assert succSucc.leavingEdges().size() == 1; // TODO test purposes
            pCoveredNodes.add(succ);
            pCoveredNodes.add(succSucc);
            // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
            CFAEdge succSub = pEdgeSubs.get(succEdge);
            CFAEdge succSuccSub = pEdgeSubs.get(succSuccEdge);
            assert succSub != null && succSuccSub != null;
            stmts.add(
                new SeqLoopCaseStmt(
                    pThread.id,
                    false,
                    Optional.of(new EdgeCodeExpr(sub).toString()),
                    Optional.empty()));
            stmts.add(
                new SeqLoopCaseStmt(
                    pThread.id,
                    false,
                    Optional.of(new EdgeCodeExpr(succSub).toString()),
                    Optional.empty()));
            stmts.add(
                new SeqLoopCaseStmt(
                    pThread.id,
                    false,
                    Optional.of(new EdgeCodeExpr(succSuccSub).toString()),
                    Optional.of(succSuccEdge.getSuccessor().pc)));

          } else if (sub instanceof CReturnStatementEdge retStmt) {
            // TODO it would be cleaner to create a switch statement for the return_pc
            //  and only assign the relevant CPAchecker_TMP var. but this solution works
            assert pReturnStmts.containsKey(threadEdge);
            if (retStmt.getSuccessor().getFunction().getType().equals(pThread.startRoutine)) {
              // exiting thread -> assign 0 to thread_active var if possible and set exit pc
              Optional<CExpression> threadObject = pThread.threadObject;
              if (!pThread.isMain() && pThreadActiveVars.containsKey(threadObject.orElseThrow())) {

                CExpressionAssignmentStatement exprAssign =
                    new CExpressionAssignmentStatement(
                        FileLocation.DUMMY,
                        pThreadActiveVars.get(threadObject.orElseThrow()),
                        SeqExpressions.INT_ZERO);
                stmts.add(
                    new SeqLoopCaseStmt(
                        pThread.id,
                        false,
                        Optional.of(exprAssign.toASTString()),
                        Optional.of(EXIT_PC)));

              } else {
                stmts.add(
                    new SeqLoopCaseStmt(pThread.id, false, Optional.empty(), Optional.of(EXIT_PC)));
              }
            } else {
              // returning from any other function: assign return value to all CPAchecker_TMP vars
              ImmutableSet<AssignExpr> assigns = pReturnStmts.get(threadEdge);
              assert assigns != null;
              for (AssignExpr assign : assigns) {
                stmts.add(
                    new SeqLoopCaseStmt(
                        pThread.id, false, Optional.of(assign.toString()), targetPc));
              }
            }

          } else if (isRelevantPthreadFunc(edge)) {
            PthreadFuncType funcType = PthreadFuncType.getPthreadFuncType(edge);
            switch (funcType) {
              case PTHREAD_CREATE:
                CExpression aPthreadT = PthreadUtil.extractPthreadT(edge);
                assert aPthreadT instanceof CIdExpression;
                CExpressionAssignmentStatement activeAssign =
                    SeqStatements.buildExprAssign(
                        pThreadActiveVars.get(aPthreadT), SeqExpressions.INT_ONE);
                stmts.add(
                    new SeqLoopCaseStmt(
                        pThread.id, false, Optional.of(activeAssign.toASTString()), targetPc));
                break;

              case PTHREAD_MUTEX_LOCK:
              case PTHREAD_MUTEX_UNLOCK:
                CExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(sub);
                assert pthreadMutexT instanceof CIdExpression;
                CIdExpression idExpr = (CIdExpression) pthreadMutexT;
                assert pMutexLockedVars.containsKey(idExpr);
                // if lock -> assign 1 to locked, otherwise 0
                CExpression value =
                    funcType.equals(PthreadFuncType.PTHREAD_MUTEX_LOCK)
                        ? SeqExpressions.INT_ONE
                        : SeqExpressions.INT_ZERO;
                CExpressionAssignmentStatement lockedAssign =
                    SeqStatements.buildExprAssign(pMutexLockedVars.get(idExpr), value);
                stmts.add(
                    new SeqLoopCaseStmt(
                        pThread.id, false, Optional.of(lockedAssign.toASTString()), targetPc));
                break;

              case PTHREAD_JOIN:
                MPORThread targetThread =
                    PthreadUtil.extractThread(pThreadJoiningVars.keySet(), edge);
                CExpressionAssignmentStatement joiningTrueAssign =
                    SeqStatements.buildExprAssign(
                        pThreadJoiningVars.get(pThread).get(targetThread), SeqExpressions.INT_ONE);
                stmts.add(
                    new SeqLoopCaseStmt(
                        pThread.id, false, Optional.of(joiningTrueAssign.toASTString()), targetPc));
                break;

              default:
                throw new IllegalArgumentException(
                    "unhandled relevant pthread method: " + funcType.name);
            }

          } else {
            stmts.add(
                new SeqLoopCaseStmt(
                    pThread.id, false, Optional.of(new EdgeCodeExpr(sub).toString()), targetPc));
          }
        }
      }
    }
    return new SeqLoopCase(originPc, stmts.build());
  }

  public static String createLineOfCode(CFAEdge pEdge) {
    if (pEdge instanceof AssumeEdge) {
      return SeqToken.ASSUME
          + SeqSyntax.BRACKET_LEFT
          + pEdge.getCode()
          + SeqSyntax.BRACKET_RIGHT
          + SeqSyntax.SEMICOLON;
    }
    if (pEdge.getCode().endsWith(SeqSyntax.SEMICOLON)) {
      return pEdge.getCode();
    } else {
      return pEdge.getCode() + SeqSyntax.SEMICOLON;
    }
  }

  /** Returns { pExpression } */
  public static String wrapInCurlyInwards(SeqExpression pExpression) {
    return SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + pExpression
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  /** Returns } pExpression { */
  public static String wrapInCurlyOutwards(SeqExpression pExpression) {
    return SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.SPACE
        + pExpression
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT;
  }

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
