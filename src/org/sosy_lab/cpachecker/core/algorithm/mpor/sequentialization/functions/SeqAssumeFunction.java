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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqParameterDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.single.SeqSingleControlStatement.SingleControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAssumeFunction extends SeqFunction {

  private static final CFunctionCallExpression abortCall =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          SeqVoidType.VOID,
          SeqIdExpression.ABORT,
          ImmutableList.of(),
          SeqFunctionDeclaration.ABORT);

  private final SeqSingleControlStatement ifCond;

  public SeqAssumeFunction(CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {
    ifCond =
        new SeqSingleControlStatement(
            pBinaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpression.COND, SeqIntegerLiteralExpression.INT_0, BinaryOperator.EQUALS),
            SingleControlStatementEncoding.IF);
  }

  @Override
  public ImmutableList<LineOfCode> buildBody() throws UnrecognizedCodeException {
    ImmutableList.Builder<LineOfCode> rBody = ImmutableList.builder();
    String code =
        SeqStringUtil.appendOpeningCurly(ifCond.toASTString())
            + SeqSyntax.SPACE
            + abortCall.toASTString()
            + SeqSyntax.SEMICOLON
            + SeqSyntax.SPACE
            + SeqSyntax.CURLY_BRACKET_RIGHT;
    rBody.add(LineOfCode.of(1, code));
    return rBody.build();
  }

  @Override
  public CType getReturnType() {
    return SeqVoidType.VOID;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpression.ASSUME;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameters() {
    ImmutableList.Builder<CParameterDeclaration> rParameters = ImmutableList.builder();
    rParameters.add(SeqParameterDeclaration.COND);
    return rParameters.build();
  }
}
