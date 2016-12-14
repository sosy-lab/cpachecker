/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

/**
 * This expression represents an array.length expression.
 *
 * <p>Example:
 *  <pre>
 *    int[] a;
 *    int b = a.length;
 *  </pre>
 * </p>
 */
public abstract class JArrayLengthExpression extends AbstractExpression implements JExpression, JAstNode, JRightHandSide {

  private JArrayLengthExpression(FileLocation pFileLocation) {
    super(pFileLocation, JSimpleType.getInt());
  }

  public static JArrayLengthExpression getInstance(JExpression pQualifier, FileLocation pLocation) {
    if (pQualifier instanceof JArraySubscriptExpression) {
      return new JSubArrayLengthExpression(pLocation, (JArraySubscriptExpression) pQualifier);

    } else if (pQualifier instanceof JIdExpression) {
      return new JTopArrayLengthExpression(pLocation, (JIdExpression) pQualifier);

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

  private static class JSubArrayLengthExpression extends JArrayLengthExpression {
    private final JArraySubscriptExpression qualifier;

    public JSubArrayLengthExpression(FileLocation pFileLocation, JArraySubscriptExpression pQualifier) {
      super(pFileLocation);
      qualifier = pQualifier;
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

  private static class JTopArrayLengthExpression extends JArrayLengthExpression {

    private final JIdExpression qualifier;

    private JTopArrayLengthExpression(FileLocation pFileLocation, JIdExpression pQualifier) {
      super(pFileLocation);
      qualifier = pQualifier;
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
