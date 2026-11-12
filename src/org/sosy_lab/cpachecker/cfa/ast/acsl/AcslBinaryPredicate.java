// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;

public final class AcslBinaryPredicate extends ABinaryExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = 2340946883565651164L;

  public AcslBinaryPredicate(
      FileLocation pFileLocation,
      AcslPredicate pOperand1,
      AcslPredicate pOperand2,
      AcslBinaryPredicateOperator pOperator) {
    super(
        checkNotNull(pFileLocation),
        AcslBuiltinLogicType.BOOLEAN,
        checkNotNull(pOperand1),
        checkNotNull(pOperand2),
        checkNotNull(pOperator));
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public AcslPredicate getOperand1() {
    return (AcslPredicate) super.getOperand1();
  }

  @Override
  public AcslPredicate getOperand2() {
    return (AcslPredicate) super.getOperand2();
  }

  @Override
  public AcslBinaryPredicateOperator getOperator() {
    return (AcslBinaryPredicateOperator) super.getOperator();
  }

  public enum AcslBinaryPredicateOperator implements ABinaryOperator {
    IMPLICATION("==>"),
    EQUIVALENT("<==>"),
    AND("&&"),
    OR("||"),
    XOR("^^");

    private final String operator;

    AcslBinaryPredicateOperator(String pOperator) {
      operator = pOperator;
    }

    public static AcslBinaryPredicateOperator of(String pOperator) {
      for (AcslBinaryPredicateOperator op : values()) {
        if (op.operator.equals(pOperator)) {
          return op;
        }
      }
      throw new IllegalArgumentException("Unknown operator: " + pOperator);
    }

    @Override
    public String toString() {
      return operator;
    }

    /** Returns the string representation of this operator (e.g. "*", "+"). */
    @Override
    public String getOperator() {
      return operator;
    }

    @Override
    public boolean isLogicalOperator() {
      return true;
    }
  }
}
