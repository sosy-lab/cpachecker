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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SingleControlExpressionEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SeqThreadStatementBlockUtil {

  static boolean isLoopStart(ImmutableList<SeqThreadStatement> pStatements) {
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
        CFANode predecessorA = substituteEdge.cfaEdge.getPredecessor();
        if (predecessorA.isLoopStart()) {
          // simple for / while loop with predicate expression -> loop is in direct predecessor
          return true;
        } else {
          // infinite while (1) loop -> loop is in predecessor of predecessor
          for (CFAEdge enteringEdgeA : CFAUtils.enteringEdges(predecessorA)) {
            CFANode predecessorB = enteringEdgeA.getPredecessor();
            if (isWhileTrueLoopStart(predecessorB)) {
              return true;
            } else if (enteringEdgeA instanceof CDeclarationEdge) {
              // edge case: while(1) starts with switch(__VERIFIER_nondet...())
              // which is transformed into separate declaration
              for (CFAEdge enteringEdgeB : CFAUtils.enteringEdges(predecessorB)) {
                if (isWhileTrueLoopStart(enteringEdgeB.getPredecessor())) {
                  return true;
                }
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
      for (CFAEdge enteringEdgeB : CFAUtils.enteringEdges(pCfaNode)) {
        if (enteringEdgeB instanceof BlankEdge blankEdge) {
          String description = blankEdge.getDescription();
          if (description.equals(SingleControlExpressionEncoding.WHILE.keyword)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
