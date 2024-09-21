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
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ASTStringExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.EdgeCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ElseIfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCase;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case.SeqLoopCaseStmt;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

public class SeqUtil {

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  public static ImmutableList<ASTStringExpr> createGlobalDeclarations(
      CSimpleDeclarationSubstitution pSubstitution) {

    ImmutableList.Builder<ASTStringExpr> rGlobalDecs = ImmutableList.builder();
    assert pSubstitution.globalVarSubs != null;
    for (CVariableDeclaration sub : pSubstitution.globalVarSubs.values()) {
      rGlobalDecs.add(new ASTStringExpr(sub.toASTString()));
    }
    return rGlobalDecs.build();
  }

  public static ImmutableList<ASTStringExpr> createLocalDeclarations(
      CSimpleDeclarationSubstitution pSubstitution) {

    ImmutableList.Builder<ASTStringExpr> rLocalDecs = ImmutableList.builder();
    assert pSubstitution.localVarSubs != null;
    for (CVariableDeclaration sub : pSubstitution.localVarSubs.values()) {
      if (!isConstCPAcheckerTMP(sub)) {
        rLocalDecs.add(new ASTStringExpr(sub.toASTString()));
      }
    }
    return rLocalDecs.build();
  }

  public static ImmutableList<ASTStringExpr> createParamDeclarations(
      CSimpleDeclarationSubstitution pSubstitution) {

    ImmutableList.Builder<ASTStringExpr> rParamDecs = ImmutableList.builder();
    assert pSubstitution.paramSubs != null;
    for (CVariableDeclaration sub : pSubstitution.paramSubs.values()) {
      rParamDecs.add(new ASTStringExpr(sub.toASTString()));
    }
    return rParamDecs.build();
  }

  /**
   * Returns a {@link SeqLoopCase} which represents case statements in the sequentializations while
   * loop. Returns null if pThreadNode has no leaving edges i.e. its pc is -1.
   */
  @Nullable
  public static SeqLoopCase createCaseFromThreadNode(
      Set<ThreadNode> pCoveredNodes,
      ThreadNode pThreadNode,
      ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs,
      ImmutableMap<ThreadEdge, AssignExpr> pReturnPcAssigns,
      ImmutableMap<ThreadEdge, ImmutableList<AssignExpr>> pParamAssigns,
      ImmutableMap<ThreadNode, AssignExpr> pPcsReturnPcAssigns) {

    pCoveredNodes.add(pThreadNode);

    int originPc = pThreadNode.pc;
    ImmutableList.Builder<SeqLoopCaseStmt> stmts = ImmutableList.builder();

    // no edges -> exit node reached (assert fail or main / start routine exit node)
    if (pThreadNode.leavingEdges().isEmpty()) {
      assert pThreadNode.pc == EXIT_PC; // TODO test, remove later
      return null;

      // exiting function: pc not relevant, assign thread and function-specific return pc
    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      assert pPcsReturnPcAssigns.containsKey(pThreadNode);
      AssignExpr assign = pPcsReturnPcAssigns.get(pThreadNode);
      assert assign != null;
      stmts.add(new SeqLoopCaseStmt(false, assign.createString(), Optional.empty()));

    } else {
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {
        CFAEdge sub = pEdgeSubs.get(threadEdge);
        Optional<Integer> targetPc = Optional.of(threadEdge.getSuccessor().pc);

        if (!emptyCaseCode(sub)) {
          // use (else) if (condition) for assumes, no matter if induced by if, for, while...
          if (sub instanceof CAssumeEdge) {
            assert allEdgesAssume(pThreadNode.leavingEdges()); // TODO test, remove later
            IfExpr ifExpr = new IfExpr(new EdgeCodeExpr(sub));
            if (firstEdge) {
              firstEdge = false;
              stmts.add(new SeqLoopCaseStmt(true, ifExpr.createString(), targetPc));
            } else {
              ElseIfExpr elseIfExpr = new ElseIfExpr(ifExpr);
              stmts.add(new SeqLoopCaseStmt(true, elseIfExpr.createString(), targetPc));
            }

          } else if (sub instanceof FunctionSummaryEdge) {
            assert pReturnPcAssigns.containsKey(threadEdge);
            AssignExpr assign = pReturnPcAssigns.get(threadEdge);
            assert assign != null;
            stmts.add(new SeqLoopCaseStmt(false, assign.createString(), targetPc));

          } else if (sub instanceof FunctionCallEdge) {
            assert pParamAssigns.containsKey(threadEdge);
            ImmutableList<AssignExpr> assigns = pParamAssigns.get(threadEdge);
            assert assigns != null;
            for (int i = 0; i < assigns.size(); i++) {
              AssignExpr assign = assigns.get(i);
              // if it is the last param assign, add the targetPc, otherwise empty
              stmts.add(
                  new SeqLoopCaseStmt(
                      false,
                      assign.createString(),
                      i == assigns.size() - 1 ? targetPc : Optional.empty()));
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
            stmts.add(new SeqLoopCaseStmt(false, sub.getCode(), Optional.empty()));
            stmts.add(new SeqLoopCaseStmt(false, succSub.getCode(), Optional.empty()));
            stmts.add(
                new SeqLoopCaseStmt(
                    false, succSuccSub.getCode(), Optional.of(succSuccEdge.getSuccessor().pc)));

          } else {
            assert sub != null;
            stmts.add(new SeqLoopCaseStmt(false, sub.getCode(), targetPc));
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

  public static String wrapInCurlyBrackets(SeqExpression pExpression) {
    return SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + pExpression.createString()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
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

  private static boolean isConstCPAcheckerTMP(CVariableDeclaration pVarDec) {
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
    }
    return PthreadFuncType.isCallToAnyPthreadFunc(pEdge);
  }
}
