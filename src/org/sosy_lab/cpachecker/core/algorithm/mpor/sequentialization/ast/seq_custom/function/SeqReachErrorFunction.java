// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.function;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqParameterDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqStringLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqReachErrorFunction implements SeqFunction {

  private final CFunctionCallExpression assertFailCall;

  public SeqReachErrorFunction() {
    assertFailCall =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            SeqVoidType.VOID,
            SeqIdExpression.ASSERT_FAIL,
            ImmutableList.of(
                SeqStringLiteralExpression.STRING_0,
                SeqIdExpression.FILE,
                SeqIdExpression.LINE,
                SeqIdExpression.FUNCTION),
            SeqFunctionDeclaration.ASSERT_FAIL);
  }

  @Override
  public String toASTString() {
    return getSignature()
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithNewline(1, assertFailCall.toASTString() + SeqSyntax.SEMICOLON)
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  @Override
  public CType getReturnType() {
    return SeqVoidType.VOID;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpression.REACH_ERROR;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameters() {
    ImmutableList.Builder<CParameterDeclaration> rParameters = ImmutableList.builder();
    rParameters.add(
        SeqParameterDeclaration.FILE,
        SeqParameterDeclaration.LINE,
        SeqParameterDeclaration.FUNCTION);
    return rParameters.build();
  }
}
