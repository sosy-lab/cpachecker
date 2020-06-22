package org.sosy_lab.cpachecker.core.algorithm.acsl;

public interface ACSLTerm {
  /**
   * Returns a copy of the term that has the same value as the original but is a valid C expression.
   */
  ACSLTerm toPureC();
}
