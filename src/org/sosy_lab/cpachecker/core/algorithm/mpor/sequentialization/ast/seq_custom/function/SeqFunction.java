// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.function;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.SeqASTNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public interface SeqFunction extends SeqASTNode {

  CType getReturnType();

  CIdExpression getFunctionName();

  ImmutableList<CParameterDeclaration> getParameters();

  CFunctionDeclaration getDeclaration();

  /**
   * Basically {@link CFunctionDeclaration#toASTString()} with parameter names but without the
   * suffix {@code ;}.
   */
  default String getDeclarationWithParameterNames() {
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
}