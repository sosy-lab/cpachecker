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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqParameterDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqStringLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqReachErrorFunction extends SeqFunction {

  private final CFunctionCallExpression assertFailCall;

  public SeqReachErrorFunction() {
    assertFailCall =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            CVoidType.VOID,
            SeqIdExpressions.ASSERT_FAIL,
            ImmutableList.of(
                SeqStringLiteralExpressions.STRING_0,
                SeqIdExpressions.FILE,
                SeqIdExpressions.LINE,
                SeqIdExpressions.FUNCTION),
            SeqFunctionDeclarations.ASSERT_FAIL);
  }

  @Override
  public ImmutableList<String> buildBody() {
    ImmutableList.Builder<String> rBody = ImmutableList.builder();
    rBody.add(assertFailCall.toASTString() + SeqSyntax.SEMICOLON);
    return rBody.build();
  }

  @Override
  public CType getReturnType() {
    return CVoidType.VOID;
  }

  @Override
  public CIdExpression getFunctionName() {
    return SeqIdExpressions.REACH_ERROR;
  }

  @Override
  public ImmutableList<CParameterDeclaration> getParameterDeclarations() {
    ImmutableList.Builder<CParameterDeclaration> rParameters = ImmutableList.builder();
    rParameters.add(
        SeqParameterDeclarations.FILE,
        SeqParameterDeclarations.LINE,
        SeqParameterDeclarations.FUNCTION);
    return rParameters.build();
  }
}
