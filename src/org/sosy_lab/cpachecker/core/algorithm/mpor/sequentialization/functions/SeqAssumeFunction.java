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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqFunctionDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqParameterDeclarations;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SingleControlStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqAssumeFunction extends SeqFunction {

  private static final CFunctionCallExpression abortFunctionCallExpression =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          SeqIdExpressions.ABORT,
          ImmutableList.of(),
          SeqFunctionDeclarations.ABORT);

  private static final CFunctionCallStatement abortFunctionCallStatement =
      new CFunctionCallStatement(FileLocation.DUMMY, abortFunctionCallExpression);

  private final CBinaryExpression condEqualsZeroExpression;

  public SeqAssumeFunction(CBinaryExpression pCondEqualsZeroExpression) {
    condEqualsZeroExpression = pCondEqualsZeroExpression;
  }

  @Override
  public ImmutableList<String> buildBody() {
    ImmutableList.Builder<String> rBody = ImmutableList.builder();
    rBody.add(SingleControlStatementType.IF.buildControlFlowPrefix(condEqualsZeroExpression));
    rBody.add(abortFunctionCallStatement.toASTString());
    rBody.add(SeqSyntax.CURLY_BRACKET_RIGHT);
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
