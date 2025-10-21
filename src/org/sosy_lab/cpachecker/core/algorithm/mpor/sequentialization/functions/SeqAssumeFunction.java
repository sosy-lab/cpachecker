// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqParameterDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAssumeFunction extends SeqFunction {

  private static final CFunctionCallExpression abortCall =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          SeqIdExpressions.ABORT,
          ImmutableList.of(),
          SeqFunctionDeclarations.ABORT);

  private final SeqSingleControlExpression ifCond;

  public SeqAssumeFunction(CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {
    ifCond =
        new SeqIfExpression(
            pBinaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpressions.COND, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS));
  }

  @Override
  public ImmutableList<String> buildBody() throws UnrecognizedCodeException {
    ImmutableList.Builder<String> rBody = ImmutableList.builder();
    String code =
        SeqStringUtil.appendCurlyBracketLeft(ifCond.toASTString())
            + SeqSyntax.SPACE
            + abortCall.toASTString()
            + SeqSyntax.SEMICOLON
            + SeqSyntax.SPACE
            + SeqSyntax.CURLY_BRACKET_RIGHT;
    rBody.add(code);
    return rBody.build();
  }

  @Override
  public CType getReturnType() {
    return CVoidType.VOID;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpressions.ASSUME;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameterDeclarations() {
    ImmutableList.Builder<CParameterDeclaration> rParameters = ImmutableList.builder();
    rParameters.add(SeqParameterDeclarations.COND);
    return rParameters.build();
  }
}
