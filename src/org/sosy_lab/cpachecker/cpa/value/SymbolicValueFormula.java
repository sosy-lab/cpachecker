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
package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;


/**
 * This class extends the ExplicitCPA with the ability to keep track of
 * "symbolic values", and formulas which contain these.
 *
 * Example values:
 * foo() + 2
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
public class SymbolicValueFormula implements Value {
  /** root of the expression tree **/
  ExpressionBase root;

  public SymbolicValueFormula(ExpressionBase root) {
    this.root = root;
  }

  /**
   * Returns a version of this symboli{c value that was simplified
   * as much as possible.
   */
  public SymbolicValueFormula simplify() {
    ExpressionBase simplifiedTree = recursiveSimplify(root);
    return new SymbolicValueFormula(simplifiedTree);
  }

  // classes used to represent the expression trees for the symbols
  /**
   * Base class for elements of a symbolic expression, e.g. "X + X"
   */
  public interface ExpressionBase {

  }

  public static class BinaryExpression implements ExpressionBase {
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

      /**
       * Get the binary operator with a specific string, or null if none.
       */
      public static BinaryOperator fromString(String key) {
        for(BinaryOperator iter : BinaryOperator.values()) {
          if(iter.op == key) {
            return iter;
          }
        }

        return null;
      }
    }

    private ExpressionBase lVal, rVal;
    private CType resultType;
    private CType calculationType;

    public CType getResultType() {
      return resultType;
    }

    public CType getCalculationType() {
      return calculationType;
    }

    public ExpressionBase getOperand1() {
      return lVal;
    }

    public ExpressionBase getOperand2() {
      return rVal;
    }

    public BinaryOperator getOperator() {
      return op;
    }

    private BinaryOperator op;

    public BinaryExpression(ExpressionBase lVal, ExpressionBase rVal, BinaryOperator op, CType resultType, CType calculationType) {
      this.lVal = lVal;
      this.rVal = rVal;
      this.op = op;
      this.resultType = resultType;
      this.calculationType = calculationType;
    }

    @Override
    public String toString() {
      return lVal.toString() + " " + op.toString() + " " + rVal.toString();
    }
  }

  /**
   * Represents an undetermined value.
   */
  public static class SymbolicValue implements ExpressionBase {
    /**
     * The location in memory this SymbolicValue occupies.
     *
     * TODO: think about potentially representing values that aren't
     *       anywhere in memory, e.g. `int x = 2 + nondet();`, now `nondet()`
     *       has no memory location, but we'd still like to treat it as
     *       symbolic value
     */
    private MemoryLocation location;

    public SymbolicValue(MemoryLocation location) {
      this.location = location;
    }

    @Override
    public boolean equals(Object other) {
      if(other instanceof SymbolicValue) {
        return location.equals(((SymbolicValue) other).location);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return location.hashCode();
    }

    @Override
    public String toString() {
      return location.getAsSimpleString();
    }
  }

  public static class ConstantValue implements ExpressionBase {
    private Value value;

    public Value getValue() {
      return value;
    }

    public ConstantValue(Value value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value.toString();
    }
  }

  public static ExpressionBase expressionFromExplicitValue(Value value) {
    if(value instanceof SymbolicValueFormula) {
      return ((SymbolicValueFormula) value).root;
    } else {
      return new ConstantValue(value);
    }
  }

  /**
   * Simplifies a CExpression to an equivalent expression
   * using recursion.
   *
   * Example: "X - X" gets simplified to "0"
   */
  private static ExpressionBase recursiveSimplify(ExpressionBase expression) {
    if(expression instanceof BinaryExpression) {
      BinaryExpression binaryExpression = (BinaryExpression) expression;

      switch(binaryExpression.getOperator()) {
      case MINUS:
        CSimpleType type = AbstractExpressionValueVisitor.getArithmeticType(binaryExpression.getResultType());
        if(type != null) {
          // TODO: recursive equivalence check in a separate function
          boolean isEqual = false;
          if(binaryExpression.getOperand1() instanceof SymbolicValue
              && binaryExpression.getOperand2() instanceof SymbolicValue) {
            SymbolicValue leftHand = (SymbolicValue) binaryExpression.getOperand1();
            SymbolicValue rightHand = (SymbolicValue) binaryExpression.getOperand2();
            if(leftHand.equals(rightHand)) {
              isEqual = true;
            }
          }

          if(isEqual) {
            return new ConstantValue(new NumericValue(0));
          }
        }
      }

      ExpressionBase newLeftHand = recursiveSimplify(binaryExpression.getOperand1());
      ExpressionBase newRightHand = recursiveSimplify(binaryExpression.getOperand2());
      return new BinaryExpression(newLeftHand, newRightHand, binaryExpression.getOperator(),
          binaryExpression.getResultType(), binaryExpression.getCalculationType());
    }
    // If we couldn't simplify it, return as-is.
    return expression;
  }

  @Override
  public boolean isNumericValue() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return true;
  }

  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  @Override
  public Long asLong(CType pType) {
    checkNotNull(pType);
    return null;
  }

  @Override
  public String toString() {
    return "SymbolicValueFormula {"+root.toString()+"}";
  }

}