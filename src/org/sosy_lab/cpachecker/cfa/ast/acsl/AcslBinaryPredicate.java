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

public final class AcslBinaryPredicate extends ABinaryExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = 7019956121956900L;

  public AcslBinaryPredicate(
      FileLocation pFileLocation,
      AcslPredicate pOperand1,
      AcslPredicate pOperand2,
      AcslBinaryPredicateOperator pOperator) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN, pOperand1, pOperand2, pOperator);
    checkNotNull(pFileLocation);
    checkNotNull(pOperand1);
    checkNotNull(pOperand2);
    checkNotNull(pOperator);
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public enum AcslBinaryPredicateOperator implements ABinaryOperator, AcslAstNode {
    IMPLICATION("==>"),
    EQUIVALENT("<==>"),
    AND("&&"),
    OR("||"),
    EQUALS("=="),
    NOT_EQUALS("!="),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    ;

    @Serial private static final long serialVersionUID = 701123361956900L;

    private final String operator;
    private final FileLocation fileLocation;

    AcslBinaryPredicateOperator(String pOperator) {
      operator = pOperator;
      fileLocation = FileLocation.DUMMY;
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
    public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
      return v.visit(this);
    }

    @Override
    public FileLocation getFileLocation() {
      return fileLocation;
    }

    @Override
    public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
      return toString();
    }

    @Override
    public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
      return toString();
    }
  }
}
