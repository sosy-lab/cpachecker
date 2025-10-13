// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.SeqASTNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqIfExpression implements SeqSingleControlExpression {

  private final Optional<CExpression> cExpression;

  private final Optional<SeqExpression> seqExpression;

  public SeqIfExpression(CExpression pCExpression) {
    cExpression = Optional.of(pCExpression);
    seqExpression = Optional.empty();
  }

  public SeqIfExpression(SeqExpression pSeqExpression) {
    cExpression = Optional.empty();
    seqExpression = Optional.of(pSeqExpression);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String expression =
        SingleControlExpressionUtil.buildExpressionString(cExpression, seqExpression);
    return SingleControlExpressionUtil.buildStatementString(this, expression);
  }

  // TODO make SeqAstNode / CAstNode parameters, and then inject automatically

  public String toASTStringWithSeqAstNodeBlock(ImmutableList<SeqASTNode> pBlockStatements)
      throws UnrecognizedCodeException {

    String block = SeqStringUtil.buildStringFromSeqASTNodes(pBlockStatements);
    return toASTString() + SeqStringUtil.wrapInCurlyBracketsInwards(block);
  }

  public String toASTStringWithCAstNodeBlock(ImmutableList<CAstNode> pBlockStatements)
      throws UnrecognizedCodeException {

    String block = SeqStringUtil.buildStringFromCAstNodes(pBlockStatements);
    return toASTString() + SeqStringUtil.wrapInCurlyBracketsInwards(block);
  }

  @Override
  public SingleControlExpressionEncoding getEncoding() {
    return SingleControlExpressionEncoding.IF;
  }
}
