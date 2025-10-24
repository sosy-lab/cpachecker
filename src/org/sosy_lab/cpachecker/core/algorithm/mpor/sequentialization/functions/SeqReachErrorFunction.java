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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqParameterDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqStringLiteralExpressions;

public class SeqReachErrorFunction extends SeqFunction {

  private static final CFunctionCallExpression assertFailFunctionCallExpression =
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

  private static final CFunctionCallStatement assertFailFunctionCallStatement =
      new CFunctionCallStatement(FileLocation.DUMMY, assertFailFunctionCallExpression);

  public SeqReachErrorFunction() {}

  @Override
  public String buildBody() {
    return assertFailFunctionCallStatement.toASTString();
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
