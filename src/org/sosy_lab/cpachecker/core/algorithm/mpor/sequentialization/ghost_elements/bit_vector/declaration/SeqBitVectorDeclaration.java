// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.declaration;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqBitVectorDeclaration implements SeqDeclaration {

  private final BitVectorDataType type;

  public final CExpression variable;

  private final BitVectorValueExpression initializer;

  public SeqBitVectorDeclaration(
      BitVectorDataType pType, CExpression pVariable, BitVectorValueExpression pInitializer) {

    type = pType;
    variable = pVariable;
    initializer = pInitializer;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
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
