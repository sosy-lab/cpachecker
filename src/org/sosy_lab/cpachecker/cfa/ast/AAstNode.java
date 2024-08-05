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

public interface AAstNode extends Serializable {

  public static enum AAstNodeRepresentation {
    DEFAULT, // Do not use qualified names for variables and do not use original names for variables
    QUALIFIED, // use qualified names for variables
    ORIGINAL_NAMES // use original names i.e. for the original program for variables
  }

  FileLocation getFileLocation();

  /**
   * Constructs a String representation of the AST represented by this node. Depending on the
   * parameter value different representations for local variables are used. Typically, you want to
   * call this method with a fixed value for the parameter. In these cases, we highly recommend to
   * either use {@link #toASTString()} (fixed parameter value false) or {@link
   * #toQualifiedASTString()} (fixed parameter value true).
   *
   * @param pAAstNodeRepresentation the method with which to represent variables
   * @return AST string either using qualified names or pure names for local variables
   */
  String toASTString(AAstNodeRepresentation pAAstNodeRepresentation);

  String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation);

  default String toASTString() {
    return toASTString(AAstNodeRepresentation.DEFAULT);
  }

  default String toParenthesizedASTString() {
    return toParenthesizedASTString(AAstNodeRepresentation.DEFAULT);
  }

  default String toQualifiedASTString() {
    return toASTString(AAstNodeRepresentation.QUALIFIED);
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
