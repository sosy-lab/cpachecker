// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

/**
 * This expression represents an array.length expression.
 *
 * <p>Example:
 *
 * <pre>
 *    int[] a;
 *    int b = a.length;
 *  </pre>
 */
public abstract sealed class JArrayLengthExpression extends AbstractExpression
    implements JExpression {

  @Serial private static final long serialVersionUID = 7278006181009822118L;

  private JArrayLengthExpression(FileLocation pFileLocation) {
    super(pFileLocation, JSimpleType.INT);
  }

  public static JArrayLengthExpression getInstance(JExpression pQualifier, FileLocation pLocation) {
    if (pQualifier instanceof JArraySubscriptExpression jArraySubscriptExpression) {
      return new JSubArrayLengthExpression(pLocation, jArraySubscriptExpression);

    } else if (pQualifier instanceof JIdExpression jIdExpression) {
      return new JTopArrayLengthExpression(pLocation, jIdExpression);

    } else {
      throw new AssertionError("Unexpected expression " + pQualifier);
    }
  }

  public abstract JExpression getQualifier();

  @Override
  public JSimpleType getExpressionType() {
    return (JSimpleType) super.getExpressionType();
  }

  @Override
  public <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  private static final class JSubArrayLengthExpression extends JArrayLengthExpression {
    @Serial private static final long serialVersionUID = 7488687702133599086L;
    private final JArraySubscriptExpression qualifier;

    JSubArrayLengthExpression(FileLocation pFileLocation, JArraySubscriptExpression pQualifier) {
      super(pFileLocation);
      qualifier = pQualifier;
    }

    @Override
    public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
      return toASTString();
    }

    @Override
    public String toASTString() {
      return qualifier + ".length";
    }

    @Override
    public JArraySubscriptExpression getQualifier() {
      return qualifier;
    }
  }

  private static final class JTopArrayLengthExpression extends JArrayLengthExpression {

    @Serial private static final long serialVersionUID = -2662310110400103416L;
    private final JIdExpression qualifier;

    private JTopArrayLengthExpression(FileLocation pFileLocation, JIdExpression pQualifier) {
      super(pFileLocation);
      qualifier = pQualifier;
    }

    @Override
    public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
      return toASTString();
    }

    @Override
    public String toASTString() {
      return qualifier + ".length";
    }

    @Override
    public JIdExpression getQualifier() {
      return qualifier;
    }
  }
}
