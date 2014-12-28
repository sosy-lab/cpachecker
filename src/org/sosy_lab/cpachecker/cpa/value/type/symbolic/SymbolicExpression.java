/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value.type.symbolic;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions.ConstraintExpression;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.SymbolicValue;

/**
 * Description of a variable's symbolic value in form of an expression. This is needed for more
 * complex cases with non-deterministic variable values.
 *
 * <p>Example:
 * <pre>
 *   int a = nondet_int();
 *   int b = a;
 *
 *   a = a + 5;
 *   if (2b + 5 != a + b) {
 * ERROR:
 *     return -1;
 *   }
 * </pre>
 * </p>
 */
public class SymbolicExpression implements SymbolicValue {

  private ConstraintExpression expression;

  /**
   * Create a new <code>SymbolicExpression</code> instance with the given expression.
   * The given expression is not cloned, but directly used.
   *
   * @param pExpression the expression describing the new <code>SymbolicExpression</code> instance
   */
  protected SymbolicExpression(ConstraintExpression pExpression) {
    expression = pExpression;
  }

  public ConstraintExpression getExpression() {
    return expression;
  }

  public <T> T accept(SymbolicValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return expression != null ? expression.hashCode() : 0;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof SymbolicExpression && ((SymbolicExpression) other).expression.equals(expression);
  }

  @Override
  public String toString() {
    return "SymbolicFormula [" + expression.toString() + "]";
  }

  @Override
  public boolean isNumericValue() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public boolean isExplicitlyKnown() {
    return false;
  }

  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  @Override
  public Long asLong(CType type) {
    return null;
  }
}
