// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.ArrayElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Value;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity.Variable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.AssignExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.EdgeCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.ElseIfCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfCodeExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.IfExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

public class SeqUtil {

  public static final int INIT_PC = 0;

  public static final int EXIT_PC = -1;

  private static final Variable pcs = new Variable(SeqToken.PCS);

  private static final Variable nextThread = new Variable(SeqToken.NEXT_THREAD);

  private static final ArrayElement pcsNextThread = new ArrayElement(pcs, nextThread);

  private static final AssignExpr resetPcsNextThread =
      new AssignExpr(pcsNextThread, new Value(Integer.toString(EXIT_PC)));

  // TODO make sure all pthread_... functions are removed (skip pcs)
  // TODO make sure all return statements are removed (skip pcs)
  // TODO make sure all function call statements are removed (skip pcs)
  // TODO make sure function parameter names are changed to original calling name
  // TODO test if blank edges can always be safely skipped
  public static String createCodeFromThreadNode(ThreadNode pThreadNode) {
    StringBuilder code = new StringBuilder();

    if (pThreadNode.pc == EXIT_PC) {
      code.append(resetPcsNextThread.createString());

    } else {

      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        AssignExpr updatePcsNextThread = createUpdatePcsNextThread(threadEdge.getSuccessor().pc);
        switch (cfaEdge.getEdgeType()) {
          // use (else) if (condition) for assumes, no matter if induced by if, for, while...
          case AssumeEdge:
            // TODO here for test purposes, can be removed later
            assert allEdgesAssume(pThreadNode.leavingEdges);
            IfExpr ifExpr = new IfExpr(new EdgeCodeExpr(cfaEdge));
            if (firstEdge) {
              firstEdge = false;
              IfCodeExpr ifCodeExpr = new IfCodeExpr(ifExpr, updatePcsNextThread);
              code.append(ifCodeExpr.createString());
            } else {
              ElseIfCodeExpr elseIfCodeExpr = new ElseIfCodeExpr(ifExpr, updatePcsNextThread);
              code.append(SeqSyntax.NEWLINE).append(elseIfCodeExpr.createString());
            }
            break;

          case FunctionCallEdge:
            // TODO
            //  extract the original parameter names, find all edges inside the called function,
            //  replace the parameter names with the original names
            code.append(cfaEdge.getCode())
                .append(SeqSyntax.SPACE)
                .append(updatePcsNextThread.createString());
            break;

          // TODO
          //  ReturnStatementEdge
          //  FunctionReturnEdge
          //  CallToReturnEdge
          //  BlankEdge

          default:
            // TODO here for test purposes, can be removed later
            assert pThreadNode.leavingEdges.size() == 1;
            // TODO not crucial but also remove pthread_t and pthread_mutex_t variables
            // do not include any pthread functions
            if (PthreadFuncType.isEdgeCallToAnyFunc(cfaEdge)) {
              code.append(updatePcsNextThread.createString());
            } else {
              code.append(cfaEdge.getCode())
                  .append(SeqSyntax.SPACE)
                  .append(updatePcsNextThread.createString());
            }
            break;
        }
      }
    }
    return code.toString();
  }

  private static AssignExpr createUpdatePcsNextThread(int pPc) {
    return new AssignExpr(pcsNextThread, new Value(Integer.toString(pPc)));
  }

  // TODO here for test purposes
  private static boolean allEdgesAssume(ImmutableSet<ThreadEdge> pThreadEdges) {
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
