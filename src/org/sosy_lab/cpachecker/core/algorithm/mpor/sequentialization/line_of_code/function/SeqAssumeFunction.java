// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqParameterDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAssumeFunction implements SeqFunction {

  private static final CFunctionCallExpression abortCall =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          SeqVoidType.VOID,
          SeqIdExpression.ABORT,
          ImmutableList.of(),
          SeqFunctionDeclaration.ABORT);

  private final SeqControlFlowStatement ifCond;

  public SeqAssumeFunction() throws UnrecognizedCodeException {
    ifCond =
        new SeqControlFlowStatement(
            SeqBinaryExpression.buildBinaryExpression(
                SeqIdExpression.COND, SeqIntegerLiteralExpression.INT_0, BinaryOperator.EQUALS),
            SeqControlFlowStatementType.IF);
  }

  @Override
  public ImmutableList<LineOfCode> buildBody() {
    ImmutableList.Builder<LineOfCode> rDefinition = ImmutableList.builder();
    rDefinition.add(LineOfCode.of(1, SeqUtil.appendOpeningCurly(ifCond.toASTString())));
    rDefinition.add(LineOfCode.of(2, abortCall.toASTString() + SeqSyntax.SEMICOLON));
    rDefinition.add(LineOfCode.of(1, SeqSyntax.CURLY_BRACKET_RIGHT));
    return rDefinition.build();
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
