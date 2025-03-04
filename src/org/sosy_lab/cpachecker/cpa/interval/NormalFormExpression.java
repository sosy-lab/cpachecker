// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import java.util.Set;
import java.util.function.BiPredicate;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/**
 * An expression in the normal form v + k. Allows for easy syntactical comparison of expressions.
 * For more details see: Patrick Cousot, Radhia Cousot, and Francesco Logozzo. 2011. A parametric
 * segmentation functor for fully automatic and scalable array content analysis. SIGPLAN Not. 46, 1
 * (January 2011), 105–118. <a href="https://doi.org/10.1145/1925844.1926399">
 * https://doi.org/10.1145/1925844.1926399</a>
 */
public class NormalFormExpression {
  private final CIdExpression variable;
  private final long constant;

  public NormalFormExpression(CIdExpression pVariable, long pConstant) {
    variable = pVariable;
    constant = pConstant;
  }

  public NormalFormExpression(long pConstant) {
    variable = null;
    constant = pConstant;
  }

  public NormalFormExpression(CIdExpression pVariable) {
    variable = pVariable;
    constant = 0;
  }

  public boolean isSyntacticallyLessThanOrEqualTo(NormalFormExpression other) {
    if (variable == null) {
      if (other.variable != null) {
        return false;
      }
    } else if (!variable.equals(other.variable)) {
      return false;
    }
    return constant <= other.constant;
  }

  public boolean isSyntacticallyGreaterThanOrEqualTo(NormalFormExpression other) {
    if (variable == null) {
      if (other.variable != null) {
        return false;
      }
    } else if (!variable.equals(other.variable)) {
      return false;
    }
    return constant >= other.constant;
  }

  public static boolean anyInSets(
      Set<NormalFormExpression> set,
      Set<NormalFormExpression> other,
      BiPredicate<NormalFormExpression, NormalFormExpression> predicate) {
    for (NormalFormExpression a : set) {
      for (NormalFormExpression b : other) {
        if (predicate.test(a, b)) {
          return true;
        }
      }
    }
    return false;
  }

  public NormalFormExpression add(Long add) {
    return new NormalFormExpression(variable, constant + add);
  }

  @Override
  public String toString() {
    if (variable == null) {
      return Long.toString(constant);
    }
    StringBuilder sb = new StringBuilder();
    sb.append(variable.getDeclaration().getQualifiedName());

    if (constant == 0) {
      return sb.toString();
    }
    if (constant > 0) {
      sb.append("+");
    }
    sb.append(constant);
    return sb.toString();
  }
}
