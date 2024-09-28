// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class InitializerListExpr implements SeqExpression {

  private final ImmutableList<SeqExpression> initializers;

  public InitializerListExpr(ImmutableList<SeqExpression> pInitializers) {
    initializers = pInitializers;
  }

  @Override
  public String createString() {
    StringBuilder inits = new StringBuilder();
    String suffix = SeqSyntax.COMMA + SeqSyntax.SPACE;
    for (int i = 0; i < initializers.size(); i++) {
      inits.append(initializers.get(i).createString());
      if (i != initializers.size() - 1) {
        inits.append(suffix);
      }
    }
    return SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + inits
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }
}
