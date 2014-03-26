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

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.simplifier.ExternalSimplifier;


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

  public ExpressionBase getRoot() {
    return root;
  }

  public SymbolicValueFormula(ExpressionBase root) {
    this.root = root;
  }

  /**
   * Returns a version of this symboli{c value that was simplified
   * as much as possible.
   */
  public Value simplify() {
    ExpressionBase simplifiedTree = root;
    if(isIntegerAddMultiplyOnly()) {
      simplifiedTree = ExternalSimplifier.simplify(simplifiedTree);
    }

    // If we actually know the value, return the known value.
    if(simplifiedTree instanceof ConstantValue) {
      return ((ConstantValue) simplifiedTree).getValue();
    }

    // Otherwise, return the formula.
    return new SymbolicValueFormula(simplifiedTree);
  }

  // classes used to represent the expression trees for the symbols
  /**
   * Base class for elements of a symbolic expression, e.g. "X + X"
   */
  public interface ExpressionBase {
    public boolean isIntegerAddMultiplyOnly();
    public ExpressionBase replaceSymbolWith(SymbolicValue symbol, ConstantValue replacement);
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
          if(iter.op.equals(key)) {
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

    @Override
    public boolean isIntegerAddMultiplyOnly() {
      final List<String> allowedOps = new ArrayList<>();
      allowedOps.add("PLUS");
      allowedOps.add("MINUS");
      allowedOps.add("MULTIPLY");

      // If it's not +, - or * return false
      if(!allowedOps.contains(this.op.toString())) {
        return false;
      }

      CSimpleType arithmeticType =
          AbstractExpressionValueVisitor.getArithmeticType(resultType);

      return arithmeticType.getType() == CBasicType.INT &&
          lVal.isIntegerAddMultiplyOnly() && rVal.isIntegerAddMultiplyOnly();
    }

    @Override
    public ExpressionBase replaceSymbolWith(SymbolicValue pSymbol, ConstantValue pReplacement) {
      ExpressionBase leftHand = lVal.replaceSymbolWith(pSymbol, pReplacement);
      ExpressionBase rightHand = rVal.replaceSymbolWith(pSymbol, pReplacement);

      return new BinaryExpression(leftHand, rightHand, op, resultType, calculationType);
    }
  }

  /**
   * Represents an undetermined input to our program, e.g. `nondet()`.
   */
  public static class SymbolicValue implements ExpressionBase {
    private String displayName;

    // Don't overwrite the equals method, SymbolicValue's are supposed
    // to be unique, even values with the same display name can refer
    // to different data.

    public SymbolicValue(String name) {
      displayName = name;
    }

    @Override
    public boolean isIntegerAddMultiplyOnly() {
      // We don't know whether this is an integer, we must let
      // the higher level expression check.
      return true;
    }

    @Override
    public String toString() {
      return displayName;
    }

    @Override
    public ExpressionBase replaceSymbolWith(SymbolicValue pSymbol, ConstantValue pReplacement) {
      if(this.equals(pSymbol)) {
        return pReplacement;
      } else {
        return this;
      }
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

    @Override
    public boolean isIntegerAddMultiplyOnly() {
      // We don't know whether this is an integer, we must let
      // the higher level expression check.
      return true;
    }

    @Override
    public ExpressionBase replaceSymbolWith(SymbolicValue pSymbol, ConstantValue pReplacement) {
      // Constants are never replaced.
      return this;
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
   * Replaces the given symbolic value with a constant. If the given symbolic value does not
   * occur, returns `this` unmodified.
   *
   * @param symbol
   * @param replacement
   * @return
   */
  public Value replaceSymbolWith(SymbolicValue pSymbol, Value pReplacement) {
    ConstantValue replacement = new ConstantValue(pReplacement);
    return new SymbolicValueFormula(root.replaceSymbolWith(pSymbol, replacement)).simplify();
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
  public Long asLong(CType pType) {
    checkNotNull(pType);
    return null;
  }

  @Override
  public String toString() {
    return "SymbolicValueFormula {"+root.toString()+"}";
  }

  /**
   * Check if this formula consists entirely of integer arithmetic
   * with +, - and *
   *
   * If that's the case, shuffling around parts of the formula will
   * not change the result, so simplification is possible.
   *
   * @return true if this formula fulfills the above requirements, false otherwise
   */
  public boolean isIntegerAddMultiplyOnly() {
    return root.isIntegerAddMultiplyOnly();
  }

}