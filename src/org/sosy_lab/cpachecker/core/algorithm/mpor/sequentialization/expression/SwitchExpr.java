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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
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
    String cases = SeqSyntax.EMPTY_STRING.getString();
    for (var entry : globalAccesses.entrySet()) {
      cases += generateCase(Integer.toString(entry.getKey().id), entry.getValue());
    }
    return SeqToken.SWITCH.getString()
        + SeqSyntax.SPACE.getString()
        + SeqSyntax.BRACKET_LEFT.getString()
        + expression.generateString()
        + SeqSyntax.BRACKET_RIGHT.getString()
        + SeqSyntax.SPACE.getString()
        + SeqSyntax.CURLY_BRACKET_LEFT.getString()
        + SeqSyntax.NEWLINE.getString()
        + cases
        + SeqUtil.tab
        + SeqSyntax.CURLY_BRACKET_RIGHT.getString();
  }

  public static String generateCase(String pCase, CFAEdge pEdge) {
    String codeBlock = pEdge.getCode();
    String suffix = SeqSyntax.SEMICOLON.getString();
    if (codeBlock.endsWith(SeqSyntax.SEMICOLON.getString())) {
      suffix = SeqSyntax.EMPTY_STRING.getString();
    }
    if (pEdge instanceof AssumeEdge) {
      codeBlock =
          SeqToken.ASSUME.getString()
              + SeqSyntax.BRACKET_LEFT.getString()
              + codeBlock
              + SeqSyntax.BRACKET_RIGHT.getString();
    }
    return SeqUtil.tab
        + SeqUtil.tab
        + SeqToken.CASE.getString()
        + SeqSyntax.SPACE.getString()
        + pCase
        + SeqSyntax.COLON.getString()
        + SeqSyntax.SPACE.getString()
        + codeBlock
        + suffix
        + SeqSyntax.SPACE.getString()
        + SeqToken.BREAK.getString()
        + SeqSyntax.SEMICOLON.getString()
        + SeqSyntax.NEWLINE.getString();
  }
}
