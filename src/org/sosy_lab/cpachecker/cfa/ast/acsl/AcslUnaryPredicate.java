// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslUnaryPredicate extends AUnaryExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = -6073836890768425L;

  public AcslUnaryPredicate(
      FileLocation pFileLocation, AcslPredicate pOperand, AcslUnaryExpressionOperator pOperator) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN, pOperand, pOperator);
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
  public int hashCode() {
    final int prime = 37;
    int result = 2;
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslUnaryPredicate && super.equals(obj);
  }

  public enum AcslUnaryExpressionOperator implements AUnaryOperator, AcslAstNode {
    NEGATION("!"),
    ;

    private final String operator;
    private final FileLocation fileLocation;

    AcslUnaryExpressionOperator(String pOperator) {
      operator = pOperator;
      fileLocation = FileLocation.DUMMY;
    }

    public static AcslUnaryExpressionOperator of(String pOperator) {
      for (AcslUnaryExpressionOperator op : values()) {
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
