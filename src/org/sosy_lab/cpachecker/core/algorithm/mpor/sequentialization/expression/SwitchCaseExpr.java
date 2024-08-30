// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SwitchCaseExpr implements SeqExpression {

  // TODO restrict to Variable, ArrayExpr
  private final SeqExpression expression;

  private final ImmutableMap<MPORThread, CFAEdge> globalAccesses;

  public SwitchCaseExpr(
      SeqExpression pExpression, ImmutableMap<MPORThread, CFAEdge> pGlobalAccesses) {
    expression = pExpression;
    globalAccesses = pGlobalAccesses;
  }

  @Override
  public String createString() {
    StringBuilder cases = new StringBuilder(SeqSyntax.EMPTY_STRING);
    for (var entry : globalAccesses.entrySet()) {
      cases.append(generateCase(Integer.toString(entry.getKey().id), entry.getValue()));
    }
    return SeqToken.SWITCH
        + SeqSyntax.SPACE
        + SeqSyntax.BRACKET_LEFT
        + expression.createString()
        + SeqSyntax.BRACKET_RIGHT
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + cases
        + SeqSyntax.TAB
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  public static String generateCase(String pCase, CFAEdge pEdge) {
    String codeBlock = pEdge.getCode();
    String suffix = SeqSyntax.SEMICOLON;
    if (codeBlock.endsWith(SeqSyntax.SEMICOLON)) {
      suffix = SeqSyntax.EMPTY_STRING;
    }
    if (pEdge instanceof AssumeEdge) {
      codeBlock = SeqToken.ASSUME + SeqSyntax.BRACKET_LEFT + codeBlock + SeqSyntax.BRACKET_RIGHT;
    }
    return SeqSyntax.TAB
        + SeqSyntax.TAB
        + SeqToken.CASE
        + SeqSyntax.SPACE
        + pCase
        + SeqSyntax.COLON
        + SeqSyntax.SPACE
        + codeBlock
        + suffix
        + SeqSyntax.SPACE
        + SeqToken.BREAK
        + SeqSyntax.SEMICOLON
        + SeqSyntax.NEWLINE;
  }
}
