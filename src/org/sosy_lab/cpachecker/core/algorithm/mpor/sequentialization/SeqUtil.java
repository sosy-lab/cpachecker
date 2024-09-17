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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ASTStringExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.EdgeCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ElseIfCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExprBuilder;
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
      // TODO handle const CPAcheckerTMPs as atomics (declared before the loop)
      if (!isConstCpaCheckerTMP(sub)) {
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

  // TODO make sure all pthread_... functions are removed (skip pcs)
  // TODO make sure all return statements are removed (skip pcs)
  // TODO make sure all function call statements are removed (skip pcs)
  // TODO make sure function parameter names are changed to original calling name
  // TODO test if blank edges can always be safely skipped
  public static String createCodeFromThreadNode(
      ThreadNode pThreadNode,
      ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs,
      ImmutableMap<ThreadEdge, ImmutableList<AssignExpr>> pParamAssigns,
      ImmutableMap<ThreadNode, AssignExpr> pReturnPcAssigns) {

    StringBuilder code = new StringBuilder();

    // no edges -> exit node reached (assert fail or main / start routine exit node)
    if (pThreadNode.leavingEdges().isEmpty()) {
      // TODO test, remove later
      assert pThreadNode.pc == EXIT_PC;
      code.append(SeqExprBuilder.setExitPc.createString());

    } else if (pThreadNode.cfaNode instanceof FunctionExitNode) {
      assert pReturnPcAssigns.containsKey(pThreadNode);
      AssignExpr assign = pReturnPcAssigns.get(pThreadNode);
      assert assign != null;
      code.append(assign.createString());

    } else {
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {
        CFAEdge sub = pEdgeSubs.get(threadEdge);
        AssignExpr updatePcsNextThread =
            SeqExprBuilder.createPcsNextThreadAssign(threadEdge.getSuccessor().pc);

        if (emptyCaseCode(sub)) {
          code.append(updatePcsNextThread.createString()); // TODO prune empty cases later

        } else {

          // use (else) if (condition) for assumes, no matter if induced by if, for, while...
          if (sub instanceof CAssumeEdge) {
            assert allEdgesAssume(pThreadNode.leavingEdges()); // TODO test, remove later
            IfExpr ifExpr = new IfExpr(new EdgeCodeExpr(sub));
            if (firstEdge) {
              firstEdge = false;
              IfCodeExpr ifCodeExpr = new IfCodeExpr(ifExpr, updatePcsNextThread);
              code.append(ifCodeExpr.createString());
            } else {
              ElseIfCodeExpr elseIfCodeExpr = new ElseIfCodeExpr(ifExpr, updatePcsNextThread);
              code.append(SeqSyntax.NEWLINE).append(elseIfCodeExpr.createString());
            }

          } else if (sub instanceof FunctionCallEdge) {
            assert pParamAssigns.containsKey(threadEdge);
            ImmutableList<AssignExpr> assigns = pParamAssigns.get(threadEdge);
            assert assigns != null;
            for (AssignExpr assign : assigns) {
              code.append(assign.createString());
            }

          } else {
            code.append(sub.getCode())
                .append(SeqSyntax.SPACE)
                .append(updatePcsNextThread.createString());
          }
        }
      }
    }
    return code.toString();
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

  private static boolean isConstCpaCheckerTMP(CVariableDeclaration pVarDec) {
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
        return !isConstCpaCheckerTMP(varDec);
      }
    }
    return PthreadFuncType.isCallToAnyPthreadFunc(pEdge);
  }
}
