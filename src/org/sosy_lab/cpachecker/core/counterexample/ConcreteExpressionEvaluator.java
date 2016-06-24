/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.counterexample;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Implement this interface to use analysis specific expression evaluation when
 * calculating the error path.
 */
public interface ConcreteExpressionEvaluator {

  /**
   * Checks, if this specific expression should be evaluated with this class.
   * If it returns false, it will be evaluated based on the concrete expression evaluation
   * of the value analysis.
   *
   * @param exp expression to be checked.
   * @return True, if the expression should be evaluated with this class, false, if the expression
   * should be evaluated based on the expression evaluation of the value analysis.
   */
  public boolean shouldEvaluateExpressionWithThisEvaluator(AExpression exp);

  /**
   * Evaluate binary expression with given concrete operators.
   *
   * @param binExp expression to be evaluated.
   * @param op1 concrete value of op1
   * @param op2 concrete value of op2
   * @return result of binary expression evaluation with concrete value of op1 and op2
   */
  public Value evaluate(ABinaryExpression binExp, Value op1, Value op2);

  /**
   * Evaluate unary expression with given concrete operator.
   *
   * @param unaryExpression expression to be evaluated.
   * @param operand concrete value of operand
   * @return result of unary expression evaluation with concrete value of op1 and op2
   */
  public Value evaluate(AUnaryExpression unaryExpression, Value operand);

  /**
   * Evaluate casr expression with given concrete operator.
   *
   * @param castExpression expression to be evaluated
   * @param operand value of operand
   * @return result of a cast expression evaluation with concrete value of operand
   */
  public Value evaluate(ACastExpression castExpression, Value operand);
}
