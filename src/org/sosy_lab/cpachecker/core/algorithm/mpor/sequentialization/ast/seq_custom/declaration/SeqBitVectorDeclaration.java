// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.declaration;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqBitVectorDeclaration implements SeqDeclaration {

  private final BitVectorDataType type;

  private final CExpression variable;

  private final BitVectorExpression initializer;

  public SeqBitVectorDeclaration(
      BitVectorDataType pType, CExpression pVariable, BitVectorExpression pInitializer) {

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
