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
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression.AUnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslUnaryTerm extends AUnaryExpression implements AcslTerm {

  @Serial private static final long serialVersionUID = 8458828043123590886L;

  public AcslUnaryTerm(
      FileLocation pFileLocation,
      AcslType pType,
      AcslTerm pOperand,
      AcslUnaryTermOperator pOperator) {
    super(pFileLocation, pType, pOperand, pOperator);
  }

  @Override
  public AcslTerm getOperand() {
    return (AcslTerm) super.getOperand();
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public AcslType getExpressionType() {
    return (AcslType) super.getExpressionType();
  }

  @Override
  public AcslUnaryTermOperator getOperator() {
    return (AcslUnaryTermOperator) super.getOperator();
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public enum AcslUnaryTermOperator implements AUnaryOperator, AcslAstNode {
    SIZEOF("sizeof"),
    PLUS("+"),
    MINUS("-"),
    POINTER_DEREFERENCE("*"),
    ADDRESS_OF("&"),
    NEGATION("!"),
    ;

    private final String operator;
    private final FileLocation fileLocation;

    AcslUnaryTermOperator(String pOperator) {
      operator = pOperator;
      fileLocation = FileLocation.DUMMY;
    }

    public static AcslUnaryTermOperator of(String pOperator) {
      for (AcslUnaryTermOperator op : values()) {
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
