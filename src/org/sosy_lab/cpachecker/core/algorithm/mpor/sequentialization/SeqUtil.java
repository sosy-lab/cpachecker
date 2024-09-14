// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.ArrayElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.EdgeCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ElseIfCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqComment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

public class SeqUtil {

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  private static final Variable pcs = new Variable(SeqToken.PCS);

  private static final Variable nextThread = new Variable(SeqToken.NEXT_THREAD);

  private static final ArrayElement pcsNextThread = new ArrayElement(pcs, nextThread);

  private static final AssignExpr setExitPc =
      new AssignExpr(pcsNextThread, new Value(Integer.toString(EXIT_PC)));

  public static ImmutableSet<CVariableDeclaration> createReturnPcVarDecs(MPORThread pThread) {
    ImmutableSet.Builder<CVariableDeclaration> rDecs = ImmutableSet.builder();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      if (threadEdge.cfaEdge instanceof FunctionSummaryEdge funcSummaryEdge) {
        String varName = createReturnPcVarName(pThread.id,
            funcSummaryEdge.getFunctionEntry().getFunctionName());
        CVariableDeclaration varDec =
            new CVariableDeclaration(
                funcSummaryEdge.getFileLocation(), // not correct, just a placeholder
                false,
                CStorageClass.AUTO,
                ,
                varName,
                varName,
                varName,
                null);
      }
    }
    return rDecs.build();
  }

  private static String createReturnPcVarName(int pThreadId, String pFuncName) {

  }

  public static String createDeclarations(
      ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> pDecSubstitutions) {

    StringBuilder rDeclarations = new StringBuilder();
    for (var entry : pDecSubstitutions.entrySet()) {
      MPORThread thread = entry.getKey();
      CSimpleDeclarationSubstitution substitution = entry.getValue();
      // declare global vars only once (because every thread substitution has every global var)
      if (thread.isMain()) {
        assert substitution.globalVarSubs != null; // should always hold, null only used in dummy
        rDeclarations.append(SeqComment.createGlobalVarsComment());
        for (CVariableDeclaration globalSub : substitution.globalVarSubs.values()) {
          rDeclarations.append(globalSub.toASTString()).append(SeqSyntax.NEWLINE);
        }
        rDeclarations.append(SeqSyntax.NEWLINE);
      }
      rDeclarations.append(SeqComment.createThreadVarsComment(thread.id));
      for (CVariableDeclaration localSub : substitution.localVarSubs.values()) {
        // TODO handle const CPAcheckerTMPs as atomics (declared before the loop)
        if (!isConstCpaCheckerTMP(localSub)) {
          rDeclarations.append(localSub.toASTString()).append(SeqSyntax.NEWLINE);
        }
      }
      for (CVariableDeclaration paramSub : substitution.paramSubs.values()) {
        rDeclarations.append(paramSub.toASTString()).append(SeqSyntax.NEWLINE);
      }
      rDeclarations.append(SeqSyntax.NEWLINE);
    }
    return rDeclarations.toString();
  }

  // TODO make sure all pthread_... functions are removed (skip pcs)
  // TODO make sure all return statements are removed (skip pcs)
  // TODO make sure all function call statements are removed (skip pcs)
  // TODO make sure function parameter names are changed to original calling name
  // TODO test if blank edges can always be safely skipped
  public static String createCodeFromThreadNode(
      ThreadNode pThreadNode, ImmutableMap<ThreadEdge, CFAEdge> pEdgeSubs) {

    StringBuilder code = new StringBuilder();

    // no edges -> exit node reached (assert fail or main / start routine exit node)
    if (pThreadNode.leavingEdges().isEmpty()) {
      // TODO test, remove later
      assert pThreadNode.pc == EXIT_PC;
      code.append(setExitPc.createString());

    } else {
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges()) {
        CFAEdge substitute = pEdgeSubs.get(threadEdge);
        AssignExpr updatePcsNextThread = createUpdatePcsNextThread(threadEdge.getSuccessor().pc);

        if (emptyCaseCode(substitute)) {
          code.append(updatePcsNextThread.createString()); // TODO prune empty cases later

        } else {
          // use (else) if (condition) for assumes, no matter if induced by if, for, while...
          if (substitute instanceof CAssumeEdge) {
            assert allEdgesAssume(pThreadNode.leavingEdges()); // TODO test, remove later
            IfExpr ifExpr = new IfExpr(new EdgeCodeExpr(substitute));
            if (firstEdge) {
              firstEdge = false;
              IfCodeExpr ifCodeExpr = new IfCodeExpr(ifExpr, updatePcsNextThread);
              code.append(ifCodeExpr.createString());
            } else {
              ElseIfCodeExpr elseIfCodeExpr = new ElseIfCodeExpr(ifExpr, updatePcsNextThread);
              code.append(SeqSyntax.NEWLINE).append(elseIfCodeExpr.createString());
            }
          } else {
            code.append(substitute.getCode())
                .append(SeqSyntax.SPACE)
                .append(updatePcsNextThread.createString());
          }
        }

        // TODO CDeclarationEdge: put them all in front of the loop
        //  declarationEdges have to be checked for a right hand side. if it is present, the
        //  declaration is transformed into an assignment in the seq. if it is not present, the
        //  edge is skipped

        /*} else if (substitute instanceof FunctionCallEdge functionCallEdge) {
        // TODO map calling parameter name to actual parameter name
        code.append(substitute.getCode())
            .append(SeqSyntax.SPACE)
            .append(updatePcsNextThread.createString()); */
      }
    }
    return code.toString();
  }

  private static boolean emptyCaseCode(CFAEdge pEdge) {
    if (pEdge instanceof BlankEdge || pEdge instanceof CFunctionReturnEdge) {
      assert pEdge.getCode().isEmpty(); // TODO test, remove later
      return true;
    } else if (pEdge instanceof CDeclarationEdge decEdge) {
      CDeclaration dec = decEdge.getDeclaration();
      if (dec instanceof CVariableDeclaration varDec) {
        // code of const int CPAchecker_TMP vars is included in the cases
        // TODO make sure that the two successors code is within the same case
        if (isConstCpaCheckerTMP(varDec)) {
          return false;
        }
      }
    }
    return PthreadFuncType.isCallToAnyPthreadFunc(pEdge);
  }

  private static boolean isConstCpaCheckerTMP(CVariableDeclaration pVarDec) {
    return pVarDec.getType().isConst()
        && !pVarDec.isGlobal()
        && pVarDec.getName().contains(SeqToken.CPACHECKER_TMP);
  }

  private static AssignExpr createUpdatePcsNextThread(int pPc) {
    return new AssignExpr(pcsNextThread, new Value(Integer.toString(pPc)));
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
}
