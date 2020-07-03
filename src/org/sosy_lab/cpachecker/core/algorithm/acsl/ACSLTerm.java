package org.sosy_lab.cpachecker.core.algorithm.acsl;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public interface ACSLTerm {
  /**
   * Returns a copy of the term that has the same value as the original but is a valid C expression.
   */
  ACSLTerm toPureC();

  CExpression accept(ACSLToCExpressionVisitor visitor) throws UnrecognizedCodeException;
}
