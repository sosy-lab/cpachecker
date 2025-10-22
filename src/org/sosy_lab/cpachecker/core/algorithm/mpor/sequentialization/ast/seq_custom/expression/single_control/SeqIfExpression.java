// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.SeqASTNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqIfExpression implements SeqSingleControlExpression {

  private final SeqExpression seqExpression;

  public SeqIfExpression(SeqExpression pSeqExpression) {
    seqExpression = pSeqExpression;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return SingleControlStatementType.IF.getKeyword()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInBrackets(seqExpression.toASTString());
  }

  // TODO make SeqAstNode / CAstNode parameters, and then inject automatically

  public String toASTStringWithSeqAstNodeBlock(ImmutableList<SeqASTNode> pBlockStatements)
      throws UnrecognizedCodeException {

    String block = SeqStringUtil.buildStringFromSeqASTNodes(pBlockStatements);
    return toASTString() + SeqStringUtil.wrapInCurlyBracketsInwards(block);
  }

  @Override
  public SingleControlStatementType getEncoding() {
    return SingleControlStatementType.IF;
  }
}
