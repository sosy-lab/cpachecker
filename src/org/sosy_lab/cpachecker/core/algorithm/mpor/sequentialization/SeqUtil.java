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
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
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

  private static final AssignExpr setExitPc =
      new AssignExpr(pcsNextThread, new Value(Integer.toString(EXIT_PC)));

  // TODO make sure all pthread_... functions are removed (skip pcs)
  // TODO make sure all return statements are removed (skip pcs)
  // TODO make sure all function call statements are removed (skip pcs)
  // TODO make sure function parameter names are changed to original calling name
  // TODO test if blank edges can always be safely skipped
  public static String createCodeFromThreadNode(ThreadNode pThreadNode) {
    StringBuilder code = new StringBuilder();

    // no edges -> exit node reached (assert fail or main / start routine exit node)
    if (pThreadNode.leavingEdges.isEmpty()) {
      // TODO test, remove later
      assert pThreadNode.pc == EXIT_PC;
      code.append(setExitPc.createString());

    } else {
      boolean firstEdge = true;
      for (ThreadEdge threadEdge : pThreadNode.leavingEdges) {
        CFAEdge substitute = threadEdge.substitute;
        AssignExpr updatePcsNextThread = createUpdatePcsNextThread(threadEdge.getSuccessor().pc);

        // use (else) if (condition) for assumes, no matter if induced by if, for, while...
        if (substitute instanceof CAssumeEdge) {
          // TODO here for test purposes, can be removed later
          assert allEdgesAssume(pThreadNode.leavingEdges);
          IfExpr ifExpr = new IfExpr(new EdgeCodeExpr(substitute));
          if (firstEdge) {
            firstEdge = false;
            IfCodeExpr ifCodeExpr = new IfCodeExpr(ifExpr, updatePcsNextThread);
            code.append(ifCodeExpr.createString());
          } else {
            ElseIfCodeExpr elseIfCodeExpr = new ElseIfCodeExpr(ifExpr, updatePcsNextThread);
            code.append(SeqSyntax.NEWLINE).append(elseIfCodeExpr.createString());
          }

          // TODO CDeclarationEdge: put them all in front of the loop
          //  declarationEdges have to be checked for a right hand side. if it is present, the
          //  declaration is transformed into an assignment in the seq. if it is not present, the
          //  edge is skipped

          /*} else if (substitute instanceof FunctionCallEdge functionCallEdge) {
            // TODO map calling parameter name to actual parameter name
            code.append(substitute.getCode())
                .append(SeqSyntax.SPACE)
                .append(updatePcsNextThread.createString());

          } else if (substitute instanceof CFunctionReturnEdge) {
            // TODO test purposes, remove later
            assert pThreadNode.leavingEdges.size() == 1;
            assert substitute.getCode().equals(SeqSyntax.EMPTY_STRING);
            // TODO pc has to be skipped

          } else if (substitute instanceof BlankEdge) {
            // TODO test purposes, remove later
            assert pThreadNode.leavingEdges.size() == 1;
            assert substitute.getCode().equals(SeqSyntax.EMPTY_STRING);
            // TODO pc has to be skipped*/

        } else {
          // TODO not crucial but also remove pthread_t and pthread_mutex_t variables
          // do not include any pthread functions
          if (PthreadFuncType.isEdgeCallToAnyFunc(substitute)) {
            code.append(updatePcsNextThread.createString());
          } else {
            code.append(substitute.getCode())
                .append(SeqSyntax.SPACE)
                .append(updatePcsNextThread.createString());
          }
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
