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
package org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;

/**
 * An expression used to describe one side of a {@link Constraint}.
 */
public interface ConstraintExpression {

  /**
   * Accepts the given {@link ConstraintExpressionVisitor}.
   *
   * @param pVisitor the visitor to accept
   * @param <VisitorReturnT> the return type of the visitor's specific <code>visit</code> method
   * @return the value returned by the visitor's <code>visit</code> method
   */
  <VisitorReturnT> VisitorReturnT accept(ConstraintExpressionVisitor<VisitorReturnT> pVisitor);

  /**
   * Returns the expression type of this <code>ConstraintExpression</code>.
   *
   * @return the expression type of this <code>ConstraintExpression</code>
   */
  Type getExpressionType();

  /**
   * Returns a copy of this <code>ConstraintExpression</code> object with the given expression type.
   *
   * @param pType the expression type of the returned object
   */
  ConstraintExpression copyWithExpressionType(Type pType);
}
