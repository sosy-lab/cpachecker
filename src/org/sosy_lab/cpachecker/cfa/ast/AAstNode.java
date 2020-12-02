// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;

@SuppressWarnings("serial") // we cannot set a UID for an interface
public interface AAstNode extends Serializable {

  FileLocation getFileLocation();

  /**
   * Constructs a String representation of the AST represented by this node. Depending on the
   * parameter value different representations for local variables are used. Typically, you want to
   * call this method with a fixed value for the parameter. In these cases, we highly recommend to
   * either use {@link #toASTString()} (fixed parameter value false) or {@link
   * #toQualifiedASTString()} (fixed parameter value true).
   *
   * @param pQualified - if true use qualified variable names, i.e., add prefix functionname__ to
   *     local variable names, where functionname is the name of the function that declared the
   *     local variable
   * @return AST string either using qualified names or pure names for local variables
   */
  String toASTString(boolean pQualified);

  String toParenthesizedASTString(boolean pQualified);

  default String toASTString() {
    return toASTString(false);
  }

  default String toParenthesizedASTString() {
    return toParenthesizedASTString(false);
  }

  default String toQualifiedASTString() {
    return toASTString(true);
  }

  /**
   * Accept methods for visitors that works with AST nodes of all languages. It requires a visitor
   * that implements the respective visitor interfaces for all languages. If you can, do not call
   * this method but one of the normal "accept" methods.
   *
   * @param v The visitor.
   * @return Returns the object returned by the visit method.
   */
  <
          R,
          R1 extends R,
          R2 extends R,
          X1 extends Exception,
          X2 extends Exception,
          V extends CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2>>
      R accept_(V v) throws X1, X2;
}
