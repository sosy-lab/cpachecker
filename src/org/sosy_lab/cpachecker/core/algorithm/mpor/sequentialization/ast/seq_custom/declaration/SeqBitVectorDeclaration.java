// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SeqBitVectorType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqBitVectorDeclaration implements SeqDeclaration {
  private final SeqBitVectorType type;
  private final CIdExpression variable;
  private final SeqBitVector initializer;

  public SeqBitVectorDeclaration(
      SeqBitVectorType pType, CIdExpression pVariable, SeqBitVector pInitializer) {
    type = pType;
    variable = pVariable;
    initializer = pInitializer;
  }

  @Override
  public String toASTString() {
    return type.toASTString()
        + SeqSyntax.SPACE
        + variable.toASTString()
        + SeqSyntax.SPACE
        + SeqSyntax.EQUALS
        + SeqSyntax.SPACE
        + initializer.toASTString()
        + SeqSyntax.SEMICOLON;
  }
}
