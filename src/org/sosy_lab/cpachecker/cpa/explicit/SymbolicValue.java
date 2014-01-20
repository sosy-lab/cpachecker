/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

/**
 * Oh, the irony of having this in the package called "ExplicitCPA".
 *
 * This class extends the ExplicitCPA with the ability to keep track of
 * "symbolic values", which means formulae with variables in them.
 *
 * Example values:
 * foo + 2
 * myarray[8] * 10
 * bar * someVariable
 *
 * Eventually, it should also become possible to simplify these formulae,
 * e.g. X + X becomes 0
 *
 * However, these simplifications are more restricted than those in purely
 * mathematical formulae. For instance, consider "X / 10 * 10". Using
 * integer arithmetics, this can not be simplified to "X".
 */
public class SymbolicValue {
  /** root of the expression tree **/
  CExpression root;

  /**
   * The C variable that this symbolic value represents.
   */
  private CIdExpression container;

  public SymbolicValue(CExpression root) {
    this.root = root;
  }

  /**
   * Returns a version of this symboli{c value that was simplified
   * as much as possible.
   */
  public SymbolicValue simplify() {
    CExpression simplifiedTree = recursiveSimplify(root);
    return new SymbolicValue(simplifiedTree);
  }

  // classes used to represent the expression trees for the symbols
  /**
   * Base class for elements of a symbolic expression, e.g. "X + X"
   */
  public interface ExpressionBase {

  }

  public static class BinaryExpression {
    public static enum BinaryOperator {
      MULTIPLY      ("*"),
      DIVIDE        ("/"),
      MODULO        ("%"),
      PLUS          ("+"),
      MINUS         ("-"),
      SHIFT_LEFT    ("<<"),
      SHIFT_RIGHT   (">>"),
      LESS_THAN     ("<"),
      GREATER_THAN  (">"),
      LESS_EQUAL    ("<="),
      GREATER_EQUAL (">="),
      BINARY_AND    ("&"),
      BINARY_XOR    ("^"),
      BINARY_OR     ("|"),
      EQUALS        ("=="),
      NOT_EQUALS    ("!="),
      ;

      private final String op;

      private BinaryOperator(String pOp) {
        op = pOp;
      }
  }

  // TODO: this would probably be better by extending
  // DefaultCExpressionVisitor<CExpression, UnrecognizedCCodeException>
  // but right now I can't figure out how to use that one

  /**
   * Simplifies a CExpression to an equivalent expression
   * using recursion.
   *
   * Example: "X - X" gets simplified to "0"
   */
  public CExpression recursiveSimplify(CExpression expression) {
    if(expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) expression;

      switch(binaryExpression.getOperator()) {
      case MINUS:
        if(binaryExpression.getExpressionType() instanceof CSimpleType) {
          // TODO: recursive equivalence check in a separate function
          boolean isEqual = false;
          if(binaryExpression.getOperand1() instanceof CIdExpression
              && binaryExpression.getOperand2() instanceof CIdExpression) {
            CIdExpression leftHand = (CIdExpression) binaryExpression.getOperand1();
            CIdExpression rightHand = (CIdExpression) binaryExpression.getOperand2();
            if(leftHand.getDeclaration().equals(rightHand.getDeclaration())) {
              isEqual = true;
            }
          }

          if(isEqual) {
            return new CIntegerLiteralExpression(null, CNumericTypes.INT, new BigInteger("0"));
          }
        }
      }

      CExpression newLeftHand = recursiveSimplify(binaryExpression.getOperand1());
      CExpression newRightHand = recursiveSimplify(binaryExpression.getOperand2());
      return new CBinaryExpression(null, binaryExpression.getExpressionType(), binaryExpression.getCalculationType(), newLeftHand, newRightHand, binaryExpression.getOperator());
    }
    // If we couldn't simplify it, return as-is.
    return expression;
  }

}