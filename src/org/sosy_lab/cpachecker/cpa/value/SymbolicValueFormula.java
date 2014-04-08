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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
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
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((root == null) ? 0 : root.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SymbolicValueFormula other = (SymbolicValueFormula) obj;
    if (root == null) {
      if (other.root != null) {
        return false;
      }
    } else if (!root.equals(other.root)) {
      return false;
    }
    return true;
  }

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
  public Value simplify(LogManagerWithoutDuplicates logger) {
    ExpressionBase simplifiedTree = root;

    // Only call the external simplifier if it's actually a complex
    // expression.
    if(root instanceof BinaryExpression) {
      if(isIntegerAddMultiplyOnly()) {
        simplifiedTree = ExternalSimplifier.simplify(simplifiedTree, logger);
      }
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

    /**
     * @return A list of all symbolic values contained in the sub-tree of this expression.
     */
    public Set<SymbolicValue> getSymbolicValues();

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
       *
       * @param key a char representing the operator, e.g. '+'
       * @return the corresponding <code>BinaryOperator</code> object
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

    @Override
    public Set<SymbolicValue> getSymbolicValues() {
      Set<SymbolicValue> leftHand = lVal.getSymbolicValues();
      Set<SymbolicValue> rightHand = rVal.getSymbolicValues();

      // It's okay to change leftHand rather than creating a copy,
      // since leftHand was only created for local use anyway.
      leftHand.addAll(rightHand);
      return leftHand;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((lVal == null) ? 0 : lVal.hashCode());
      result = prime * result + ((op == null) ? 0 : op.hashCode());
      result = prime * result + ((rVal == null) ? 0 : rVal.hashCode());
      result = prime * result + ((resultType == null) ? 0 : resultType.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      BinaryExpression other = (BinaryExpression) obj;
      if (lVal == null) {
        if (other.lVal != null) {
          return false;
        }
      } else if (!lVal.equals(other.lVal)) {
        return false;
      }
      if (op != other.op) {
        return false;
      }
      if (rVal == null) {
        if (other.rVal != null) {
          return false;
        }
      } else if (!rVal.equals(other.rVal)) {
        return false;
      }
      if (resultType == null) {
        if (other.resultType != null) {
          return false;
        }
      } else if (!resultType.equals(other.resultType)) {
        return false;
      }
      return true;
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

    @Override
    public Set<SymbolicValue> getSymbolicValues() {
      Set<SymbolicValue> rval = new HashSet<>();
      rval.add(this);
      return rval;
    }
  }

  public static class ConstantValue implements ExpressionBase {
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ConstantValue other = (ConstantValue) obj;
      if (value == null) {
        if (other.value != null) {
          return false;
        }
      } else if (!value.equals(other.value)) {
        return false;
      }
      return true;
    }

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

    @Override
    public Set<SymbolicValue> getSymbolicValues() {
      return new HashSet<>();
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
   * occur, returns an unmodified version of `this`.
   *
   * @param symbol
   * @param replacement
   * @param logger logging
   * @return The simplified formula with the given symbol replaced.
   */
  public Value replaceSymbolWith(SymbolicValue pSymbol, Value pReplacement, LogManagerWithoutDuplicates logger) {
    ConstantValue replacement = new ConstantValue(pReplacement);
    SymbolicValueFormula rval = new SymbolicValueFormula(root.replaceSymbolWith(pSymbol, replacement));
    if(!rval.root.equals(root)) {
      // Only simplify if we actually managed to replace something.
      return rval.simplify(logger);
    }
    return this;
  }

  /**
   * Check if there's only a single symbolic value in this formula. If so, try
   * to solve the formula for that variable.
   *
   * @param truthValue If false, this formula is assumed to be false, otherwise
   *        this formula is assumed to be true.
   * @param logger logging
   * @return A pair of the single symbolic value that was found, and the value
   *         it must have according to this formula. null if no such pair exists.
   */
  public Pair<SymbolicValue, Value> inferAssignment(boolean truthValue, LogManagerWithoutDuplicates logger) {
    Set<SymbolicValue> symbolicValues = root.getSymbolicValues();

    ExpressionBase root = this.root;

    // Less or more than a single symbolic value, impossible to infer anything.
    if(symbolicValues.size() != 1) {
      return null;
    }
    SymbolicValue valueToSolveFor = symbolicValues.iterator().next();

    // We want an == to solve, not an !=, but with truthValue == false,
    // an != comes out as an ==
    if(root instanceof BinaryExpression) {
      BinaryExpression rootBinaryExpression = (BinaryExpression) root;
      String operator = rootBinaryExpression.getOperator().op;

      if(operator.equals("!=") && !truthValue) {
        // If we have != and truthValue is false, convert to == instead
        root = new BinaryExpression(rootBinaryExpression.getOperand1(),
            rootBinaryExpression.getOperand2(),
            BinaryExpression.BinaryOperator.EQUALS,
            rootBinaryExpression.getCalculationType(),
            rootBinaryExpression.getResultType());
        operator = "==";
        truthValue = true;
      }

      if(operator.equals("==") && truthValue) {
        // If the left-hand or right-hand side already are the variable itself, we don't need
        // to do expensive solving.
        if(rootBinaryExpression.lVal instanceof SymbolicValue && rootBinaryExpression.rVal instanceof ConstantValue) {
          SymbolicValue symbol = (SymbolicValue) rootBinaryExpression.lVal;
          ConstantValue value = (ConstantValue) rootBinaryExpression.rVal;
          return Pair.of(symbol, value.getValue());
        } else if(rootBinaryExpression.rVal instanceof SymbolicValue && rootBinaryExpression.lVal instanceof ConstantValue) {
          SymbolicValue symbol = (SymbolicValue) rootBinaryExpression.rVal;
          ConstantValue value = (ConstantValue) rootBinaryExpression.lVal;
          return Pair.of(symbol, value.getValue());
        }

        // Can only solve anything if we have an == at the top level.
        Value result = ExternalSimplifier.solve(valueToSolveFor, root, logger);
        if(result == null) {
          return null;
        } else {
          return Pair.of(valueToSolveFor, result);
        }
      }
    }

    return null;
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