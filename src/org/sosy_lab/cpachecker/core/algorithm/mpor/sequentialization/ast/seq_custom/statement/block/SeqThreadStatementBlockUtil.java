// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SingleControlExpressionEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SeqThreadStatementBlockUtil {

  static boolean isLoopStart(ImmutableList<SeqThreadStatement> pStatements) {
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
        CFANode predecessor = substituteEdge.cfaEdge.getPredecessor();
        if (predecessor.isLoopStart()) {
          // simple for / while loop with predicate expression -> loop is in direct predecessor
          return true;
        } else if (isAnyWhileTrueLoopStart(predecessor)) {
          // infinite while (1) loop -> loop is in predecessor of predecessor
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isAnyWhileTrueLoopStart(CFANode pCfaNode) {
    for (CFAEdge enteringEdge : CFAUtils.enteringEdges(pCfaNode)) {
      CFANode predecessor = enteringEdge.getPredecessor();
      if (isWhileTrueLoopStart(predecessor)) {
        return true;
      } else if (isWhileTrueLoopStartWithDeclaration(enteringEdge, predecessor)) {
        return true;
      } else if (isWhileTrueLoopStartWithFunctionCall(predecessor)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isWhileTrueLoopStartWithDeclaration(CFAEdge pCfaEdge, CFANode pCfaNode) {
    if (pCfaEdge instanceof CDeclarationEdge) {
      // edge case: while(1) starts e.g. with switch(__VERIFIER_nondet...())
      // which is transformed into separate declaration
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(pCfaNode)) {
        if (isWhileTrueLoopStart(enteringEdge.getPredecessor())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isWhileTrueLoopStartWithFunctionCall(CFANode pCfaNode) {
    if (pCfaNode instanceof CFunctionEntryNode) {
      // edge case: while(1) is followed directly by function call
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(pCfaNode)) {
        if (leavingEdge.getDescription().equals("Function start dummy edge")) {
          for (CFAEdge functionCallEdge : CFAUtils.enteringEdges(pCfaNode)) {
            for (CFAEdge enteringEdge : CFAUtils.enteringEdges(functionCallEdge.getPredecessor())) {
              if (isWhileTrueLoopStart(enteringEdge.getPredecessor())) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean isWhileTrueLoopStart(CFANode pCfaNode) {
    if (pCfaNode.isLoopStart()) {
      for (CFAEdge enteringEdge : CFAUtils.enteringEdges(pCfaNode)) {
        if (enteringEdge instanceof BlankEdge blankEdge) {
          if (blankEdge.getDescription().equals(SingleControlExpressionEncoding.WHILE.keyword)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
