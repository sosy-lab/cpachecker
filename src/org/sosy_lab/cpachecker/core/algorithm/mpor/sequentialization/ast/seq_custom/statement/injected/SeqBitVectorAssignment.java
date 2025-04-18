// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqBitVectorAssignment implements SeqInjectedStatement {
  private final CIdExpression variable;
  private final SeqBitVector value;

  public SeqBitVectorAssignment(CIdExpression pVariable, SeqBitVector pValue) {
    variable = pVariable;
    value = pValue;
  }

  @Override
  public boolean priorCriticalSection() {
    return false;
  }

  @Override
  public Optional<CIdExpression> getIdExpression() {
    return Optional.of(variable);
  }

  @Override
  public String toASTString() {
    return variable.toASTString()
        + SeqSyntax.SPACE
        + SeqSyntax.EQUALS
        + SeqSyntax.SPACE
        + value.toASTString()
        + SeqSyntax.SEMICOLON;
  }
}
