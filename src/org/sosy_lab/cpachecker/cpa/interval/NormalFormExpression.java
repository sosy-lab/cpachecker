// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/**
 * An expression in the normal form v + k. Allows for easy syntactical comparison of expressions.
 * For more details see: Patrick Cousot, Radhia Cousot, and Francesco Logozzo. 2011. A parametric
 * segmentation functor for fully automatic and scalable array content analysis. SIGPLAN Not. 46, 1
 * (January 2011), 105–118. <a href="https://doi.org/10.1145/1925844.1926399">
 *   https://doi.org/10.1145/1925844.1926399</a>
 */
public class NormalFormExpression {
  private final CIdExpression variable;
  private final long constant;

  public NormalFormExpression(CIdExpression pVariable, long pConstant) {
    variable = pVariable;
    constant = pConstant;
  }

  public boolean isSyntacticallyLessThanOrEqualTo(NormalFormExpression other) {
    if (!variable.equals(other.variable)) {
      return false;
    }
    return constant <= other.constant;
  }

  public boolean isSyntacticallyGreaterThanOrEqualTo(NormalFormExpression other) {
    if (!variable.equals(other.variable)) {
      return false;
    }
    return constant >= other.constant;
  }

}
