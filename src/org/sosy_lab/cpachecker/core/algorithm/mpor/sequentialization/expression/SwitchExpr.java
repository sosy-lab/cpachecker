// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SwitchExpr implements SeqExpression {

  // TODO restrict to Variable, ArrayExpr
  private final SeqExpression expression;

  private final ImmutableMap<MPORThread, CFAEdge> globalAccesses;

  public SwitchExpr(SeqExpression pExpression, ImmutableMap<MPORThread, CFAEdge> pGlobalAccesses) {
    expression = pExpression;
    globalAccesses = pGlobalAccesses;
  }

  @Override
  public String generateString() {
    String cases = "";
    int count = 0;
    // TODO handle assumeEdges
    // TODO create separate createCaseFromEdge Method
    for (var entry : globalAccesses.entrySet()) {
      cases +=
          SeqToken.CASE.getString()
              + SeqSyntax.SPACE.getString()
              + count
              + SeqSyntax.COLON.getString()
              + SeqSyntax.SPACE.getString()
              + entry.getValue().getCode()
              + SeqSyntax.SEMICOLON.getString()
              + SeqSyntax.SPACE.getString()
              + SeqToken.BREAK.getString()
              + SeqSyntax.SEMICOLON.getString()
              + SeqSyntax.NEWLINE.getString();
      count++;
    }
    return SeqToken.SWITCH.getString()
        + SeqSyntax.BRACKET_LEFT.getString()
        + expression.generateString()
        + SeqSyntax.BRACKET_RIGHT.getString()
        + SeqSyntax.SPACE.getString()
        + SeqSyntax.CURLY_BRACKET_LEFT.getString()
        + cases
        + SeqSyntax.CURLY_BRACKET_RIGHT.getString();
  }
}
