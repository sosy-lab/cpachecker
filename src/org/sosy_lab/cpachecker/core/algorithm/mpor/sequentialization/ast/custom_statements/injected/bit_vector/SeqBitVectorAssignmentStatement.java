// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqBitVectorAssignmentStatement implements SeqInjectedBitVectorStatement {

  public final CIdExpression variable;

  public final BitVectorValueExpression value;

  public SeqBitVectorAssignmentStatement(CIdExpression pVariable, BitVectorValueExpression pValue) {
    variable = pVariable;
    value = pValue;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return variable.toASTString()
        + SeqSyntax.SPACE
        + SeqSyntax.EQUALS
        + SeqSyntax.SPACE
        + value.toASTString()
        + SeqSyntax.SEMICOLON;
  }
}
