// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.function;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public abstract class SeqFunction {

  abstract ImmutableList<LineOfCode> buildBody();

  abstract CType getReturnType();

  abstract CIdExpression getFunctionName();

  abstract ImmutableList<CParameterDeclaration> getParameters();

  /**
   * Basically {@link CFunctionDeclaration#toASTString()} with parameter names but without the
   * suffix {@code ;}.
   */
  private String buildSignature() {
    StringBuilder parameters = new StringBuilder();
    for (int i = 0; i < getParameters().size(); i++) {
      CParameterDeclaration param = getParameters().get(i);
      String suffix =
          i == getParameters().size() - 1
              ? SeqSyntax.EMPTY_STRING
              : SeqSyntax.COMMA + SeqSyntax.SPACE;
      parameters.append(param.toASTString()).append(suffix);
    }
    return getReturnType().toASTString(SeqSyntax.EMPTY_STRING)
        + SeqSyntax.SPACE
        + getFunctionName().getName()
        + SeqSyntax.BRACKET_LEFT
        + parameters
        + SeqSyntax.BRACKET_RIGHT;
  }

  public final ImmutableList<LineOfCode> buildDefinition() {
    ImmutableList.Builder<LineOfCode> rDefinition = ImmutableList.builder();
    rDefinition.add(LineOfCode.of(0, SeqUtil.appendOpeningCurly(buildSignature())));
    rDefinition.addAll(buildBody());
    rDefinition.add(LineOfCode.of(0, SeqSyntax.CURLY_BRACKET_RIGHT));
    return rDefinition.build();
  }
}
